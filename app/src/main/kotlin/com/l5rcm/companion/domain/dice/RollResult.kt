package com.l5rcm.companion.domain.dice

/**
 * One rolled die and everything the UI needs to draw it: [rolls] is the chain of raw d10 faces
 * (a single value normally, or the exploding sequence e.g. `[10, 4]`), [kept] marks whether this
 * die survived the "keep the highest" cut.
 *
 * [value] is the die's contribution (the summed chain); [exploded] drives the spark icon and the
 * `a+b+c` breakdown shown on kept dice.
 */
data class Die(
    val rolls: List<Int>,
    val kept: Boolean,
) {
    val value: Int get() = rolls.sum()
    val exploded: Boolean get() = rolls.size > 1
}

/**
 * Outcome of rolling a [RollSpec]. [results] holds every die (in roll order) with its exploding
 * chain and kept flag; [total] is `sum(kept die values) + bonus`. [normalized] is what the Ten
 * Dice Rule produced, exposed so the UI can show *why* a large pool was reduced (e.g.
 * "14k12 → 10k10 +12").
 *
 * [dice] and [kept] are flat convenience views (per-die totals) for callers that don't care about
 * the explosion breakdown.
 */
data class RollResult(
    val results: List<Die>,
    val bonus: Int,
    val total: Int,
    val normalized: NormalizedPool,
) {
    /** Every die's fully-exploded total, in roll order. */
    val dice: List<Int> get() = results.map { it.value }

    /** The kept dice's totals (highest [NormalizedPool.kept] of them), highest first. */
    val kept: List<Int> get() = results.filter { it.kept }.map { it.value }.sortedDescending()
}
