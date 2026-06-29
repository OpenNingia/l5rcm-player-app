package com.l5rcm.companion.data.save

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * One entry of the save's `advans[]` history.
 *
 * The `.l5r` format discriminates entries by a literal `"type"` string value
 * (not by kotlinx's default class-discriminator key), so we resolve the concrete
 * subclass with a [JsonContentPolymorphicSerializer] keyed on that field. See
 * docs/FILE_FORMAT_L5R.md §3.
 */
@Serializable(with = AdvancementSerializer::class)
sealed class Advancement {
    abstract val type: String
    abstract val desc: String

    /** XP delta: positive = spent, negative = gained (flaws), 0 = free grant. */
    abstract val cost: Int
    abstract val timestamp: Double
    abstract val rule: String?
}

/** `type: "rank"` — an insight-rank step. The only advancement that nests typed sub-objects. */
@Serializable
data class RankAdv(
    override val type: String = "rank",
    override val desc: String = "",
    override val cost: Int = 0,
    override val timestamp: Double = 0.0,
    override val rule: String? = null,
    val rank: Int = 1,
    val school: String? = null,
    val school_rank: Int = 0,
    val replaced: String? = null,
    val skills: List<String> = emptyList(),
    val emphases: Map<String, List<String>> = emptyMap(),
    val spells: List<String> = emptyList(),
    val gained_spells_count: Int = 0,
    val affinities: List<String> = emptyList(),
    val deficiencies: List<String> = emptyList(),
    val kiho: List<String> = emptyList(),
    val gained_kiho_count: Int = 0,
    val merits: List<PerkAdv> = emptyList(),
    val flaws: List<PerkAdv> = emptyList(),
    val outfit: List<String> = emptyList(),
    val money: List<Int> = listOf(0, 0, 0),
) : Advancement() {
    /** Legacy saves stored `school_rank = 0`; the desktop back-fills it to [rank] on load. */
    val effectiveSchoolRank: Int get() = if (school_rank == 0) rank else school_rank
}

/** `type: "attrib"` — one rank bought in a trait (index into the trait array). */
@Serializable
data class AttribAdv(
    override val type: String = "attrib",
    override val desc: String = "",
    override val cost: Int = 0,
    override val timestamp: Double = 0.0,
    override val rule: String? = null,
    val attrib: Int = 0,
) : Advancement()

/** `type: "void"` — one void rank bought. */
@Serializable
data class VoidAdv(
    override val type: String = "void",
    override val desc: String = "",
    override val cost: Int = 0,
    override val timestamp: Double = 0.0,
    override val rule: String? = null,
) : Advancement()

/** `type: "skill"` — one rank bought in a skill. */
@Serializable
data class SkillAdv(
    override val type: String = "skill",
    override val desc: String = "",
    override val cost: Int = 0,
    override val timestamp: Double = 0.0,
    override val rule: String? = null,
    val skill: String = "",
) : Advancement()

/** `type: "emph"` — a skill emphasis acquired. */
@Serializable
data class SkillEmph(
    override val type: String = "emph",
    override val desc: String = "",
    override val cost: Int = 0,
    override val timestamp: Double = 0.0,
    override val rule: String? = null,
    val skill: String = "",
    val text: String = "",
) : Advancement()

/** `type: "kata"` — a kata learned. */
@Serializable
data class KataAdv(
    override val type: String = "kata",
    override val desc: String = "",
    override val cost: Int = 0,
    override val timestamp: Double = 0.0,
    override val rule: String? = null,
    val kata: String = "",
) : Advancement()

/** `type: "kiho"` — a kiho learned. */
@Serializable
data class KihoAdv(
    override val type: String = "kiho",
    override val desc: String = "",
    override val cost: Int = 0,
    override val timestamp: Double = 0.0,
    override val rule: String? = null,
    val kiho: String = "",
) : Advancement()

/** `type: "spell"` — a spell gained via rank/school (cost 0). */
@Serializable
data class SpellAdv(
    override val type: String = "spell",
    override val desc: String = "",
    override val cost: Int = 0,
    override val timestamp: Double = 0.0,
    override val rule: String? = null,
    val spell: String = "",
) : Advancement()

/** `type: "memo_spell"` — a spell memorized/bought (cost = mastery). */
@Serializable
data class MemoSpellAdv(
    override val type: String = "memo_spell",
    override val desc: String = "",
    override val cost: Int = 0,
    override val timestamp: Double = 0.0,
    override val rule: String? = null,
    val spell: String = "",
) : Advancement()

/**
 * `type: "perk"` — a merit (`tag="merit"`, cost > 0) or flaw (`tag="flaw"`, cost < 0).
 * Also used (without a `type` key) for the nested `merits[]`/`flaws[]` inside [RankAdv];
 * the [type] default keeps those instances valid.
 */
@Serializable
data class PerkAdv(
    override val type: String = "perk",
    override val desc: String = "",
    override val cost: Int = 0,
    override val timestamp: Double = 0.0,
    override val rule: String? = null,
    val perk: String = "",
    val rank: Int = 1,
    val tag: String? = null,
    val extra: String = "",
) : Advancement() {
    val isFlaw: Boolean get() = tag == "flaw" || cost < 0
    val isMerit: Boolean get() = tag == "merit" || (tag == null && cost > 0)
}

/** Fallback for any unrecognized advancement `type`, so parsing never fails on new kinds. */
@Serializable
data class GenericAdv(
    override val type: String = "",
    override val desc: String = "",
    override val cost: Int = 0,
    override val timestamp: Double = 0.0,
    override val rule: String? = null,
) : Advancement()

/** Picks the concrete [Advancement] subclass from the JSON `"type"` field. */
object AdvancementSerializer :
    JsonContentPolymorphicSerializer<Advancement>(Advancement::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<Advancement> {
        val type = element.jsonObject["type"]?.jsonPrimitive?.contentOrNull
        return when (type) {
            "rank" -> RankAdv.serializer()
            "attrib" -> AttribAdv.serializer()
            "void" -> VoidAdv.serializer()
            "skill" -> SkillAdv.serializer()
            "emph" -> SkillEmph.serializer()
            "kata" -> KataAdv.serializer()
            "kiho" -> KihoAdv.serializer()
            "spell" -> SpellAdv.serializer()
            "memo_spell" -> MemoSpellAdv.serializer()
            "perk" -> PerkAdv.serializer()
            else -> GenericAdv.serializer()
        }
    }
}
