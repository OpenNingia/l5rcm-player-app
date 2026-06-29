package com.l5rcm.companion.data.datapack

import com.l5rcm.companion.TestXmlParserFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.StringReader

class DatapackParserTest {

    private val xml = """
        <?xml version="1.0" encoding="utf-8"?>
        <L5RCM>
          <Clan name="Crane" id="crane"/>
          <Family name="Doji" id="crane_doji" clanid="crane"><Trait>awareness</Trait></Family>
          <SkillDef trait="strength" type="bugei" id="kenjutsu" name="Kenjutsu">
            <Tags><Tag>bugei</Tag></Tags>
            <Description>The art of the sword.</Description>
            <MasteryAbilities>
              <MasteryAbility rank="3">Free attack</MasteryAbility>
            </MasteryAbilities>
          </SkillDef>
          <SkillCateg id="bugei">Bugei (Martial)</SkillCateg>
          <RingDef id="earth">Earth</RingDef>
          <TraitDef id="awareness">Awareness</TraitDef>
          <School clanid="crane" id="crane_kakita_bushi_school" name="Kakita Bushi School">
            <Trait>reflexes</Trait>
            <Honor>6.5</Honor>
            <Tags><Tag>bushi</Tag><Tag>crane_bushi</Tag></Tags>
            <Skills>
              <Skill id="kenjutsu" rank="1"/>
              <Skill emphases="Focus" id="iaijutsu" rank="1"/>
              <PlayerChoose rank="1"><Wildcard>bugei</Wildcard></PlayerChoose>
            </Skills>
            <Spells/>
            <Techs>
              <Tech id="crane_the_way_of_the_crane" name="The Way Of The Crane" rank="1">
                <Description>Add twice Iaijutsu to Initiative.</Description>
              </Tech>
              <Tech id="crane_speed_of_lightning" name="Speed Of Lightning" rank="2">
                <Description>+2k0 vs lower initiative.</Description>
              </Tech>
            </Techs>
            <Outfit bu="0" koku="10" zeni="0">
              <Item>Daisho</Item>
            </Outfit>
          </School>
          <SpellDef id="fires_of_purity" name="Fires of Purity" element="fire" mastery="3" range="Touch"/>
          <Merit type="material" id="wealthy" name="Wealthy">
            <Rank id="1" value="3"/>
            <Description>Rich.</Description>
          </Merit>
          <Flaw type="mental" id="brash" name="Brash">
            <Rank id="1" value="2"/>
            <Description>Hotheaded.</Description>
          </Flaw>
          <Weapon skill="kenjutsu" dr="3k2" name="Katana">
            <Tags><Tag>medium</Tag><Tag>samurai</Tag></Tags>
            <Effect id="void_rise_dr_rule"/>
          </Weapon>
          <Armor name="Light Armor" tn="5" rd="3"/>
          <EffectDef id="void_rise_dr_rule">Spend a Void Point to increase damage.</EffectDef>
        </L5RCM>
    """.trimIndent()

    private fun parse(): Datapack {
        val parser = DatapackParser(TestXmlParserFactory)
        val builder = DatapackBuilder("test", "Test Pack", "1.0")
        parser.parseXml(StringReader(xml), builder)
        return builder.build()
    }

    @Test
    fun dispatchesEntitiesByTag() {
        val pack = parse()
        assertEquals(1, pack.clansById.size)
        assertEquals("Crane", pack.clansById["crane"]?.name)
        assertEquals("awareness", pack.familiesById["crane_doji"]?.trait)
        assertEquals("strength", pack.skillsById["kenjutsu"]?.trait)
        assertEquals("Bugei (Martial)", pack.skillCategoriesById["bugei"]?.name)
        assertEquals("Earth", pack.ringsById["earth"]?.name)
        assertEquals("Awareness", pack.traitsById["awareness"]?.name)
    }

    @Test
    fun parsesSchoolWithTraitHonorSkillsAndTechs() {
        val school = parse().schoolsById["crane_kakita_bushi_school"]!!
        assertEquals("reflexes", school.trait)
        assertEquals(6.5, school.honor, 0.0001)
        assertTrue("bushi" in school.tags)
        assertTrue(school.skills.any { it.id == "kenjutsu" })
        assertEquals("Focus", school.skills.first { it.id == "iaijutsu" }.emphases)
        // Technique-at-rank lookup (the save→datapack bridge).
        assertEquals("The Way Of The Crane", school.techAtRank(1)?.name)
        assertEquals("Speed Of Lightning", school.techAtRank(2)?.name)
        assertEquals(listOf(10, 0, 0), school.money)
        assertTrue("Daisho" in school.outfit)
    }

    @Test
    fun parsesSkillMasteryAndSpellAndPerksAndWeapon() {
        val pack = parse()
        assertEquals(1, pack.skillsById["kenjutsu"]?.masteryAbilities?.size)
        assertEquals("fire", pack.spellsById["fires_of_purity"]?.element)
        assertEquals(3, pack.spellsById["fires_of_purity"]?.mastery)
        assertTrue(pack.perksById["wealthy"]!!.isMerit)
        assertTrue(!pack.perksById["brash"]!!.isMerit)
        val katana = pack.weaponsByName["Katana"]!!
        assertEquals("3k2", katana.dr)
        assertEquals("void_rise_dr_rule", katana.effectId)
        assertEquals(5, pack.armorsByName["Light Armor"]?.tn)
        assertNotNull(pack.effectsById["void_rise_dr_rule"])
    }
}
