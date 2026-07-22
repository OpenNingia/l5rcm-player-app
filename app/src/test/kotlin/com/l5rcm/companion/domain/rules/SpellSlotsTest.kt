package com.l5rcm.companion.domain.rules

import com.l5rcm.companion.domain.model.Ring
import org.junit.Assert.assertEquals
import org.junit.Test

class SpellSlotsTest {

    @Test fun capacityIsElementRingPlusFlexibleVoidPool() {
        val slots = spellSlots(
            elementRanks = mapOf(Ring.EARTH to 2, Ring.AIR to 3, Ring.WATER to 2, Ring.FIRE to 3),
            voidRank = 2,
            spent = emptyMap(),
        )
        // Order: the four element rings (Earth, Air, Water, Fire) then the flexible Void pool.
        assertEquals(
            listOf(
                SpellSlot(Ring.EARTH, current = 2, max = 2),
                SpellSlot(Ring.AIR, current = 3, max = 3),
                SpellSlot(Ring.WATER, current = 2, max = 2),
                SpellSlot(Ring.FIRE, current = 3, max = 3),
                SpellSlot(Ring.VOID, current = 2, max = 2),
            ),
            slots,
        )
    }

    @Test fun currentIsMaxMinusSpent() {
        val slots = spellSlots(
            elementRanks = mapOf(Ring.FIRE to 3),
            voidRank = 2,
            spent = mapOf(Ring.FIRE to 2, Ring.VOID to 1),
        )
        assertEquals(SpellSlot(Ring.FIRE, current = 1, max = 3), slots.first { it.ring == Ring.FIRE })
        assertEquals(SpellSlot(Ring.VOID, current = 1, max = 2), slots.first { it.ring == Ring.VOID })
    }

    @Test fun spentIsClampedIntoRange() {
        // Over-spent (e.g. a stale row) never drives current below zero.
        val slots = spellSlots(
            elementRanks = mapOf(Ring.AIR to 2),
            voidRank = 0,
            spent = mapOf(Ring.AIR to 5),
        )
        assertEquals(SpellSlot(Ring.AIR, current = 0, max = 2), slots.single())
    }

    @Test fun poolsWithNoCapacityAreOmitted() {
        // A shugenja with no Void Ring has no flexible pool; a zero element Ring drops out too.
        val slots = spellSlots(
            elementRanks = mapOf(Ring.EARTH to 0, Ring.FIRE to 2),
            voidRank = 0,
            spent = emptyMap(),
        )
        assertEquals(listOf(SpellSlot(Ring.FIRE, current = 2, max = 2)), slots)
    }
}
