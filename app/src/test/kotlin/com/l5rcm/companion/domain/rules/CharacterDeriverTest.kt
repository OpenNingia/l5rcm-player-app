package com.l5rcm.companion.domain.rules

import com.l5rcm.companion.data.datapack.ClanDef
import com.l5rcm.companion.data.datapack.DatapackBuilder
import com.l5rcm.companion.data.datapack.DatapackSet
import com.l5rcm.companion.data.datapack.FamilyDef
import com.l5rcm.companion.data.datapack.SchoolDef
import com.l5rcm.companion.data.datapack.SkillDef
import com.l5rcm.companion.data.datapack.Tech
import com.l5rcm.companion.data.save.AttribAdv
import com.l5rcm.companion.data.save.PerkAdv
import com.l5rcm.companion.data.save.RankAdv
import com.l5rcm.companion.data.save.SaveModel
import com.l5rcm.companion.data.save.SkillAdv
import com.l5rcm.companion.data.save.VoidAdv
import com.l5rcm.companion.data.save.WeaponOutfit
import com.l5rcm.companion.domain.model.Ring
import com.l5rcm.companion.domain.model.Trait
import org.junit.Assert.assertEquals
import org.junit.Test

class CharacterDeriverTest {

    private fun datapacks(): DatapackSet {
        val b = DatapackBuilder("core", "Core", "1.0")
        b.clans["crane"] = ClanDef("crane", "Crane")
        b.families["crane_doji"] = FamilyDef("crane_doji", "Doji", "crane", trait = "awareness")
        b.schools["kakita"] = SchoolDef(
            id = "kakita", name = "Kakita Bushi", clanId = "crane", trait = "reflexes", honor = 6.5,
            techs = listOf(Tech("crane_way", "The Way Of The Crane", 1, "desc")),
        )
        b.skills["kenjutsu"] = SkillDef("kenjutsu", "Kenjutsu", "agility", "bugei")
        b.skills["iaijutsu"] = SkillDef("iaijutsu", "Iaijutsu", "reflexes", "bugei")
        return DatapackSet(listOf(b.build()))
    }

    private fun sampleSave() = SaveModel(
        name = "Test",
        clan = "crane",
        family = "crane_doji",
        starting_traits = listOf(2, 2, 2, 2, 2, 2, 2, 2),
        starting_void = 2,
        honor = 0.0,
        exp_limit = 40,
        advans = listOf(
            RankAdv(
                rank = 1, school = "kakita", school_rank = 1,
                skills = listOf("kenjutsu", "iaijutsu"),
                emphases = mapOf("iaijutsu" to listOf("Focus")),
            ),
            AttribAdv(attrib = Trait.REFLEXES.index, cost = 4),
            SkillAdv(skill = "kenjutsu", cost = 2),
            VoidAdv(cost = 6),
            PerkAdv(perk = "wealthy", tag = "merit", cost = 3),
            PerkAdv(perk = "brash", tag = "flaw", cost = -2),
        ),
        weapons = listOf(
            // trait "water" here is deliberately a ring, not a trait: the skill's own trait
            // (Kenjutsu → Agility) must win when building the attack pool.
            WeaponOutfit(name = "Katana", dr = "3k2", skill_id = "kenjutsu", skill_nm = "Kenjutsu", trait = "water"),
            // A bow whose Strength Rating (1) is below the wielder's Strength (2): the ranged
            // damage roll must cap the added Strength at the bow's rating.
            WeaponOutfit(name = "Yumi", dr = "2k2", strength = "1", tags = listOf("ranged"), skill_nm = "Kyujutsu"),
        ),
    )

    @Test
    fun derivesTraitsRingsAndVoid() {
        val v = CharacterDeriver.derive(sampleSave(), datapacks())
        // reflexes: base 2 + 1 bought + school(+1) = 4; awareness: base 2 + family(+1) = 3.
        assertEquals(4, v.traits.first { it.trait == Trait.REFLEXES }.rank)
        assertEquals(3, v.traits.first { it.trait == Trait.AWARENESS }.rank)
        assertEquals(2, v.rings.first { it.ring == Ring.EARTH }.rank) // min(2,2)
        assertEquals(3, v.rings.first { it.ring == Ring.AIR }.rank)   // min(4,3)
        assertEquals(3, v.voidRank) // 2 + 1 bought
    }

    @Test
    fun derivesSkillsAndEmphases() {
        val v = CharacterDeriver.derive(sampleSave(), datapacks())
        val kenjutsu = v.skills.first { it.id == "kenjutsu" }
        assertEquals(2, kenjutsu.rank) // 1 granted + 1 bought
        val iaijutsu = v.skills.first { it.id == "iaijutsu" }
        assertEquals(1, iaijutsu.rank)
        assertEquals(listOf("Focus"), iaijutsu.emphases)
    }

    @Test
    fun derivesInsight() {
        val v = CharacterDeriver.derive(sampleSave(), datapacks())
        // rings (2+3+2+2+3)*10 = 120; skills 2+1 = 3 → 123 → rank 1.
        assertEquals(123, v.insightValue)
        assertEquals(1, v.insightRank)
    }

    @Test
    fun derivesHealthAndCombat() {
        val v = CharacterDeriver.derive(sampleSave(), datapacks())
        assertEquals(2, v.health.earthRank)
        assertEquals(38, v.health.maxWounds) // 2*5 + 7*(2*2)
        assertEquals(5, v.health.healRate)   // stamina 2 *2 + insight 1
        assertEquals(25, v.combat.baseTn)    // reflexes 4 *5 +5
        assertEquals(25, v.combat.fullTn)    // no armor
        assertEquals(5, v.combat.initiativeRoll) // insight 1 + reflexes 4
        assertEquals(4, v.combat.initiativeKeep)
        // Wound penalties array for Nicked..
        assertEquals(0, v.health.levels[0].penalty)
        assertEquals(3, v.health.levels[1].penalty)
        assertEquals(40, v.health.levels[6].penalty)
        assertEquals(null, v.health.levels[7].penalty) // Out
    }

    @Test
    fun derivesXp() {
        val v = CharacterDeriver.derive(sampleSave(), datapacks())
        assertEquals(15, v.xp.spent)      // 4 + 2 + 6 + 3
        assertEquals(2, v.xp.fromFlaws)   // -(-2)
        assertEquals(42, v.xp.available)  // 40 + 2
        assertEquals(27, v.xp.left)       // 42 - 15
    }

    @Test
    fun resolvesTechniqueFromSchoolAndRank() {
        val v = CharacterDeriver.derive(sampleSave(), datapacks())
        assertEquals("The Way Of The Crane", v.techniques.single().name)
    }

    @Test
    fun honorIsSchoolStartingPlusDelta() {
        val v = CharacterDeriver.derive(sampleSave().copy(honor = 0.3), datapacks())
        assertEquals(6.8, v.honor, 0.0001)
    }

    @Test
    fun weakTraitReducesModifiedRank() {
        val save = sampleSave().copy(
            advans = sampleSave().advans + PerkAdv(perk = "weak", rule = "weak_reflexes", cost = -3, tag = "flaw"),
        )
        val v = CharacterDeriver.derive(save, datapacks())
        val reflexes = v.traits.first { it.trait == Trait.REFLEXES }
        assertEquals(4, reflexes.rank)
        assertEquals(3, reflexes.modifiedRank) // -1 from weak_reflexes
    }

    @Test
    fun derivesWeaponAttackAndDamagePools() {
        val v = CharacterDeriver.derive(sampleSave(), datapacks())
        val katana = v.weapons.first { it.name == "Katana" }
        // Attack = (Agility 2 + Kenjutsu 2) k Agility 2 = 4k2 (skill's Trait beats the weapon's).
        assertEquals(4, katana.attackRolled)
        assertEquals(2, katana.attackKept)
        // Damage = Strength 2 + weapon DR 3k2 = 5k2 (Strength folds into the rolled dice).
        assertEquals(5, katana.damageRolled)
        assertEquals(2, katana.damageKept)
        assertEquals("5k2", katana.damageRoll)

        // Ranged: the Yumi's Strength Rating (1) is below the wielder's Strength (2), so its
        // damage adds only 1 → 1 + DR 2k2 = 3k2.
        val yumi = v.weapons.first { it.name == "Yumi" }
        assertEquals(3, yumi.damageRolled)
        assertEquals(2, yumi.damageKept)
    }

    @Test
    fun insightRankTableBoundaries() {
        assertEquals(1, CharacterDeriver.insightRankFromValue(149))
        assertEquals(2, CharacterDeriver.insightRankFromValue(150))
        assertEquals(2, CharacterDeriver.insightRankFromValue(174))
        assertEquals(3, CharacterDeriver.insightRankFromValue(175))
        assertEquals(9, CharacterDeriver.insightRankFromValue(325))
        assertEquals(10, CharacterDeriver.insightRankFromValue(350))
        assertEquals(11, CharacterDeriver.insightRankFromValue(375))
    }
}
