package com.brickgame.tetris.gl

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.util.Log
import com.brickgame.tetris.game.Game3DState
import com.brickgame.tetris.game.Tetris3DGame
import com.brickgame.tetris.ui.components.PieceMaterial
import com.google.ar.core.Anchor
import com.google.ar.core.Plane
import com.google.ar.core.Session
import com.google.ar.core.TrackingState
import java.util.concurrent.ConcurrentLinkedQueue
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.atan2
import kotlin.math.sqrt

/** What the AR view is doing, for the on-screen hints. */
enum class ArStatus { STARTING, SEARCHING, READY_TO_PLACE, PLACED, TRACKING_LOST, FAILED }

/**
 * AR renderer: camera image + the 3D well anchored on a real horizontal surface.
 *
 * The well is placed with a tap on a detected plane and keeps its real-world position; walking
 * around it changes the view naturally. Each board cell is [CELL_METERS] wide, so the 6×6 well is
 * about 18 cm across and 42 cm tall — table-sized.
 *
 * All ARCore calls happen on the GL thread; UI callbacks are posted by [ArBoardView].
 */
class ArBoardRenderer(
    context: Context,
    private val session: Session,
    private val onStatus: (ArStatus) -> Unit,
    /** Viewing direction relative to the well, in the orbit camera's terms (for D-pad mapping). */
    private val onViewAngle: (azimuthDeg: Float, elevationDeg: Float) -> Unit
) : GLSurfaceView.Renderer {

    companion object {
        private const val TAG = "ArBoardRenderer"
        const val CELL_METERS = 0.03f
    }

    private val board = BoardRenderer(context)
    private var background: ArBackground? = null

    private val taps = ConcurrentLinkedQueue<FloatArray>()
    @Volatile private var replaceRequested = false
    @Volatile var displayRotation = 0
    private var viewportChanged = false
    private var width = 1
    private var height = 1

    private var anchor: Anchor? = null
    private var lastStatus: ArStatus? = null
    private var lastAngleReport = 0L

    private val view = FloatArray(16)
    private val proj = FloatArray(16)
    private val viewProj = FloatArray(16)
    private val anchorMatrix = FloatArray(16)
    private val base = FloatArray(16)
    private val camLocal = FloatArray(3)

    fun updateState(state: Game3DState, material: PieceMaterial, ghost: Boolean, themeColor: Long) =
        board.updateState(state, material, ghost, themeColor)

    /** Screen tap (view pixels): place / move the well onto the surface under the finger. */
    fun queueTap(x: Float, y: Float) { taps.offer(floatArrayOf(x, y)) }

    /** Forget the current placement so the next tap places the well again. */
    fun requestReplace() { replaceRequested = true }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0f, 0f, 0f, 1f)
        board.initGl()
        background = ArBackground().also { session.setCameraTextureName(it.textureId) }
        report(ArStatus.SEARCHING)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        this.width = width; this.height = height
        GLES20.glViewport(0, 0, width, height)
        viewportChanged = true
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        val bg = background ?: return
        if (viewportChanged) {
            session.setDisplayGeometry(displayRotation, width, height)
            viewportChanged = false
        }
        val frame = try { session.update() } catch (e: Exception) {
            Log.e(TAG, "session.update failed", e); report(ArStatus.FAILED); return
        }
        bg.draw(frame)

        val camera = frame.camera
        if (camera.trackingState != TrackingState.TRACKING) {
            report(if (anchor == null) ArStatus.SEARCHING else ArStatus.TRACKING_LOST)
            return
        }

        if (replaceRequested) {
            anchor?.detach(); anchor = null; replaceRequested = false
        }

        // Taps: place (or move) the well on a horizontal surface
        while (true) {
            val tap = taps.poll() ?: break
            val hit = frame.hitTest(tap[0], tap[1]).firstOrNull { h ->
                val t = h.trackable
                t is Plane && t.type == Plane.Type.HORIZONTAL_UPWARD_FACING && t.isPoseInPolygon(h.hitPose)
            }
            if (hit != null) {
                anchor?.detach()
                anchor = hit.createAnchor()
            }
        }

        val a = anchor
        if (a == null) {
            val hasPlane = session.getAllTrackables(Plane::class.java).any {
                it.trackingState == TrackingState.TRACKING && it.type == Plane.Type.HORIZONTAL_UPWARD_FACING && it.subsumedBy == null
            }
            report(if (hasPlane) ArStatus.READY_TO_PLACE else ArStatus.SEARCHING)
            return
        }
        if (a.trackingState != TrackingState.TRACKING) { report(ArStatus.TRACKING_LOST); return }
        report(ArStatus.PLACED)

        // board → world: anchor pose × scale × centre the well's footprint on the anchor
        camera.getViewMatrix(view, 0)
        camera.getProjectionMatrix(proj, 0, 0.05f, 50f)
        Matrix.multiplyMM(viewProj, 0, proj, 0, view, 0)
        a.pose.toMatrix(anchorMatrix, 0)
        System.arraycopy(anchorMatrix, 0, base, 0, 16)
        Matrix.scaleM(base, 0, CELL_METERS, CELL_METERS, CELL_METERS)
        Matrix.translateM(base, 0, -Tetris3DGame.BOARD_W / 2f, 0f, -Tetris3DGame.BOARD_D / 2f)

        val camPose = camera.pose
        board.drawScene(viewProj, base, camPose.tx(), camPose.ty(), camPose.tz())

        // Where am I standing relative to the well? (anchor-local, metres) → orbit angles
        val now = System.currentTimeMillis()
        if (now - lastAngleReport > 100) {
            lastAngleReport = now
            val local = a.pose.inverse().transformPoint(floatArrayOf(camPose.tx(), camPose.ty(), camPose.tz()))
            camLocal[0] = local[0]; camLocal[1] = local[1]; camLocal[2] = local[2]
            val horiz = sqrt(camLocal[0] * camLocal[0] + camLocal[2] * camLocal[2])
            val az = Math.toDegrees(atan2(camLocal[0], camLocal[2]).toDouble()).toFloat()
            val el = Math.toDegrees(atan2(camLocal[1], horiz).toDouble()).toFloat()
            onViewAngle(az, el)
        }
    }

    private fun report(status: ArStatus) {
        if (status != lastStatus) { lastStatus = status; onStatus(status) }
    }

    /** Release ARCore objects held by the renderer (session is closed by its owner). */
    fun release() { anchor?.detach(); anchor = null }
}
