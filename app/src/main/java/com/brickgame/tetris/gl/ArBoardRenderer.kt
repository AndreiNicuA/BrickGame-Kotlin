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
import com.google.ar.core.Config
import com.google.ar.core.Plane
import com.google.ar.core.Session
import com.google.ar.core.TrackingState
import java.util.concurrent.ConcurrentLinkedQueue
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.sqrt

/** What the AR view is doing, for the on-screen hints. */
enum class ArStatus { STARTING, SEARCHING, READY_TO_PLACE, PLACED, TRACKING_LOST, FAILED }

/**
 * AR renderer: camera image + the 3D well anchored on a real horizontal surface.
 *
 * - Tap a detected surface to place the well; it keeps its real-world position.
 * - [cellMeters] sets the size (3 cm cubes = table-top, 20 cm cubes = a well you can stand in),
 *   [yawDegrees] turns it, [queueMove] slides it to the surface under a screen point.
 * - [screenToBoard] turns a screen point into a board cell, so pieces can be dragged by touch.
 *
 * Battery/heat: once the well is placed, surface detection is switched off (it is the most
 * expensive part of tracking) and only switched back on to move the well.
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
        const val MIN_CELL = 0.012f
        const val MAX_CELL = 0.25f
    }

    private val board = BoardRenderer(context)
    private var background: ArBackground? = null

    private val taps = ConcurrentLinkedQueue<FloatArray>()
    @Volatile private var moveTarget: FloatArray? = null
    @Volatile private var replaceRequested = false
    @Volatile var displayRotation = 0
    @Volatile var cellMeters = 0.03f
        set(v) { field = v.coerceIn(MIN_CELL, MAX_CELL) }
    @Volatile var yawDegrees = 0f
    /** Set by the view while a two-finger move is in progress (surface detection stays on). */
    @Volatile var gestureMoving = false
    /** True once the well stands somewhere (read by the touch handler). */
    @Volatile var isPlaced = false
        private set

    private var viewportChanged = false
    private var width = 1
    private var height = 1
    private var anchor: Anchor? = null
    private var lastStatus: ArStatus? = null
    private var lastAngleReport = 0L
    private var planeFinding = true

    private val view = FloatArray(16)
    private val proj = FloatArray(16)
    private val viewProj = FloatArray(16)
    private val anchorMatrix = FloatArray(16)
    private val base = FloatArray(16)
    private val boardMvp = FloatArray(16)

    // Snapshot for touch → board ray casts from the UI thread
    private val rayLock = Any()
    private val invBoardMvp = FloatArray(16)
    private var rayReady = false
    private var rayW = 1
    private var rayH = 1

    fun updateState(state: Game3DState, material: PieceMaterial, ghost: Boolean, themeColor: Long) =
        board.updateState(state, material, ghost, themeColor)

    /** Screen tap (view pixels): place the well onto the surface under the finger. */
    fun queueTap(x: Float, y: Float) { taps.offer(floatArrayOf(x, y)) }

    /** Slide the well to the surface under this screen point (two-finger drag). */
    fun queueMove(x: Float, y: Float) { moveTarget = floatArrayOf(x, y) }

    /** Forget the current placement so the next tap places the well again. */
    fun requestReplace() { replaceRequested = true }

    /**
     * Where a screen point hits the horizontal plane y = [boardY] inside the well, in board units
     * (x, z). Null if the well isn't placed or the ray misses the plane.
     */
    fun screenToBoard(x: Float, y: Float, boardY: Float): FloatArray? {
        val inv = FloatArray(16); val w: Int; val h: Int
        synchronized(rayLock) {
            if (!rayReady) return null
            System.arraycopy(invBoardMvp, 0, inv, 0, 16); w = rayW; h = rayH
        }
        val nx = 2f * x / w - 1f
        val ny = 1f - 2f * y / h
        val near = unproject(inv, nx, ny, -1f) ?: return null
        val far = unproject(inv, nx, ny, 1f) ?: return null
        val dy = far[1] - near[1]
        if (abs(dy) < 1e-6f) return null
        val t = (boardY - near[1]) / dy
        if (t < 0f) return null
        return floatArrayOf(near[0] + (far[0] - near[0]) * t, near[2] + (far[2] - near[2]) * t)
    }

    private fun unproject(inv: FloatArray, x: Float, y: Float, z: Float): FloatArray? {
        val out = FloatArray(4)
        Matrix.multiplyMV(out, 0, inv, 0, floatArrayOf(x, y, z, 1f), 0)
        if (abs(out[3]) < 1e-9f) return null
        return floatArrayOf(out[0] / out[3], out[1] / out[3], out[2] / out[3])
    }

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

    /** Surface detection on/off (it is the heaviest part of tracking). */
    private fun setPlaneFinding(on: Boolean) {
        if (planeFinding == on) return
        try {
            val cfg = session.config
            cfg.planeFindingMode = if (on) Config.PlaneFindingMode.HORIZONTAL else Config.PlaneFindingMode.DISABLED
            session.configure(cfg)
            planeFinding = on
        } catch (e: Exception) {
            Log.w(TAG, "Couldn't switch plane finding", e)
        }
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
            anchor?.detach(); anchor = null; isPlaced = false; replaceRequested = false
            setPlaneFinding(true)
        }

        // Taps: place the well on a horizontal surface
        while (true) {
            val tap = taps.poll() ?: break
            hitPlane(frame, tap[0], tap[1])?.let { placeAt(it) }
        }
        // Two-finger drag: slide the well (needs surfaces, so detection is on while moving)
        if (gestureMoving) setPlaneFinding(true)
        moveTarget?.let { m ->
            moveTarget = null
            hitPlane(frame, m[0], m[1])?.let { placeAt(it) }
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
        // Placed and not being moved: stop looking for surfaces to save power and heat
        if (!gestureMoving) setPlaneFinding(false)

        // board → world: anchor × turn × scale × centre the footprint on the anchor
        camera.getViewMatrix(view, 0)
        camera.getProjectionMatrix(proj, 0, 0.03f, 50f)
        Matrix.multiplyMM(viewProj, 0, proj, 0, view, 0)
        a.pose.toMatrix(anchorMatrix, 0)
        System.arraycopy(anchorMatrix, 0, base, 0, 16)
        Matrix.rotateM(base, 0, yawDegrees, 0f, 1f, 0f)
        val s = cellMeters
        Matrix.scaleM(base, 0, s, s, s)
        Matrix.translateM(base, 0, -Tetris3DGame.BOARD_W / 2f, 0f, -Tetris3DGame.BOARD_D / 2f)

        val camPose = camera.pose
        board.drawScene(viewProj, base, camPose.tx(), camPose.ty(), camPose.tz())

        // Keep a snapshot for touch ray casts
        Matrix.multiplyMM(boardMvp, 0, viewProj, 0, base, 0)
        synchronized(rayLock) {
            rayReady = Matrix.invertM(invBoardMvp, 0, boardMvp, 0)
            rayW = width; rayH = height
        }

        // Where am I standing relative to the well? → orbit angles (board space, so the turn counts)
        val now = System.currentTimeMillis()
        if (now - lastAngleReport > 100) {
            lastAngleReport = now
            val invBase = FloatArray(16)
            if (Matrix.invertM(invBase, 0, base, 0)) {
                val c = FloatArray(4)
                Matrix.multiplyMV(c, 0, invBase, 0, floatArrayOf(camPose.tx(), camPose.ty(), camPose.tz(), 1f), 0)
                // relative to the well's centre axis
                val lx = c[0] - Tetris3DGame.BOARD_W / 2f
                val lz = c[2] - Tetris3DGame.BOARD_D / 2f
                val horiz = sqrt(lx * lx + lz * lz)
                onViewAngle(
                    Math.toDegrees(atan2(lx, lz).toDouble()).toFloat(),
                    Math.toDegrees(atan2(c[1], horiz).toDouble()).toFloat()
                )
            }
        }
    }

    private fun hitPlane(frame: com.google.ar.core.Frame, x: Float, y: Float) =
        frame.hitTest(x, y).firstOrNull { h ->
            val t = h.trackable
            t is Plane && t.type == Plane.Type.HORIZONTAL_UPWARD_FACING && t.isPoseInPolygon(h.hitPose)
        }

    private fun placeAt(hit: com.google.ar.core.HitResult) {
        anchor?.detach()
        anchor = hit.createAnchor()
        isPlaced = true
    }

    private fun report(status: ArStatus) {
        if (status != lastStatus) { lastStatus = status; onStatus(status) }
    }

    /** Release ARCore objects held by the renderer (session is closed by its owner). */
    fun release() { anchor?.detach(); anchor = null; isPlaced = false }
}
