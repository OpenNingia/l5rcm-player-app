package com.l5rcm.companion.domain.dice

/**
 * Structured description of a Roll & Keep pool, *before* the Ten Dice Rule is applied.
 *
 * This is a plain statement of the roller's *intent*; the engine ([TenDiceRule] + [Roller])
 * enforces every rule interaction (void boost, dice-penalty, Ten Dice Rule, explosion) so callers
 * never pre-cook the numbers. Ports the mechanical roll of the desktop `l5r/diceroller/drcore.py`
 * (`roll_l5r_pool`) and layers on the RAW rules from `docs/RULES.md` (rulebook pp. 78–83, 136).
 *
 * @param rolled raw dice to roll (before the Ten Dice Rule).
 * @param kept   raw dice to keep (highest); clamped to [rolled] by the dice-penalty rule.
 * @param bonus  flat modifier added to the summed kept dice.
 * @param explodeOn die value at/above which a die is re-rolled and added, chaining indefinitely
 *   (RAW = 10). Use [NO_EXPLODE] to disable. [unskilled] always disables explosion.
 * @param unskilled skill rank 0: dice never explode and no Raises are allowed (p. 83).
 * @param emphasis re-roll any die that comes up 1 — once, on its first roll only (p. 136).
 * @param voidBoost spend a Void Point for +1k1, declared before the roll (p. 81); not for damage.
 */
data class RollSpec(
    val rolled: Int,
    val kept: Int,
    val bonus: Int = 0,
    val explodeOn: Int = 10,
    val unskilled: Boolean = false,
    val emphasis: Boolean = false,
    val voidBoost: Boolean = false,
) {
    init {
        require(rolled >= 0) { "rolled must be >= 0, was $rolled" }
        require(kept >= 0) { "kept must be >= 0, was $kept" }
    }

    companion object {
        /** Threshold that disables exploding dice (no d10 face reaches it). */
        const val NO_EXPLODE = 11
    }
}
