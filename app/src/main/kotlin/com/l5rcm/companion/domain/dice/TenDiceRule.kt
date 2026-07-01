package com.l5rcm.companion.domain.dice

/**
 * A pool after the Ten Dice Rule has normalized it: [rolled] ≤ 10 and [kept] ≤ 10, with any
 * overflow folded into [bonus]. Deterministic — carries no randomness.
 */
data class NormalizedPool(
    val rolled: Int,
    val kept: Int,
    val bonus: Int,
)

/**
 * The Ten Dice Rule (rulebook p. 80) plus the dice-penalty rule and Void boost, as a pure,
 * RNG-free normalizer. This is the easy-to-get-wrong core the roadmap flags for exact unit tests.
 *
 * Order of operations:
 *  1. **Void boost** (if [RollSpec.voidBoost]): +1k1 — declared before the roll (p. 81).
 *  2. **Ten Dice Rule:** no roll uses more than ten dice. Excess *rolled* dice become kept dice at
 *     two rolled → one kept (a leftover odd rolled die becomes +2). Once both rolled and kept are at
 *     ten, every further die of either type becomes +2. The manual applies this even when kept
 *     exceeds rolled (10k12 → 10k10 +4), so it runs *before* the penalty cap below.
 *  3. **Dice-penalty rule:** you can never keep more dice than you roll, so `kept = min(kept, rolled)`
 *     — applied to whatever the Ten Dice Rule left (caps 3k5 → 3k3 without disturbing 10k12).
 *
 * Reproduces the manual's worked examples exactly: 12k4→10k5, 13k9→10k10+2, 10k12→10k10+4,
 * 14k12→10k10+12.
 */
object TenDiceRule {

    fun normalize(spec: RollSpec): NormalizedPool {
        var rolled = spec.rolled
        var kept = spec.kept
        var bonus = spec.bonus

        // 1. Void boost: +1k1 before anything else.
        if (spec.voidBoost) {
            rolled += 1
            kept += 1
        }

        // 2. Ten Dice Rule — drain excess rolled dice first, then excess kept dice.
        while (rolled > 10) {
            when {
                // Room to keep more: two extra rolled → one kept.
                kept < 10 && rolled >= 12 -> { rolled -= 2; kept += 1 }
                // A single leftover rolled die (rolled == 11 with room) → +2.
                kept < 10 -> { rolled -= 1; bonus += 2 }
                // Kept already maxed: each extra rolled → +2.
                else -> { rolled -= 1; bonus += 2 }
            }
        }
        while (kept > 10) {
            kept -= 1
            bonus += 2
        }

        // 3. Dice-penalty rule: cannot keep more than rolled (applied post-normalization).
        if (kept > rolled) kept = rolled

        return NormalizedPool(rolled = rolled, kept = kept, bonus = bonus)
    }
}
