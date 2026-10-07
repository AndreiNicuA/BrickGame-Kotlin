package com.brickgame.tetris.gl

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.opengl.GLSurfaceView
import android.os.Build
import android.os.PowerManager
import android.view.MotionEvent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.brickgame.tetris.game.Game3DState
import com.brickgame.tetris.ui.components.PieceMaterial
import com.google.ar.core.ArCoreApk
import com.google.ar.core.CameraConfig
import com.google.ar.core.CameraConfigFilter
import com.google.ar.core.Config
import com.google.ar.core.Session
import java.util.EnumSet
import kotlin.math.atan2
import kotlin.math.hypot

/** Handle the screen keeps to drive the AR renderer's play-area setup. */
class ArBoardController {
    @Volatile var renderer: ArBoardRenderer? = null
    fun startBoundary() { renderer?.startBoundary() }
    fun undoBoundaryCorner() { renderer?.undoBoundaryCorner() }
    fun finishBoundary() { renderer?.finishBoundary() }
    fun clearBoundary() { renderer?.clearBoundary() }
    fun setPresetArea(widthM: Float, depthM: Float) { renderer?.setPresetArea(widthM, depthM) }
}

/** Whether this phone can run the AR mode. */
enum class ArSupport { CHECKING, SUPPORTED, UNSUPPORTED }

/** Android's own thermal verdict (API 29+; NORMAL on older phones). */
enum class ArHeat { NORMAL, WARM, HOT, CRITICAL }

/**
 * Phone temperature while AR runs: battery temperature in °C (null if the phone doesn't report
 * it), whether it is charging (charging adds heat), and Android's thermal status.
 */
data class ArHeatInfo(val batteryC: Float?, val charging: Boolean, val thermal: ArHeat)

/**
 * Asks ARCore whether the device is capable. The first answer can be "still checking" (it may
 * query the network), so it polls briefly. Unsupported phones simply don't get the AR button.
 */
@Composable
fun rememberArSupport(): ArSupport {
    val context = LocalContext.current
    var support by remember { mutableStateOf(ArSupport.CHECKING) }
    LaunchedEffect(Unit) {
        repeat(25) {
            val availability = try { ArCoreApk.getInstance().checkAvailability(context) } catch (_: Exception) { null }
            when {
                availability == null -> { support = ArSupport.UNSUPPORTED; return@LaunchedEffect }
                availability.isTransient -> kotlinx.coroutines.delay(200)
                availability.isSupported -> { support = ArSupport.SUPPORTED; return@LaunchedEffect }
                else -> { support = ArSupport.UNSUPPORTED; return@LaunchedEffect }
            }
        }
        support = ArSupport.UNSUPPORTED
    }
    return support
}

/**
 * Creates an ARCore session, first asking Google Play Services for AR to install/update itself if
 * needed. Returns null while an install is in progress (the activity is resumed afterwards and
 * this is called again) and throws if AR can't be used. Call from the main thread with the
 * camera permission already granted.
 *
 * Tuned for heat: 30 fps camera, the smallest GPU camera image ARCore offers, no depth sensor,
 * no light estimation, fixed focus.
 */
fun createArSession(activity: Activity, userRequestedInstall: Boolean): Session? {
    val status = ArCoreApk.getInstance().requestInstall(activity, userRequestedInstall)
    if (status != ArCoreApk.InstallStatus.INSTALLED) return null
    val session = Session(activity)
    try {
        val filter = CameraConfigFilter(session)
            .setTargetFps(EnumSet.of(CameraConfig.TargetFps.TARGET_FPS_30))
            .setDepthSensorUsage(EnumSet.of(CameraConfig.DepthSensorUsage.DO_NOT_USE))
        session.getSupportedCameraConfigs(filter)
            .minByOrNull { it.textureSize.width * it.textureSize.height }
            ?.let { session.cameraConfig = it }
    } catch (_: Exception) { /* keep ARCore's default camera config */ }
    val config = Config(session).apply {
        planeFindingMode = Config.PlaneFindingMode.HORIZONTAL
        updateMode = Config.UpdateMode.BLOCKING
        focusMode = Config.FocusMode.FIXED
        lightEstimationMode = Config.LightEstimationMode.DISABLED
        depthMode = Config.DepthMode.DISABLED
        instantPlacementMode = Config.InstantPlacementMode.DISABLED
    }
    session.configure(config)
    return session
}

/**
 * The AR board: camera feed with the well anchored in the room.
 *
 * Touch, before the well is placed: tap a surface to place it.
 * Touch, once placed:
 *  - one finger drag: grab the falling piece and slide it to the cell under your finger
 *  - tap: spin the piece
 *  - two fingers: pinch to resize, twist to turn, drag to move the well
 *
 * The session is resumed/paused with the screen and closed when this leaves composition.
 */
@SuppressLint("ClickableViewAccessibility")
@Composable
fun ArBoardView(
    session: Session,
    state: Game3DState,
    material: PieceMaterial,
    themeColor: Long,
    replaceKey: Int,
    /** Bump to put the well on the floor around the player (stand-inside mode). */
    insideKey: Int,
    cellMeters: Float,
    onCellMeters: (Float) -> Unit,
    onStatus: (ArStatus) -> Unit,
    onViewAngle: (Float, Float) -> Unit,
    /** Board coordinates (x, z) under the finger while dragging the piece. */
    onPieceDrag: (Float, Float) -> Unit,
    onTap: () -> Unit,
    onHardDrop: () -> Unit,
    onHeat: (ArHeatInfo) -> Unit,
    modifier: Modifier = Modifier,
    /** Lets the screen drive the play-area setup (start / undo / done / clear). */
    controller: ArBoardController? = null,
    onBoundary: (BoundaryInfo) -> Unit = {},
    /** 3D pointer arrow towards the falling piece when it's out of view. */
    arrow: Boolean = true,
    /** Show detected surfaces as a mesh while placing. */
    floorMesh: Boolean = true,
    /** Hands (beta): pinch the piece in front of the camera. */
    hands: Boolean = false,
    onPlacementBlocked: () -> Unit = {}
) {
    val latestStatus by rememberUpdatedState(onStatus)
    val latestAngle by rememberUpdatedState(onViewAngle)
    val latestDrag by rememberUpdatedState(onPieceDrag)
    val latestTap by rememberUpdatedState(onTap)
    val latestHardDrop by rememberUpdatedState(onHardDrop)
    val latestCell by rememberUpdatedState(onCellMeters)
    val latestHeat by rememberUpdatedState(onHeat)
    val latestBoundary by rememberUpdatedState(onBoundary)
    val latestBlocked by rememberUpdatedState(onPlacementBlocked)
    val latestState by rememberUpdatedState(state)
    val viewRef = remember { mutableStateOf<GLSurfaceView?>(null) }
    val rendererRef = remember { mutableStateOf<ArBoardRenderer?>(null) }
    val context = LocalContext.current

    LaunchedEffect(state, material, themeColor) { rendererRef.value?.updateState(state, material, true, themeColor) }
    LaunchedEffect(replaceKey) { if (replaceKey > 0) rendererRef.value?.requestReplace() }
    LaunchedEffect(insideKey) { if (insideKey > 0) rendererRef.value?.requestInside() }
    LaunchedEffect(arrow) { rendererRef.value?.arrowEnabled = arrow }
    // Hands (beta): the tracker only exists while the setting is on
    var handOverlay by remember { mutableStateOf<FloatArray?>(null) }
    DisposableEffect(hands, rendererRef.value) {
        val renderer = rendererRef.value
        val tracker = if (hands && renderer != null) HandTracker(context) { renderer.onHandResult(it) } else null
        renderer?.handTracker = tracker
        onDispose {
            renderer?.handTracker = null
            tracker?.close()
            handOverlay = null
        }
    }
    LaunchedEffect(floorMesh) { rendererRef.value?.floorMeshEnabled = floorMesh }
    LaunchedEffect(cellMeters) { rendererRef.value?.cellMeters = cellMeters }

    // Phone temperature, every few seconds: battery °C + Android's thermal status.
    // The caller decides when to warn and when to leave AR.
    LaunchedEffect(Unit) {
        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        while (true) {
            val battery = context.registerReceiver(null, android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED))
            val tenths = battery?.getIntExtra(android.os.BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE) ?: Int.MIN_VALUE
            val plugged = (battery?.getIntExtra(android.os.BatteryManager.EXTRA_PLUGGED, 0) ?: 0) != 0
            val thermal = if (pm != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val st = pm.currentThermalStatus
                when {
                    st >= PowerManager.THERMAL_STATUS_CRITICAL -> ArHeat.CRITICAL
                    st >= PowerManager.THERMAL_STATUS_SEVERE -> ArHeat.HOT
                    st >= PowerManager.THERMAL_STATUS_MODERATE -> ArHeat.WARM
                    else -> ArHeat.NORMAL
                }
            } else ArHeat.NORMAL
            latestHeat(ArHeatInfo(if (tenths == Int.MIN_VALUE) null else tenths / 10f, plugged, thermal))
            kotlinx.coroutines.delay(3000)
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, session) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> { try { session.resume() } catch (_: Exception) { latestStatus(ArStatus.FAILED) }; viewRef.value?.onResume() }
                Lifecycle.Event.ON_PAUSE -> { viewRef.value?.onPause(); session.pause() }
                else -> {}
            }
        }
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            try { session.resume() } catch (_: Exception) { latestStatus(ArStatus.FAILED) }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewRef.value?.onPause()          // stops the GL thread before the session goes away
            rendererRef.value?.release()
            controller?.renderer = null
            session.pause()
            session.close()
        }
    }

    Box(modifier) {
    AndroidView(
        factory = { ctx: Context ->
            GLSurfaceView(ctx).also { view ->
                val renderer = ArBoardRenderer(ctx, session,
                    onStatus = { s -> view.post { latestStatus(s) } },
                    onViewAngle = { az, el -> view.post { latestAngle(az, el) } },
                    rotationProvider = { view.display?.rotation ?: 0 },
                    onBoundary = { b -> view.post { latestBoundary(b) } },
                    onPlacementBlocked = { view.post { latestBlocked() } })
                controller?.renderer = renderer
                renderer.updateState(state, material, true, themeColor)
                renderer.cellMeters = cellMeters
                renderer.arrowEnabled = arrow
                renderer.onHandDrag = { x, z -> view.post { latestDrag(x, z) } }
                renderer.onHandSpin = { view.post { latestTap() } }
                renderer.onHandDrop = { view.post { latestHardDrop() } }
                renderer.onHandOverlay = { o -> view.post { handOverlay = o } }
                renderer.floorMeshEnabled = floorMesh
                view.preserveEGLContextOnPause = true
                view.setEGLContextClientVersion(2)
                view.setEGLConfigChooser(8, 8, 8, 8, 16, 0)
                view.setRenderer(renderer)
                view.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY  // paced by the 30 fps camera
                view.setOnTouchListener(ArTouch(renderer,
                    pieceHeight = {
                        latestState.currentPiece?.let { p -> p.y + p.blocks.map { it.y }.average().toFloat() + 0.5f }
                    },
                    onDrag = { x, z -> latestDrag(x, z) },
                    onTap = { latestTap() },
                    onHardDrop = { latestHardDrop() },
                    onCell = { latestCell(it) }))
                rendererRef.value = renderer
                viewRef.value = view
            }
        },
        modifier = Modifier.fillMaxSize(),
        update = { }
    )
    if (hands) HandOverlay(handOverlay)
    }
}

/** Fingertip markers for Hands (beta): two dots, joined and ringed while pinching. */
@Composable
private fun HandOverlay(o: FloatArray?) {
    androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
        if (o == null) return@Canvas
        val thumb = androidx.compose.ui.geometry.Offset(o[0], o[1])
        val index = androidx.compose.ui.geometry.Offset(o[2], o[3])
        val pinching = o[4] > 0.5f
        val cyan = androidx.compose.ui.graphics.Color(0xFF22D3EE)
        val pink = androidx.compose.ui.graphics.Color(0xFFF472B6)
        val c = if (pinching) pink else cyan
        drawLine(c.copy(alpha = 0.7f), thumb, index, 6f)
        drawCircle(c, 14f, thumb)
        drawCircle(c, 14f, index)
        if (pinching) drawCircle(pink, 46f, (thumb + index) / 2f, style = androidx.compose.ui.graphics.drawscope.Stroke(6f))
    }
}

/** Touch handling for the AR view (see [ArBoardView] for the gestures). Runs on the UI thread. */
private class ArTouch(
    private val renderer: ArBoardRenderer,
    private val pieceHeight: () -> Float?,
    private val onDrag: (Float, Float) -> Unit,
    private val onTap: () -> Unit,
    private val onHardDrop: () -> Unit,
    private val onCell: (Float) -> Unit
) : android.view.View.OnTouchListener {

    private var downX = 0f; private var downY = 0f; private var downTime = 0L
    private var dragging = false
    private var multi = false
    private var startSpan = 0f; private var startAngle = 0f
    private var startCell = 0f; private var startYaw = 0f
    private var startMidX = 0f; private var startMidY = 0f
    private var moving = false

    override fun onTouch(v: android.view.View, e: MotionEvent): Boolean {
        val slop = 12f * v.resources.displayMetrics.density
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = e.x; downY = e.y; downTime = e.eventTime
                dragging = false; multi = false; moving = false
            }
            MotionEvent.ACTION_POINTER_DOWN -> if (e.pointerCount == 2 && renderer.isPlaced && !renderer.boundaryDrawing) {
                multi = true; dragging = false; moving = false
                startSpan = span(e); startAngle = angle(e)
                startCell = renderer.cellMeters; startYaw = renderer.yawDegrees
                startMidX = midX(e); startMidY = midY(e)
            }
            MotionEvent.ACTION_MOVE -> {
                if (multi && e.pointerCount >= 2) {
                    // Pinch → size, twist → turn, drag → move the well
                    val sp = span(e)
                    if (startSpan > 20f && sp > 20f) {
                        val cell = (startCell * sp / startSpan).coerceIn(ArBoardRenderer.MIN_CELL, ArBoardRenderer.MAX_CELL)
                        renderer.cellMeters = cell
                        onCell(cell)
                    }
                    renderer.yawDegrees = startYaw - Math.toDegrees((angle(e) - startAngle).toDouble()).toFloat()
                    if (!moving && hypot(midX(e) - startMidX, midY(e) - startMidY) > slop * 3) {
                        moving = true; renderer.gestureMoving = true
                    }
                    if (moving) renderer.queueMove(midX(e), midY(e))
                } else if (!multi && renderer.isPlaced && !renderer.boundaryDrawing) {
                    if (!dragging && hypot(e.x - downX, e.y - downY) > slop) dragging = true
                    // A fast downward swipe may become a flick-to-drop: don't slide the piece yet
                    val dx = e.x - downX; val dy = e.y - downY
                    val maybeFlick = dy > 0 && dy > 2 * kotlin.math.abs(dx) && e.eventTime - downTime < 140
                    if (dragging && !maybeFlick) {
                        val h = pieceHeight() ?: return true
                        renderer.screenToBoard(e.x, e.y, h)?.let { onDrag(it[0], it[1]) }
                    }
                }
            }
            MotionEvent.ACTION_POINTER_UP -> if (e.pointerCount <= 2) {
                renderer.gestureMoving = false; moving = false
            }
            MotionEvent.ACTION_UP -> {
                renderer.gestureMoving = false
                val isTap = !multi && !dragging && e.eventTime - downTime < 350
                // Flick down: a short, fast, mostly vertical swipe drops the piece
                val dx = e.x - downX; val dy = e.y - downY
                val density = v.resources.displayMetrics.density
                val isFlick = !multi && renderer.isPlaced && !renderer.boundaryDrawing && dy > 70f * density &&
                    dy > 2 * kotlin.math.abs(dx) && e.eventTime - downTime < 300
                if (isFlick) onHardDrop()
                if (isTap) {
                    // Setting up the play area: taps are corners
                    if (renderer.isPlaced && !renderer.boundaryDrawing) onTap() else renderer.queueTap(e.x, e.y)
                    v.performClick()
                }
                multi = false; dragging = false; moving = false
            }
            MotionEvent.ACTION_CANCEL -> { renderer.gestureMoving = false; multi = false; dragging = false; moving = false }
        }
        return true
    }

    private fun span(e: MotionEvent) = hypot(e.getX(0) - e.getX(1), e.getY(0) - e.getY(1))
    private fun angle(e: MotionEvent) = atan2(e.getY(1) - e.getY(0), e.getX(1) - e.getX(0))
    private fun midX(e: MotionEvent) = (e.getX(0) + e.getX(1)) / 2f
    private fun midY(e: MotionEvent) = (e.getY(0) + e.getY(1)) / 2f
}
