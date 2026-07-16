package com.l5rcm.companion.domain.rules

import com.l5rcm.companion.domain.model.WoundLevel

/**
 * The wound level a character is *currently* at for a given amount of wounds taken, plus the
 * TN penalty in effect there. This is a play-time (Layer B) read: the desktop only prints the
 * static wound table, so there is no desktop oracle for "current penalty from wounds taken" —
 * the boundary follows RAW ("a character stays at a rank until his total Wounds *exceed* its
 * capacity"), i.e. the first level whose cumulative [WoundLevel.threshold] is `>= wounds`.
 */
data class WoundStatus(
    val levelIndex: Int,
    val levelName: String,
    /** TN penalty currently applied; `null` when Out (incapacitated). */
    val penalty: Int?,
    val isOut: Boolean,
)

/**
 * Resolves [wounds] against the character's [levels] (as built by
 * [com.l5rcm.companion.domain.rules.CharacterDeriver], cumulative thresholds, penalties already
 * reduced by any active rules). [wounds] is assumed clamped to `0..maxWounds` by the caller.
 */
fun woundStatus(wounds: Int, levels: List<WoundLevel>): WoundStatus {
    if (levels.isEmpty()) return WoundStatus(0, "Healthy", 0, isOut = false)
    // First level still able to "hold" these wounds (wounds <= its cumulative capacity).
    val index = levels.indexOfFirst { it.threshold != null && wounds <= it.threshold }
        .let { if (it == -1) levels.lastIndex else it }
    val level = levels[index]
    val out = level.penalty == null
    return WoundStatus(
        levelIndex = index,
        levelName = level.name,
        penalty = level.penalty,
        isOut = out,
    )
}
