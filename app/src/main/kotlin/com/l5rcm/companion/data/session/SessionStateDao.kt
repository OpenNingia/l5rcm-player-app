package com.l5rcm.companion.data.session

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionStateDao {

    /** Emits the overlay for [uuid], or null when no row exists yet (fall back to baseline). */
    @Query("SELECT * FROM session_state WHERE characterUuid = :uuid")
    fun observe(uuid: String): Flow<SessionState?>

    @Upsert
    suspend fun upsert(state: SessionState)

    /** Drops the overlay row so the character reverts to its derived baseline. */
    @Query("DELETE FROM session_state WHERE characterUuid = :uuid")
    suspend fun delete(uuid: String)
}
