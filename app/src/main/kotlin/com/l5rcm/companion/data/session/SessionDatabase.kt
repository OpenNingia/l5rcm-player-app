package com.l5rcm.companion.data.session

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Room store for the per-character session overlay. `exportSchema = false` for now — there are no
 * migrations yet (v1 session state); enable schema export once the entity starts evolving.
 */
@Database(entities = [SessionState::class], version = 1, exportSchema = false)
abstract class SessionDatabase : RoomDatabase() {
    abstract fun sessionStateDao(): SessionStateDao
}
