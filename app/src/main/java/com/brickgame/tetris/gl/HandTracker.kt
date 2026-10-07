package com.brickgame.tetris.gl

import android.content.Context
import android.graphics.Bitmap
import android.media.Image
import android.util.Log
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Finds one hand in the AR camera image (Google MediaPipe hand landmarks, on the phone, no
 * network). The renderer hands over a camera frame whenever the tracker is free; the frame is
 * copied, turned upright at half resolution and detected on a background thread, so the
 * camera / drawing never waits. One frame at a time keeps the extra heat down.
 *
 * Results are the 21 hand landmarks in normalised coordinates of the original (unrotated)
 * camera image, ready for ARCore's coordinate transform to screen pixels.
 */
class HandTracker(context: Context, private val onHand: (HandPoints?) -> Unit) {

    /** Landmarks in IMAGE_NORMALIZED space (x, y pairs; MediaPipe's 21-point hand). */
    class HandPoints(val xy: FloatArray, val timestampMs: Long)

    companion object {
        private const val TAG = "HandTracker"
    }

    private val executor = Executors.newSingleThreadExecutor()
    private val busy = AtomicBoolean(false)
    @Volatile private var closed = false
    private var landmarker: HandLandmarker? = null

    // Reused buffers (only one frame is ever in flight)
    private var yBuf = ByteArray(0); private var uBuf = ByteArray(0); private var vBuf = ByteArray(0)
    private var pixels = IntArray(0)
    private var bitmap: Bitmap? = null
    private var inflightRotation = 0
    private var lastTimestamp = 0L

    init {
        val appContext = context.applicationContext
        executor.execute {
            try {
                val options = HandLandmarker.HandLandmarkerOptions.builder()
                    .setBaseOptions(BaseOptions.builder().setModelAssetPath("hand_landmarker.task").build())
                    .setRunningMode(RunningMode.LIVE_STREAM)
                    .setNumHands(1)
                    .setMinHandDetectionConfidence(0.5f)
                    .setMinHandPresenceConfidence(0.5f)
                    .setMinTrackingConfidence(0.5f)
                    .setResultListener { result, _ ->
                        val hands = result.landmarks()
                        if (hands.isEmpty()) onHand(null)
                        else {
                            val lm = hands[0]
                            val out = FloatArray(lm.size * 2)
                            for (i in lm.indices) {
                                val (x, y) = toImageNormalized(lm[i].x(), lm[i].y(), inflightRotation)
                                out[2 * i] = x; out[2 * i + 1] = y
                            }
                            onHand(HandPoints(out, result.timestampMs()))
                        }
                        busy.set(false)
                    }
                    .setErrorListener { e -> Log.w(TAG, "hand landmarker error", e); busy.set(false) }
                    .build()
                landmarker = HandLandmarker.createFromOptions(appContext, options)
            } catch (e: Exception) {
                Log.e(TAG, "Hand tracking unavailable", e)
            }
        }
    }

    /** True when a new frame can be handed over. */
    fun ready(): Boolean = !closed && landmarker != null && !busy.get()

    /**
     * Copy the camera [image] (YUV_420_888) and detect on the background thread.
     * [rotation] = clockwise degrees the image must turn to look upright on screen.
     * Call on the GL thread; the caller closes the image right after.
     */
    fun submit(image: Image, rotation: Int, timestampMs: Long) {
        if (!ready() || timestampMs <= lastTimestamp) return
        if (!busy.compareAndSet(false, true)) return
        lastTimestamp = timestampMs
        val w = image.width; val h = image.height
        val yp = image.planes[0]; val up = image.planes[1]; val vp = image.planes[2]
        yBuf = copy(yp.buffer, yBuf); uBuf = copy(up.buffer, uBuf); vBuf = copy(vp.buffer, vBuf)
        val yRow = yp.rowStride; val uvRow = up.rowStride; val uvPixel = up.pixelStride
        executor.execute {
            try {
                if (closed) { busy.set(false); return@execute }
                val bmp = toUprightBitmap(w, h, yRow, uvRow, uvPixel, rotation)
                inflightRotation = rotation
                landmarker?.detectAsync(BitmapImageBuilder(bmp).build(), timestampMs) ?: busy.set(false)
            } catch (e: Exception) {
                Log.w(TAG, "detect failed", e); busy.set(false)
            }
        }
    }

    fun close() {
        closed = true
        executor.execute { try { landmarker?.close() } catch (_: Exception) {}; landmarker = null }
        executor.shutdown()
    }

    private fun copy(src: java.nio.ByteBuffer, reuse: ByteArray): ByteArray {
        src.rewind()
        val out = if (reuse.size == src.remaining()) reuse else ByteArray(src.remaining())
        src.get(out)
        return out
    }

    /** YUV → ARGB at half resolution, turned [rotation]° clockwise so the hand is upright. */
    private fun toUprightBitmap(w: Int, h: Int, yRow: Int, uvRow: Int, uvPixel: Int, rotation: Int): Bitmap {
        val w0 = w / 2; val h0 = h / 2
        val sideways = rotation == 90 || rotation == 270
        val bw = if (sideways) h0 else w0
        val bh = if (sideways) w0 else h0
        if (pixels.size != bw * bh) pixels = IntArray(bw * bh)
        var bmp = bitmap
        if (bmp == null || bmp.width != bw || bmp.height != bh) { bmp = Bitmap.createBitmap(bw, bh, Bitmap.Config.ARGB_8888); bitmap = bmp }
        var i = 0
        for (bv in 0 until bh) {
            for (bu in 0 until bw) {
                val sx: Int; val sy: Int
                when (rotation) {
                    90 -> { sx = bv; sy = h0 - 1 - bu }
                    180 -> { sx = w0 - 1 - bu; sy = h0 - 1 - bv }
                    270 -> { sx = w0 - 1 - bv; sy = bu }
                    else -> { sx = bu; sy = bv }
                }
                val col = sx * 2; val row = sy * 2
                val yv = (yBuf[row * yRow + col].toInt() and 0xFF).toFloat()
                val uvIdx = (row / 2) * uvRow + (col / 2) * uvPixel
                val u = ((if (uvIdx < uBuf.size) uBuf[uvIdx].toInt() else 128) and 0xFF) - 128
                val v = ((if (uvIdx < vBuf.size) vBuf[uvIdx].toInt() else 128) and 0xFF) - 128
                val r = (yv + 1.402f * v).toInt().coerceIn(0, 255)
                val g = (yv - 0.344f * u - 0.714f * v).toInt().coerceIn(0, 255)
                val b = (yv + 1.772f * u).toInt().coerceIn(0, 255)
                pixels[i++] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
            }
        }
        bmp.setPixels(pixels, 0, bw, 0, 0, bw, bh)
        return bmp
    }

    /** Upright-bitmap normalised point → original camera image normalised point. */
    private fun toImageNormalized(u: Float, v: Float, rotation: Int): Pair<Float, Float> = when (rotation) {
        90 -> v to 1f - u
        180 -> 1f - u to 1f - v
        270 -> 1f - v to u
        else -> u to v
    }
}
