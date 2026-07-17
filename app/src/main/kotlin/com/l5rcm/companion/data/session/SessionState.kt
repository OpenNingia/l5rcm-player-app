package com.l5rcm.companion.data.session

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Per-character play-time session overlay, keyed by the character's stable `uuid`. This is the
 * local, resettable state the companion tracks at the table (Theme 1) — it **never** writes back
 * to the imported `.l5r` and is kept separate from the parity-tested derived sheet (Layer A/B).
 *
 * A missing row means "no overlay yet": the effective value falls back to the derived baseline.
 * For wounds, [wounds] is the *absolute* current wounds taken once the player has touched it —
 * the first write to any field seeds the row from the derived baseline (see AppViewModel.mutate),
 * so setting a stance or spending Void never silently zeroes the tracked wounds.
 * Fields will grow as later Theme-1 pieces land (spell slots, conditions).
 */
@Entity(tableName = "session_state")
data class SessionState(
    @PrimaryKey val characterUuid: String,
    val wounds: Int = 0,
    /** Void Points spent this scene; current pool = Void Ring − this, refreshed on a rest. */
    val voidSpent: Int = 0,
    /** Selected combat [com.l5rcm.companion.domain.rules.Stance] name; null = Attack (default). */
    val stance: String? = null,
    /** Name of the equipped weapon (matches [com.l5rcm.companion.domain.model.WeaponView.name]); null = none. */
    val equippedWeapon: String? = null,
    /** Captured Defense/Reflexes roll total that fuels the Full Defense Armor TN bonus; null until rolled. */
    val fullDefenseTotal: Int? = null,
)
