package com.l5rcm.companion.data.repository

import com.l5rcm.companion.data.save.PackRef
import com.l5rcm.companion.data.save.SaveModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pure-function coverage for the implicit `core` dependency (see the empty-`pack_refs` bug). */
class DatapackRepositoryTest {

    @Test
    fun `empty pack_refs still requires core`() {
        // Real desktop exports frequently ship an empty pack_refs list.
        val required = DatapackRepository.requiredDependencies(SaveModel(pack_refs = emptyList()))
        assertEquals(listOf("core"), required.map { it.id })
    }

    @Test
    fun `core is prepended when missing from explicit refs`() {
        val save = SaveModel(pack_refs = listOf(PackRef(id = "great_clan_pack", name = "Great Clans")))
        val ids = DatapackRepository.requiredDependencies(save).map { it.id }
        assertEquals(listOf("core", "great_clan_pack"), ids)
    }

    @Test
    fun `explicit core is not duplicated`() {
        val save = SaveModel(
            pack_refs = listOf(
                PackRef(id = "core", name = "Core book", version = "5.0"),
                PackRef(id = "book_of_air", name = "Book of Air"),
            ),
        )
        val required = DatapackRepository.requiredDependencies(save)
        assertEquals(listOf("core", "book_of_air"), required.map { it.id })
        // The explicit core (with its version) is kept, not replaced by the synthetic ref.
        assertEquals("5.0", required.first { it.id == "core" }.version)
    }

    @Test
    fun `blank ids are dropped`() {
        val save = SaveModel(pack_refs = listOf(PackRef(id = "", name = "junk")))
        val required = DatapackRepository.requiredDependencies(save)
        assertEquals(listOf("core"), required.map { it.id })
        assertTrue(required.none { it.id.isEmpty() })
    }
}
