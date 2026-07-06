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

        // Roll every die, capturing its exploding chain in roll order.
        val chains = ArrayList<List<Int>>(pool.rolled)
        repeat(pool.rolled) {
            chains.add(rollDie(spec, explodes))
        }

        // "Keep the highest": pick the top `kept` dice by value, breaking ties by roll order so the
        // choice is deterministic. Mark those indices kept; everything else is discarded.
        val keptIndices = chains.indices
            .sortedWith(compareByDescending<Int> { chains[it].sum() }.thenBy { it })
            .take(pool.kept)
            .toHashSet()

        val results = chains.mapIndexed { i, chain -> Die(rolls = chain, kept = i in keptIndices) }
        val total = results.filter { it.kept }.sumOf { it.value } + pool.bonus
        return RollResult(results = results, bonus = pool.bonus, total = total, normalized = pool)
    }

    /** One die: optional emphasis re-roll of a first-roll 1, then an exploding chain of raw faces. */
    private fun rollDie(spec: RollSpec, explodes: Boolean): List<Int> {
        var face = rng.d10()
        // Emphasis: re-roll a 1 — once, on the first roll only (p. 136).
        if (spec.emphasis && face == 1) {
            face = rng.d10()
        }
        val chain = ArrayList<Int>()
        chain.add(face)
        var guard = 0
        while (explodes && face >= spec.explodeOn) {
            face = rng.d10()
            chain.add(face)
            if (++guard >= EXPLODE_GUARD) break
        }
        return chain
    }

    private companion object {
        /** Defensive cap on an exploding chain (RAW is unbounded; a stuck RNG stub must not hang). */
        const val EXPLODE_GUARD = 1_000
    }
}
