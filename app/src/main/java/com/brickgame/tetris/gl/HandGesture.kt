package com.brickgame.tetris.gl

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot

/** MediaPipe's 21-point hand: the landmarks the gestures use. */
object HandLandmarks {
    const val WRIST = 0; const val THUMB_TIP = 4; const val INDEX_MCP = 5; const val INDEX_TIP = 8
    const val MIDDLE_MCP = 9; const val PINKY_MCP = 17
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
        const val LOST_MS = 400L
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

        val out = ArrayList<Action>()
        if (!pinching && ratio < PINCH_ON) {
            pinching = true; twistRef = angle; dropArmed = true; trail.clear()
        } else if (pinching && ratio > PINCH_OFF) {
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
