package com.l5rcm.companion.data.save

import kotlinx.serialization.Serializable

/**
 * In-memory mirror of a `.l5r` save file (`AdvancedPcModel`).
 *
 * Every field is optional with a default: old saves omit newer fields and several
 * fields are legacy. Unknown keys (`attribs`, `void`, `insight`, `outfit`, …) are
 * dropped by the lenient [SaveParser] and are intentionally absent here.
 * See docs/FILE_FORMAT_L5R.md §2.
 */
@Serializable
data class SaveModel(
    // 2.1 Identity & metadata
    val name: String = "",
    val clan: String? = null,
    val family: String? = null,
    val version: String = "0.0",
    val unsaved: Boolean = false,
    @Serializable(with = LenientStringMapSerializer::class)
    val properties: Map<String, String> = emptyMap(),
    /** Free-form notes — Qt rich-text HTML, not plain text. */
    val extra_notes: String = "",
    val pack_refs: List<PackRef> = emptyList(),

    // 2.2 Traits / void — BASE values; current ranks are derived from advans[]
    val starting_traits: List<Int> = listOf(2, 2, 2, 2, 2, 2, 2, 2),
    val starting_void: Int = 2,

    // 2.3 Experience & advancement
    val advans: List<Advancement> = emptyList(),
    val exp_limit: Int = 40,
    val insight_calculation: Int = 1,

    // 2.4 Reputation tracks (stored as deltas over the rules-derived starting value)
    val honor: Double = 0.0,
    val glory: Double = 0.0,
    val status: Double = 0.0,
    val infamy: Double = 0.0,
    val taint: Double = 0.0,

    // 2.5 Health & current play state
    val wounds: Int = 0,
    val void_points: Int = 0,
    val health_multiplier: Int = 2,
    val health_base_multiplier: Int = 5,

    // 2.6 Equipment & wealth
    val armor: ArmorOutfit? = null,
    val weapons: List<WeaponOutfit> = emptyList(),
    val modifiers: List<ModifierModel> = emptyList(),
    val money: List<Int> = listOf(0, 0, 0),

    // 2.7 Costs & spell config
    val attrib_costs: List<Int> = listOf(4, 4, 4, 4, 4, 4, 4, 4),
    val void_cost: Int = 6,
    val spells_per_rank: Int = 3,
) {
    /** Only the `rank` advancements, in save order. */
    val rankAdvancements: List<RankAdv> get() = advans.filterIsInstance<RankAdv>()

    /** Whether the character has a rule (e.g. `weak_stamina`) recorded on any advancement. */
    fun hasRule(rule: String): Boolean = advans.any { it.rule == rule }

    /** Count of advancements carrying [rule] (some rules stack, e.g. mastery insight bonuses). */
    fun countRule(rule: String): Int = advans.count { it.rule == rule }
}
