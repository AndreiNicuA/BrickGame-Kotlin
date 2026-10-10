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

/**
 * Play-area state for the UI: [drawing] while the player taps corners, [corners] so far,
 * [set] once finished, and [distance] = metres from the phone to the nearest edge
 * (negative = outside), null when no area is set or tracking is lost.
 */
/**
 * What the hand tracker sees, for the UI: all 21 landmarks in view pixels (x, y pairs),
 * whether they pinch, the raw pinch ratio (Hand setup) and the hold-to-place progress (0..1).
 */
class HandFrame(val pts: FloatArray, val pinching: Boolean, val ratio: Float, val holdProgress: Float)

data class BoundaryInfo(
    val drawing: Boolean = false, val corners: Int = 0, val set: Boolean = false, val distance: Float? = null,
    /** A preset area is waiting for the floor under the player to be found. */
    val findingFloor: Boolean = false,
    /** The placed well (after resizing / turning) sticks out of the play area. */
    val wellOutside: Boolean = false
)

/** What the AR view is doing, for the on-screen hints. */
enum class ArStatus { STARTING, SEARCHING, READY_TO_PLACE, PLACED, FINDING_FLOOR, TRACKING_LOST, FAILED }

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
    private val onViewAngle: (azimuthDeg: Float, elevationDeg: Float) -> Unit,
    /** Current display rotation (Surface.ROTATION_*); read whenever the surface changes size. */
    private val rotationProvider: () -> Int = { 0 },
    private val onBoundary: (BoundaryInfo) -> Unit = {},
    /** A placement was refused because the well would stick out of the play area. */
    private val onPlacementBlocked: () -> Unit = {}
) : GLSurfaceView.Renderer {

    companion object {
        private const val TAG = "ArBoardRenderer"
        private val CYAN = floatArrayOf(0.133f, 0.827f, 0.933f)
        private val PINK = floatArrayOf(0.957f, 0.447f, 0.714f)
        const val MIN_CELL = 0.012f
        const val MAX_CELL = 0.25f
    }

    private val board = BoardRenderer(context)
    private val boundary = BoundaryRenderer()

    // ===== Play area (all touched on the GL thread; the UI posts commands) =====
    private val commands = ConcurrentLinkedQueue<() -> Unit>()
    private var boundaryAnchor: Anchor? = null
    private val bxs = ArrayList<Float>()
    private val bzs = ArrayList<Float>()
    private var playArea: PlayArea? = null
    /** True while the player is tapping the corners of their play area. */
    @Volatile var boundaryDrawing = false
        private set
    private var lastBoundaryReport = 0L
    /** Last check of the placed well against the play area (resizing / turning can push it out). */
    private var wellOutside = false
    private var lastBoundaryInfo: BoundaryInfo? = null
    private val boundaryMatrix = FloatArray(16)
    private val boundaryInv = FloatArray(16)
    private val boundaryMvp = FloatArray(16)
    private var background: ArBackground? = null

    private val taps = ConcurrentLinkedQueue<FloatArray>()
    @Volatile private var moveTarget: FloatArray? = null
    @Volatile private var replaceRequested = false
    @Volatile var cellMeters = 0.03f
        set(v) { field = v.coerceIn(MIN_CELL, MAX_CELL) }
    @Volatile var yawDegrees = 0f
    /** Set by the view while a two-finger move is in progress (surface detection stays on). */
    @Volatile var gestureMoving = false
    /** Stand-inside mode: the well was placed on the floor centred on the player. */
    @Volatile var insideMode = false
        private set
    @Volatile private var insideRequested = false
    /** Falling piece centre in board units (x, y, z), from the latest state. */
    @Volatile private var pieceCenter: FloatArray? = null
    /** The falling piece is out of view (checked a few times a second) → show the 3D arrow. */
    private var pieceOffScreen = false
    /** 3D pointer arrow on/off (setting). */
    @Volatile var arrowEnabled = true
    /** Draw detected surfaces as a mesh while placing the well / setting up the area (setting). */
    @Volatile var floorMeshEnabled = true
    /** Preset play area (width, depth in metres) waiting to be centred on the player. */
    private var presetPending: FloatArray? = null

    // ===== Hands (beta) =====
    /** Set by the view while Hands is on; null = off (no camera frames are copied). */
    @Volatile var handTracker: HandTracker? = null
    @Volatile private var latestHand: HandTracker.HandPoints? = null
    @Volatile private var handResultSeq = 0
    private var handSeqSeen = 0
    private var lastHandSubmit = 0L
    private val handGesture = HandGesture { height.toFloat() }
    /** Pinch / twist / drop move the piece (the Hands setting); off = tracking only (setup, placing). */
    @Volatile var handGesturesEnabled = false
    /** Hold your hand still on a surface for 3 s to place the well there. */
    @Volatile var handPlaceEnabled = true
    /** Per-player pinch thresholds (Hand setup). */
    fun setPinchThresholds(on: Float, off: Float) { handGesture.pinchOn = on; handGesture.pinchOff = off }
    private val handFilters = Array(42) { OneEuro() }
    private val holdStill = HoldStill(3000L)

    /** Hand actions, called on the GL thread (the view posts them to the UI). */
    @Volatile var onHandDrag: (Float, Float) -> Unit = { _, _ -> }
    @Volatile var onHandSpin: () -> Unit = {}
    @Volatile var onHandDrop: () -> Unit = {}
    /** The tracked hand for drawing / Hand setup; null = no hand. */
    @Volatile var onHandOverlay: (HandFrame?) -> Unit = {}

    /** From the tracker's thread: the newest hand (or null when none is visible). */
    fun onHandResult(points: HandTracker.HandPoints?) { latestHand = points; handResultSeq++ }
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

    fun updateState(state: Game3DState, material: PieceMaterial, ghost: Boolean, themeColor: Long) {
        board.updateState(state, material, ghost, themeColor)
        pieceCenter = state.currentPiece?.let { p ->
            floatArrayOf(
                p.x + p.blocks.map { it.x }.average().toFloat() + 0.5f,
                p.y + p.blocks.map { it.y }.average().toFloat() + 0.5f,
                p.z + p.blocks.map { it.z }.average().toFloat() + 0.5f
            )
        }
    }

    /** Start (or redo) the play area: the next taps on the floor are its corners. */
    fun startBoundary() = commands.offer {
        boundaryAnchor?.detach(); boundaryAnchor = null
        bxs.clear(); bzs.clear(); playArea = null
        boundaryDrawing = true
        setPlaneFinding(true)
        pushBoundaryInfo(null)
    }
    /** Remove the last corner. */
    fun undoBoundaryCorner() = commands.offer {
        if (bxs.isNotEmpty()) { bxs.removeAt(bxs.lastIndex); bzs.removeAt(bzs.lastIndex) }
        pushBoundaryInfo(null)
    }
    /** Close the outline (needs 3 corners). */
    fun finishBoundary() = commands.offer {
        if (bxs.size >= 3) { playArea = PlayArea(bxs.toList(), bzs.toList()); boundaryDrawing = false }
        pushBoundaryInfo(null)
    }
    /**
     * A ready-made rectangular play area, [widthM] × [depthM], centred on where the player stands
     * and facing where they look. Waits until the floor under them is found.
     */
    fun setPresetArea(widthM: Float, depthM: Float) = commands.offer {
        boundaryAnchor?.detach(); boundaryAnchor = null
        bxs.clear(); bzs.clear(); playArea = null; boundaryDrawing = false
        presetPending = floatArrayOf(widthM, depthM)
        setPlaneFinding(true)
        pushBoundaryInfo(null)
    }

    /** Forget the play area. */
    fun clearBoundary() = commands.offer {
        boundaryAnchor?.detach(); boundaryAnchor = null
        bxs.clear(); bzs.clear(); playArea = null; boundaryDrawing = false; presetPending = null; wellOutside = false
        pushBoundaryInfo(null)
    }

    private fun pushBoundaryInfo(distance: Float?) {
        val info = BoundaryInfo(boundaryDrawing, bxs.size, playArea != null, distance, presetPending != null, wellOutside)
        if (info != lastBoundaryInfo) { lastBoundaryInfo = info; onBoundary(info) }
    }

    /** Put the well on the floor around the player (they stand in its centre). */
    fun requestInside() { insideRequested = true }

    /** Screen tap (view pixels): place the well onto the surface under the finger. */
    fun queueTap(x: Float, y: Float) { taps.offer(floatArrayOf(x, y)) }

    /** Slide the well to the surface under this screen point (two-finger drag). */
    fun queueMove(x: Float, y: Float) { moveTarget = floatArrayOf(x, y) }

    /** Forget the current placement so the next tap places the well again. */
    fun requestReplace() { replaceRequested = true; insideRequested = false }

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
        board.contactShadow = true
        boundary.init()
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

    /** Set when a frame failed; the view reports it and AR switches off instead of the app closing. */
    @Volatile var failure: Throwable? = null
        private set

    override fun onDrawFrame(gl: GL10?) {
        // An exception on the GL thread would close the whole app: catch it, report, stop drawing AR
        if (failure != null) { GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT); return }
        try {
            drawFrame()
        } catch (t: Throwable) {
            Log.e(TAG, "AR frame failed", t)
            failure = t
            report(ArStatus.FAILED)
        }
    }

    private fun drawFrame() {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        val bg = background ?: return
        if (viewportChanged) {
            // Rotating the phone changes both size and rotation; ARCore needs both to line up the camera
            session.setDisplayGeometry(rotationProvider(), width, height)
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

        while (true) { val c = commands.poll() ?: break; c() }
        camera.getViewMatrix(view, 0)
        camera.getProjectionMatrix(proj, 0, 0.03f, 50f)
        Matrix.multiplyMM(viewProj, 0, proj, 0, view, 0)
        if (boundaryDrawing) {
            // Taps add corners instead of placing the well
            while (true) {
                val tap = taps.poll() ?: break
                hitPlane(frame, tap[0], tap[1])?.let { addBoundaryCorner(it.hitPose) }
            }
        }
        presetPending?.let { wd -> tryPlacePresetArea(frame, wd[0], wd[1]) }
        if (floorMeshEnabled && (anchor == null || boundaryDrawing || presetPending != null)) drawFloorMesh()
        drawBoundary(frame)
        handTracker?.let { updateHands(frame, it) }

        if (replaceRequested) {
            anchor?.detach(); anchor = null; isPlaced = false; insideMode = false; replaceRequested = false
            setPlaneFinding(true)
        }
        if (insideRequested) {
            val floorY = findFloorBelow(frame)
            if (floorY == null) {
                // Keep looking: the player points the phone at the floor around their feet
                setPlaneFinding(true)
                report(ArStatus.FINDING_FLOOR)
                return
            }
            val cam = camera.pose
            val pose = com.google.ar.core.Pose.makeTranslation(cam.tx(), floorY, cam.tz())
            insideRequested = false
            if (fitsPlayArea(pose)) {
                setAnchor(session.createAnchor(pose))
                insideMode = true
            } else onPlacementBlocked()
        }

        // Taps: place the well under the finger, or at the previewed spot
        while (true) {
            val tap = taps.poll() ?: break
            val hit = hitPlane(frame, tap[0], tap[1])
            val preview = previewPose
            if (hit == null && preview == null) {
                // No surface found yet: place at a guessed distance (instant placement)
                placeInstant(frame, tap[0], tap[1])
                continue
            }
            val pose = hit?.hitPose ?: preview!!.second
            if (!fitsPlayArea(pose)) { onPlacementBlocked(); continue }
            if (hit != null) placeAt(hit) else setAnchor(preview!!.first.createAnchor(preview.second))
        }
        // Two-finger drag: slide the well (needs surfaces, so detection is on while moving)
        if (gestureMoving) setPlaneFinding(true)
        moveTarget?.let { m ->
            moveTarget = null
            hitPlane(frame, m[0], m[1])?.let { if (fitsPlayArea(it.hitPose)) placeAt(it) }
        }

        val a = anchor
        if (a == null) {
            // Preview: show the well where the screen centre meets a surface; a tap places it there
            val preview = hitPlane(frame, width / 2f, height / 2f)
            if (preview != null && !boundaryDrawing) {
                report(ArStatus.READY_TO_PLACE)
                previewPose = (preview.trackable as Plane) to preview.hitPose
                preview.hitPose.toMatrix(anchorMatrix, 0)
                buildBase()
                val camPose = camera.pose
                board.drawScene(viewProj, base, camPose.tx(), camPose.ty(), camPose.tz())
            } else {
                previewPose = null
                report(ArStatus.SEARCHING)
            }
            return
        }
        if (a.trackingState != TrackingState.TRACKING) { report(ArStatus.TRACKING_LOST); return }
        report(ArStatus.PLACED)
        // Placed and not being moved: stop looking for surfaces to save power and heat
        if (!gestureMoving && !boundaryDrawing && presetPending == null) setPlaneFinding(false)

        // board → world: anchor × turn × scale × centre the footprint on the anchor
        a.pose.toMatrix(anchorMatrix, 0)
        buildBase()

        val camPose = camera.pose
        board.drawScene(viewProj, base, camPose.tx(), camPose.ty(), camPose.tz())
        if (arrowEnabled && pieceOffScreen) drawPointerArrow(camera)

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
                if (insideMode) {
                    // Standing in the middle: "away" is wherever you look. An orbit camera looking
                    // that way would sit on the opposite side, so use the reversed view direction.
                    val z = camera.displayOrientedPose.zAxis   // camera looks along -Z
                    val f = FloatArray(4)
                    Matrix.multiplyMV(f, 0, invBase, 0, floatArrayOf(z[0], z[1], z[2], 0f), 0)
                    val horiz = sqrt(f[0] * f[0] + f[2] * f[2])
                    onViewAngle(
                        Math.toDegrees(atan2(f[0], f[2]).toDouble()).toFloat(),
                        Math.toDegrees(atan2(f[1], horiz).toDouble()).toFloat()
                    )
                } else {
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
                reportPieceOnScreen()
                val outside = playArea != null && !fitsPlayArea(a.pose)
                if (outside != wellOutside) { wellOutside = outside; pushBoundaryInfo(lastBoundaryInfo?.distance) }
            }
        }
    }

    private fun addBoundaryCorner(pose: com.google.ar.core.Pose) {
        var a = boundaryAnchor
        if (a == null) {
            // Axis-aligned anchor at the first corner; corners are kept in its space (it follows tracking fixes)
            a = session.createAnchor(com.google.ar.core.Pose.makeTranslation(pose.tx(), pose.ty(), pose.tz()))
            boundaryAnchor = a
        }
        a.pose.toMatrix(boundaryMatrix, 0)
        if (!Matrix.invertM(boundaryInv, 0, boundaryMatrix, 0)) return
        val l = FloatArray(4)
        Matrix.multiplyMV(l, 0, boundaryInv, 0, floatArrayOf(pose.tx(), pose.ty(), pose.tz(), 1f), 0)
        bxs.add(l[0]); bzs.add(l[2])
        pushBoundaryInfo(null)
    }

    /** World point → play-area (x, z), or null without an anchor. */
    private fun toBoundaryLocal(x: Float, y: Float, z: Float): FloatArray? {
        val a = boundaryAnchor ?: return null
        a.pose.toMatrix(boundaryMatrix, 0)
        if (!Matrix.invertM(boundaryInv, 0, boundaryMatrix, 0)) return null
        val l = FloatArray(4)
        Matrix.multiplyMV(l, 0, boundaryInv, 0, floatArrayOf(x, y, z, 1f), 0)
        return floatArrayOf(l[0], l[2])
    }

    /** Would the well, anchored at [pose] with the current size and turn, stay inside the play area? */
    private fun fitsPlayArea(pose: com.google.ar.core.Pose): Boolean {
        val area = playArea ?: return true
        val m = FloatArray(16)
        pose.toMatrix(m, 0)
        Matrix.rotateM(m, 0, yawDegrees, 0f, 1f, 0f)
        Matrix.scaleM(m, 0, cellMeters, cellMeters, cellMeters)
        Matrix.translateM(m, 0, -Tetris3DGame.BOARD_W / 2f, 0f, -Tetris3DGame.BOARD_D / 2f)
        val w = Tetris3DGame.BOARD_W.toFloat(); val d = Tetris3DGame.BOARD_D.toFloat()
        val corners = listOf(0f to 0f, w to 0f, w to d, 0f to d).map { (cx, cz) ->
            val out = FloatArray(4)
            Matrix.multiplyMV(out, 0, m, 0, floatArrayOf(cx, 0f, cz, 1f), 0)
            val l = toBoundaryLocal(out[0], out[1], out[2]) ?: return true
            l[0] to l[1]
        }
        return area.containsAll(corners)
    }

    /**
     * Draw the play area and report how close the phone is to its edge. While setting up, a
     * cursor shows where the screen centre meets the floor.
     */
    private fun drawBoundary(frame: com.google.ar.core.Frame) {
        val a = boundaryAnchor
        val cam = frame.camera.pose
        if (a == null || a.trackingState != TrackingState.TRACKING) {
            if (boundaryDrawing) {
                // No corner yet: just the cursor, drawn at the hit itself
                val hit = hitPlane(frame, width / 2f, height / 2f) ?: return
                val m = FloatArray(16)
                Matrix.setIdentityM(m, 0); Matrix.translateM(m, 0, hit.hitPose.tx(), hit.hitPose.ty(), hit.hitPose.tz())
                Matrix.multiplyMM(boundaryMvp, 0, viewProj, 0, m, 0)
                boundary.draw(boundaryMvp, FloatArray(0), FloatArray(0), 0, false, CYAN, 0.95f, 0f, cursor = floatArrayOf(0f, 0f))
            }
            if (playArea != null) pushBoundaryInfo(null)
            return
        }
        a.pose.toMatrix(boundaryMatrix, 0)
        Matrix.multiplyMM(boundaryMvp, 0, viewProj, 0, boundaryMatrix, 0)
        val xs = bxs.toFloatArray(); val zs = bzs.toFloatArray()
        if (boundaryDrawing) {
            val hit = hitPlane(frame, width / 2f, height / 2f)
            val cursor = hit?.let { toBoundaryLocal(it.hitPose.tx(), it.hitPose.ty(), it.hitPose.tz()) }
            boundary.draw(boundaryMvp, xs, zs, xs.size, false, CYAN, 0.95f, 0.35f, wallHeight = 0.5f, cursor = cursor)
            return
        }
        val area = playArea ?: return
        val me = toBoundaryLocal(cam.tx(), cam.ty(), cam.tz()) ?: return
        val dist = area.signedDistance(me[0], me[1])
        // The wall fades in from 1 m away and is fully there at the edge
        val wallAlpha = ((1f - dist) / 0.8f).coerceIn(0f, 1f)
        val rgb = if (dist < 0.4f) PINK else CYAN
        boundary.draw(boundaryMvp, xs, zs, xs.size, true, rgb, 0.35f + 0.5f * wallAlpha, wallAlpha)
        val now = System.currentTimeMillis()
        if (now - lastBoundaryReport > 150) {
            lastBoundaryReport = now
            // Rounded so the UI only hears about real changes
            pushBoundaryInfo(kotlin.math.round(dist * 20f) / 20f)
        }
    }

    /** Is the falling piece outside the view? (Decides whether the 3D arrow shows.) */
    private fun reportPieceOnScreen() {
        val pc = pieceCenter
        if (pc == null) { pieceOffScreen = false; return }
        val clip = FloatArray(4)
        Matrix.multiplyMV(clip, 0, boardMvp, 0, floatArrayOf(pc[0], pc[1], pc[2], 1f), 0)
        val w = clip[3]
        val nx = clip[0] / abs(w).coerceAtLeast(1e-4f)
        val ny = clip[1] / abs(w).coerceAtLeast(1e-4f)
        pieceOffScreen = !(w > 0f && abs(nx) <= 0.9f && abs(ny) <= 0.9f)
    }

    // ===== 3D pointer arrow =====
    /** Voxel arrow pointing along +Z: a 3-cube shaft, a plus-shaped head and a tip. */
    private val arrowVoxels = listOf(
        intArrayOf(0, 0, 0), intArrayOf(0, 0, 1), intArrayOf(0, 0, 2),
        intArrayOf(0, 0, 3), intArrayOf(1, 0, 3), intArrayOf(-1, 0, 3), intArrayOf(0, 1, 3), intArrayOf(0, -1, 3),
        intArrayOf(0, 0, 4)
    )
    private val arrowModels = List(arrowVoxels.size) { FloatArray(16) }

    /**
     * A brick arrow floating a little in front of and below the phone, turning to point at the
     * falling piece wherever it is (behind you included). Pulses so it catches the eye.
     */
    private fun drawPointerArrow(camera: com.google.ar.core.Camera) {
        val pc = pieceCenter ?: return
        val target = FloatArray(4)
        Matrix.multiplyMV(target, 0, base, 0, floatArrayOf(pc[0], pc[1], pc[2], 1f), 0)
        val pose = camera.displayOrientedPose
        val z = pose.zAxis; val y = pose.yAxis
        // 32 cm ahead (camera looks along -Z), 7 cm below the centre of the view
        val px = pose.tx() - z[0] * 0.32f - y[0] * 0.07f
        val py = pose.ty() - z[1] * 0.32f - y[1] * 0.07f
        val pz = pose.tz() - z[2] * 0.32f - y[2] * 0.07f
        var dx = target[0] - px; var dy = target[1] - py; var dz = target[2] - pz
        val len = sqrt(dx * dx + dy * dy + dz * dz)
        if (len < 1e-4f) return
        dx /= len; dy /= len; dz /= len
        // Orthonormal frame with +Z along the direction (x = up × dir, unless pointing straight up/down)
        var ax = dz; var ay = 0f; var az = -dx            // (0,1,0) × dir
        var al = sqrt(ax * ax + az * az)
        if (al < 1e-3f) { ax = 1f; ay = 0f; az = 0f; al = 1f }
        ax /= al; az /= al
        val bx = dy * az - dz * ay; val by = dz * ax - dx * az; val bz = dx * ay - dy * ax   // dir × x
        val rot = floatArrayOf(
            ax, ay, az, 0f,
            bx, by, bz, 0f,
            dx, dy, dz, 0f,
            px, py, pz, 1f
        )
        val cell = 0.014f
        for ((i, v) in arrowVoxels.withIndex()) {
            val m = arrowModels[i]
            System.arraycopy(rot, 0, m, 0, 16)
            Matrix.scaleM(m, 0, cell, cell, cell)
            // Centre the arrow on its middle (cube spans 0..1)
            Matrix.translateM(m, 0, v[0] - 0.5f, v[1] - 0.5f, v[2] - 2.5f)
        }
        val pulse = 0.75f + 0.25f * kotlin.math.sin(System.currentTimeMillis() / 160.0).toFloat()
        board.drawFreeCubes(viewProj, arrowModels, floatArrayOf(PINK[0] * pulse, PINK[1] * pulse, PINK[2] * pulse),
            pose.tx(), pose.ty(), pose.tz())
    }

    // ===== Hands (beta) =====
    /** Feed the tracker a camera frame now and then, and turn new results into game actions. */
    private fun updateHands(frame: com.google.ar.core.Frame, tracker: HandTracker) {
        val now = System.currentTimeMillis()
        // ~10 checks a second: enough to follow a hand, and much less heat than every frame
        if (now - lastHandSubmit >= 100 && tracker.ready()) {
            lastHandSubmit = now
            try {
                frame.acquireCameraImage().use { img ->
                    tracker.submit(img, imageRotation(frame), frame.timestamp / 1_000_000)
                }
            } catch (_: Exception) { /* camera image not ready this frame */ }
        }
        val seq = handResultSeq
        if (seq == handSeqSeen) {
            if (handGesturesEnabled) handGesture.update(null, now)
            return
        }
        handSeqSeen = seq
        val hand = latestHand
        if (hand == null) {
            handGesture.update(null, now)
            for (f in handFilters) f.reset()
            holdStill.reset()
            onHandOverlay(null)
            return
        }
        val raw = FloatArray(hand.xy.size)
        frame.transformCoordinates2d(com.google.ar.core.Coordinates2d.IMAGE_NORMALIZED, hand.xy,
            com.google.ar.core.Coordinates2d.VIEW, raw)
        // Smooth each coordinate: steady when still, quick when moving
        val pts = FloatArray(raw.size) { i -> if (i < handFilters.size) handFilters[i].filter(raw[i], now) else raw[i] }

        val actions = handGesture.update(pts, now)
        if (handGesturesEnabled && anchor != null) for (action in actions) when (action) {
            is HandGesture.Action.Drag -> {
                val h = pieceCenter?.get(1) ?: continue
                screenToBoard(action.x, action.y, h)?.let { onHandDrag(it[0], it[1]) }
            }
            HandGesture.Action.Spin -> onHandSpin()
            HandGesture.Action.Drop -> onHandDrop()
        }

        // No well yet: a hand held still on a surface for 3 s puts the well right there
        var hold = 0f
        if (anchor == null && handPlaceEnabled && !boundaryDrawing && presetPending == null && !handGesture.pinching) {
            val px = (pts[0] + pts[2 * HandLandmarks.INDEX_MCP] + pts[2 * HandLandmarks.MIDDLE_MCP] + pts[2 * HandLandmarks.PINKY_MCP]) / 4f
            val py = (pts[1] + pts[2 * HandLandmarks.INDEX_MCP + 1] + pts[2 * HandLandmarks.MIDDLE_MCP + 1] + pts[2 * HandLandmarks.PINKY_MCP + 1]) / 4f
            if (holdStill.update(px, py, width.toFloat(), now)) placeAtHand(frame, px, py, hand.xy)
            hold = holdStill.progress
        } else holdStill.reset()

        onHandOverlay(HandFrame(pts, handGesture.pinching, handGesture.lastRatio, hold))
    }

    /**
     * Place the well where the hand rests: on a detected surface if there is one under the palm,
     * otherwise at the hand's distance, estimated from how big the palm looks (≈7.5 cm across).
     */
    private fun placeAtHand(frame: com.google.ar.core.Frame, x: Float, y: Float, imageXy: FloatArray) {
        hitPlane(frame, x, y)?.let { hit ->
            if (fitsPlayArea(hit.hitPose)) placeAt(hit) else onPlacementBlocked()
            return
        }
        val intr = frame.camera.imageIntrinsics
        val dims = intr.imageDimensions; val focal = intr.focalLength
        val dx = (imageXy[2 * HandLandmarks.INDEX_MCP] - imageXy[2 * HandLandmarks.PINKY_MCP]) * dims[0]
        val dy = (imageXy[2 * HandLandmarks.INDEX_MCP + 1] - imageXy[2 * HandLandmarks.PINKY_MCP + 1]) * dims[1]
        val palmPx = sqrt(dx * dx + dy * dy)
        val distance = if (palmPx > 1f) (focal[0] * 0.075f / palmPx).coerceIn(0.2f, 3f) else 0.5f
        val hit = try { frame.hitTestInstantPlacement(x, y, distance).firstOrNull() } catch (_: Exception) { null } ?: return
        if (!fitsPlayArea(hit.hitPose)) { onPlacementBlocked(); return }
        setAnchor(hit.createAnchor())
    }

    /** Clockwise degrees the CPU camera image must turn to look upright on screen. */
    private fun imageRotation(frame: com.google.ar.core.Frame): Int {
        val out = FloatArray(4)
        frame.transformCoordinates2d(com.google.ar.core.Coordinates2d.IMAGE_NORMALIZED, floatArrayOf(0f, 0f, 1f, 0f),
            com.google.ar.core.Coordinates2d.VIEW_NORMALIZED, out)
        val dx = out[2] - out[0]; val dy = out[3] - out[1]
        return if (abs(dx) >= abs(dy)) (if (dx > 0) 0 else 180) else (if (dy > 0) 90 else 270)
    }

    // ===== Floor mesh and preset play areas =====
    private val planeMatrix = FloatArray(16)
    private val planeMvp = FloatArray(16)

    /** Detected horizontal surfaces as a mesh ("the camera has scanned this"). */
    private fun drawFloorMesh() {
        for (plane in session.getAllTrackables(Plane::class.java)) {
            if (plane.trackingState != TrackingState.TRACKING || plane.subsumedBy != null) continue
            if (plane.type != Plane.Type.HORIZONTAL_UPWARD_FACING) continue
            val buf = plane.polygon ?: continue
            buf.rewind()
            val poly = FloatArray(buf.remaining()).also { buf.get(it) }
            plane.centerPose.toMatrix(planeMatrix, 0)
            Matrix.multiplyMM(planeMvp, 0, viewProj, 0, planeMatrix, 0)
            boundary.drawMesh(planeMvp, poly, CYAN)
        }
    }

    /** Centre a [w] × [d] m rectangle on the player once the floor under them is known. */
    private fun tryPlacePresetArea(frame: com.google.ar.core.Frame, w: Float, d: Float) {
        val floorY = findFloorBelow(frame) ?: run { pushBoundaryInfo(null); return }
        val pose = frame.camera.displayOrientedPose
        val z = pose.zAxis
        // Face the way the player looks: local -Z = horizontal forward
        val fx = -z[0]; val fz = -z[2]
        val theta = if (fx * fx + fz * fz < 1e-6f) 0f else kotlin.math.atan2(-fx, -fz)
        val half = theta / 2f
        val anchorPose = com.google.ar.core.Pose(
            floatArrayOf(pose.tx(), floorY, pose.tz()),
            floatArrayOf(0f, kotlin.math.sin(half), 0f, kotlin.math.cos(half))
        )
        boundaryAnchor?.detach()
        boundaryAnchor = session.createAnchor(anchorPose)
        bxs.clear(); bzs.clear()
        val hw = w / 2f; val hd = d / 2f
        bxs.addAll(listOf(-hw, hw, hw, -hw)); bzs.addAll(listOf(-hd, -hd, hd, hd))
        playArea = PlayArea(bxs.toList(), bzs.toList())
        presetPending = null
        pushBoundaryInfo(null)
    }

    /**
     * Height of the floor under the player: a straight-down hit from the phone, else the lowest
     * detected horizontal surface well below the phone. Null until one is found.
     */
    private fun findFloorBelow(frame: com.google.ar.core.Frame): Float? {
        val cam = frame.camera.pose
        val down = try {
            frame.hitTest(floatArrayOf(cam.tx(), cam.ty(), cam.tz()), 0, floatArrayOf(0f, -1f, 0f), 0)
                .firstOrNull { h -> val t = h.trackable; t is Plane && t.type == Plane.Type.HORIZONTAL_UPWARD_FACING }
        } catch (_: Exception) { null }
        if (down != null && cam.ty() - down.hitPose.ty() > 0.6f) return down.hitPose.ty()
        return session.getAllTrackables(Plane::class.java)
            .filter { it.trackingState == TrackingState.TRACKING && it.type == Plane.Type.HORIZONTAL_UPWARD_FACING && it.subsumedBy == null }
            .map { it.centerPose.ty() }
            .filter { cam.ty() - it > 0.6f }
            .minOrNull()
    }

    /** Surface + pose under the screen centre while placing (a fresh anchor can be made from it later). */
    private var previewPose: Pair<Plane, com.google.ar.core.Pose>? = null

    /** base = anchorMatrix × turn × scale × centre the footprint on the anchor point. */
    private fun buildBase() {
        System.arraycopy(anchorMatrix, 0, base, 0, 16)
        Matrix.rotateM(base, 0, yawDegrees, 0f, 1f, 0f)
        val s = cellMeters
        Matrix.scaleM(base, 0, s, s, s)
        Matrix.translateM(base, 0, -Tetris3DGame.BOARD_W / 2f, 0f, -Tetris3DGame.BOARD_D / 2f)
    }

    private fun hitPlane(frame: com.google.ar.core.Frame, x: Float, y: Float) =
        frame.hitTest(x, y).firstOrNull { h ->
            val t = h.trackable
            t is Plane && t.type == Plane.Type.HORIZONTAL_UPWARD_FACING && t.isPoseInPolygon(h.hitPose)
        }

    private fun placeAt(hit: com.google.ar.core.HitResult) = setAnchor(hit.createAnchor())

    /**
     * Place where the user tapped without a detected surface: ARCore guesses the point at a
     * distance that suits the well's size and refines it as it learns the scene.
     */
    private fun placeInstant(frame: com.google.ar.core.Frame, x: Float, y: Float) {
        val distance = when { cellMeters < 0.05f -> 0.5f; cellMeters < 0.12f -> 1.0f; else -> 1.8f }
        val hit = try { frame.hitTestInstantPlacement(x, y, distance).firstOrNull() } catch (_: Exception) { null } ?: return
        if (!fitsPlayArea(hit.hitPose)) { onPlacementBlocked(); return }
        setAnchor(hit.createAnchor())
    }

    private fun setAnchor(newAnchor: Anchor) {
        anchor?.detach()
        insideMode = false
        anchor = newAnchor
        isPlaced = true
        previewPose = null
    }

    private fun report(status: ArStatus) {
        if (status != lastStatus) { lastStatus = status; onStatus(status) }
    }

    /** Release ARCore objects held by the renderer (session is closed by its owner). */
    fun release() {
        anchor?.detach(); anchor = null; isPlaced = false; insideMode = false
        boundaryAnchor?.detach(); boundaryAnchor = null
    }
}
