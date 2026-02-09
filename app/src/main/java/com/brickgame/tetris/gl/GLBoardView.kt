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
 * Compose wrapper for OpenGL 3D board.
 *
 * Camera is the single source of truth inside BoardGLSurfaceView.
 * Touch gestures update the view's camera directly → pushed to renderer each frame.
 * onCameraChange callback reports back to Compose for the ViewCube overlay only.
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
    onCameraChange: ((azimuth: Float, elevation: Float, zoom: Float, panX: Float, panY: Float) -> Unit)? = null,
    onViewCreated: ((BoardGLSurfaceView) -> Unit)? = null
) {
    val rendererRef = remember { mutableStateOf<BoardRenderer?>(null) }
    val viewRef = remember { mutableStateOf<BoardGLSurfaceView?>(null) }

    // Push game state changes to renderer (NOT camera — camera is managed by touch)
    LaunchedEffect(state, material, showGhost, themePixelOn, themeBg) {
        rendererRef.value?.updateState(state, material, showGhost, themePixelOn, themeBg)
    }

    AndroidView(
        factory = { context ->
            BoardGLSurfaceView(context).also { view ->
                val renderer = BoardRenderer(context)
                rendererRef.value = renderer
                viewRef.value = view

                view.setEGLContextClientVersion(2)
                view.setEGLConfigChooser(8, 8, 8, 0, 16, 0)
                view.holder.setFormat(android.graphics.PixelFormat.OPAQUE)
                view.setRenderer(renderer)
                view.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY

                // Set initial camera
                view.azimuth = cameraAngleY
                view.camElevation = cameraAngleX
                view.currentZoom = zoom
                view.panX = panOffsetX
                view.panY = panOffsetY
                view.rendererRef = renderer
                view.onCameraChange = onCameraChange

                // Push initial state + camera
                renderer.updateState(state, material, showGhost, themePixelOn, themeBg)
                renderer.updateCamera(cameraAngleY, cameraAngleX, panOffsetX, panOffsetY, zoom)

                onViewCreated?.invoke(view)
            }
        },
        modifier = modifier,
        update = { view ->
            // Update camera from Compose state (sliders, presets, zoom buttons)
            // Only apply if the values actually differ to avoid fighting with touch
            view.rendererRef?.updateCamera(
                view.azimuth, view.camElevation,
                view.panX, view.panY, view.currentZoom
            )
        }
    )
}

class BoardGLSurfaceView(context: Context) : GLSurfaceView(context) {

    var onCameraChange: ((Float, Float, Float, Float, Float) -> Unit)? = null
    var rendererRef: BoardRenderer? = null

    var azimuth = 35f
    var camElevation = 25f
    var currentZoom = 1f
    var panX = 0f
    var panY = 0f

    private var prevX = 0f
    private var prevY = 0f
    private var prevSpread = 0f
    private var fingerCount = 0
    private var isPinching = false

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                prevX = event.x; prevY = event.y
                fingerCount = 1; prevSpread = 0f; isPinching = false
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                fingerCount = event.pointerCount
                if (fingerCount >= 2) {
                    prevSpread = spread(event)
                    prevX = centerX(event); prevY = centerY(event)
                    isPinching = false
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (fingerCount >= 2 && event.pointerCount >= 2) {
                    val cx = centerX(event); val cy = centerY(event)
                    val sp = spread(event)
                    val dx = cx - prevX; val dy = cy - prevY

                    if (prevSpread > 10f && sp > 10f) {
                        val sd = sp - prevSpread
                        // Determine if pinching or panning based on initial gesture
                        if (!isPinching && abs(sd) > 8f) isPinching = true

                        if (isPinching) {
                            currentZoom = (currentZoom + sd * 0.004f).coerceIn(0.3f, 3f)
                        } else {
                            panX += dx
                            panY += dy
                        }
                    }
                    prevSpread = sp; prevX = cx; prevY = cy
                } else if (fingerCount == 1) {
                    // Single finger drag → orbit
                    val dx = event.x - prevX
                    val dy = event.y - prevY
                    azimuth = (azimuth + dx * 0.35f) % 360f
                    camElevation = (camElevation - dy * 0.25f).coerceIn(-80f, 80f)
                    prevX = event.x; prevY = event.y
                }
                // Push camera to renderer directly (no Compose round-trip)
                rendererRef?.updateCamera(azimuth, camElevation, panX, panY, currentZoom)
                // Notify Compose for ViewCube overlay
                onCameraChange?.invoke(azimuth, camElevation, currentZoom, panX, panY)
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                fingerCount = 0; isPinching = false
            }
            MotionEvent.ACTION_POINTER_UP -> {
                fingerCount = maxOf(0, event.pointerCount - 1)
                isPinching = false
                if (event.pointerCount > 1) {
                    val remaining = (0 until event.pointerCount).firstOrNull { it != event.actionIndex }
                    if (remaining != null) {
                        prevX = event.getX(remaining); prevY = event.getY(remaining)
                    }
                }
            }
        }
        return true
    }

    /** Set camera from external source (sliders/presets). Updates both view and renderer. */
    fun setCameraExternal(az: Float, el: Float, z: Float, px: Float, py: Float) {
        azimuth = az; camElevation = el; currentZoom = z; panX = px; panY = py
        rendererRef?.updateCamera(az, el, px, py, z)
    }

    private fun centerX(e: MotionEvent): Float { var s = 0f; for (i in 0 until e.pointerCount) s += e.getX(i); return s / e.pointerCount }
    private fun centerY(e: MotionEvent): Float { var s = 0f; for (i in 0 until e.pointerCount) s += e.getY(i); return s / e.pointerCount }
    private fun spread(e: MotionEvent): Float {
        if (e.pointerCount < 2) return 0f
        val dx = e.getX(0) - e.getX(1); val dy = e.getY(0) - e.getY(1)
        return sqrt(dx * dx + dy * dy)
    }
}
