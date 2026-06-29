package com.l5rcm.companion.data.datapack

/**
 * Typed datapack entities, keyed by `id` (or name where the format has no id).
 * One pack parses into a [Datapack]; enabled packs merge into a [DatapackSet].
 * See docs/DATAPACK_FORMAT.md §2.
 */

data class ClanDef(
    val id: String,
    val name: String,
    val page: String? = null,
)

data class FamilyDef(
    val id: String,
    val name: String,
    val clanId: String?,
    /** The +1 trait this family grants (trait id), used in trait/ring derivation. */
    val trait: String?,
    val page: String? = null,
)

/** A granted (fixed) school skill. */
data class SchoolSkill(
    val id: String,
    val rank: Int,
    val emphases: String? = null,
)

/** A technique granted at a given school rank. */
data class Tech(
    val id: String,
    val name: String,
    val rank: Int,
    val description: String = "",
)

data class SchoolDef(
    val id: String,
    val name: String,
    val clanId: String?,
    /** The school's +1 trait (trait id). */
    val trait: String?,
    val honor: Double = 0.0,
    val tags: List<String> = emptyList(),
    val affinity: String? = null,
    val deficiency: String? = null,
    val skills: List<SchoolSkill> = emptyList(),
    val spells: List<String> = emptyList(),
    val techs: List<Tech> = emptyList(),
    val outfit: List<String> = emptyList(),
    val money: List<Int> = listOf(0, 0, 0),
    val page: String? = null,
) {
    fun techAtRank(schoolRank: Int): Tech? = techs.firstOrNull { it.rank == schoolRank }
}

data class MasteryAbility(
    val rank: Int,
    val rule: String? = null,
    val text: String = "",
)

data class SkillDef(
    val id: String,
    val name: String,
    /** Trait the skill rolls with (trait id). */
    val trait: String?,
    /** Category id (→ [SkillCategDef]), e.g. `bugei`. */
    val type: String?,
    val tags: List<String> = emptyList(),
    val desc: String = "",
    val masteryAbilities: List<MasteryAbility> = emptyList(),
    val page: String? = null,
)

/** Shared id→name shape for `<SkillCateg>`, `<PerkCateg>`, `<RingDef>`, `<TraitDef>`. */
data class NamedDef(
    val id: String,
    val name: String,
)

data class SpellDef(
    val id: String,
    val name: String,
    val element: String? = null,
    val mastery: Int = 0,
    val area: String = "",
    val range: String = "",
    val duration: String = "",
    val tags: List<String> = emptyList(),
    val elements: List<String> = emptyList(),
    val desc: String = "",
    val page: String? = null,
)

data class KataDef(
    val id: String,
    val name: String,
    val element: String? = null,
    val mastery: Int = 0,
    val desc: String = "",
    val page: String? = null,
)

data class KihoDef(
    val id: String,
    val name: String,
    val element: String? = null,
    val mastery: Int = 0,
    /** internal / mystical / martial / kharmic. */
    val type: String? = null,
    val desc: String = "",
    val page: String? = null,
)

data class PerkRank(
    val id: Int,
    val value: Int,
)

/** A `<Merit>` or `<Flaw>` — both build the same shape; [isMerit] distinguishes. */
data class PerkDef(
    val id: String,
    val name: String,
    val type: String? = null,
    val rule: String? = null,
    val isMerit: Boolean = true,
    val ranks: List<PerkRank> = emptyList(),
    val desc: String = "",
    val page: String? = null,
)

data class WeaponDef(
    val name: String,
    val skill: String? = null,
    val dr: String = "",
    val drAlt: String = "",
    val range: String = "",
    val cost: String = "",
    val strength: Int = 0,
    val minStrength: Int = 0,
    val tags: List<String> = emptyList(),
    val effectId: String? = null,
)

data class ArmorDef(
    val name: String,
    val tn: Int = 0,
    val rd: Int = 0,
    val cost: String = "",
    val effectId: String? = null,
)

data class EffectDef(
    val id: String,
    val text: String,
)

/**
 * Declarative stat modifier — parsed and stored, NOT evaluated in v1
 * (the expression engine is deferred). See docs/DATAPACK_FORMAT.md §2.10.
 */
data class ModifierDef(
    val target: String,
    val kind: String,
    val rank: Int? = null,
    val partial: Boolean = false,
)
