package com.brickgame.tetris.gl

import com.brickgame.tetris.gl.HandGesture.Action
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.cos
import kotlin.math.sin

class HandGestureTest {

    /**
     * A hand at (cx, cy), 200 px from wrist to middle knuckle, rotated [deg] on screen.
     * [gap] = thumb tip to index tip; [middleGap] = thumb tip to middle tip (null: middle finger
     * stretched out); [fist] curls every finger onto its knuckle.
     */
    private fun hand(cx: Float, cy: Float, gap: Float, deg: Float = 0f, middleGap: Float? = null, fist: Boolean = false): FloatArray {
        val p = FloatArray(42)
        fun set(i: Int, x: Float, y: Float) {
            val r = Math.toRadians(deg.toDouble())
            val rx = (x * cos(r) - y * sin(r)).toFloat(); val ry = (x * sin(r) + y * cos(r)).toFloat()
            p[2 * i] = cx + rx; p[2 * i + 1] = cy + ry
        }
        set(HandLandmarks.WRIST, 0f, 200f)
        set(HandLandmarks.INDEX_MCP, -40f, 0f)
        set(HandLandmarks.MIDDLE_MCP, 0f, 0f)
        set(HandLandmarks.RING_MCP, 30f, 5f)
        set(HandLandmarks.PINKY_MCP, 60f, 10f)
        val reach = if (fist) -40f else -170f
        set(HandLandmarks.RING_TIP, 30f, 5f + reach)
        set(HandLandmarks.PINKY_TIP, 60f, 10f + reach)
        if (fist) {
            set(HandLandmarks.INDEX_TIP, -40f, -40f); set(HandLandmarks.MIDDLE_TIP, 0f, -40f)
            set(HandLandmarks.THUMB_TIP, -80f, 40f)
        } else {
            // Thumb tip sits under the index tip; the middle tip is either far up or next to the thumb
            set(HandLandmarks.THUMB_TIP, -gap / 2f, -60f)
            set(HandLandmarks.INDEX_TIP, gap / 2f, -60f)
            if (middleGap == null) set(HandLandmarks.MIDDLE_TIP, 0f, -170f)
            else set(HandLandmarks.MIDDLE_TIP, -gap / 2f + middleGap, -60f)
        }
        return p
    }

    private val g get() = HandGesture { 2000f }

    @Test fun `open hand does nothing, pinch grabs, then drags`() {
        val h = g
        assertTrue(h.update(hand(500f, 1000f, gap = 150f), 0).isEmpty())
        assertFalse(h.pinching)
        val a = h.update(hand(500f, 1000f, gap = 20f), 50)
        assertTrue(h.pinching)
        assertTrue(a.single() is Action.Grab)
        assertTrue(h.update(hand(520f, 1000f, gap = 20f), 100).single() is Action.Drag)
        assertEquals(HandGesture.Shape.PINCH, h.shape)
    }

    @Test fun `opening the fingers releases`() {
        val h = g
        h.update(hand(500f, 1000f, gap = 20f), 0)
        assertEquals(listOf(Action.Release), h.update(hand(500f, 1000f, gap = 150f), 50))
        assertFalse(h.pinching)
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
        assertTrue(h.update(hand(500f, 1000f, gap = 20f, deg = 30f), 300).none { it is Action.Spin })
        assertTrue(h.update(hand(500f, 1000f, gap = 20f, deg = 55f), 600).any { it is Action.Spin })
        assertTrue(h.update(hand(500f, 1000f, gap = 20f, deg = 70f), 900).none { it is Action.Spin })
    }

    @Test fun `quick move down drops once`() {
        val h = g
        h.update(hand(500f, 800f, gap = 20f), 0)
        val a = h.update(hand(500f, 1200f, gap = 20f), 150)  // 400 px in 150 ms > 16% of 2000
        assertEquals(listOf(Action.Drop), a)
        assertTrue(h.update(hand(500f, 1600f, gap = 20f), 250).isEmpty())  // no second drop, no drag
    }

    @Test fun `slow move down just drags`() {
        val h = g
        h.update(hand(500f, 800f, gap = 20f), 0)
        h.update(hand(500f, 900f, gap = 20f), 300)
        val a = h.update(hand(500f, 1000f, gap = 20f), 600)
        assertTrue(a.single() is Action.Drag)
    }

    @Test fun `thumb and middle finger tilt once per pinch`() {
        val h = g
        val a = h.update(hand(500f, 1000f, gap = 150f, middleGap = 20f), 0)
        assertEquals(listOf(Action.Tilt), a)
        assertFalse(h.pinching)
        assertEquals(HandGesture.Shape.TILT, h.shape)
        assertTrue(h.update(hand(500f, 1000f, gap = 150f, middleGap = 20f), 100).isEmpty())   // still held: no repeat
        h.update(hand(500f, 1000f, gap = 150f), 200)                                          // let go
        assertEquals(listOf(Action.Tilt), h.update(hand(500f, 1000f, gap = 150f, middleGap = 20f), 300))
    }

    @Test fun `a fist soft-drops while held, after a short moment`() {
        val h = g
        assertTrue(h.update(hand(500f, 1000f, gap = 0f, fist = true), 0).isEmpty())       // just closed
        assertEquals(listOf(Action.SoftDrop), h.update(hand(500f, 1000f, gap = 0f, fist = true), 300))
        assertEquals(HandGesture.Shape.FIST, h.shape)
        assertTrue(h.update(hand(500f, 1000f, gap = 0f, fist = true), 350).isEmpty())     // too soon for the next
        assertEquals(listOf(Action.SoftDrop), h.update(hand(500f, 1000f, gap = 0f, fist = true), 450))
        assertTrue(h.update(hand(500f, 1000f, gap = 150f), 500).isEmpty())                // opened: stops
    }

    @Test fun `a fist lets go of a grabbed piece`() {
        val h = g
        h.update(hand(500f, 1000f, gap = 20f), 0)
        h.update(hand(500f, 1000f, gap = 0f, fist = true), 50)
        val a = h.update(hand(500f, 1000f, gap = 0f, fist = true), 400)
        assertEquals(listOf(Action.Release, Action.SoftDrop), a)
        assertFalse(h.pinching)
    }

    @Test fun `losing the hand releases after a grace period`() {
        val h = g
        h.update(hand(500f, 1000f, gap = 20f), 0)
        h.update(null, 500)
        assertTrue(h.pinching)
        assertEquals(listOf(Action.Release), h.update(null, 800))
        assertFalse(h.pinching)
    }

    @Test fun `per-player thresholds`() {
        val h = g
        h.pinchOn = 0.6f; h.pinchOff = 0.8f
        h.update(hand(500f, 1000f, gap = 100f), 0)      // 0.5 < 0.6: pinched for this player
        assertTrue(h.pinching)
        assertEquals(0.5f, h.lastRatio, 1e-3f)
    }

    @Test fun `stepper moves whole cells relative to the grab`() {
        val s = CellStepper(gain = 1f, deadZone = 0.25f)
        assertNull(s.target(1f, 1f))
        s.grab(pieceX = 3f, pieceZ = 2f, boardX = 10f, boardZ = 10f)   // fingers far from the piece: no jump
        assertArrayEquals(floatArrayOf(3f, 2f), s.target(10.4f, 10f), 1e-4f)
        assertArrayEquals(floatArrayOf(4f, 2f), s.target(10.8f, 10f), 1e-4f)
        assertArrayEquals(floatArrayOf(5f, 1f), s.target(12.1f, 8.9f), 1e-4f)
    }

    @Test fun `stepper ignores shaking around a cell edge`() {
        val s = CellStepper(gain = 1f, deadZone = 0.25f)
        s.grab(0f, 0f, 0f, 0f)
        assertEquals(1f, s.target(1.0f, 0f)!![0], 1e-4f)
        assertEquals(1f, s.target(0.4f, 0f)!![0], 1e-4f)      // back past half-way, but inside the dead zone
        assertEquals(1f, s.target(1.6f, 0f)!![0], 1e-4f)
        assertEquals(0f, s.target(0.2f, 0f)!![0], 1e-4f)      // clearly back
    }

    @Test fun `stepper gain gives finer control`() {
        val s = CellStepper(gain = 0.5f)
        s.grab(0f, 0f, 0f, 0f)
        assertEquals(0f, s.target(1.2f, 0f)!![0], 1e-4f)      // 1.2 cells of hand = 0.6 of piece
        assertEquals(1f, s.target(2f, 0f)!![0], 1e-4f)
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
