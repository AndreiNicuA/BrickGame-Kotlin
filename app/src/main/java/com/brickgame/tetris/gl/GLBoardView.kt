package com.brickgame.tetris.gl

import android.content.Context
import android.opengl.GLSurfaceView
import android.view.MotionEvent
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.brickgame.tetris.game.Game3DState
import com.brickgame.tetris.ui.components.PieceMaterial
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Jetpack Compose wrapper for the OpenGL 3D board renderer.
 *
 * Drop-in replacement for Tetris3DBoard() in Game3DScreen.
 * Touch gestures:
 *   - 1 finger drag: orbit camera (azimuth/elevation)
 *   - 2 finger drag: pan camera
 *   - 2 finger pinch/spread: zoom
 */
@Composable
fun GLBoardView(
    state: Game3DState,
    modifier: Modifier = Modifier,
    showGhost: Boolean = true,
    cameraAngleY: Float = 35f,
    cameraAngleX: Float = 25f,
    panOffsetX: Float = 0f,
    panOffsetY: Float = 0f,
    zoom: Float = 1f,
    themePixelOn: Long = 0xFF22C55EL,
    themeBg: Long = 0xFF0A0A0AL,
    material: PieceMaterial = PieceMaterial.CLASSIC,
    onCameraChange: ((azimuth: Float, elevation: Float, zoom: Float, panX: Float, panY: Float) -> Unit)? = null
) {
    val rendererRef = remember { mutableStateOf<BoardRenderer?>(null) }

    // Push state changes to renderer
    LaunchedEffect(state, material, showGhost, themePixelOn, themeBg) {
        rendererRef.value?.updateState(state, material, showGhost, themePixelOn, themeBg)
    }

    // Push camera changes
    LaunchedEffect(cameraAngleY, cameraAngleX, panOffsetX, panOffsetY, zoom) {
        rendererRef.value?.updateCamera(cameraAngleY, cameraAngleX, panOffsetX, panOffsetY, zoom)
    }

    AndroidView(
        factory = { context ->
            BoardGLSurfaceView(context).also { view ->
                val renderer = BoardRenderer(context)
                rendererRef.value = renderer

                view.setEGLContextClientVersion(2)
                // Request RGBA8888 with depth buffer and alpha for transparency
                view.setEGLConfigChooser(8, 8, 8, 8, 16, 0)
                view.setRenderer(renderer)
                view.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY

                // Touch gesture callbacks
                view.onCameraChange = onCameraChange
                view.azimuth = cameraAngleY
                view.elevation = cameraAngleX
                view.currentZoom = zoom
                view.panX = panOffsetX
                view.panY = panOffsetY

                // Push initial state
                renderer.updateState(state, material, showGhost, themePixelOn, themeBg)
                renderer.updateCamera(cameraAngleY, cameraAngleX, panOffsetX, panOffsetY, zoom)
            }
        },
        modifier = modifier,
        update = { /* Updates handled via LaunchedEffect */ }
    )
}

/**
 * Custom GLSurfaceView with multi-touch gesture handling for camera control.
 * Mimics the existing pointer event handling from the Canvas-based Game3DScreen.
 */
class BoardGLSurfaceView(context: Context) : GLSurfaceView(context) {

    var onCameraChange: ((Float, Float, Float, Float, Float) -> Unit)? = null

    var azimuth = 35f
    var elevation = 25f
    var currentZoom = 1f
    var panX = 0f
    var panY = 0f

    private var prevX = 0f
    private var prevY = 0f
    private var prevSpread = 0f
    private var fingerCount = 0

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                prevX = event.x
                prevY = event.y
                fingerCount = 1
                prevSpread = 0f
            }

            MotionEvent.ACTION_POINTER_DOWN -> {
                fingerCount = event.pointerCount
                if (fingerCount >= 2) {
                    prevSpread = spread(event)
                    prevX = centerX(event)
                    prevY = centerY(event)
                }
            }

            MotionEvent.ACTION_MOVE -> {
                if (fingerCount >= 2 && event.pointerCount >= 2) {
                    // Two-finger gesture: pinch-zoom or pan
                    val cx = centerX(event)
                    val cy = centerY(event)
                    val sp = spread(event)
                    val dx = cx - prevX
                    val dy = cy - prevY

                    if (prevSpread > 0f && sp > 0f) {
                        val sd = sp - prevSpread
                        if (abs(sd) > 3f) {
                            currentZoom = (currentZoom + sd * 0.003f).coerceIn(0.3f, 3f)
                        } else {
                            panX += dx
                            panY += dy
                        }
                    }
                    prevSpread = sp
                    prevX = cx
                    prevY = cy
                } else {
                    // Single-finger drag: orbit camera
                    val dx = event.x - prevX
                    val dy = event.y - prevY
                    azimuth = (azimuth + dx * 0.3f) % 360f
                    elevation = (elevation - dy * 0.2f).coerceIn(-85f, 85f)
                    prevX = event.x
                    prevY = event.y
                }
                onCameraChange?.invoke(azimuth, elevation, currentZoom, panX, panY)
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                fingerCount = 0
            }

            MotionEvent.ACTION_POINTER_UP -> {
                fingerCount = maxOf(0, fingerCount - 1)
                // Recenter tracking to remaining finger
                if (event.pointerCount > 1) {
                    val remaining = (0 until event.pointerCount).firstOrNull { it != event.actionIndex }
                    if (remaining != null) {
                        prevX = event.getX(remaining)
                        prevY = event.getY(remaining)
                    }
                }
            }
        }
        return true
    }

    private fun centerX(e: MotionEvent): Float {
        var sum = 0f
        for (i in 0 until e.pointerCount) sum += e.getX(i)
        return sum / e.pointerCount
    }

    private fun centerY(e: MotionEvent): Float {
        var sum = 0f
        for (i in 0 until e.pointerCount) sum += e.getY(i)
        return sum / e.pointerCount
    }

    private fun spread(e: MotionEvent): Float {
        if (e.pointerCount < 2) return 0f
        val dx = e.getX(0) - e.getX(1)
        val dy = e.getY(0) - e.getY(1)
        return sqrt(dx * dx + dy * dy)
    }
}
