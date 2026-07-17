package com.l5rcm.companion.data.session

import kotlinx.coroutines.flow.Flow

/**
 * Reads/writes the per-character session overlay. Values are absolute (current wounds taken, Void
 * spent, chosen stance, equipped weapon…), so callers combine the emitted row with the derived
 * baseline: a null row means the player hasn't tracked anything yet and the baseline applies.
 */
class SessionRepository(private val dao: SessionStateDao) {

    /** The whole overlay row for [uuid], or null when none has been recorded yet. */
    fun observe(uuid: String): Flow<SessionState?> = dao.observe(uuid)

    /** Upsert the full overlay row. Callers merge onto the current state before saving. */
    suspend fun save(state: SessionState) = dao.upsert(state)

    /** Clears the overlay so the character reverts to its imported/derived baseline. */
    suspend fun clear(uuid: String) = dao.delete(uuid)
}
