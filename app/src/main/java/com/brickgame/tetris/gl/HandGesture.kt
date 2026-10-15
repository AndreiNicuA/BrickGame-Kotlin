package com.brickgame.tetris.gl

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot

/** MediaPipe's 21-point hand: the landmarks the gestures use. */
object HandLandmarks {
    const val WRIST = 0; const val THUMB_TIP = 4; const val INDEX_MCP = 5; const val INDEX_TIP = 8
    const val MIDDLE_MCP = 9; const val MIDDLE_TIP = 12; const val RING_MCP = 13; const val RING_TIP = 16
    const val PINKY_MCP = 17; const val PINKY_TIP = 20

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
 * Hand shapes, chosen so the camera can tell them apart from a flat picture:
 * - Thumb + index pinch = grab ([Action.Grab] once, then [Action.Drag] while held, [Action.Release]
 *   when the fingers open). The renderer moves the piece by how far the pinch travels, so the
 *   piece doesn't jump to the fingers and can be placed cell by cell.
 * - Twisting the pinched hand by ~50° spins the piece (again for every further ~50°).
 * - A quick downward move of the pinched hand drops the piece.
 * - Thumb + middle finger pinch (index stays out) tilts the piece, once per pinch.
 * - A fist soft-drops the piece while it is held.
 * Hysteresis on the pinches, and losing the hand releases everything.
 */
class HandGesture(private val viewHeight: () -> Float) {

    /** Pinch thresholds (thumb–finger gap ÷ hand size); tuned per player by Hand setup. */
    var pinchOn = PINCH_ON
    var pinchOff = PINCH_OFF
    /** The latest thumb–index gap ÷ hand size (Hand setup measures with it). */
    var lastRatio = Float.NaN
        private set

    sealed class Action {
        /** Pinch started at this screen point. */
        data class Grab(val x: Float, val y: Float) : Action()
        /** The pinch is now here (move the piece by the distance from the grab). */
        data class Drag(val x: Float, val y: Float) : Action()
        object Release : Action()
        object Spin : Action()
        object Tilt : Action()
        object Drop : Action()
        object SoftDrop : Action()
    }

    /** What the hand is doing, for the on-screen hand. */
    enum class Shape { OPEN, PINCH, TILT, FIST }

    companion object {
        const val PINCH_ON = 0.30f
        const val PINCH_OFF = 0.45f
        const val TWIST_DEG = 50f
        const val DROP_FRACTION = 0.16f
        const val DROP_WINDOW_MS = 260L
        const val LOST_MS = 700L
        /** Every fingertip this close to its knuckle (÷ hand size) = a fist. */
        const val FIST_CURL = 0.5f
        /** A fist must be held this long before soft drop starts (a hand closing on the way to a pinch passes through it). */
        const val FIST_MS = 250L
        const val SOFT_DROP_EVERY_MS = 130L
    }

    var pinching = false
        private set
    var shape = Shape.OPEN
        private set
    private var tilting = false
    private var fistSince = -1L
    private var lastSoftDrop = 0L
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
            if (timeMs - lastSeen > LOST_MS) return releaseAll()
            return emptyList()
        }
        lastSeen = timeMs
        fun px(i: Int) = xy[2 * i]
        fun py(i: Int) = xy[2 * i + 1]
        fun dist(a: Int, b: Int) = hypot(px(a) - px(b), py(a) - py(b))
        val size = dist(HandLandmarks.MIDDLE_MCP, HandLandmarks.WRIST)
        if (size < 1f) return emptyList()
        val ratio = dist(HandLandmarks.THUMB_TIP, HandLandmarks.INDEX_TIP) / size
        val middleRatio = dist(HandLandmarks.THUMB_TIP, HandLandmarks.MIDDLE_TIP) / size
        lastRatio = ratio
        val out = ArrayList<Action>()

        // Fist: every fingertip curled onto its knuckle (the least curled one decides, so a pinch
        // with the other fingers folded, where the index stays fairly long, isn't a fist)
        val curl = maxOf(dist(HandLandmarks.INDEX_TIP, HandLandmarks.INDEX_MCP), dist(HandLandmarks.MIDDLE_TIP, HandLandmarks.MIDDLE_MCP),
            dist(HandLandmarks.RING_TIP, HandLandmarks.RING_MCP), dist(HandLandmarks.PINKY_TIP, HandLandmarks.PINKY_MCP)) / size
        if (curl < FIST_CURL) {
            if (fistSince < 0) fistSince = timeMs
            if (timeMs - fistSince >= FIST_MS) {
                if (pinching) { out += Action.Release; pinching = false; trail.clear() }
                tilting = false
                shape = Shape.FIST
                if (timeMs - lastSoftDrop >= SOFT_DROP_EVERY_MS) { out += Action.SoftDrop; lastSoftDrop = timeMs }
                return out
            }
        } else fistSince = -1L

        // Thumb + middle (index out): tilt once per pinch
        if (!pinching) {
            if (!tilting && middleRatio < pinchOn && ratio > pinchOff) { tilting = true; out += Action.Tilt }
            else if (tilting && middleRatio > pinchOff) tilting = false
        }

        // Thumb + index: grab
        val mx = (px(HandLandmarks.THUMB_TIP) + px(HandLandmarks.INDEX_TIP)) / 2f
        val my = (py(HandLandmarks.THUMB_TIP) + py(HandLandmarks.INDEX_TIP)) / 2f
        val angle = Math.toDegrees(atan2(
            (py(HandLandmarks.PINKY_MCP) - py(HandLandmarks.INDEX_MCP)).toDouble(),
            (px(HandLandmarks.PINKY_MCP) - px(HandLandmarks.INDEX_MCP)).toDouble())).toFloat()
        if (!pinching && !tilting && ratio < pinchOn) {
            pinching = true; twistRef = angle; dropArmed = true; trail.clear()
            out += Action.Grab(mx, my)
        } else if (pinching && ratio > pinchOff) {
            pinching = false; trail.clear(); out += Action.Release
        }
        shape = when { pinching -> Shape.PINCH; tilting -> Shape.TILT; else -> Shape.OPEN }
        if (!pinching) return out

        // Twist → spin
        var d = angle - twistRef
        while (d > 180f) d -= 360f
        while (d < -180f) d += 360f
        if (abs(d) >= TWIST_DEG) { out += Action.Spin; twistRef = angle }

        // Quick move down → drop (once per pinch); otherwise the piece follows
        trail.addLast(timeMs to my)
        while (trail.isNotEmpty() && timeMs - trail.first().first > DROP_WINDOW_MS) trail.removeFirst()
        val fall = my - trail.minOf { it.second }
        if (dropArmed && fall > viewHeight() * DROP_FRACTION) {
            out += Action.Drop; dropArmed = false
        } else if (dropArmed && out.none { it is Action.Grab }) {
            out += Action.Drag(mx, my)
        }
        return out
    }

    /** Hand lost: let go of everything. */
    private fun releaseAll(): List<Action> {
        val wasPinching = pinching
        pinching = false; tilting = false; fistSince = -1L; trail.clear(); shape = Shape.OPEN
        return if (wasPinching) listOf(Action.Release) else emptyList()
    }

    fun release() { releaseAll() }
}

/**
 * Moves the piece by how far the pinch travelled on the board, in whole cells, with a dead zone
 * so a shaky hand doesn't flick it back and forth between two cells. Pure Kotlin, unit-tested.
 *
 * [gain] < 1 means the hand travels a bit further than the piece: finer control.
 */
class CellStepper(private val gain: Float = 0.75f, private val deadZone: Float = 0.25f) {
    private var startX = 0f; private var startZ = 0f
    private var grabX = Float.NaN; private var grabZ = 0f
    private var cellsX = 0; private var cellsZ = 0

    /** Start a grab: [pieceX]/[pieceZ] = the piece centre now, [boardX]/[boardZ] = the board point under the fingers. */
    fun grab(pieceX: Float, pieceZ: Float, boardX: Float, boardZ: Float) {
        startX = pieceX; startZ = pieceZ; grabX = boardX; grabZ = boardZ; cellsX = 0; cellsZ = 0
    }
    val grabbing get() = !grabX.isNaN()
    fun release() { grabX = Float.NaN }

    /** The fingers are over board point (x, z): where the piece centre should go, or null if not grabbing. */
    fun target(boardX: Float, boardZ: Float): FloatArray? {
        if (!grabbing) return null
        cellsX = snap((boardX - grabX) * gain, cellsX)
        cellsZ = snap((boardZ - grabZ) * gain, cellsZ)
        return floatArrayOf(startX + cellsX, startZ + cellsZ)
    }

    /** Keep the current cell until the hand is clearly past half-way to the next one. */
    private fun snap(v: Float, held: Int): Int =
        if (abs(v - held) > 0.5f + deadZone) kotlin.math.round(v).toInt() else held
}
