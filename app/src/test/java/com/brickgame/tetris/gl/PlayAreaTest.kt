package com.brickgame.tetris.gl

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayAreaTest {

    // 2 m × 2 m square from (0,0) to (2,2)
    private val square = PlayArea(listOf(0f, 2f, 2f, 0f), listOf(0f, 0f, 2f, 2f))

    @Test fun `centre is inside, outside is outside`() {
        assertTrue(square.contains(1f, 1f))
        assertFalse(square.contains(3f, 1f))
        assertFalse(square.contains(-0.1f, 1f))
    }

    @Test fun `distance to the nearest wall`() {
        assertEquals(1f, square.distanceToEdge(1f, 1f), 1e-4f)
        assertEquals(0.3f, square.distanceToEdge(1.7f, 1f), 1e-4f)
        assertEquals(0.5f, square.distanceToEdge(2.5f, 1f), 1e-4f)
    }

    @Test fun `signed distance is negative outside`() {
        assertEquals(0.3f, square.signedDistance(0.3f, 1f), 1e-4f)
        assertEquals(-0.5f, square.signedDistance(1f, 2.5f), 1e-4f)
    }

    @Test fun `winding direction doesn't matter`() {
        val reversed = PlayArea(listOf(0f, 0f, 2f, 2f), listOf(0f, 2f, 2f, 0f))
        assertTrue(reversed.contains(1f, 1f))
        assertEquals(0.3f, reversed.signedDistance(1.7f, 1f), 1e-4f)
    }

    @Test fun `L-shaped room`() {
        // L: the notch at x>1, z>1 is outside
        val l = PlayArea(listOf(0f, 2f, 2f, 1f, 1f, 0f), listOf(0f, 0f, 1f, 1f, 2f, 2f))
        assertTrue(l.contains(0.5f, 1.5f))
        assertFalse(l.contains(1.5f, 1.5f))
        assertTrue(l.containsAll(listOf(0.2f to 0.2f, 1.8f to 0.5f)))
        assertFalse(l.containsAll(listOf(0.2f to 0.2f, 1.5f to 1.5f)))
    }

    @Test fun `fewer than three corners contains nothing`() {
        assertFalse(PlayArea(listOf(0f, 1f), listOf(0f, 1f)).contains(0.5f, 0.5f))
    }
}
