package com.l5rcm.companion.data.session

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Room store for the per-character session overlay. `exportSchema = false` — schema history is
 * tracked by hand-written [Migration]s (the overlay is small, resettable play state).
 */
@Database(entities = [SessionState::class, SessionNote::class], version = 5, exportSchema = false)
abstract class SessionDatabase : RoomDatabase() {
    abstract fun sessionStateDao(): SessionStateDao
    abstract fun sessionNoteDao(): SessionNoteDao

    companion object {
        /** v1 → v2: wounds-only overlay grows to carry Void, stance, equipped weapon, Full Defense. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE session_state ADD COLUMN voidSpent INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE session_state ADD COLUMN stance TEXT")
                db.execSQL("ALTER TABLE session_state ADD COLUMN equippedWeapon TEXT")
                db.execSQL("ALTER TABLE session_state ADD COLUMN fullDefenseTotal INTEGER")
            }
        }

        /** v2 → v3: add the armor-worn toggle (defaults to worn, so existing rows keep their armor). */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE session_state ADD COLUMN armorEquipped INTEGER NOT NULL DEFAULT 1")
            }
        }

        /** v3 → v4: add per-element daily spell slots + the flexible Void pool (all start unspent). */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE session_state ADD COLUMN spellEarthSpent INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE session_state ADD COLUMN spellAirSpent INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE session_state ADD COLUMN spellWaterSpent INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE session_state ADD COLUMN spellFireSpent INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE session_state ADD COLUMN spellVoidSpent INTEGER NOT NULL DEFAULT 0")
            }
        }

        /** v4 → v5: add the one-to-many session-notes log (the play-session worklist). */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `session_notes` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`characterUuid` TEXT NOT NULL, " +
                        "`number` INTEGER NOT NULL, " +
                        "`date` INTEGER NOT NULL, " +
                        "`body` TEXT NOT NULL)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_session_notes_characterUuid` " +
                        "ON `session_notes` (`characterUuid`)",
                )
            }
        }
    }
}
