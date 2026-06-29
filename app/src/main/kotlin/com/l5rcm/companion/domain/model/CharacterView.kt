package com.l5rcm.companion.domain.model

/**
 * The fully-derived, immutable read model the UI consumes. Assembled by
 * [com.l5rcm.companion.domain.rules.CharacterDeriver] from a SaveModel + DatapackSet.
 * All "current" values (trait/ring ranks, skills, insight, wounds, TN/RD, XP) are
 * computed here so composables never re-derive.
 */
data class CharacterView(
    val name: String,
    val clanId: String?,
    val clanName: String?,
    val familyName: String?,
    val schoolName: String?,

    val traits: List<TraitView>,
    val rings: List<RingView>,
    val voidRank: Int,
    val insightRank: Int,
    val insightValue: Int,

    val honor: Double,
    val glory: Double,
    val status: Double,
    val infamy: Double,
    val taint: Double,

    val health: HealthView,
    val combat: CombatView,
    val xp: XpView,

    val skills: List<SkillView>,
    val schools: List<SchoolRankView>,
    val techniques: List<TechniqueView>,
    val spells: SpellsView,
    val katas: List<NamedEntry>,
    val kihos: List<NamedEntry>,
    val merits: List<PerkView>,
    val flaws: List<PerkView>,
    val weapons: List<WeaponView>,
    val armor: ArmorView?,
    val modifiers: List<ModifierView>,

    val money: Money,
    val properties: Map<String, String>,
    /** Free-form notes — Qt rich-text HTML. */
    val notesHtml: String,
    val packRefs: List<PackRefView>,
)

data class TraitView(val trait: Trait, val rank: Int, val modifiedRank: Int)

data class RingView(val ring: Ring, val rank: Int)

data class HealthView(
    val earthRank: Int,
    val currentWounds: Int,
    val maxWounds: Int,
    val healRate: Int,
    /** One per wound level (Healthy..Out), with cumulative capacity and penalty. */
    val levels: List<WoundLevel>,
)

data class WoundLevel(
    val name: String,
    /** Total accumulated wounds at which this level is reached (null for "Out"). */
    val threshold: Int?,
    /** TN penalty at this level (null for "Out" = incapacitated). */
    val penalty: Int?,
)

data class CombatView(
    val baseTn: Int,
    val fullTn: Int,
    val baseRd: Int,
    val fullRd: Int,
    val initiativeRoll: Int,
    val initiativeKeep: Int,
)

data class XpView(
    val spent: Int,
    val available: Int,
    val left: Int,
    val fromFlaws: Int,
)

data class SkillView(
    val id: String,
    val name: String,
    val rank: Int,
    val trait: String?,
    val category: String?,
    val emphases: List<String>,
)

data class SchoolRankView(val schoolId: String, val name: String, val rank: Int)

data class TechniqueView(
    val id: String,
    val name: String,
    val schoolName: String?,
    val insightRank: Int,
    val schoolRank: Int,
    val description: String,
)

data class SpellsView(
    val known: List<SpellEntry>,
    val memorized: List<SpellEntry>,
    val affinities: List<String>,
    val deficiencies: List<String>,
)

data class SpellEntry(
    val id: String,
    val name: String,
    val element: String?,
    val mastery: Int,
    val range: String,
    val area: String,
    val duration: String,
    val description: String,
)

data class NamedEntry(val id: String, val name: String, val description: String = "")

data class PerkView(val id: String, val name: String, val rank: Int, val isFlaw: Boolean)

data class WeaponView(
    val name: String,
    val skillName: String,
    val damageRoll: String,
    val range: String,
    val tags: List<String>,
)

data class ArmorView(val name: String, val tn: Int, val rd: Int)

data class ModifierView(val target: String, val detail: String?, val value: Int, val reason: String, val active: Boolean)

data class Money(val koku: Int, val bu: Int, val zeni: Int)

data class PackRefView(val id: String, val name: String, val version: String, val installed: Boolean)
