package com.l5rcm.companion.domain.dice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RollerTest {

    /** A [DieRoller] that returns pre-scripted d10 values in order — deterministic tests. */
    private class ScriptedRoller(vararg values: Int) : DieRoller {
        private val queue = ArrayDeque(values.toList())
        override fun d10(): Int = queue.removeFirst()
    }

    private fun roll(spec: RollSpec, vararg dice: Int): RollResult =
        Roller(ScriptedRoller(*dice)).roll(spec)

    @Test fun explodesOnceThenStops() {
        // 10 then 3 → one die worth 13 (rulebook p. 80 example).
        val r = roll(RollSpec(rolled = 1, kept = 1), 10, 3)
        assertEquals(listOf(13), r.dice)
        assertEquals(13, r.total)
    }

    @Test fun explosionChainsIndefinitely() {
        // 10, 10, 7 → 27 (rulebook p. 80 example).
        val r = roll(RollSpec(rolled = 1, kept = 1), 10, 10, 7)
        assertEquals(27, r.total)
    }

    @Test fun noExplodeThresholdConsumesOneDie() {
        // explodeOn = NO_EXPLODE: a 10 does not re-roll, so the second value is never drawn.
        val r = roll(RollSpec(rolled = 1, kept = 1, explodeOn = RollSpec.NO_EXPLODE), 10, 99)
        assertEquals(10, r.total)
    }

    @Test fun unskilledNeverExplodes() {
        val r = roll(RollSpec(rolled = 1, kept = 1, unskilled = true), 10, 99)
        assertEquals(10, r.total)
    }

    @Test fun emphasisRerollsAFirstRollOne() {
        // 1 is re-rolled into 7 (p. 136).
        val r = roll(RollSpec(rolled = 1, kept = 1, emphasis = true), 1, 7)
        assertEquals(7, r.total)
    }

    @Test fun emphasisRerollsOnlyOnce() {
        // a second 1 is kept — the die that rolled a one the second time is not re-rolled.
        val r = roll(RollSpec(rolled = 1, kept = 1, emphasis = true), 1, 1)
        assertEquals(1, r.total)
    }

    @Test fun emphasisRerollIntoTenStillExplodes() {
        // 1 → re-rolled to 10 → explodes, adding 4 → 14.
        val r = roll(RollSpec(rolled = 1, kept = 1, emphasis = true), 1, 10, 4)
        assertEquals(14, r.total)
    }

    @Test fun keepsHighestAndAddsBonus() {
        // 4k2+3 with no explosions: keep 8 and 5 → 13, +3 → 16.
        val r = roll(RollSpec(rolled = 4, kept = 2, bonus = 3), 3, 8, 5, 2)
        assertEquals(listOf(8, 5), r.kept)
        assertEquals(16, r.total)
    }

    @Test fun rollsTheTenDiceNormalizedPool() {
        // 12k4 normalizes to 10k5: exactly ten dice rolled, five kept (no 10s → no explosions).
        val r = roll(RollSpec(rolled = 12, kept = 4), 1, 2, 3, 4, 5, 6, 7, 8, 9, 9)
        assertEquals(NormalizedPool(rolled = 10, kept = 5, bonus = 0), r.normalized)
        assertEquals(10, r.dice.size)
        assertEquals(5, r.kept.size)
    }

    @Test fun exposesExplodingChainAndKeptFlagPerDie() {
        // 3k1: dice 10→4 (=14, kept), 6 (discarded), 2 (discarded).
        val r = roll(RollSpec(rolled = 3, kept = 1), 10, 4, 6, 2)
        assertEquals(3, r.results.size)

        val exploded = r.results[0]
        assertEquals(listOf(10, 4), exploded.rolls)
        assertEquals(14, exploded.value)
        assertTrue(exploded.exploded)
        assertTrue(exploded.kept)

        val plain = r.results[1]
        assertEquals(listOf(6), plain.rolls)
        assertFalse(plain.exploded)
        assertFalse(plain.kept)

        assertEquals(1, r.results.count { it.kept })
        assertEquals(14, r.total)
    }

    @Test fun keepsExactlyKeptDiceOnTies() {
        // Two dice tie at 7; keep only one (roll order breaks the tie deterministically).
        val r = roll(RollSpec(rolled = 3, kept = 1), 7, 7, 3)
        assertEquals(1, r.results.count { it.kept })
        assertTrue(r.results[0].kept)
        assertFalse(r.results[1].kept)
    }
}
