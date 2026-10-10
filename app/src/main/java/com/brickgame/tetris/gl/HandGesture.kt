package com.brickgame.tetris.gl

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot

/** MediaPipe's 21-point hand: the landmarks the gestures use. */
object HandLandmarks {
    const val WRIST = 0; const val THUMB_TIP = 4; const val INDEX_MCP = 5; const val INDEX_TIP = 8
    const val MIDDLE_MCP = 9; const val PINKY_MCP = 17

    /** Bones of the 21-point hand, as landmark index pairs (for drawing the skeleton). */
    val BONES = intArrayOf(
        0, 1, 1, 2, 2, 3, 3, 4,          // thumb
        0, 5, 5, 6, 6, 7, 7, 8,          // index
        5, 9, 9, 10, 10, 11, 11, 12,     // middle
        9, 13, 13, 14, 14, 15, 15, 16,   // ring
        13, 17, 17, 18, 18, 19, 19, 20,  // little finger
        0, 17                            // palm edge
    )
    /** Palm outline (for the filled "mesh" look). */
    val PALM = intArrayOf(0, 1, 5, 9, 13, 17)
}

/**
 * One-Euro filter (Casiez et al.): smooths jitter when the hand is still, follows quickly when
 * it moves. One instance per coordinate.
 */
class OneEuro(private val minCutoff: Float = 2.0f, private val beta: Float = 0.03f, private val dCutoff: Float = 1f) {
    private var x = Float.NaN
    private var dx = 0f
    private var t = 0L
    fun reset() { x = Float.NaN; dx = 0f }
    fun filter(value: Float, timeMs: Long): Float {
        if (x.isNaN()) { x = value; t = timeMs; return value }
        val dt = ((timeMs - t).coerceAtLeast(1)) / 1000f
        t = timeMs
        val ad = alpha(dCutoff, dt)
        dx += ad * ((value - x) / dt - dx)
        val a = alpha(minCutoff + beta * abs(dx), dt)
        x += a * (value - x)
        return x
    }
    private fun alpha(cutoff: Float, dt: Float): Float {
        val tau = 1f / (2f * Math.PI.toFloat() * cutoff)
        return 1f / (1f + tau / dt)
    }
}

/**
 * "Hold your hand still on the surface": true once the palm has stayed within a small radius
 * for [holdMs]. [progress] runs 0..1 for the on-screen ring. Pure Kotlin, unit-tested.
 */
class HoldStill(private val holdMs: Long = 3000L, private val radiusFraction: Float = 0.05f) {
    private var startX = 0f; private var startY = 0f; private var since = -1L
    var progress = 0f
        private set
    /** Feed the palm centre (px) and the view width; returns true once, when the hold completes. */
    fun update(x: Float, y: Float, viewWidth: Float, timeMs: Long): Boolean {
        if (since < 0 || hypot(x - startX, y - startY) > viewWidth * radiusFraction) {
            startX = x; startY = y; since = timeMs; progress = 0f
            return false
        }
        progress = ((timeMs - since).toFloat() / holdMs).coerceIn(0f, 1f)
        if (progress >= 1f) { reset(); return true }
        return false
    }
    fun reset() { since = -1L; progress = 0f }
}

/**
 * Turns hand landmarks (screen pixels) into game actions. Pure Kotlin, unit-tested.
 *
 * - Pinch (thumb tip to index tip, relative to hand size) = grab; while held, the piece follows
 *   the pinch point.
 * - Twisting the pinched hand by ~50° spins the piece (again for every further ~50°).
 * - A quick downward move of the pinched hand drops the piece.
 * Hysteresis on the pinch, and losing the hand releases it.
 */
class HandGesture(private val viewHeight: () -> Float) {

    /** Pinch thresholds (thumb–index gap ÷ hand size); tuned per player by Hand setup. */
    var pinchOn = PINCH_ON
    var pinchOff = PINCH_OFF
    /** The latest thumb–index gap ÷ hand size (Hand setup measures with it). */
    var lastRatio = Float.NaN
        private set

    sealed class Action {
        /** Move the piece towards this screen point. */
        data class Drag(val x: Float, val y: Float) : Action()
        object Spin : Action()
        object Drop : Action()
    }

    companion object {
        const val PINCH_ON = 0.30f
        const val PINCH_OFF = 0.45f
        const val TWIST_DEG = 50f
        const val DROP_FRACTION = 0.16f
        const val DROP_WINDOW_MS = 260L
        const val LOST_MS = 700L
    }

    var pinching = false
        private set
    private var twistRef = 0f
    private var dropArmed = true
    private val trail = ArrayDeque<Pair<Long, Float>>()   // (time, pinch y)
    private var lastSeen = 0L

    /**
     * One hand observation. [xy] = 21 landmarks as (x, y) screen pixel pairs, or null when no
     * hand was found. Returns what to do (often nothing).
     */
    fun update(xy: FloatArray?, timeMs: Long): List<Action> {
        if (xy == null || xy.size < 42) {
            if (timeMs - lastSeen > LOST_MS) release()
            return emptyList()
        }
        lastSeen = timeMs
        fun px(i: Int) = xy[2 * i]
        fun py(i: Int) = xy[2 * i + 1]
        val size = hypot(px(HandLandmarks.MIDDLE_MCP) - px(HandLandmarks.WRIST), py(HandLandmarks.MIDDLE_MCP) - py(HandLandmarks.WRIST))
        if (size < 1f) return emptyList()
        val ratio = hypot(px(HandLandmarks.THUMB_TIP) - px(HandLandmarks.INDEX_TIP), py(HandLandmarks.THUMB_TIP) - py(HandLandmarks.INDEX_TIP)) / size
        val mx = (px(HandLandmarks.THUMB_TIP) + px(HandLandmarks.INDEX_TIP)) / 2f
        val my = (py(HandLandmarks.THUMB_TIP) + py(HandLandmarks.INDEX_TIP)) / 2f
        val angle = Math.toDegrees(atan2(
            (py(HandLandmarks.PINKY_MCP) - py(HandLandmarks.INDEX_MCP)).toDouble(),
            (px(HandLandmarks.PINKY_MCP) - px(HandLandmarks.INDEX_MCP)).toDouble())).toFloat()

        lastRatio = ratio
        val out = ArrayList<Action>()
        if (!pinching && ratio < pinchOn) {
            pinching = true; twistRef = angle; dropArmed = true; trail.clear()
        } else if (pinching && ratio > pinchOff) {
            release(); return out
        }
        if (!pinching) return out

        // Twist → spin
        var d = angle - twistRef
        while (d > 180f) d -= 360f
        while (d < -180f) d += 360f
        if (abs(d) >= TWIST_DEG) { out += Action.Spin; twistRef = angle }

        // Quick move down → drop (once per pinch)
        trail.addLast(timeMs to my)
        while (trail.isNotEmpty() && timeMs - trail.first().first > DROP_WINDOW_MS) trail.removeFirst()
        val fall = my - trail.minOf { it.second }
        if (dropArmed && fall > viewHeight() * DROP_FRACTION) {
            out += Action.Drop; dropArmed = false
        } else if (dropArmed) {
            out += Action.Drag(mx, my)
        }
        return out
    }

    fun release() { pinching = false; trail.clear() }
}
