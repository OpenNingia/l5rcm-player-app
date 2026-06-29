package com.l5rcm.companion.data.save

import com.l5rcm.companion.resourceText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SaveParserTest {

    private fun load(name: String): SaveModel = SaveParser.parse(resourceText("saves/$name"))

    @Test
    fun parsesIdentityAndAdvancementDiscrimination() {
        val save = load("doji_sumata.l5r")
        assertEquals("Sumata", save.name)
        assertEquals("crane", save.clan)
        assertEquals("crane_doji", save.family)
        // Discriminator picks the right subclasses.
        assertEquals(2, save.advans.filterIsInstance<RankAdv>().size)
        assertEquals(8, save.advans.filterIsInstance<AttribAdv>().size)
        assertEquals(2, save.advans.filterIsInstance<VoidAdv>().size)
        assertEquals(4, save.advans.filterIsInstance<SkillAdv>().size)
        assertEquals(1, save.advans.filterIsInstance<SkillEmph>().size)
    }

    @Test
    fun rankAdvancementCarriesSchoolAndGrantedSkills() {
        val save = load("doji_sumata.l5r")
        val first = save.rankAdvancements.first()
        assertEquals("crane_doji_courtier_school", first.school)
        assertTrue("etiquette" in first.skills)
        // school_rank back-fill: legacy 0 → rank.
        assertEquals(first.rank, first.effectiveSchoolRank)
    }

    @Test
    fun perkAdvancementsParse() {
        val save = load("kitsuki_jondavid.l5r")
        val perks = save.advans.filterIsInstance<PerkAdv>()
        assertEquals(7, perks.size)
        // Every perk classifies as merit or flaw.
        assertTrue(perks.all { it.isMerit || it.isFlaw })
    }

    @Test
    fun propertiesKeepStringsAndDropArrayValues() {
        // Real saves carry array junk in `properties` ("money":[0,0,0], "equip":[]).
        val save = load("kitsuki_jondavid.l5r")
        assertEquals("Male", save.properties["sex"])
        assertTrue("money" !in save.properties) // array value dropped, not crashed
        assertTrue("equip" !in save.properties)
    }

    @Test
    fun weaponStrengthAcceptsNonNumericNAString() {
        // Desktop writes `weapon.strength or 'N/A'` (outfit.py), so strength/min_str are an
        // int OR the literal string "N/A". Parsing must not crash on the string form.
        val save = load("kitsuki_jondavid.l5r")
        val wakizashi = save.weapons.first { it.name == "Wakizashi" }
        assertEquals("N/A", wakizashi.strength)
        assertEquals("N/A", wakizashi.min_str)
    }

    @Test
    fun defaultsApplyForOmittedAndLegacyFields() {
        val save = SaveParser.parse("""{"name":"X"}""")
        assertEquals(listOf(2, 2, 2, 2, 2, 2, 2, 2), save.starting_traits)
        assertEquals(2, save.starting_void)
        assertEquals(40, save.exp_limit)
        assertEquals(5, save.health_base_multiplier)
    }

    @Test
    fun unknownAdvancementTypeFallsBackToGeneric() {
        val save = SaveParser.parse(
            """{"advans":[{"type":"future_thing","cost":3,"desc":"d"}]}""",
        )
        assertEquals(1, save.advans.size)
        assertTrue(save.advans.first() is GenericAdv)
        assertEquals(3, save.advans.first().cost)
    }
}
