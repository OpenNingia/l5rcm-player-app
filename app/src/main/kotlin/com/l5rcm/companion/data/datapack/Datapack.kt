package com.l5rcm.companion.data.datapack

/** Mutable accumulator used while parsing the XML files of one pack. */
class DatapackBuilder(val id: String, val displayName: String, val version: String) {
    val clans = LinkedHashMap<String, ClanDef>()
    val families = LinkedHashMap<String, FamilyDef>()
    val schools = LinkedHashMap<String, SchoolDef>()
    val skills = LinkedHashMap<String, SkillDef>()
    val skillCategories = LinkedHashMap<String, NamedDef>()
    val perkCategories = LinkedHashMap<String, NamedDef>()
    val spells = LinkedHashMap<String, SpellDef>()
    val katas = LinkedHashMap<String, KataDef>()
    val kihos = LinkedHashMap<String, KihoDef>()
    val perks = LinkedHashMap<String, PerkDef>()
    val weapons = LinkedHashMap<String, WeaponDef>()
    val armors = LinkedHashMap<String, ArmorDef>()
    val effects = LinkedHashMap<String, EffectDef>()
    val rings = LinkedHashMap<String, NamedDef>()
    val traits = LinkedHashMap<String, NamedDef>()
    val modifiers = ArrayList<ModifierDef>()

    fun build(): Datapack = Datapack(
        id = id,
        displayName = displayName,
        version = version,
        clansById = clans,
        familiesById = families,
        schoolsById = schools,
        skillsById = skills,
        skillCategoriesById = skillCategories,
        perkCategoriesById = perkCategories,
        spellsById = spells,
        katasById = katas,
        kihosById = kihos,
        perksById = perks,
        weaponsByName = weapons,
        armorsByName = armors,
        effectsById = effects,
        ringsById = rings,
        traitsById = traits,
        modifiers = modifiers,
    )
}

/** One parsed datapack: lookup maps keyed by id (weapons/armor by name). */
data class Datapack(
    val id: String,
    val displayName: String,
    val version: String,
    val clansById: Map<String, ClanDef>,
    val familiesById: Map<String, FamilyDef>,
    val schoolsById: Map<String, SchoolDef>,
    val skillsById: Map<String, SkillDef>,
    val skillCategoriesById: Map<String, NamedDef>,
    val perkCategoriesById: Map<String, NamedDef>,
    val spellsById: Map<String, SpellDef>,
    val katasById: Map<String, KataDef>,
    val kihosById: Map<String, KihoDef>,
    val perksById: Map<String, PerkDef>,
    val weaponsByName: Map<String, WeaponDef>,
    val armorsByName: Map<String, ArmorDef>,
    val effectsById: Map<String, EffectDef>,
    val ringsById: Map<String, NamedDef>,
    val traitsById: Map<String, NamedDef>,
    val modifiers: List<ModifierDef>,
)

/**
 * A merged view across all enabled packs. De-duplicated by id; the desktop is
 * core-first / last-wins. We merge in the order packs are supplied, so callers
 * should pass core first and let later packs override.
 */
class DatapackSet(packs: List<Datapack>) {
    val clansById = mergeMaps(packs) { it.clansById }
    val familiesById = mergeMaps(packs) { it.familiesById }
    val schoolsById = mergeMaps(packs) { it.schoolsById }
    val skillsById = mergeMaps(packs) { it.skillsById }
    val skillCategoriesById = mergeMaps(packs) { it.skillCategoriesById }
    val perkCategoriesById = mergeMaps(packs) { it.perkCategoriesById }
    val spellsById = mergeMaps(packs) { it.spellsById }
    val katasById = mergeMaps(packs) { it.katasById }
    val kihosById = mergeMaps(packs) { it.kihosById }
    val perksById = mergeMaps(packs) { it.perksById }
    val weaponsByName = mergeMaps(packs) { it.weaponsByName }
    val armorsByName = mergeMaps(packs) { it.armorsByName }
    val effectsById = mergeMaps(packs) { it.effectsById }
    val ringsById = mergeMaps(packs) { it.ringsById }
    val traitsById = mergeMaps(packs) { it.traitsById }
    val modifiers: List<ModifierDef> = packs.flatMap { it.modifiers }

    /** Ids of the packs merged into this set. */
    val packIds: Set<String> = packs.map { it.id }.toSet()

    val isEmpty: Boolean = packs.isEmpty()

    fun school(id: String?): SchoolDef? = id?.let { schoolsById[it] }
    fun skill(id: String?): SkillDef? = id?.let { skillsById[it] }
    fun family(id: String?): FamilyDef? = id?.let { familiesById[it] }
    fun clan(id: String?): ClanDef? = id?.let { clansById[it] }
    fun spell(id: String?): SpellDef? = id?.let { spellsById[it] }
    fun kata(id: String?): KataDef? = id?.let { katasById[it] }
    fun kiho(id: String?): KihoDef? = id?.let { kihosById[it] }
    fun perk(id: String?): PerkDef? = id?.let { perksById[it] }
    fun effect(id: String?): EffectDef? = id?.let { effectsById[it] }

    /** Resolves the technique granted by [schoolId] at [schoolRank]. */
    fun techAt(schoolId: String?, schoolRank: Int): Tech? = school(schoolId)?.techAtRank(schoolRank)

    private companion object {
        fun <V> mergeMaps(packs: List<Datapack>, sel: (Datapack) -> Map<String, V>): Map<String, V> {
            val out = LinkedHashMap<String, V>()
            packs.forEach { out.putAll(sel(it)) } // later packs win
            return out
        }
    }
}
