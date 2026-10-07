package com.brickgame.tetris.gl

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.cos
import kotlin.math.sin

class HandGestureTest {

    /**
     * A hand at (cx, cy), 200 px from wrist to middle knuckle, rotated [deg] on screen, with
     * thumb and index tips [gap] px apart.
     */
    private fun hand(cx: Float, cy: Float, gap: Float, deg: Float = 0f): FloatArray {
        val p = FloatArray(42)
        fun set(i: Int, x: Float, y: Float) {
            val r = Math.toRadians(deg.toDouble())
            val rx = (x * cos(r) - y * sin(r)).toFloat(); val ry = (x * sin(r) + y * cos(r)).toFloat()
            p[2 * i] = cx + rx; p[2 * i + 1] = cy + ry
        }
        set(HandLandmarks.WRIST, 0f, 200f)
        set(HandLandmarks.MIDDLE_MCP, 0f, 0f)
        set(HandLandmarks.INDEX_MCP, -40f, 0f)
        set(HandLandmarks.PINKY_MCP, 60f, 10f)
        set(HandLandmarks.THUMB_TIP, -gap / 2f, -60f)
        set(HandLandmarks.INDEX_TIP, gap / 2f, -60f)
        return p
    }

    private val g get() = HandGesture { 2000f }

    @Test fun `open hand does nothing, pinch grabs and drags`() {
        val h = g
        assertTrue(h.update(hand(500f, 1000f, gap = 150f), 0).isEmpty())
        assertFalse(h.pinching)
        val a = h.update(hand(500f, 1000f, gap = 20f), 50)
        assertTrue(h.pinching)
        assertTrue(a.single() is HandGesture.Action.Drag)
    }

    @Test fun `pinch has hysteresis`() {
        val h = g
        h.update(hand(500f, 1000f, gap = 20f), 0)
        h.update(hand(500f, 1000f, gap = 75f), 50)    // ratio 0.375: between on and off
        assertTrue(h.pinching)
        h.update(hand(500f, 1000f, gap = 120f), 100)  // 0.6: released
        assertFalse(h.pinching)
    }

    @Test fun `twisting the pinched hand spins once per step`() {
        val h = g
        h.update(hand(500f, 1000f, gap = 20f, deg = 0f), 0)
        assertTrue(h.update(hand(500f, 1000f, gap = 20f, deg = 30f), 300).none { it is HandGesture.Action.Spin })
        assertTrue(h.update(hand(500f, 1000f, gap = 20f, deg = 55f), 600).any { it is HandGesture.Action.Spin })
        assertTrue(h.update(hand(500f, 1000f, gap = 20f, deg = 70f), 900).none { it is HandGesture.Action.Spin })
    }

    @Test fun `quick move down drops once`() {
        val h = g
        h.update(hand(500f, 800f, gap = 20f), 0)
        val a = h.update(hand(500f, 1200f, gap = 20f), 150)  // 400 px in 150 ms > 16% of 2000
        assertEquals(listOf(HandGesture.Action.Drop), a)
        assertTrue(h.update(hand(500f, 1600f, gap = 20f), 250).isEmpty())  // no second drop, no drag
    }

    @Test fun `slow move down just drags`() {
        val h = g
        h.update(hand(500f, 800f, gap = 20f), 0)
        h.update(hand(500f, 900f, gap = 20f), 300)
        val a = h.update(hand(500f, 1000f, gap = 20f), 600)
        assertTrue(a.single() is HandGesture.Action.Drag)
    }

    @Test fun `losing the hand releases`() {
        val h = g
        h.update(hand(500f, 1000f, gap = 20f), 0)
        h.update(null, 200)
        assertTrue(h.pinching)
        h.update(null, 500)
        assertFalse(h.pinching)
    }
}
