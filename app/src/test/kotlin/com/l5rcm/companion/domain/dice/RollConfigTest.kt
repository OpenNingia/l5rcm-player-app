package com.l5rcm.companion.domain.dice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The rules model from `design_handoff_l5r_dice_roller/README.md` "Effective values". These pin
 * the exact interactions (Void promotion, emphasis gating, unskilled forcing, TN raises) so the UI
 * can stay a thin renderer over [RollConfig.effective].
 */
class RollConfigTest {

    @Test fun skilledSkillRollExplodesAndAllowsRaises() {
        val e = RollConfig(rollType = RollType.SKILL, skilled = true, raises = 2, tn = 15).effective()
        assertTrue(e.effSkilled)
        assertFalse(e.promoted)
        assertEquals(2, e.effRaises)
        assertEquals(25, e.effTn) // 15 + 2*5
    }

    @Test fun unskilledSkillRollForcesRaisesToZero() {
        val e = RollConfig(rollType = RollType.SKILL, skilled = false, raises = 3, tn = 20).effective()
        assertFalse(e.effSkilled)
        assertEquals(0, e.effRaises)
        assertEquals(20, e.effTn) // raises forced to 0 → no +5s
        assertTrue(e.toSpec().unskilled)
    }

    @Test fun nonSkillRollsAreAlwaysSkilled() {
        for (type in listOf(RollType.TRAIT, RollType.RING, RollType.SPELL)) {
            val e = RollConfig(rollType = type, skilled = false, raises = 1).effective()
            assertTrue("$type should be skilled", e.effSkilled)
            assertFalse(e.isSkill)
            assertEquals(1, e.effRaises)
            assertFalse(e.emphasis)
        }
    }

    @Test fun voidGrantsPlus1k1CappedAtTen() {
        val e = RollConfig(rolled = 6, kept = 3, voidSpent = true).effective()
        assertEquals(7, e.effRolled)
        assertEquals(4, e.effKept)

        val capped = RollConfig(rolled = 10, kept = 10, voidSpent = true).effective()
        assertEquals(10, capped.effRolled) // capped, Void's dice bonus is wasted
        assertEquals(10, capped.effKept)
    }

    @Test fun voidPromotesUnskilledSkillRoll() {
        val e = RollConfig(rollType = RollType.SKILL, skilled = false, voidSpent = true, raises = 2).effective()
        assertTrue(e.promoted)
        assertTrue(e.effSkilled) // now explodes and Raises allowed
        assertEquals(2, e.effRaises)
        assertFalse(e.toSpec().unskilled)
    }

    @Test fun emphasisNeedsGenuineSkillNotPromotion() {
        // Promoted-by-Void roll does NOT grant emphasis even with the toggle on.
        val promoted = RollConfig(
            rollType = RollType.SKILL, skilled = false, voidSpent = true, emphasisToggle = true,
        ).effective()
        assertFalse(promoted.emphasis)

        // Genuinely skilled Skill roll with the toggle on does.
        val real = RollConfig(rollType = RollType.SKILL, skilled = true, emphasisToggle = true).effective()
        assertTrue(real.emphasis)
        assertTrue(real.toSpec().emphasis)

        // Emphasis never applies to non-skill rolls.
        val trait = RollConfig(rollType = RollType.TRAIT, emphasisToggle = true).effective()
        assertFalse(trait.emphasis)
    }

    @Test fun notationFormatsSign() {
        assertEquals("6k3", notation(6, 3, 0))
        assertEquals("7k4+5", notation(7, 4, 5))
        assertEquals("5k2-3", notation(5, 2, -3))
        assertEquals("7k4+5", RollConfig(rolled = 7, kept = 4, bonus = 5).effective().notation)
    }
}
