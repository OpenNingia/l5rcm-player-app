package com.l5rcm.companion.ui.dice

import com.l5rcm.companion.domain.dice.RollConfig

/**
 * A pre-filled dice roll handed from the Combat tab to the shared [DiceScreen]: the [config] seeds
 * the roller (pool, mode, type, TN…) and [title] labels what is being rolled ("Initiative",
 * "Attack — Katana"). Held transiently in [com.l5rcm.companion.ui.AppViewModel.pendingDice] until
 * the screen consumes it.
 */
data class DicePreset(
    val config: RollConfig,
    val title: String,
)
