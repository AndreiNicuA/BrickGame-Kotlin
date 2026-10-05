package com.brickgame.tetris.input

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.unit.dp

/**
 * Swipe controls for a pointerInput block: feeds every finger gesture through a
 * [SwipeInterpreter] and reports the resulting actions as they happen (moves fire mid-drag,
 * so the piece follows the finger; drops/holds/rotates fire on release).
 *
 * @param stepPx finger travel per board cell (≈ one cell width)
 */
suspend fun PointerInputScope.detectSwipeControls(stepPx: Float, onAction: (SwipeAction) -> Unit) {
    val interpreter = SwipeInterpreter(
        stepPx = stepPx,
        slopPx = viewConfiguration.touchSlop,
        flickPxPerSec = 1100.dp.toPx()
    )
    val tracker = VelocityTracker()
    awaitEachGesture {
        val down = awaitFirstDown()
        interpreter.down()
        tracker.resetTracking()
        tracker.addPosition(down.uptimeMillis, down.position)
        down.consume()
        while (true) {
            val event = awaitPointerEvent()
            val change = event.changes.firstOrNull { it.id == down.id } ?: break
            tracker.addPosition(change.uptimeMillis, change.position)
            if (!change.pressed) {
                interpreter.up(tracker.calculateVelocity().y)?.let(onAction)
                change.consume()
                break
            }
            val delta = change.positionChange()
            if (delta != Offset.Zero) {
                interpreter.drag(delta.x, delta.y).forEach(onAction)
                change.consume()
            }
        }
    }
}
