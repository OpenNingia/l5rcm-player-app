package com.l5rcm.companion.data.session

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Per-character play-time session overlay, keyed by the character's stable `uuid`. This is the
 * local, resettable state the companion tracks at the table (Theme 1) — it **never** writes back
 * to the imported `.l5r` and is kept separate from the parity-tested derived sheet (Layer A/B).
 *
 * A missing row means "no overlay yet": the effective value falls back to the derived baseline.
 * For wounds, [wounds] is the *absolute* current wounds taken once the player has touched it.
 * Fields will grow as later Theme-1 pieces land (Void points, spell slots, conditions).
 */
@Entity(tableName = "session_state")
data class SessionState(
    @PrimaryKey val characterUuid: String,
    val wounds: Int = 0,
)
