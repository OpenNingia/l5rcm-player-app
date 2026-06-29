package com.l5rcm.companion.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.l5rcm.companion.data.catalog.CatalogEntry
import com.l5rcm.companion.data.catalog.CatalogException
import com.l5rcm.companion.data.repository.CharacterRepository
import com.l5rcm.companion.data.repository.DatapackRepository
import com.l5rcm.companion.data.repository.StoredCharacter
import com.l5rcm.companion.data.save.SaveModel
import com.l5rcm.companion.domain.rules.CharacterDeriver
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Single app ViewModel coordinating import, the missing-datapack gate, sheet
 * derivation and the Library. Holds the parsed [SaveModel] so the sheet can be
 * (re)built once required packs are installed.
 */
@HiltViewModel
class AppViewModel @Inject constructor(
    private val characterRepo: CharacterRepository,
    private val datapackRepo: DatapackRepository,
) : ViewModel() {

    private val _character = MutableStateFlow<CharacterUiState>(CharacterUiState.Empty)
    val character: StateFlow<CharacterUiState> = _character.asStateFlow()

    private val _library = MutableStateFlow(LibraryUiState())
    val library: StateFlow<LibraryUiState> = _library.asStateFlow()

    /** Save + uuid held while we resolve missing datapacks. */
    private var pendingSave: SaveModel? = null
    private var pendingUuid: String? = null

    init {
        observeInstalledPacks()
        resumeLastCharacter()
    }

    private fun observeInstalledPacks() {
        viewModelScope.launch {
            datapackRepo.installedPacks.collect { packs ->
                _library.value = _library.value.copy(installed = packs)
            }
        }
    }

    private fun resumeLastCharacter() {
        viewModelScope.launch {
            val stored = runCatching { characterRepo.resumeLast() }.getOrNull() ?: return@launch
            runCatching { ingest(stored, remember = false) }
        }
    }

    /** Entry point from the SAF picker. */
    fun importCharacter(uri: Uri) {
        viewModelScope.launch {
            _character.value = CharacterUiState.Loading
            try {
                ingest(characterRepo.importFromUri(uri), remember = true)
            } catch (e: Exception) {
                _character.value = CharacterUiState.Error(e.message ?: "Failed to read character")
            }
        }
    }

    /** Entry point from the QR scanner (a `.l5r` document reassembled from scanned frames). */
    fun importCharacterFromJson(json: String) {
        viewModelScope.launch {
            _character.value = CharacterUiState.Loading
            try {
                ingest(characterRepo.importFromJson(json), remember = true)
            } catch (e: Exception) {
                _character.value = CharacterUiState.Error(e.message ?: "Failed to read character")
            }
        }
    }

    /** Holds the parsed save + its uuid, optionally records it as last-opened, then resolves. */
    private suspend fun ingest(stored: StoredCharacter, remember: Boolean) {
        pendingSave = stored.save
        pendingUuid = stored.uuid
        if (remember) characterRepo.rememberLast(stored.uuid)
        resolveOrGate(stored.save)
    }

    /** Re-checks dependencies and builds the sheet, or gates to the Library. */
    private suspend fun resolveOrGate(save: SaveModel) {
        val missing = datapackRepo.missingDependencies(save)
        if (missing.isNotEmpty()) {
            _character.value = CharacterUiState.NeedsDatapacks(save.name, missing)
        } else {
            val packs = datapackRepo.loadEnabledSet()
            _character.value = CharacterUiState.Ready(CharacterDeriver.derive(save, packs))
        }
    }

    /** Called after the user installs packs in the Library, to retry building the sheet. */
    fun retryPending() {
        val save = pendingSave ?: return
        viewModelScope.launch {
            _character.value = CharacterUiState.Loading
            resolveOrGate(save)
        }
    }

    fun clearCharacter() {
        pendingSave = null
        pendingUuid = null
        _character.value = CharacterUiState.Empty
        viewModelScope.launch { characterRepo.rememberLast(null) }
    }

    // --- Library ---

    fun loadCatalog() {
        viewModelScope.launch {
            _library.value = _library.value.copy(loadingCatalog = true, error = null)
            try {
                val entries = datapackRepo.fetchCatalog()
                _library.value = _library.value.copy(catalog = entries, loadingCatalog = false)
            } catch (e: CatalogException) {
                _library.value = _library.value.copy(loadingCatalog = false, error = catalogMessage(e))
            }
        }
    }

    fun install(entry: CatalogEntry) {
        viewModelScope.launch {
            _library.value = _library.value.copy(installingPackName = entry.name, error = null)
            try {
                datapackRepo.install(entry)
                _library.value = _library.value.copy(installingPackName = null)
            } catch (e: CatalogException) {
                _library.value = _library.value.copy(installingPackName = null, error = catalogMessage(e))
            }
        }
    }

    fun setPackEnabled(id: String, enabled: Boolean) {
        viewModelScope.launch { datapackRepo.setEnabled(id, enabled) }
    }

    fun removePack(id: String) {
        viewModelScope.launch { datapackRepo.remove(id) }
    }

    private fun catalogMessage(e: CatalogException): String = when (e) {
        is CatalogException.Offline -> "You appear to be offline. Check your connection and try again."
        is CatalogException.RateLimited -> "GitHub rate limit reached. Please try again later."
        is CatalogException.Generic -> "Could not load datapacks: ${e.message}"
    }
}
