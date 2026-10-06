package com.brickgame.tetris.gl

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.opengl.GLSurfaceView
import android.view.MotionEvent
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
import com.google.ar.core.Config
import com.google.ar.core.Session

/** Whether this phone can run the AR mode. */
enum class ArSupport { CHECKING, SUPPORTED, UNSUPPORTED }

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
 */
fun createArSession(activity: Activity, userRequestedInstall: Boolean): Session? {
    val status = ArCoreApk.getInstance().requestInstall(activity, userRequestedInstall)
    if (status != ArCoreApk.InstallStatus.INSTALLED) return null
    val session = Session(activity)
    val config = Config(session).apply {
        planeFindingMode = Config.PlaneFindingMode.HORIZONTAL
        updateMode = Config.UpdateMode.BLOCKING
        focusMode = Config.FocusMode.AUTO
        lightEstimationMode = Config.LightEstimationMode.DISABLED
    }
    session.configure(config)
    return session
}

/**
 * The AR board: camera feed with the well anchored in the room. Tap a surface to place it.
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
    onStatus: (ArStatus) -> Unit,
    onViewAngle: (Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val latestStatus by rememberUpdatedState(onStatus)
    val latestAngle by rememberUpdatedState(onViewAngle)
    val viewRef = remember { mutableStateOf<GLSurfaceView?>(null) }
    val rendererRef = remember { mutableStateOf<ArBoardRenderer?>(null) }

    LaunchedEffect(state, material, themeColor) { rendererRef.value?.updateState(state, material, true, themeColor) }
    LaunchedEffect(replaceKey) { if (replaceKey > 0) rendererRef.value?.requestReplace() }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, session) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> { try { session.resume() } catch (_: Exception) { latestStatus(ArStatus.FAILED) }; viewRef.value?.onResume() }
                Lifecycle.Event.ON_PAUSE -> { viewRef.value?.onPause(); session.pause() }
                else -> {}
            }
        }
        // Already resumed when we arrive: start now
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            try { session.resume() } catch (_: Exception) { latestStatus(ArStatus.FAILED) }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewRef.value?.onPause()          // stops the GL thread before the session goes away
            rendererRef.value?.release()
            session.pause()
            session.close()
        }
    }

    AndroidView(
        factory = { ctx: Context ->
            GLSurfaceView(ctx).also { view ->
                val renderer = ArBoardRenderer(ctx, session,
                    onStatus = { s -> view.post { latestStatus(s) } },
                    onViewAngle = { az, el -> view.post { latestAngle(az, el) } })
                renderer.updateState(state, material, true, themeColor)
                renderer.displayRotation = (ctx as? Activity)?.windowManager?.defaultDisplay?.rotation ?: 0
                view.preserveEGLContextOnPause = true
                view.setEGLContextClientVersion(2)
                view.setEGLConfigChooser(8, 8, 8, 8, 16, 0)
                view.setRenderer(renderer)
                view.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY  // camera feed needs every frame
                view.setOnTouchListener { v, e ->
                    if (e.action == MotionEvent.ACTION_UP) { renderer.queueTap(e.x, e.y); v.performClick() }
                    true
                }
                rendererRef.value = renderer
                viewRef.value = view
            }
        },
        modifier = modifier,
        update = { view ->
            rendererRef.value?.displayRotation = (view.context as? Activity)?.windowManager?.defaultDisplay?.rotation ?: 0
        }
    )
}
