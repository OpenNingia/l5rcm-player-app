package com.l5rcm.companion.domain.rules

import com.l5rcm.companion.domain.model.Ring

/** One castable spell-slot pool: an element pool, or the flexible Void pool. */
data class SpellSlot(val ring: Ring, val current: Int, val max: Int)

/**
 * A shugenja's daily spell slots (RAW p.166). One pool per element with capacity equal to that
 * element Ring, plus a flexible **Void** pool with capacity equal to the Void Ring whose slots may
 * be spent on *any* element. [spent] holds slots already burned this day — a failed Spell Casting
 * Roll still burns a slot, so the tracker is spent by tapping, not by a success check. [current] is
 * `max − spent`, clamped to `0..max`; pools with no capacity are omitted.
 *
 * Pure Layer-B play-time state — it never touches the parity-tested derived sheet. Refreshed to
 * full on a daily rest (spent reset to 0), mirroring the sunrise refresh in the rules.
 */
fun spellSlots(
    elementRanks: Map<Ring, Int>,
    voidRank: Int,
    spent: Map<Ring, Int>,
): List<SpellSlot> {
    val pools = Ring.elementRings.map { it to (elementRanks[it] ?: 0) } + (Ring.VOID to voidRank)
    return pools
        .filter { (_, max) -> max > 0 }
        .map { (ring, max) ->
            val current = (max - (spent[ring] ?: 0)).coerceIn(0, max)
            SpellSlot(ring, current, max)
        }
}
