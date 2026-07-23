package com.l5rcm.companion.data.session

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionNoteDao {

    /** All of [uuid]'s session notes, newest session first. */
    @Query("SELECT * FROM session_notes WHERE characterUuid = :uuid ORDER BY number DESC")
    fun observe(uuid: String): Flow<List<SessionNote>>

    /** Insert (id = 0) or update an existing note. */
    @Upsert
    suspend fun upsert(note: SessionNote)

    @Query("DELETE FROM session_notes WHERE id = :id")
    suspend fun delete(id: Long)
}
