package com.l5rcm.companion.domain.dice

import kotlin.random.Random

/** Source of individual d10 results (1..10). Injected so tests can script exact rolls. */
fun interface DieRoller {
    fun d10(): Int
}

/** A [DieRoller] backed by [Random]; the production default. */
class RandomDieRoller(private val random: Random = Random.Default) : DieRoller {
    override fun d10(): Int = random.nextInt(1, 11)
}

/**
 * Rolls a [RollSpec] into a [RollResult]. Ports the per-die loop of the desktop
 * `l5r/diceroller/drcore.py` `roll_l5r_pool`: roll a d10, explode while it shows [RollSpec.explodeOn]
 * or higher (summing the chain into one die total), then keep the highest dice. Adds the RAW rules
 * the Python omits — Ten Dice Rule normalization ([TenDiceRule]), emphasis re-roll-1s, and the
 * unskilled no-explode restriction (`docs/RULES.md`, pp. 80–83, 136).
 */
class Roller(private val rng: DieRoller = RandomDieRoller()) {

    fun roll(spec: RollSpec): RollResult {
        val pool = TenDiceRule.normalize(spec)
        val explodes = !spec.unskilled && spec.explodeOn in 1..10

        val dice = ArrayList<Int>(pool.rolled)
        repeat(pool.rolled) {
            dice.add(rollDie(spec, explodes))
        }

        val kept = dice.sortedDescending().take(pool.kept)
        val total = kept.sum() + pool.bonus
        return RollResult(dice = dice, kept = kept, bonus = pool.bonus, total = total, normalized = pool)
    }

    /** One die: optional emphasis re-roll of a first-roll 1, then an exploding chain. */
    private fun rollDie(spec: RollSpec, explodes: Boolean): Int {
        var face = rng.d10()
        // Emphasis: re-roll a 1 — once, on the first roll only (p. 136).
        if (spec.emphasis && face == 1) {
            face = rng.d10()
        }
        var total = face
        var guard = 0
        while (explodes && face >= spec.explodeOn) {
            face = rng.d10()
            total += face
            if (++guard >= EXPLODE_GUARD) break
        }
        return total
    }

    private companion object {
        /** Defensive cap on an exploding chain (RAW is unbounded; a stuck RNG stub must not hang). */
        const val EXPLODE_GUARD = 1_000
    }
}
