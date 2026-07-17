package com.l5rcm.companion.domain.rules

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StanceRuleTest {

    // armorTnDelta(stance, airRank, defenseRank, fullDefenseTotal)
    @Test fun armorTnDeltaPerStance() {
        assertEquals(0, StanceRules.armorTnDelta(Stance.ATTACK, 3, 2, null))
        assertEquals(-10, StanceRules.armorTnDelta(Stance.FULL_ATTACK, 3, 2, null))
        assertEquals(0, StanceRules.armorTnDelta(Stance.CENTER, 3, 2, null))
    }

    @Test fun defenseAddsAirRingPlusDefenseRank() {
        assertEquals(5, StanceRules.armorTnDelta(Stance.DEFENSE, 3, 2, null))
    }

    @Test fun fullDefenseAddsHalfTotalRoundedUp() {
        // Not yet rolled → no bonus; then ceil(21/2) = 11, ceil(20/2) = 10.
        assertEquals(0, StanceRules.armorTnDelta(Stance.FULL_DEFENSE, 0, 3, null))
        assertEquals(11, StanceRules.armorTnDelta(Stance.FULL_DEFENSE, 0, 3, 21))
        assertEquals(10, StanceRules.armorTnDelta(Stance.FULL_DEFENSE, 0, 3, 20))
    }

    @Test fun onlyFullAttackBoostsTheAttackPool() {
        assertEquals(2 to 1, StanceRules.attackPoolBonus(Stance.FULL_ATTACK))
        for (s in listOf(Stance.ATTACK, Stance.DEFENSE, Stance.FULL_DEFENSE, Stance.CENTER)) {
            assertEquals(0 to 0, StanceRules.attackPoolBonus(s))
        }
    }

    @Test fun defensiveStancesRequireDefenseRankOne() {
        assertFalse(StanceRules.canUse(Stance.DEFENSE, defenseRank = 0))
        assertFalse(StanceRules.canUse(Stance.FULL_DEFENSE, defenseRank = 0))
        assertTrue(StanceRules.canUse(Stance.DEFENSE, defenseRank = 1))
        assertTrue(StanceRules.canUse(Stance.FULL_DEFENSE, defenseRank = 3))
        // Non-defensive stances are always allowed, even with no Defense skill.
        assertTrue(StanceRules.canUse(Stance.ATTACK, defenseRank = 0))
        assertTrue(StanceRules.canUse(Stance.FULL_ATTACK, defenseRank = 0))
        assertTrue(StanceRules.canUse(Stance.CENTER, defenseRank = 0))
    }
}
