package com.l5rcm.companion.data.session

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Reads/writes the per-character session overlay. Values are absolute (the current wounds taken),
 * so callers combine the emitted override with the derived baseline: a null override means the
 * player hasn't tracked anything yet and the baseline applies.
 */
class SessionRepository(private val dao: SessionStateDao) {

    /** Current wounds override for [uuid], or null when none has been recorded yet. */
    fun observeWounds(uuid: String): Flow<Int?> = dao.observe(uuid).map { it?.wounds }

    suspend fun setWounds(uuid: String, wounds: Int) = dao.upsert(SessionState(uuid, wounds))

    /** Clears the overlay so the character reverts to its imported/derived baseline. */
    suspend fun resetWounds(uuid: String) = dao.delete(uuid)
}
