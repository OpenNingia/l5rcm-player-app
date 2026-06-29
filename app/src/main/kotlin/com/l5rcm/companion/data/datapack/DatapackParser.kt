package com.l5rcm.companion.data.datapack

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.xmlpull.v1.XmlPullParser
import java.io.File
import java.io.Reader

/** JSON shape of a pack's `manifest` file. See docs/DATAPACK_FORMAT.md §1.1. */
@Serializable
private data class Manifest(
    val id: String = "",
    val display_name: String = "",
    val version: String = "",
    val language: String = "",
)

/**
 * Parses an on-disk datapack directory into a [Datapack].
 *
 * A directory is a pack iff it contains a `manifest` file. All `*.xml` under it are
 * scanned recursively; each child of `<L5RCM>` is dispatched by tag name into typed
 * collections keyed by id. See docs/DATAPACK_FORMAT.md §2 / §4.
 */
class DatapackParser(private val parserFactory: XmlParserFactory) {

    private val manifestJson = Json { ignoreUnknownKeys = true; isLenient = true }

    /** True if [dir] looks like a pack root (has a `manifest`). */
    fun isPack(dir: File): Boolean = File(dir, "manifest").isFile

    fun parsePack(dir: File): Datapack {
        val manifestFile = File(dir, "manifest")
        require(manifestFile.isFile) { "Not a datapack (no manifest): ${dir.path}" }
        val manifest = manifestJson.decodeFromString(
            Manifest.serializer(),
            manifestFile.readText(Charsets.UTF_8),
        )
        val builder = DatapackBuilder(manifest.id, manifest.display_name, manifest.version)
        dir.walkTopDown()
            .filter { it.isFile && it.extension.equals("xml", ignoreCase = true) }
            .forEach { xml -> xml.reader(Charsets.UTF_8).use { parseXml(it, builder) } }
        return builder.build()
    }

    /** Parses a single XML document (root `<L5RCM>`) into [builder]. */
    fun parseXml(reader: Reader, builder: DatapackBuilder) {
        val p = parserFactory.create()
        p.setInput(reader)
        var ev = p.eventType
        while (ev != XmlPullParser.END_DOCUMENT) {
            if (ev == XmlPullParser.START_TAG && p.name == "L5RCM") {
                p.forEachChild { tag -> dispatch(p, tag, builder) }
                return
            }
            ev = p.next()
        }
    }

    private fun dispatch(p: XmlPullParser, tag: String, b: DatapackBuilder) {
        when (tag) {
            "Clan" -> readClan(p).let { b.clans[it.id] = it }
            "Family" -> readFamily(p).let { b.families[it.id] = it }
            "School" -> readSchool(p).let { b.schools[it.id] = it }
            "SkillDef" -> readSkill(p).let { b.skills[it.id] = it }
            "SkillCateg" -> readNamed(p).let { b.skillCategories[it.id] = it }
            "PerkCateg" -> readNamed(p).let { b.perkCategories[it.id] = it }
            "RingDef" -> readNamed(p).let { b.rings[it.id] = it }
            "TraitDef" -> readNamed(p).let { b.traits[it.id] = it }
            "SpellDef" -> readSpell(p).let { b.spells[it.id] = it }
            "KataDef" -> readKata(p).let { b.katas[it.id] = it }
            "KihoDef" -> readKiho(p).let { b.kihos[it.id] = it }
            "Merit" -> readPerk(p, isMerit = true).let { b.perks[it.id] = it }
            "Flaw" -> readPerk(p, isMerit = false).let { b.perks[it.id] = it }
            "Weapon" -> readWeapon(p).let { b.weapons[it.name] = it }
            "Armor" -> readArmor(p).let { b.armors[it.name] = it }
            "EffectDef" -> readEffect(p).let { b.effects[it.id] = it }
            "ModifierDef" -> readModifier(p).let { b.modifiers.add(it) }
            // Ancestor/Path/Tattoo/Requirements and others are not needed for v1 read-only.
        }
    }

    // --- Entity readers (parser sits on the element's START_TAG on entry). ---

    private fun readClan(p: XmlPullParser) = ClanDef(
        id = p.attr("id").orEmpty(),
        name = p.attr("name").orEmpty(),
        page = p.attr("page"),
    )

    private fun readFamily(p: XmlPullParser): FamilyDef {
        val id = p.attr("id").orEmpty()
        val name = p.attr("name").orEmpty()
        val clanId = p.attr("clanid")
        val page = p.attr("page")
        var trait: String? = null
        p.forEachChild { tag -> if (tag == "Trait") trait = p.readText() }
        return FamilyDef(id, name, clanId, trait, page)
    }

    private fun readSchool(p: XmlPullParser): SchoolDef {
        val id = p.attr("id").orEmpty()
        val name = p.attr("name").orEmpty()
        val clanId = p.attr("clanid")
        val page = p.attr("page")
        var trait: String? = null
        var honor = 0.0
        var affinity: String? = null
        var deficiency: String? = null
        var tags: List<String> = emptyList()
        var skills: List<SchoolSkill> = emptyList()
        var spells: List<String> = emptyList()
        var techs: List<Tech> = emptyList()
        var outfit: List<String> = emptyList()
        var money = listOf(0, 0, 0)
        p.forEachChild { tag ->
            when (tag) {
                "Trait" -> trait = p.readText()
                "Honor" -> honor = p.readText().toDoubleOrNull() ?: 0.0
                "Affinity" -> affinity = p.readText()
                "Deficiency" -> deficiency = p.readText()
                "Tags" -> tags = p.readTags()
                "Skills" -> skills = readSchoolSkills(p)
                "Spells" -> spells = readSchoolSpells(p)
                "Techs" -> techs = readTechs(p)
                "Outfit" -> {
                    money = listOf(p.attrInt("koku"), p.attrInt("bu"), p.attrInt("zeni"))
                    val items = ArrayList<String>()
                    p.forEachChild { c -> if (c == "Item") items.add(p.readText()) }
                    outfit = items
                }
            }
        }
        return SchoolDef(
            id, name, clanId, trait, honor, tags, affinity, deficiency,
            skills, spells, techs, outfit, money, page,
        )
    }

    private fun readSchoolSkills(p: XmlPullParser): List<SchoolSkill> {
        val out = ArrayList<SchoolSkill>()
        p.forEachChild { tag ->
            if (tag == "Skill") {
                out.add(
                    SchoolSkill(
                        id = p.attr("id").orEmpty(),
                        rank = p.attrInt("rank", 1),
                        emphases = p.attr("emphases"),
                    ),
                )
            }
        }
        return out
    }

    private fun readSchoolSpells(p: XmlPullParser): List<String> {
        val out = ArrayList<String>()
        p.forEachChild { tag -> if (tag == "Spell") p.attr("id")?.let { out.add(it) } }
        return out
    }

    private fun readTechs(p: XmlPullParser): List<Tech> {
        val out = ArrayList<Tech>()
        p.forEachChild { tag ->
            if (tag == "Tech") {
                val id = p.attr("id").orEmpty()
                val name = p.attr("name").orEmpty()
                val rank = p.attrInt("rank", 1)
                var desc = ""
                p.forEachChild { c -> if (c == "Description") desc = p.readText() }
                out.add(Tech(id, name, rank, desc))
            }
        }
        return out
    }

    private fun readSkill(p: XmlPullParser): SkillDef {
        val id = p.attr("id").orEmpty()
        val name = p.attr("name").orEmpty()
        val trait = p.attr("trait")
        val type = p.attr("type")
        val page = p.attr("page")
        var tags: List<String> = emptyList()
        var desc = ""
        val mastery = ArrayList<MasteryAbility>()
        p.forEachChild { tag ->
            when (tag) {
                "Tags" -> tags = p.readTags()
                "Description" -> desc = p.readText()
                "MasteryAbilities" -> p.forEachChild { c ->
                    if (c == "MasteryAbility") {
                        val rank = p.attrInt("rank", 0)
                        val rule = p.attr("rule")
                        mastery.add(MasteryAbility(rank, rule, p.readText()))
                    }
                }
            }
        }
        return SkillDef(id, name, trait, type, tags, desc, mastery, page)
    }

    private fun readNamed(p: XmlPullParser) = NamedDef(
        id = p.attr("id").orEmpty(),
        name = p.readText(),
    )

    private fun readSpell(p: XmlPullParser): SpellDef {
        val id = p.attr("id").orEmpty()
        val name = p.attr("name").orEmpty()
        val element = p.attr("element")
        val mastery = p.attrInt("mastery", 0)
        val area = p.attr("area").orEmpty()
        val range = p.attr("range").orEmpty()
        val duration = p.attr("duration").orEmpty()
        val page = p.attr("page")
        var tags: List<String> = emptyList()
        var elements: List<String> = emptyList()
        var desc = ""
        p.forEachChild { tag ->
            when (tag) {
                "Tags" -> tags = p.readTags()
                "Description" -> desc = p.readText()
                "MultiElement" -> {
                    val els = ArrayList<String>()
                    p.forEachChild { c -> if (c == "Element") els.add(p.readText()) }
                    elements = els
                }
            }
        }
        return SpellDef(id, name, element, mastery, area, range, duration, tags, elements, desc, page)
    }

    private fun readKata(p: XmlPullParser): KataDef {
        val id = p.attr("id").orEmpty()
        val name = p.attr("name").orEmpty()
        val element = p.attr("element")
        val mastery = p.attrInt("mastery", 0)
        val page = p.attr("page")
        var desc = ""
        p.forEachChild { tag -> if (tag == "Description") desc = p.readText() }
        return KataDef(id, name, element, mastery, desc, page)
    }

    private fun readKiho(p: XmlPullParser): KihoDef {
        val id = p.attr("id").orEmpty()
        val name = p.attr("name").orEmpty()
        val element = p.attr("element")
        val mastery = p.attrInt("mastery", 0)
        val type = p.attr("type")
        val page = p.attr("page")
        var desc = ""
        p.forEachChild { tag -> if (tag == "Description") desc = p.readText() }
        return KihoDef(id, name, element, mastery, type, desc, page)
    }

    private fun readPerk(p: XmlPullParser, isMerit: Boolean): PerkDef {
        val id = p.attr("id").orEmpty()
        val name = p.attr("name").orEmpty()
        val type = p.attr("type")
        val rule = p.attr("rule")
        val page = p.attr("page")
        var desc = ""
        val ranks = ArrayList<PerkRank>()
        p.forEachChild { tag ->
            when (tag) {
                "Description" -> desc = p.readText()
                "Rank" -> ranks.add(PerkRank(p.attrInt("id", 0), p.attrInt("value", 0)))
            }
        }
        return PerkDef(id, name, type, rule, isMerit, ranks, desc, page)
    }

    private fun readWeapon(p: XmlPullParser): WeaponDef {
        val name = p.attr("name").orEmpty()
        val skill = p.attr("skill")
        val dr = p.attr("dr").orEmpty()
        val drAlt = p.attr("dr_alt").orEmpty()
        val range = p.attr("range").orEmpty()
        val cost = p.attr("cost").orEmpty()
        val strength = p.attrInt("strength", 0)
        val minStrength = p.attrInt("min_strength", 0)
        var tags: List<String> = emptyList()
        var effectId: String? = null
        p.forEachChild { tag ->
            when (tag) {
                "Tags" -> tags = p.readTags()
                "Effect" -> effectId = p.attr("id")
            }
        }
        return WeaponDef(name, skill, dr, drAlt, range, cost, strength, minStrength, tags, effectId)
    }

    private fun readArmor(p: XmlPullParser): ArmorDef {
        val name = p.attr("name").orEmpty()
        val tn = p.attrInt("tn", 0)
        val rd = p.attrInt("rd", 0)
        val cost = p.attr("cost").orEmpty()
        var effectId: String? = null
        p.forEachChild { tag -> if (tag == "Effect") effectId = p.attr("id") }
        return ArmorDef(name, tn, rd, cost, effectId)
    }

    private fun readEffect(p: XmlPullParser) = EffectDef(
        id = p.attr("id").orEmpty(),
        text = p.readText(),
    )

    private fun readModifier(p: XmlPullParser): ModifierDef {
        val target = p.attr("target").orEmpty()
        val kind = p.attr("kind").orEmpty()
        val rank = p.attr("rank")?.toIntOrNull()
        val partial = p.attrBool("partial", false)
        return ModifierDef(target, kind, rank, partial)
    }
}
