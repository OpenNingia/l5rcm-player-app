package com.l5rcm.companion.data.session

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A single **session note** — the player's log for one play session (Theme 1). Each is identified by
 * a [date] and a per-character sequential [number] (e.g. "16 giugno 2026 — Session #1"); the [body]
 * is the free-form worklist (XP earned, loot, plot beats) to later transcribe into the L5RCM desktop
 * app. This is Layer-B session state: a character owns many of these and none of it is ever written
 * back to the imported `.l5r`.
 */
@Entity(tableName = "session_notes", indices = [Index("characterUuid")])
data class SessionNote(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Owning character's stable uuid (same key the rest of the overlay uses). */
    val characterUuid: String,
    /** Per-character session counter, assigned on creation and never reused. */
    val number: Int,
    /** The session's date, epoch millis (only the calendar day is shown). */
    val date: Long,
    val body: String = "",
)
