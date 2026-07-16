package com.l5rcm.companion.ui

import com.l5rcm.companion.data.catalog.CatalogEntry
import com.l5rcm.companion.data.save.PackRef
import com.l5rcm.companion.data.repository.InstalledPack
import com.l5rcm.companion.domain.model.CharacterView
import com.l5rcm.companion.domain.rules.WoundStatus

/** State of the imported character / sheet. */
sealed interface CharacterUiState {
    data object Empty : CharacterUiState
    data object Loading : CharacterUiState
    data class Ready(val view: CharacterView) : CharacterUiState

    /** Save parsed, but required datapacks are not installed — gate to the Library. */
    data class NeedsDatapacks(val characterName: String, val missing: List<PackRef>) : CharacterUiState

    data class Error(val message: String) : CharacterUiState
}

/**
 * Play-time combat/session state for the loaded character — the wounds overlay merged onto the
 * derived baseline (Layer B on Layer A). Emitted by [AppViewModel] and consumed by the Combat tab.
 */
data class CombatUiState(
    val currentWounds: Int,
    val maxWounds: Int,
    val healRate: Int,
    val status: WoundStatus,
)

/** State of the datapack Library (catalog + installed registry). */
data class LibraryUiState(
    val installed: List<InstalledPack> = emptyList(),
    val catalog: List<CatalogEntry> = emptyList(),
    val loadingCatalog: Boolean = false,
    val installingPackName: String? = null,
    val error: String? = null,
)
