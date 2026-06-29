package com.l5rcm.companion.domain.model

/**
 * The eight traits in their fixed array order (chmodel.ATTRIBS). Array indices in
 * `starting_traits` and `AttribAdv.attrib` map to these. See docs/FILE_FORMAT_L5R.md §2.7.
 */
enum class Trait(val index: Int, val id: String, val ring: Ring) {
    STAMINA(0, "stamina", Ring.EARTH),
    WILLPOWER(1, "willpower", Ring.EARTH),
    REFLEXES(2, "reflexes", Ring.AIR),
    AWARENESS(3, "awareness", Ring.AIR),
    STRENGTH(4, "strength", Ring.WATER),
    PERCEPTION(5, "perception", Ring.WATER),
    AGILITY(6, "agility", Ring.FIRE),
    INTELLIGENCE(7, "intelligence", Ring.FIRE),
    ;

    companion object {
        fun byId(id: String): Trait? = entries.firstOrNull { it.id == id }
        fun byIndex(index: Int): Trait? = entries.firstOrNull { it.index == index }
    }
}

/** The five rings. Void is its own ring (not derived from a trait pair). */
enum class Ring(val id: String) {
    EARTH("earth"),
    AIR("air"),
    WATER("water"),
    FIRE("fire"),
    VOID("void"),
    ;

    /** The two traits whose minimum defines this ring's rank (empty for Void). */
    val traits: List<Trait> get() = Trait.entries.filter { it.ring == this }

    companion object {
        fun byId(id: String): Ring? = entries.firstOrNull { it.id == id }

        /** The four element rings derived from trait pairs (excludes Void). */
        val elementRings: List<Ring> = listOf(EARTH, AIR, WATER, FIRE)
    }
}
