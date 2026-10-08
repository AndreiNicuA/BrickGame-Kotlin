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

    @Test fun `losing the hand releases after a grace period`() {
        val h = g
        h.update(hand(500f, 1000f, gap = 20f), 0)
        h.update(null, 500)
        assertTrue(h.pinching)
        h.update(null, 800)
        assertFalse(h.pinching)
    }

    @Test fun `per-player thresholds`() {
        val h = g
        h.pinchOn = 0.6f; h.pinchOff = 0.8f
        h.update(hand(500f, 1000f, gap = 100f), 0)      // 0.5 < 0.6: pinched for this player
        assertTrue(h.pinching)
        assertEquals(0.5f, h.lastRatio, 1e-3f)
    }

    @Test fun `holding still completes after three seconds`() {
        val s = HoldStill(holdMs = 3000L)
        assertFalse(s.update(500f, 500f, 1000f, 0))
        assertFalse(s.update(510f, 505f, 1000f, 1500))
        assertEquals(0.5f, s.progress, 0.01f)
        assertTrue(s.update(505f, 500f, 1000f, 3000))
    }

    @Test fun `moving restarts the hold`() {
        val s = HoldStill(holdMs = 3000L)
        s.update(500f, 500f, 1000f, 0)
        s.update(700f, 500f, 1000f, 2000)              // moved 200 px > 5% of 1000
        assertEquals(0f, s.progress, 0.001f)
        assertFalse(s.update(700f, 500f, 1000f, 4000))
        assertTrue(s.update(700f, 500f, 1000f, 5000))
    }

    @Test fun `one-euro smooths jitter but follows real moves`() {
        val f = OneEuro()
        var v = 0f
        for (i in 0 until 30) v = f.filter(if (i % 2 == 0) 100f else 104f, i * 33L)
        assertTrue(v in 100.5f..103.5f)                 // jitter averaged out
        for (i in 30 until 60) v = f.filter(300f, i * 33L)
        assertEquals(300f, v, 5f)                       // a real move is followed
    }
}
