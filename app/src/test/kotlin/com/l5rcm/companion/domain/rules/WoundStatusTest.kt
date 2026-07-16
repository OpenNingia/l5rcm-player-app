package com.l5rcm.companion.domain.rules

import com.l5rcm.companion.domain.model.WoundLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WoundStatusTest {

    // Earth 3 → Healthy holds 15 (3×5), each further rank 6 (3×2). Cumulative thresholds:
    // Healthy 15, Nicked 21, Grazed 27, Hurt 33, Injured 39, Crippled 45, Down 51, Out 57.
    private val names = listOf(
        "Healthy", "Nicked", "Grazed", "Hurt", "Injured", "Crippled", "Down", "Out",
    )
    private val penalties = listOf(0, 3, 5, 10, 15, 20, 40) // index 0..6; Out has none

    private val levels: List<WoundLevel> = names.mapIndexed { i, name ->
        val threshold = 15 + (i * 6) // 15, 21, 27, ...
        WoundLevel(name = name, threshold = threshold, penalty = penalties.getOrNull(i))
    }

    @Test fun zeroWoundsIsHealthy() {
        val s = woundStatus(0, levels)
        assertEquals(0, s.levelIndex)
        assertEquals("Healthy", s.levelName)
        assertEquals(0, s.penalty)
        assertFalse(s.isOut)
    }

    @Test fun stillHealthyAtExactCapacity() {
        // RAW: a character stays at a rank *until* his wounds exceed its capacity.
        assertEquals("Healthy", woundStatus(15, levels).levelName)
    }

    @Test fun oneOverHealthyDropsToNicked() {
        val s = woundStatus(16, levels)
        assertEquals("Nicked", s.levelName)
        assertEquals(3, s.penalty)
    }

    @Test fun grazedBoundaries() {
        assertEquals("Nicked", woundStatus(21, levels).levelName)
        assertEquals("Grazed", woundStatus(22, levels).levelName)
        assertEquals(5, woundStatus(22, levels).penalty)
    }

    @Test fun downIsHighestPenalizedRank() {
        val s = woundStatus(51, levels)
        assertEquals("Down", s.levelName)
        assertEquals(40, s.penalty)
        assertFalse(s.isOut)
    }

    @Test fun beyondDownIsOut() {
        val s = woundStatus(52, levels)
        assertEquals("Out", s.levelName)
        assertNull(s.penalty)
        assertTrue(s.isOut)
    }

    @Test fun atMaxWoundsIsOut() {
        assertTrue(woundStatus(57, levels).isOut)
    }

    @Test fun emptyLevelsFallBackToHealthy() {
        val s = woundStatus(10, emptyList())
        assertEquals("Healthy", s.levelName)
        assertEquals(0, s.penalty)
        assertFalse(s.isOut)
    }
}
