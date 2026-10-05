package com.brickgame.tetris.input

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SwipeInterpreterTest {

    private lateinit var s: SwipeInterpreter

    @Before
    fun setUp() {
        s = SwipeInterpreter(stepPx = 50f, slopPx = 10f, flickPxPerSec = 1000f)
        s.down()
    }

    private fun dragInSteps(dx: Float, dy: Float, steps: Int = 10): List<SwipeAction> =
        (1..steps).flatMap { s.drag(dx / steps, dy / steps) }

    @Test fun `tap rotates`() {
        assertTrue(s.drag(3f, -2f).isEmpty())
        assertEquals(SwipeAction.ROTATE, s.up(0f))
    }

    @Test fun `sideways drag moves one cell per step`() {
        assertEquals(List(3) { SwipeAction.RIGHT }, dragInSteps(160f, 0f))
        assertNull(s.up(0f))
    }

    @Test fun `left drag moves left`() {
        assertEquals(List(2) { SwipeAction.LEFT }, dragInSteps(-110f, 5f))
    }

    @Test fun `direction change mid-drag moves back`() {
        dragInSteps(100f, 0f)
        assertEquals(List(2) { SwipeAction.LEFT }, dragInSteps(-100f, 0f))
    }

    @Test fun `horizontal drag never drops even if finger drifts down`() {
        val actions = dragInSteps(120f, 0f) + dragInSteps(0f, 200f)
        assertTrue(SwipeAction.SOFT_DROP !in actions)
        assertNull(s.up(5000f))
    }

    @Test fun `slow drag down soft drops per step`() {
        assertEquals(List(2) { SwipeAction.SOFT_DROP }, dragInSteps(0f, 120f))
        assertNull(s.up(200f))
    }

    @Test fun `flick down hard drops`() {
        dragInSteps(0f, 40f, 2)
        assertEquals(SwipeAction.HARD_DROP, s.up(2500f))
    }

    @Test fun `fast swipe up holds`() {
        dragInSteps(0f, -80f)
        assertEquals(SwipeAction.HOLD, s.up(-2000f))
    }

    @Test fun `short or slow swipe up does nothing`() {
        dragInSteps(0f, -30f)
        assertNull(s.up(-2000f))
        s.down()
        dragInSteps(0f, -80f)
        assertNull(s.up(-300f))
    }

    @Test fun `each gesture starts fresh`() {
        dragInSteps(130f, 0f)
        s.up(0f)
        s.down()
        assertEquals(SwipeAction.ROTATE, s.up(0f))
    }
}
