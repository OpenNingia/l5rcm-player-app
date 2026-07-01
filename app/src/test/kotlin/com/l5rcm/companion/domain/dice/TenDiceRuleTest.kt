package com.l5rcm.companion.domain.dice

import org.junit.Assert.assertEquals
import org.junit.Test

class TenDiceRuleTest {

    private fun norm(rolled: Int, kept: Int, bonus: Int = 0, voidBoost: Boolean = false) =
        TenDiceRule.normalize(RollSpec(rolled = rolled, kept = kept, bonus = bonus, voidBoost = voidBoost))

    // --- The four worked examples from the manual (rulebook p. 80). ---

    @Test fun example12k4becomes10k5() {
        // two extra rolled dice → one extra kept die.
        assertEquals(NormalizedPool(rolled = 10, kept = 5, bonus = 0), norm(12, 4))
    }

    @Test fun example13k9becomes10k10plus2() {
        // two extra rolled → one kept (9→10); the odd leftover rolled die → +2.
        assertEquals(NormalizedPool(rolled = 10, kept = 10, bonus = 2), norm(13, 9))
    }

    @Test fun example10k12becomes10k10plus4() {
        // each extra kept die above ten → +2.
        assertEquals(NormalizedPool(rolled = 10, kept = 10, bonus = 4), norm(10, 12))
    }

    @Test fun example14k12becomes10k10plus12() {
        // four extra rolled → +8, two extra kept → +4, total +12.
        assertEquals(NormalizedPool(rolled = 10, kept = 10, bonus = 12), norm(14, 12))
    }

    // --- Dice-penalty rule: can never keep more than rolled (p. 80). ---

    @Test fun keptCannotExceedRolled() {
        assertEquals(NormalizedPool(rolled = 3, kept = 3, bonus = 0), norm(3, 5))
    }

    // --- Void boost: +1k1 before normalization (p. 81). ---

    @Test fun voidBoostAddsOneKeepOne() {
        assertEquals(NormalizedPool(rolled = 6, kept = 3, bonus = 0), norm(5, 2, voidBoost = true))
    }

    @Test fun voidBoostThenTenDiceRule() {
        // 10k4 +void → 11k5 → 10k5 with the single extra rolled die becoming +2.
        assertEquals(NormalizedPool(rolled = 10, kept = 5, bonus = 2), norm(10, 4, voidBoost = true))
    }

    // --- Boundaries. ---

    @Test fun exactlyTenByTenIsUnchanged() {
        assertEquals(NormalizedPool(rolled = 10, kept = 10, bonus = 0), norm(10, 10))
    }

    @Test fun elevenRolledOneKeptGivesPlusTwo() {
        // the single extra rolled die can't form a kept die → +2.
        assertEquals(NormalizedPool(rolled = 10, kept = 1, bonus = 2), norm(11, 1))
    }

    @Test fun tenKeptElevenGivesPlusTwo() {
        assertEquals(NormalizedPool(rolled = 10, kept = 10, bonus = 2), norm(10, 11))
    }

    @Test fun existingBonusIsPreserved() {
        assertEquals(NormalizedPool(rolled = 10, kept = 5, bonus = 3), norm(12, 4, bonus = 3))
    }
}
