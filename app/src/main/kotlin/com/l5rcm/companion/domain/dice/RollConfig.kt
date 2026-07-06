package com.l5rcm.companion.domain.dice

/** What the pool represents. Only [SKILL] carries the skilled/unskilled + emphasis distinction. */
enum class RollType { SKILL, TRAIT, RING, SPELL }

/** [OPEN] = just show the total; [TN] = compare against a Target Number for success/failure. */
enum class RollMode { OPEN, TN }

/**
 * The roller's *raw intent* as configured on screen, before any rule interaction is resolved.
 * This is the single source of state the UI edits and history restores; call [effective] to turn
 * it into the numbers actually rolled.
 *
 * @param skilled Skill rolls only: `true` = has rank in the skill (10s explode, Raises allowed).
 * @param emphasisToggle Skill rolls only: the Emphasis switch (re-roll 1s once). Gated in [effective]
 *   to *genuinely* skilled rolls — a Void-Point promotion does not grant an emphasis.
 * @param voidSpent a Void Point declared for this roll: +1k1, and it promotes an unskilled Skill
 *   roll to skilled. A per-roll resource — the caller resets it after each completed roll.
 */
data class RollConfig(
    val mode: RollMode = RollMode.TN,
    val rollType: RollType = RollType.SKILL,
    val skilled: Boolean = true,
    val emphasisToggle: Boolean = false,
    val voidSpent: Boolean = false,
    val rolled: Int = 5,
    val kept: Int = 3,
    val bonus: Int = 0,
    val tn: Int = 15,
    val raises: Int = 0,
)

/**
 * A [RollConfig] with every rule interaction resolved (see `design_handoff_l5r_dice_roller`
 * "Effective values" and rulebook pp. 79–83, 136). Pure and RNG-free; [toSpec] hands the pool to
 * the [Roller].
 */
data class EffectiveRoll(
    val isSkill: Boolean,
    /** Explosions on & Raises allowed: real skill, any non-skill roll, or a Void-promoted unskilled roll. */
    val effSkilled: Boolean,
    /** Emphasis actually applies (genuinely skilled Skill roll with the toggle on). */
    val emphasis: Boolean,
    val effRolled: Int,
    val effKept: Int,
    val effRaises: Int,
    val effTn: Int,
    /** An unskilled Skill roll that the Void Point turned skilled (drives the `→ ESPERTO` pill/note). */
    val promoted: Boolean,
    val voidSpent: Boolean,
    val bonus: Int,
) {
    /** Roll & Keep notation, e.g. `6k3`, `7k4+5`, `5k2-3`. */
    val notation: String get() = notation(effRolled, effKept, bonus)

    /** The pool handed to the engine. Void's +1k1 is already folded into [effRolled]/[effKept], so
     *  [RollSpec.voidBoost] stays off to avoid double-counting; unskilled disables explosion. */
    fun toSpec(): RollSpec = RollSpec(
        rolled = effRolled,
        kept = effKept,
        bonus = bonus,
        unskilled = !effSkilled,
        emphasis = emphasis,
        voidBoost = false,
    )
}

/** Resolve a [RollConfig] into its [EffectiveRoll]. Never mutates the config. */
fun RollConfig.effective(): EffectiveRoll {
    val isSkill = rollType == RollType.SKILL
    val add = voidSpent // Void Point → +1k1
    val effSkilled = if (isSkill) skilled || voidSpent else true
    val emphasis = isSkill && skilled && emphasisToggle // real skill, not a Void promotion
    val effRolled = minOf(10, rolled + (if (add) 1 else 0))
    val effKept = minOf(effRolled, kept + (if (add) 1 else 0))
    val effRaises = if (effSkilled) raises else 0 // unskilled forces 0
    val effTn = tn + effRaises * 5
    val promoted = isSkill && !skilled && voidSpent
    return EffectiveRoll(
        isSkill = isSkill,
        effSkilled = effSkilled,
        emphasis = emphasis,
        effRolled = effRolled,
        effKept = effKept,
        effRaises = effRaises,
        effTn = effTn,
        promoted = promoted,
        voidSpent = voidSpent,
        bonus = bonus,
    )
}

/** Roll & Keep notation: `{rolled}k{kept}` plus a signed bonus when non-zero. */
fun notation(rolled: Int, kept: Int, bonus: Int): String {
    val base = "${rolled}k$kept"
    return when {
        bonus > 0 -> "$base+$bonus"
        bonus < 0 -> "$base$bonus" // negative sign is already part of the number
        else -> base
    }
}
