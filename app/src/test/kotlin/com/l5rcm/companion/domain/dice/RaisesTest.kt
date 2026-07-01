package com.l5rcm.companion.domain.dice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RaisesTest {

    @Test fun calledRaisesAddFivePerRaise() {
        assertEquals(25, Raises.effectiveTn(baseTn = 15, calledRaises = 2))
    }

    @Test fun freeRaisesCanLowerTheTn() {
        assertEquals(10, Raises.effectiveTn(baseTn = 15, freeRaisesToTn = 1))
    }

    @Test fun calledAndFreeRaisesCombine() {
        // +5 from one called Raise, −5 from one free Raise spent on TN.
        assertEquals(15, Raises.effectiveTn(baseTn = 15, calledRaises = 1, freeRaisesToTn = 1))
    }

    @Test fun maxCalledRaisesIsVoidRing() {
        assertEquals(2, Raises.maxCalledRaises(voidRank = 2))
    }

    @Test fun unskilledAllowsNoCalledRaises() {
        assertEquals(0, Raises.maxCalledRaises(voidRank = 3, unskilled = true))
    }

    @Test fun meetingRaisedTnSucceeds() {
        val tn = Raises.effectiveTn(baseTn = 15, calledRaises = 1) // 20
        assertTrue(Raises.isSuccess(total = 20, effectiveTn = tn))
    }

    @Test fun meetingBaseButNotRaisedTnFails() {
        // Total meets the original TN (15) but not the raised TN (20) → failure (p. 82).
        val tn = Raises.effectiveTn(baseTn = 15, calledRaises = 1)
        assertFalse(Raises.isSuccess(total = 17, effectiveTn = tn))
    }
}
