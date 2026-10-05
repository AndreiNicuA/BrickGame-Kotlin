package com.brickgame.tetris.input

import kotlin.math.abs

/** Game actions produced by swipe controls. */
enum class SwipeAction { LEFT, RIGHT, SOFT_DROP, HARD_DROP, ROTATE, HOLD }

/**
 * Turns one finger gesture into game actions. Pure Kotlin (no Android types) so it is unit-tested.
 *
 * - Drag sideways: one LEFT/RIGHT per [stepPx] of travel (the piece follows the finger).
 * - Drag down slowly: one SOFT_DROP per [stepPx].
 * - Flick down (release faster than [flickPxPerSec]): HARD_DROP.
 * - Swipe up (fast, at least one step): HOLD.
 * - Tap (never left the [slopPx] radius): ROTATE.
 *
 * The first movement past [slopPx] locks the gesture to one axis, so a sideways drag can't
 * accidentally drop the piece and a drop can't nudge it sideways.
 */
class SwipeInterpreter(
    private val stepPx: Float,
    private val slopPx: Float,
    private val flickPxPerSec: Float
) {
    private enum class Axis { HORIZONTAL, VERTICAL }

    private var axis: Axis? = null
    private var totalX = 0f
    private var totalY = 0f
    private var accX = 0f
    private var accY = 0f

    /** Finger touched down. */
    fun down() {
        axis = null
        totalX = 0f; totalY = 0f
        accX = 0f; accY = 0f
    }

    /** Finger moved by ([dx], [dy]) px since the last call. Returns the actions to apply now. */
    fun drag(dx: Float, dy: Float): List<SwipeAction> {
        totalX += dx; totalY += dy
        if (axis == null) {
            if (abs(totalX) < slopPx && abs(totalY) < slopPx) return emptyList()
            axis = if (abs(totalX) >= abs(totalY)) Axis.HORIZONTAL else Axis.VERTICAL
            // Count the travel that happened before the axis was decided
            accX = totalX; accY = totalY
        } else {
            accX += dx; accY += dy
        }
        val out = ArrayList<SwipeAction>(2)
        when (axis) {
            Axis.HORIZONTAL -> {
                while (accX >= stepPx) { out += SwipeAction.RIGHT; accX -= stepPx }
                while (accX <= -stepPx) { out += SwipeAction.LEFT; accX += stepPx }
            }
            Axis.VERTICAL -> {
                while (accY >= stepPx) { out += SwipeAction.SOFT_DROP; accY -= stepPx }
                if (accY < 0f && totalY > 0f) accY = 0f  // moving back up doesn't "bank" drops
            }
            null -> {}
        }
        return out
    }

    /** Finger lifted with vertical velocity [velocityY] px/s (positive = downward). */
    fun up(velocityY: Float): SwipeAction? = when (axis) {
        null -> SwipeAction.ROTATE
        Axis.VERTICAL -> when {
            velocityY >= flickPxPerSec && totalY > 0f -> SwipeAction.HARD_DROP
            velocityY <= -flickPxPerSec && totalY <= -stepPx -> SwipeAction.HOLD
            else -> null
        }
        Axis.HORIZONTAL -> null
    }
}
