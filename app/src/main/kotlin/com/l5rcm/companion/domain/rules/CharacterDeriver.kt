package com.l5rcm.companion.domain.rules

import com.l5rcm.companion.data.datapack.DatapackSet
import com.l5rcm.companion.data.save.AttribAdv
import com.l5rcm.companion.data.save.KataAdv
import com.l5rcm.companion.data.save.KihoAdv
import com.l5rcm.companion.data.save.MemoSpellAdv
import com.l5rcm.companion.data.save.PerkAdv
import com.l5rcm.companion.data.save.SaveModel
import com.l5rcm.companion.data.save.SkillAdv
import com.l5rcm.companion.data.save.SkillEmph
import com.l5rcm.companion.data.save.SpellAdv
import com.l5rcm.companion.data.save.VoidAdv
import com.l5rcm.companion.domain.model.ArmorView
import com.l5rcm.companion.domain.model.CharacterView
import com.l5rcm.companion.domain.model.CombatView
import com.l5rcm.companion.domain.model.HealthView
import com.l5rcm.companion.domain.model.Money
import com.l5rcm.companion.domain.model.ModifierView
import com.l5rcm.companion.domain.model.NamedEntry
import com.l5rcm.companion.domain.model.PackRefView
import com.l5rcm.companion.domain.model.PerkView
import com.l5rcm.companion.domain.model.Ring
import com.l5rcm.companion.domain.model.RingView
import com.l5rcm.companion.domain.model.SchoolRankView
import com.l5rcm.companion.domain.model.SkillView
import com.l5rcm.companion.domain.model.SpellEntry
import com.l5rcm.companion.domain.model.SpellsView
import com.l5rcm.companion.domain.model.TechniqueView
import com.l5rcm.companion.domain.model.Trait
import com.l5rcm.companion.domain.model.TraitView
import com.l5rcm.companion.domain.model.WeaponView
import com.l5rcm.companion.domain.model.WoundLevel

/**
 * Pure derivation engine — the heart of the companion. Reproduces the desktop's
 * pure-arithmetic subset (citations in docs plan §4) to assemble a [CharacterView]
 * from a [SaveModel] + [DatapackSet]. No Android dependencies; fully unit-testable.
 *
 * **v1 approximation:** dynamic datapack `ModifierDef` expressions are NOT evaluated.
 * Only flat family/school +1 bonuses, simple rule flags, and the save's own
 * `ModifierModel` entries (artn/arrd/hrnk/wpen) are applied.
 */
object CharacterDeriver {

    val WOUND_LEVEL_NAMES = listOf(
        "Healthy", "Nicked", "Grazed", "Hurt", "Injured", "Crippled", "Down", "Out",
    )

    /** Base TN penalty per wound level 0..6 (level 7 "Out" has no TN). */
    val WOUND_PENALTIES = intArrayOf(0, 3, 5, 10, 15, 20, 40)

    fun derive(save: SaveModel, packs: DatapackSet): CharacterView = Derivation(save, packs).build()

    /** Insight value → insight rank, per the RAW threshold table (rules/__init__.py:615-645). */
    fun insightRankFromValue(value: Int): Int = when {
        value > 349 -> (value - 349) / 25 + 10
        value > 324 -> 9
        value > 299 -> 8
        value > 274 -> 7
        value > 249 -> 6
        value > 224 -> 5
        value > 199 -> 4
        value > 174 -> 3
        value > 149 -> 2
        else -> 1
    }

    /** Encapsulates one derivation pass so intermediate values are computed once. */
    private class Derivation(val save: SaveModel, val packs: DatapackSet) {

        private val firstRank = save.rankAdvancements.firstOrNull()
        private val firstSchoolDef = packs.school(firstRank?.school)
        private val familyDef = packs.family(save.family)

        // --- Traits / rings / void (character/__init__.py:354-427) ---

        fun traitRank(t: Trait): Int {
            val base = save.starting_traits.getOrElse(t.index) { 2 }
            val bought = save.advans.filterIsInstance<AttribAdv>().count { it.attrib == t.index }
            val family = if (familyDef?.trait == t.id) 1 else 0
            val school = if (firstSchoolDef?.trait == t.id) 1 else 0
            return base + bought + family + school
        }

        fun modifiedTraitRank(t: Trait): Int =
            traitRank(t) - if (save.hasRule("weak_${t.id}")) 1 else 0

        fun voidRank(): Int {
            val base = save.starting_void
            val bought = save.advans.filterIsInstance<VoidAdv>().size
            val family = if (familyDef?.trait == "void") 1 else 0
            val school = if (firstSchoolDef?.trait == "void") 1 else 0
            return base + bought + family + school
        }

        fun ringRank(ring: Ring): Int = when (ring) {
            Ring.VOID -> voidRank()
            else -> ring.traits.minOf { traitRank(it) }
        }

        // --- Skills (skills.py) ---

        private val skillIds: List<String> = buildList {
            save.rankAdvancements.forEach { addAll(it.skills) }
            save.advans.filterIsInstance<SkillAdv>().forEach { add(it.skill) }
        }.distinct()

        fun skillRank(id: String): Int {
            val fromRanks = save.rankAdvancements.sumOf { rank -> rank.skills.count { it == id } }
            val bought = save.advans.filterIsInstance<SkillAdv>().count { it.skill == id }
            return fromRanks + bought
        }

        fun skillEmphases(id: String): List<String> = buildList {
            save.rankAdvancements.forEach { addAll(it.emphases[id].orEmpty()) }
            save.advans.filterIsInstance<SkillEmph>().filter { it.skill == id }.forEach { add(it.text) }
        }.filter { it.isNotBlank() }.distinct()

        // --- Insight (rules/__init__.py:142-195) ---

        fun insightValue(): Int {
            val ringSum = Ring.entries.sumOf { ringRank(it) * 10 }
            val skillSum = when (save.insight_calculation) {
                // Method 2: skills at rank 1 don't contribute.
                2 -> skillIds.map { skillRank(it) }.filter { it != 1 }.sum()
                // Method 1 (default) and 3 ≈ count every skill rank.
                else -> skillIds.sumOf { skillRank(it) }
            }
            val mastery = 3 * save.countRule("ma_insight_plus_3") + 7 * save.countRule("ma_insight_plus_7")
            return ringSum + skillSum + mastery
        }

        fun insightRank(): Int = insightRankFromValue(insightValue())

        // --- Reputation (datapack-derived starting + stored delta) ---

        fun honor(): Double = (firstSchoolDef?.honor ?: 0.0) + save.honor

        // --- Health / wounds (rules/__init__.py:474-528) ---

        private fun activeModSum(type: String): Int =
            save.modifiers.filter { it.active && it.type == type }.sumOf { it.finalValue }

        fun healthRankMod(): Int {
            var mod = 0
            if (save.hasRule("crane_the_force_of_honor")) {
                mod = maxOf(1, (honor() - 4).toInt())
            }
            mod += activeModSum("hrnk")
            return mod
        }

        fun healthRank(index: Int): Int {
            val earth = ringRank(Ring.EARTH)
            val mod = healthRankMod()
            return if (index == 0) {
                earth * save.health_base_multiplier + mod
            } else {
                earth * save.health_multiplier + mod
            }
        }

        fun maxWounds(): Int = (0..7).sumOf { healthRank(it) }

        fun woundPenalty(level: Int): Int {
            var result = WOUND_PENALTIES.getOrElse(level) { 0 }
            if (save.hasRule("strength_of_earth")) result = maxOf(0, result - 3)
            if (save.hasRule("monkey_tokus_lesson")) {
                val reduction = insightRank() * 2 + traitRank(Trait.WILLPOWER)
                result = maxOf(0, result - reduction)
            }
            result = maxOf(0, result - activeModSum("wpen"))
            return result
        }

        fun healRate(): Int = modifiedTraitRank(Trait.STAMINA) * 2 + insightRank()

        // --- Defense / initiative (character/__init__.py:430-488, rules:445-471) ---

        fun baseTn(): Int = traitRank(Trait.REFLEXES) * 5 + 5
        fun fullTn(): Int = baseTn() + (save.armor?.tn ?: 0) + activeModSum("artn")
        fun baseRd(): Int = if (save.hasRule("crab_the_mountain_does_not_move")) ringRank(Ring.EARTH) else 0
        fun fullRd(): Int = (save.armor?.rd ?: 0) + baseRd() + activeModSum("arrd")
        fun initiativeRoll(): Int = insightRank() + traitRank(Trait.REFLEXES)
        fun initiativeKeep(): Int = traitRank(Trait.REFLEXES)

        // --- XP (character/__init__.py:235-256) ---

        fun xpSpent(): Int = save.advans.filter { it.cost > 0 }.sumOf { it.cost }
        fun xpFromFlaws(): Int =
            save.advans.filterIsInstance<PerkAdv>().filter { it.isFlaw }.sumOf { -it.cost }
        fun xpAvailable(): Int = save.exp_limit + xpFromFlaws()
        fun xpLeft(): Int = xpAvailable() - xpSpent()

        // --- Assembly ---

        fun build(): CharacterView {
            val insightV = insightValue()
            val insightR = insightRankFromValue(insightV)

            return CharacterView(
                name = save.name,
                clanId = save.clan,
                clanName = packs.clan(save.clan)?.name ?: save.clan,
                familyName = familyDef?.name ?: save.family,
                schoolName = firstSchoolDef?.name ?: firstRank?.school,
                traits = Trait.entries.map { TraitView(it, traitRank(it), modifiedTraitRank(it)) },
                rings = Ring.elementRings.map { RingView(it, ringRank(it)) },
                voidRank = voidRank(),
                insightRank = insightR,
                insightValue = insightV,
                honor = honor(),
                glory = 1.0 + save.glory,
                status = 1.0 + save.status,
                infamy = save.infamy,
                taint = save.taint,
                health = buildHealth(insightR),
                combat = CombatView(baseTn(), fullTn(), baseRd(), fullRd(), initiativeRoll(), initiativeKeep()),
                xp = com.l5rcm.companion.domain.model.XpView(xpSpent(), xpAvailable(), xpLeft(), xpFromFlaws()),
                skills = buildSkills(),
                schools = buildSchools(),
                techniques = buildTechniques(),
                spells = buildSpells(),
                katas = buildKatas(),
                kihos = buildKihos(),
                merits = buildPerks(merits = true),
                flaws = buildPerks(merits = false),
                weapons = buildWeapons(),
                armor = save.armor?.let { ArmorView(it.name, it.tn, it.rd) },
                modifiers = save.modifiers.map { ModifierView(it.type, it.dtl, it.finalValue, it.reason, it.active) },
                money = Money(
                    save.money.getOrElse(0) { 0 },
                    save.money.getOrElse(1) { 0 },
                    save.money.getOrElse(2) { 0 },
                ),
                properties = save.properties,
                packRefs = save.pack_refs.map {
                    PackRefView(it.id, it.name, it.version, installed = it.id in packs.packIds)
                },
            )
        }

        private fun buildHealth(insightR: Int): HealthView {
            val levels = WOUND_LEVEL_NAMES.mapIndexed { i, levelName ->
                val cumulative = (0..i).sumOf { healthRank(it) }
                WoundLevel(
                    name = levelName,
                    threshold = cumulative,
                    penalty = if (i <= 6) woundPenalty(i) else null,
                )
            }
            return HealthView(
                earthRank = ringRank(Ring.EARTH),
                currentWounds = save.wounds,
                maxWounds = maxWounds(),
                healRate = healRate(),
                levels = levels,
            )
        }

        private fun buildSkills(): List<SkillView> = skillIds
            .map { id ->
                val def = packs.skill(id)
                SkillView(
                    id = id,
                    name = def?.name ?: id,
                    rank = skillRank(id),
                    trait = def?.trait,
                    category = def?.type?.let { packs.skillCategoriesById[it]?.name ?: it },
                    emphases = skillEmphases(id),
                )
            }
            .sortedBy { it.name.lowercase() }

        private fun buildSchools(): List<SchoolRankView> {
            val ids = save.rankAdvancements.mapNotNull { it.school }.distinct()
            return ids.map { id ->
                val rank = save.rankAdvancements.count { it.school == id || it.replaced == id }
                SchoolRankView(id, packs.school(id)?.name ?: id, rank)
            }
        }

        private fun buildTechniques(): List<TechniqueView> =
            save.rankAdvancements.filter { it.school != null }.mapNotNull { ra ->
                val schoolDef = packs.school(ra.school)
                val tech = schoolDef?.techAtRank(ra.effectiveSchoolRank)
                TechniqueView(
                    id = tech?.id ?: "",
                    name = tech?.name ?: "Technique (school rank ${ra.effectiveSchoolRank})",
                    schoolName = schoolDef?.name ?: ra.school,
                    insightRank = ra.rank,
                    schoolRank = ra.effectiveSchoolRank,
                    description = tech?.description.orEmpty(),
                )
            }

        private fun buildSpells(): SpellsView {
            val known = buildList {
                save.rankAdvancements.forEach { addAll(it.spells) }
                save.advans.filterIsInstance<SpellAdv>().forEach { add(it.spell) }
            }.distinct().map { spellEntry(it) }
            val memorized = save.advans.filterIsInstance<MemoSpellAdv>()
                .map { it.spell }.distinct().map { spellEntry(it) }
            val affinities = save.rankAdvancements.flatMap { it.affinities }.distinct()
            val deficiencies = save.rankAdvancements.flatMap { it.deficiencies }.distinct()
            return SpellsView(known, memorized, affinities, deficiencies)
        }

        private fun spellEntry(id: String): SpellEntry {
            val def = packs.spell(id)
            return SpellEntry(
                id = id,
                name = def?.name ?: id,
                element = def?.element,
                mastery = def?.mastery ?: 0,
                range = def?.range.orEmpty(),
                area = def?.area.orEmpty(),
                duration = def?.duration.orEmpty(),
                description = def?.desc.orEmpty(),
            )
        }

        private fun buildKatas(): List<NamedEntry> =
            save.advans.filterIsInstance<KataAdv>().map { it.kata }.distinct().map { id ->
                val def = packs.kata(id)
                NamedEntry(id, def?.name ?: id, def?.desc.orEmpty())
            }

        private fun buildKihos(): List<NamedEntry> =
            save.advans.filterIsInstance<KihoAdv>().map { it.kiho }.distinct().map { id ->
                val def = packs.kiho(id)
                NamedEntry(id, def?.name ?: id, def?.desc.orEmpty())
            }

        private fun buildPerks(merits: Boolean): List<PerkView> {
            val starting = (firstRank?.merits.orEmpty().takeIf { merits }
                ?: firstRank?.flaws.orEmpty())
            val bought = save.advans.filterIsInstance<PerkAdv>()
                .filter { if (merits) it.isMerit else it.isFlaw }
            return (starting + bought).map { perk ->
                PerkView(
                    id = perk.perk,
                    name = packs.perk(perk.perk)?.name ?: perk.perk,
                    rank = perk.rank,
                    isFlaw = !merits,
                )
            }
        }

        private fun buildWeapons(): List<WeaponView> = save.weapons.map { w ->
            val skillDef = w.skill_id?.let { packs.skill(it) }
            val skillName = w.skill_nm.ifEmpty { skillDef?.name.orEmpty() }

            // Attack pool = (Trait + skill rank) k Trait — the standard skill roll for the weapon.
            // Prefer the skill's associated Trait; fall back to the weapon's own trait id, then to
            // any base_atk notation the desktop pre-computed, then to an empty (0k0) pool.
            val attackTrait = Trait.byId(skillDef?.trait ?: "") ?: Trait.byId(w.trait)
            val (atkRolled, atkKept) = when {
                attackTrait != null -> {
                    val t = traitRank(attackTrait)
                    (t + w.skill_id?.let { skillRank(it) }.orZero()) to t
                }
                else -> com.l5rcm.companion.domain.dice.parseNotation(w.base_atk) ?: (0 to 0)
            }
            // Damage roll = (Strength + weapon DR rolled) k (weapon DR kept), matching the desktop's
            // calculate_base_damage_roll. A ranged weapon caps the added Strength at its own
            // Strength Rating: a bow uses min(bow strength, wielder Strength).
            val (drRolled, drKept) = com.l5rcm.companion.domain.dice.parseNotation(w.dr) ?: (0 to 0)
            val weaponStrength = w.strength.toIntOrNull() ?: 0
            val strengthForDamage = modifiedTraitRank(Trait.STRENGTH).let { str ->
                if ("ranged" in w.tags && weaponStrength > 0) minOf(weaponStrength, str) else str
            }
            val dmgRolled = drRolled + strengthForDamage
            val dmgKept = drKept

            WeaponView(
                name = w.name,
                skillName = skillName,
                // Show the effective damage pool (Strength folded in), falling back to the raw DR
                // notation when it doesn't parse into a real pool.
                damageRoll = if (drKept > 0) "${dmgRolled}k$dmgKept" else w.dr,
                range = w.range,
                tags = w.tags,
                attackRolled = atkRolled,
                attackKept = atkKept,
                damageRolled = dmgRolled,
                damageKept = dmgKept,
            )
        }

        private fun Int?.orZero(): Int = this ?: 0
    }
}
