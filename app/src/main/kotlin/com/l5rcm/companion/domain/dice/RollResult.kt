package com.l5rcm.companion.domain.dice

/**
 * Outcome of rolling a [RollSpec]. [dice] holds every die's fully-exploded total in roll order;
 * [kept] is the highest [NormalizedPool.kept] of them; [total] is `sum(kept) + bonus`.
 * [normalized] is what the Ten Dice Rule produced, exposed so the UI can show *why* a large pool
 * was reduced (e.g. "14k12 → 10k10 +12").
 */
data class RollResult(
    val dice: List<Int>,
    val kept: List<Int>,
    val bonus: Int,
    val total: Int,
    val normalized: NormalizedPool,
)
