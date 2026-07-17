package com.l5rcm.companion.domain.rules

/**
 * The five combat stances (rulebook pp. 84–85). A character picks one during the Initiative stage;
 * the choice is play-time session state (Layer B), never derived from the `.l5r`.
 *
 * [isDefensive] stances ([DEFENSE], [FULL_DEFENSE]) require at least one rank in the Defense skill
 * to be adopted — [StanceRules.canUse] enforces that. [effects] is the short RAW summary shown in
 * the Combat tab; the numeric consequences the app actually applies live in [StanceRules].
 */
enum class Stance(
    val displayName: String,
    val effects: String,
    val isDefensive: Boolean = false,
) {
    ATTACK(
        "Attacco",
        "Nessuna restrizione.",
    ),
    FULL_ATTACK(
        "Attacco Totale",
        "+2k1 ai tiri d'attacco, Armor TN −10. Solo azioni d'attacco/avvicinamento, mischia.",
    ),
    DEFENSE(
        "Difesa",
        "Armor TN +Anello Aria +grado Difesa. Non puoi attaccare.",
        isDefensive = true,
    ),
    FULL_DEFENSE(
        "Difesa Totale",
        "Tira Difesa/Reflessi: aggiungi metà del totale (arrotonda su) all'Armor TN fino al prossimo turno.",
        isDefensive = true,
    ),
    CENTER(
        "Centrato",
        "Nessuna azione questo round; il prossimo +1k1+Anello Vuoto a un tiro e +10 Iniziativa.",
    ),
}

/**
 * Pure, RNG-free rules for how a [Stance] bends the numbers the Combat tab surfaces. Kept separate
 * from the [Stance] enum so the character-dependent maths (Air Ring, Defense rank, the captured
 * Full Defense roll) stay in one testable place.
 */
object StanceRules {

    /**
     * How the stance shifts the character's Armor TN ("TN to be hit").
     *
     * @param airRank Air Ring rank (Defense stance bonus).
     * @param defenseRank Defense skill rank (Defense stance bonus).
     * @param fullDefenseTotal the captured Defense/Reflexes roll total; the Full Defense bonus is
     *   `ceil(total / 2)`. Null (not yet rolled) contributes 0.
     */
    fun armorTnDelta(
        stance: Stance,
        airRank: Int,
        defenseRank: Int,
        fullDefenseTotal: Int?,
    ): Int = when (stance) {
        Stance.ATTACK -> 0
        Stance.FULL_ATTACK -> -10
        Stance.DEFENSE -> airRank + defenseRank
        Stance.FULL_DEFENSE -> fullDefenseTotal?.let { (it + 1) / 2 } ?: 0
        Stance.CENTER -> 0
    }

    /** Extra dice added to an attack pool by the stance: Full Attack grants +2k1, others nothing. */
    fun attackPoolBonus(stance: Stance): Pair<Int, Int> =
        if (stance == Stance.FULL_ATTACK) 2 to 1 else 0 to 0

    /** Defensive stances need at least one rank of Defense; every other stance is always allowed. */
    fun canUse(stance: Stance, defenseRank: Int): Boolean =
        !stance.isDefensive || defenseRank >= 1
}
