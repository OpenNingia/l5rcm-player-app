package com.l5rcm.companion.ui.dice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.l5rcm.companion.domain.dice.EffectiveRoll
import com.l5rcm.companion.domain.dice.RollConfig
import com.l5rcm.companion.domain.dice.RollMode
import com.l5rcm.companion.domain.dice.RollResult
import com.l5rcm.companion.domain.dice.RollType
import com.l5rcm.companion.domain.dice.Roller
import com.l5rcm.companion.domain.dice.effective
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Which config tile the numeric keypad is currently editing. */
enum class KeypadField(val min: Int, val max: Int, val signed: Boolean) {
    ROLLED(1, 10, false),
    KEPT(1, 10, false),
    BONUS(0, 50, true),
    TN(5, 120, false),
}

/** A completed roll kept for the STORICO list; [config] restores the full setup on tap. */
data class HistoryEntry(
    val id: Long,
    val config: RollConfig,
    val effective: EffectiveRoll,
    val result: RollResult,
)

/** Transient keypad-sheet state. [buffer] is the digits typed so far; [negative] the ± sign. */
data class KeypadState(
    val field: KeypadField,
    val buffer: String = "",
    val negative: Boolean = false,
)

data class DiceUiState(
    val config: RollConfig = RollConfig(),
    val effective: EffectiveRoll = RollConfig().effective(),
    val result: RollResult? = null,
    val rolling: Boolean = false,
    val history: List<HistoryEntry> = emptyList(),
    val keypad: KeypadState? = null,
    /**
     * Void Points available to this roll when it is driven by the Combat tab (a context roll):
     * spending is gated on `>= 1` and a completed spend decrements the character's tracker. Null for
     * the standalone roller, which stays character-agnostic (Void toggle is a free +1k1 aid).
     */
    val voidBudget: Int? = null,
)

/**
 * Session state for the dice roller screen. Pure rule resolution lives in [RollConfig.effective];
 * this holds the editable [RollConfig], drives the [Roller], and keeps a capped history. No
 * persistence or repositories — a self-contained play-at-the-table tool.
 */
@HiltViewModel
class DiceViewModel @Inject constructor() : ViewModel() {

    private val roller = Roller()

    private val _state = MutableStateFlow(DiceUiState())
    val state: StateFlow<DiceUiState> = _state.asStateFlow()

    /** Remaining Void the character can spend (null = standalone/ungated), decremented on a spend. */
    private var voidBudget: Int? = null

    /** Invoked when a roll that spent a Void Point completes, so the Combat tracker decrements. */
    private var onVoidConsumed: (() -> Unit)? = null

    /**
     * Seed the roller from a [DicePreset] handed over by the Combat tab (initiative, weapon attack,
     * damage, Full Defense). Replaces the current config and clears any previous result so the
     * screen opens ready to roll.
     *
     * [voidBudget] wires the "spend Void" toggle to the character's tracker: non-null gates the
     * toggle on `>= 1` and makes a completed spend decrement it via [onVoidConsumed]. Null keeps the
     * standalone behaviour (free +1k1). Clears any Void already toggled if the budget is empty.
     */
    fun applyPreset(config: RollConfig, voidBudget: Int? = null, onVoidConsumed: (() -> Unit)? = null) {
        this.voidBudget = voidBudget
        this.onVoidConsumed = onVoidConsumed
        val gated = if (voidBudget != null && voidBudget < 1) config.copy(voidSpent = false) else config
        _state.update {
            it.copy(config = gated, effective = gated.effective(), result = null, voidBudget = voidBudget)
        }
    }

    // --- Config edits (each recomputes the effective roll) ---

    fun setMode(mode: RollMode) = updateConfig { it.copy(mode = mode) }

    fun setRollType(type: RollType) = updateConfig { it.copy(rollType = type) }

    fun setSkilled(skilled: Boolean) = updateConfig {
        // Leaving skilled hides Emphasis, so drop the toggle to keep state honest.
        it.copy(skilled = skilled, emphasisToggle = if (skilled) it.emphasisToggle else false)
    }

    fun toggleEmphasis() = updateConfig { it.copy(emphasisToggle = !it.emphasisToggle) }

    fun toggleVoid() = _state.update {
        val turningOn = !it.config.voidSpent
        // Context rolls can only spend Void the character actually has.
        if (turningOn && it.voidBudget != null && it.voidBudget < 1) return@update it
        var cfg = it.config.copy(voidSpent = turningOn)
        if (cfg.kept > cfg.rolled) cfg = cfg.copy(kept = cfg.rolled)
        it.copy(config = cfg, effective = cfg.effective())
    }

    fun setRaises(raises: Int) = updateConfig { it.copy(raises = raises.coerceIn(0, 10)) }

    private inline fun updateConfig(crossinline edit: (RollConfig) -> RollConfig) {
        _state.update {
            var cfg = edit(it.config)
            // Dice-penalty rule mirrored in the UI: can't keep more than you roll.
            if (cfg.kept > cfg.rolled) cfg = cfg.copy(kept = cfg.rolled)
            it.copy(config = cfg, effective = cfg.effective())
        }
    }

    // --- Numeric keypad ---

    fun openKeypad(field: KeypadField) {
        val cfg = _state.value.config
        val negative = field == KeypadField.BONUS && cfg.bonus < 0
        _state.update { it.copy(keypad = KeypadState(field = field, negative = negative)) }
    }

    fun closeKeypad() = _state.update { it.copy(keypad = null) }

    fun keypadDigit(digit: Int) = _state.update { s ->
        val k = s.keypad ?: return@update s
        // Max 3 digits; a leading zero is meaningless (blocked).
        if (k.buffer.length >= 3) return@update s
        if (k.buffer.isEmpty() && digit == 0) return@update s
        s.copy(keypad = k.copy(buffer = k.buffer + digit))
    }

    fun keypadBackspace() = _state.update { s ->
        val k = s.keypad ?: return@update s
        s.copy(keypad = k.copy(buffer = k.buffer.dropLast(1)))
    }

    fun keypadToggleSign() = _state.update { s ->
        val k = s.keypad ?: return@update s
        if (!k.field.signed) return@update s
        s.copy(keypad = k.copy(negative = !k.negative))
    }

    /** Commit the buffered value into the config, clamped to the field's range. */
    fun keypadCommit() = _state.update { s ->
        val k = s.keypad ?: return@update s
        val magnitude = k.buffer.toIntOrNull()
        var cfg = s.config
        if (magnitude != null) {
            val signed = if (k.negative) -magnitude else magnitude
            cfg = when (k.field) {
                KeypadField.ROLLED -> {
                    val rolled = signed.coerceIn(k.field.min, k.field.max)
                    // Reducing rolled below kept also lowers kept.
                    cfg.copy(rolled = rolled, kept = cfg.kept.coerceAtMost(rolled))
                }
                KeypadField.KEPT -> cfg.copy(kept = signed.coerceIn(k.field.min, cfg.rolled))
                KeypadField.BONUS -> {
                    val abs = magnitude.coerceIn(0, k.field.max)
                    cfg.copy(bonus = if (k.negative) -abs else abs)
                }
                KeypadField.TN -> cfg.copy(tn = signed.coerceIn(k.field.min, k.field.max))
            }
        }
        s.copy(config = cfg, effective = cfg.effective(), keypad = null)
    }

    // --- Rolling ---

    fun roll() {
        if (_state.value.rolling) return
        val cfg = _state.value.config
        val eff = cfg.effective()
        _state.update { it.copy(rolling = true) }
        viewModelScope.launch {
            delay(ROLL_ANIM_MS) // the "LANCIO…" ticker phase; reveal is animated in Compose
            val result = roller.roll(eff.toSpec())
            val entry = HistoryEntry(
                id = System.nanoTime(),
                config = cfg.copy(voidSpent = cfg.voidSpent), // capture Void as it was spent
                effective = eff,
                result = result,
            )
            // A context roll that spent Void decrements the character's tracker (once, on completion).
            if (cfg.voidSpent && voidBudget != null) {
                onVoidConsumed?.invoke()
                voidBudget = (voidBudget!! - 1).coerceAtLeast(0)
            }
            _state.update {
                it.copy(
                    result = result,
                    rolling = false,
                    // Void Point is a per-roll resource — reset after each roll.
                    config = it.config.copy(voidSpent = false),
                    effective = it.config.copy(voidSpent = false).effective(),
                    history = (listOf(entry) + it.history).take(HISTORY_CAP),
                    voidBudget = voidBudget,
                )
            }
        }
    }

    fun clearHistory() = _state.update { it.copy(history = emptyList()) }

    /** Reload a past roll's full configuration (mode, type, toggles, pool, TN, raises). */
    fun restore(entry: HistoryEntry) = _state.update {
        it.copy(config = entry.config, effective = entry.config.effective(), result = null)
    }

    private companion object {
        const val HISTORY_CAP = 8
        const val ROLL_ANIM_MS = 550L
    }
}
