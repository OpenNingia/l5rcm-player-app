package com.l5rcm.companion.data.save

import kotlinx.serialization.Serializable

/** Datapack reference recorded in the save (`pack_refs[]`). See docs/FILE_FORMAT_L5R.md §2.1. */
@Serializable
data class PackRef(
    val id: String = "",
    val name: String = "",
    val version: String = "",
)

/** Worn armor (`armor`). See docs/FILE_FORMAT_L5R.md §5.1. */
@Serializable
data class ArmorOutfit(
    val name: String = "",
    val tn: Int = 0,
    val rd: Int = 0,
    val desc: String = "",
    val rule: String = "",
    val cost: String = "",
)

/** A carried weapon (each element of `weapons`). See docs/FILE_FORMAT_L5R.md §5.2. */
@Serializable
data class WeaponOutfit(
    val name: String = "",
    val dr: String = "",
    val dr_alt: String = "",
    val range: String = "",
    val strength: Int = 0,
    val min_str: Int = 0,
    val qty: Int = 1,
    val skill_id: String? = null,
    val skill_nm: String = "",
    val base_atk: String = "",
    val max_atk: String = "",
    val base_dmg: String = "",
    val max_dmg: String = "",
    val tags: List<String> = emptyList(),
    val trait: String = "",
    val desc: String = "",
    val rule: String = "",
    val cost: String = "",
)

/**
 * A user-defined stat modifier (each element of `modifiers`).
 * Parsed and stored; v1 does not evaluate it. See docs/FILE_FORMAT_L5R.md §5.3.
 */
@Serializable
data class ModifierModel(
    val type: String = "none",
    val dtl: String? = null,
    val value: List<Int> = listOf(0, 0, 0),
    val reason: String = "",
    val active: Boolean = false,
) {
    /** `[base, increment, final]` — the applied amount is the final element. */
    val finalValue: Int get() = value.getOrElse(2) { 0 }
}
