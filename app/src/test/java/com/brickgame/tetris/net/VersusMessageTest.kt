package com.brickgame.tetris.net

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VersusMessageTest {

    private fun roundTrip(m: VersusMessage) = assertEquals(m, VersusMessage.parse(m.encode()))

    @Test fun `every message survives encode and parse`() {
        roundTrip(VersusMessage.Hello("Andrei"))
        roundTrip(VersusMessage.Go("THREE_D"))
        roundTrip(VersusMessage.Attack(4))
        roundTrip(VersusMessage.Status(12840, 42, 7))
        roundTrip(VersusMessage.Over)
        roundTrip(VersusMessage.Bye)
    }

    @Test fun `names can't break the format`() {
        assertEquals(VersusMessage.Hello("a b"), VersusMessage.parse(VersusMessage.Hello("a|b").encode()))
    }

    @Test fun `unknown or broken lines are ignored`() {
        assertNull(VersusMessage.parse("PING|1"))
        assertNull(VersusMessage.parse("ATK|lots"))
        assertNull(VersusMessage.parse("ST|1|2"))
        assertNull(VersusMessage.parse(""))
    }

    @Test fun `attacks are capped`() {
        assertEquals(VersusMessage.Attack(20), VersusMessage.parse("ATK|999"))
    }

    @Test fun `attack tables`() {
        assertEquals(listOf(0, 0, 1, 2, 4), (0..4).map { VersusMessage.attackFor2DClear(it) })
        assertEquals(listOf(0, 1, 2, 3, 4, 4), (0..5).map { VersusMessage.attackFor3DClear(it) })
    }
}
