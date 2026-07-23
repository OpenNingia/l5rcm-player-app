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
import com.l5rcm.companion.data.session.SessionNote
import com.l5rcm.companion.data.session.SessionNotesRepository
import com.l5rcm.companion.data.session.SessionRepository
import com.l5rcm.companion.data.session.SessionState
import com.l5rcm.companion.domain.model.CharacterView
import com.l5rcm.companion.domain.model.HealthView
import com.l5rcm.companion.domain.model.Ring
import com.l5rcm.companion.domain.rules.CharacterDeriver
import com.l5rcm.companion.domain.rules.Stance
import com.l5rcm.companion.domain.rules.StanceRules
import com.l5rcm.companion.domain.rules.spellSlots
import com.l5rcm.companion.domain.rules.woundStatus
import com.l5rcm.companion.ui.dice.DicePreset
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
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
    private val sessionRepo: SessionRepository,
    private val notesRepo: SessionNotesRepository,
) : ViewModel() {

    private val _character = MutableStateFlow<CharacterUiState>(CharacterUiState.Empty)
    val character: StateFlow<CharacterUiState> = _character.asStateFlow()

    private val _library = MutableStateFlow(LibraryUiState())
    val library: StateFlow<LibraryUiState> = _library.asStateFlow()

    /** Save + uuid held while we resolve missing datapacks. */
    private var pendingSave: SaveModel? = null
    private var pendingUuid: String? = null

    /**
     * The loaded character's derived baseline needed by the session overlay: stable [uuid], the
     * [health] baseline, plus [voidRank]/[defenseRank] for clamping the Void pool and gating the
     * defensive stances. [elementRanks]/[hasSpells] size the daily spell-slot pools (only shugenja
     * — characters with spells — get them).
     */
    private data class CharContext(
        val uuid: String,
        val health: HealthView,
        val voidRank: Int,
        val defenseRank: Int,
        val elementRanks: Map<Ring, Int>,
        val hasSpells: Boolean,
    )

    private val charContext = MutableStateFlow<CharContext?>(null)

    /** Latest persisted overlay row, kept so partial edits read-modify-write the whole [SessionState]. */
    @Volatile
    private var lastSession: SessionState? = null

    /** A dice roll pre-filled by the Combat tab, awaiting the shared dice screen. */
    private val _pendingDice = MutableStateFlow<DicePreset?>(null)
    val pendingDice: StateFlow<DicePreset?> = _pendingDice.asStateFlow()

    /**
     * Play-time combat/session state: the overlay (wounds, Void, stance, equipped weapon, armor
     * worn, Full Defense) merged onto the derived baseline. Null until a character is loaded; a
     * missing overlay row falls back to the baseline (full Void pool, Attack stance, no weapon
     * equipped, armor worn).
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val combat: StateFlow<CombatUiState?> = charContext
        .flatMapLatest { ctx ->
            if (ctx == null) {
                lastSession = null
                flowOf(null)
            } else {
                sessionRepo.observe(ctx.uuid).map { row ->
                    lastSession = row
                    val current = (row?.wounds ?: ctx.health.currentWounds)
                        .coerceIn(0, ctx.health.maxWounds)
                    val voidCurrent = (ctx.voidRank - (row?.voidSpent ?: 0)).coerceIn(0, ctx.voidRank)
                    val stance = row?.stance?.let { runCatching { Stance.valueOf(it) }.getOrNull() }
                        ?.takeIf { StanceRules.canUse(it, ctx.defenseRank) }
                        ?: Stance.ATTACK
                    val slots = if (ctx.hasSpells) {
                        spellSlots(
                            elementRanks = ctx.elementRanks,
                            voidRank = ctx.voidRank,
                            spent = mapOf(
                                Ring.EARTH to (row?.spellEarthSpent ?: 0),
                                Ring.AIR to (row?.spellAirSpent ?: 0),
                                Ring.WATER to (row?.spellWaterSpent ?: 0),
                                Ring.FIRE to (row?.spellFireSpent ?: 0),
                                Ring.VOID to (row?.spellVoidSpent ?: 0),
                            ),
                        )
                    } else {
                        emptyList()
                    }
                    CombatUiState(
                        currentWounds = current,
                        maxWounds = ctx.health.maxWounds,
                        healRate = ctx.health.healRate,
                        status = woundStatus(current, ctx.health.levels),
                        voidCurrent = voidCurrent,
                        voidMax = ctx.voidRank,
                        stance = stance,
                        equippedWeaponName = row?.equippedWeapon,
                        fullDefenseTotal = row?.fullDefenseTotal,
                        armorEquipped = row?.armorEquipped ?: true,
                        spellSlots = slots,
                    )
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Latest emitted notes list, cached so [saveSessionNote] can assign/preserve the session number. */
    @Volatile
    private var lastNotes: List<SessionNote> = emptyList()

    /** The loaded character's session notes (newest first), or empty when no character is loaded. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val sessionNotes: StateFlow<List<SessionNote>> = charContext
        .flatMapLatest { ctx ->
            if (ctx == null) {
                lastNotes = emptyList()
                flowOf(emptyList())
            } else {
                notesRepo.observe(ctx.uuid).map { notes -> notes.also { lastNotes = it } }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

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
            charContext.value = null
            _character.value = CharacterUiState.NeedsDatapacks(save.name, missing)
        } else {
            val packs = datapackRepo.loadEnabledSet()
            val view = CharacterDeriver.derive(save, packs)
            charContext.value = pendingUuid?.let {
                CharContext(
                    uuid = it,
                    health = view.health,
                    voidRank = view.voidRank,
                    defenseRank = defenseRankOf(view),
                    elementRanks = view.rings.associate { r -> r.ring to r.rank },
                    hasSpells = view.spells.known.isNotEmpty() || view.spells.memorized.isNotEmpty(),
                )
            }
            _character.value = CharacterUiState.Ready(view)
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
        charContext.value = null
        _character.value = CharacterUiState.Empty
        viewModelScope.launch { characterRepo.rememberLast(null) }
    }

    // --- Combat / session state (wounds · Void · stance · equipped weapon · armor overlay) ---

    /** Apply [amount] wounds (e.g. from a damage roll). Clamped to `0..maxWounds`. */
    fun applyDamage(amount: Int) = mutate { it.copy(wounds = adjustWounds(it.wounds + amount)) }

    /** Heal [amount] wounds. Clamped at 0. */
    fun applyHeal(amount: Int) = mutate { it.copy(wounds = adjustWounds(it.wounds - amount)) }

    /**
     * A night's rest: heal the character's heal rate (2×Stamina + Insight Rank), refresh the Void
     * pool to full (Void refreshes on a daily rest, rulebook p. 78) and restore every daily spell
     * slot (the sunrise refresh, p. 166).
     */
    fun rest() {
        val rate = charContext.value?.health?.healRate ?: return
        mutate {
            it.copy(
                wounds = adjustWounds(it.wounds - rate),
                voidSpent = 0,
                spellEarthSpent = 0,
                spellAirSpent = 0,
                spellWaterSpent = 0,
                spellFireSpent = 0,
                spellVoidSpent = 0,
            )
        }
    }

    /** Revert wounds to the imported/derived baseline, leaving Void/stance/weapon untouched. */
    fun resetWounds() {
        val baseline = charContext.value?.health?.currentWounds ?: return
        mutate { it.copy(wounds = baseline.coerceIn(0, charContext.value?.health?.maxWounds ?: baseline)) }
    }

    /** Select a combat [stance]; ignored if a defensive stance is picked without a Defense rank. */
    fun setStance(stance: Stance) {
        val ctx = charContext.value ?: return
        if (!StanceRules.canUse(stance, ctx.defenseRank)) return
        // Leaving Full Defense drops its captured roll so the Armor TN bonus doesn't linger.
        mutate {
            it.copy(
                stance = stance.name,
                fullDefenseTotal = if (stance == Stance.FULL_DEFENSE) it.fullDefenseTotal else null,
            )
        }
    }

    /** Record the Defense/Reflexes roll total that fuels the Full Defense Armor TN bonus. */
    fun setFullDefenseTotal(total: Int) = mutate { it.copy(fullDefenseTotal = total.coerceAtLeast(0)) }

    /** Spend one Void Point (no-op when the pool is empty). */
    fun spendVoid() {
        val ctx = charContext.value ?: return
        mutate { it.copy(voidSpent = (it.voidSpent + 1).coerceAtMost(ctx.voidRank)) }
    }

    /** Regain one Void Point (no-op when the pool is already full). */
    fun regainVoid() = mutate { it.copy(voidSpent = (it.voidSpent - 1).coerceAtLeast(0)) }

    /** Equip [name] (or unequip when null / re-selecting the same weapon). */
    fun equipWeapon(name: String?) = mutate {
        it.copy(equippedWeapon = if (name == it.equippedWeapon) null else name)
    }

    /** Toggle whether the character's armor is worn; when removed its Armor TN / RD bonus drops. */
    fun toggleArmor() = mutate { it.copy(armorEquipped = !it.armorEquipped) }

    // --- Session notes (per-character play-session log; never written to the .l5r) ---

    /**
     * Create ([id] == 0) or update a session note. New notes get the next per-character session
     * number; edits keep their existing number. The owning [characterUuid][SessionNote.characterUuid]
     * comes from the loaded character, so callers only supply the date + body.
     */
    fun saveSessionNote(id: Long, date: Long, body: String) {
        val ctx = charContext.value ?: return
        val number = lastNotes.firstOrNull { it.id == id }?.number
            ?: ((lastNotes.maxOfOrNull { it.number } ?: 0) + 1)
        viewModelScope.launch {
            notesRepo.save(SessionNote(id = id, characterUuid = ctx.uuid, number = number, date = date, body = body))
        }
    }

    fun deleteSessionNote(id: Long) {
        viewModelScope.launch { notesRepo.delete(id) }
    }

    /**
     * Burn ([delta] = +1) or reclaim ([delta] = −1) one spell slot from [ring]'s pool (Void = the
     * flexible bonus pool). Clamped to `0..capacity`; a no-op when the character has no such pool.
     */
    fun adjustSpellSlot(ring: Ring, delta: Int) {
        val ctx = charContext.value ?: return
        val max = if (ring == Ring.VOID) ctx.voidRank else (ctx.elementRanks[ring] ?: 0)
        if (max <= 0) return
        mutate { it.withSpellSpent(ring, (it.spellSpent(ring) + delta).coerceIn(0, max)) }
    }

    private fun SessionState.spellSpent(ring: Ring): Int = when (ring) {
        Ring.EARTH -> spellEarthSpent
        Ring.AIR -> spellAirSpent
        Ring.WATER -> spellWaterSpent
        Ring.FIRE -> spellFireSpent
        Ring.VOID -> spellVoidSpent
    }

    private fun SessionState.withSpellSpent(ring: Ring, value: Int): SessionState = when (ring) {
        Ring.EARTH -> copy(spellEarthSpent = value)
        Ring.AIR -> copy(spellAirSpent = value)
        Ring.WATER -> copy(spellWaterSpent = value)
        Ring.FIRE -> copy(spellFireSpent = value)
        Ring.VOID -> copy(spellVoidSpent = value)
    }

    private fun adjustWounds(value: Int): Int =
        value.coerceIn(0, charContext.value?.health?.maxWounds ?: value)

    /**
     * Read-modify-write the whole overlay row. When no row exists yet the default is seeded from the
     * derived baseline (current wounds), so the first edit to *any* field never silently resets the
     * tracked wounds to zero.
     */
    private inline fun mutate(edit: (SessionState) -> SessionState) {
        val ctx = charContext.value ?: return
        val base = lastSession ?: SessionState(ctx.uuid, wounds = ctx.health.currentWounds)
        val next = edit(base)
        lastSession = next
        viewModelScope.launch { sessionRepo.save(next) }
    }

    // --- Dice roll presets (Combat tab → shared dice screen) ---

    /** Stash a pre-filled roll and let the UI navigate to the dice screen, which consumes it. */
    fun prepareRoll(preset: DicePreset) { _pendingDice.value = preset }

    /** Clear the pending preset once the dice screen has applied it. */
    fun consumePendingDice() { _pendingDice.value = null }

    private fun defenseRankOf(view: CharacterView): Int =
        view.skills.firstOrNull { it.id == "defense" || it.name.equals("Defense", ignoreCase = true) }
            ?.rank ?: 0

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
