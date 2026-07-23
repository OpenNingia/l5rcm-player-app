package com.l5rcm.companion.data.session

import kotlinx.coroutines.flow.Flow

/**
 * Reads/writes a character's [SessionNote]s (the play-session log, Theme 1). Purely additive
 * overlay state — it never touches the imported `.l5r`.
 */
class SessionNotesRepository(private val dao: SessionNoteDao) {

    fun observe(uuid: String): Flow<List<SessionNote>> = dao.observe(uuid)

    suspend fun save(note: SessionNote) = dao.upsert(note)

    suspend fun delete(id: Long) = dao.delete(id)
}
