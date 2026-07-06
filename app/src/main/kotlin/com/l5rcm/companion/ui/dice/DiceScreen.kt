package com.l5rcm.companion.ui.dice

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.l5rcm.companion.domain.dice.RollMode
import com.l5rcm.companion.domain.dice.RollType
import com.l5rcm.companion.ui.theme.L5RTheme
import com.l5rcm.companion.ui.theme.Radii
import com.l5rcm.companion.ui.theme.Spacing
import com.l5rcm.companion.ui.widgets.RicePaperOverlay

/**
 * The Roll & Keep dice roller (`design_handoff_l5r_dice_roller`). A single scrollable screen:
 * configure the pool + TN, spend Void, tap TIRA I DADI, read the animated result, and re-roll from
 * history. All rule interaction is resolved by [com.l5rcm.companion.domain.dice.RollConfig.effective].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiceScreen(
    characterName: String,
    characterSubtitle: String,
    onBack: () -> Unit,
    badgeGlyph: String = "賽",
    viewModel: DiceViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val cfg = state.config
    val eff = state.effective

    Box(Modifier.fillMaxSize().background(L5RTheme.colors.paper)) {
        RicePaperOverlay(Modifier.fillMaxSize())
        Column(Modifier.fillMaxSize()) {
            DiceTopBar(characterName, characterSubtitle, badgeGlyph, onBack)
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Spacing.s4, vertical = Spacing.s4),
                verticalArrangement = Arrangement.spacedBy(Spacing.s4),
            ) {
                RollTypeControl(cfg.rollType, viewModel::setRollType)
                ModeControl(cfg.mode, viewModel::setMode)

                if (eff.isSkill) {
                    SkilledControl(cfg.skilled, viewModel::setSkilled)
                    if (cfg.skilled) {
                        ToggleBar(
                            label = if (cfg.emphasisToggle) "✦ Enfasi attiva" else "✦ Enfasi — ritira gli 1",
                            active = cfg.emphasisToggle,
                            onClick = viewModel::toggleEmphasis,
                            activeBg = L5RTheme.colors.accentGold,
                        )
                    }
                }

                NoteLine(eff.effSkilled, eff.emphasis)
                HeroNotation(state)

                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s3)) {
                    ConfigTile("Dadi", "${cfg.rolled}") { viewModel.openKeypad(KeypadField.ROLLED) }
                    ConfigTile("Tenuti", "${cfg.kept}") { viewModel.openKeypad(KeypadField.KEPT) }
                    ConfigTile("Bonus", signed(cfg.bonus)) { viewModel.openKeypad(KeypadField.BONUS) }
                }

                VoidCard(cfg.voidSpent, promotes = eff.isSkill && !cfg.skilled, onToggle = viewModel::toggleVoid)

                if (cfg.mode == RollMode.TN) {
                    TnRow(state, viewModel)
                }

                RollButton(state.rolling, viewModel::roll)

                state.result?.let { ResultCard(eff, it, cfg.mode) }

                if (state.history.isNotEmpty()) {
                    HistorySection(state.history, viewModel::clearHistory, viewModel::restore)
                }
            }
        }

        state.keypad?.let { keypad ->
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ModalBottomSheet(
                onDismissRequest = viewModel::keypadCommit, // scrim tap commits (design)
                sheetState = sheetState,
                containerColor = L5RTheme.colors.paper,
            ) {
                KeypadContent(
                    state = keypad,
                    onDigit = viewModel::keypadDigit,
                    onBackspace = viewModel::keypadBackspace,
                    onSign = viewModel::keypadToggleSign,
                    onCommit = viewModel::keypadCommit,
                )
            }
        }
    }
}

@Composable
private fun DiceTopBar(name: String, subtitle: String, badgeGlyph: String, onBack: () -> Unit) {
    val colors = L5RTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .background(colors.paperDark)
            .windowInsetsPadding(
                WindowInsets.statusBars
                    .union(WindowInsets.displayCutout)
                    .only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
            )
            .height(56.dp)
            .padding(horizontal = Spacing.s4),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "‹",
            style = L5RTheme.type.display.copy(color = colors.ink),
            modifier = Modifier.clickable(onClick = onBack).padding(end = Spacing.s4),
        )
        Column(Modifier.weight(1f)) {
            Text(name.ifBlank { "Tiro Dadi" }, style = L5RTheme.type.heading2.copy(color = colors.ink))
            if (subtitle.isNotBlank()) {
                Text(subtitle, style = L5RTheme.type.caption.copy(color = colors.inkMuted))
            }
        }
        Box(
            Modifier.size(36.dp).clip(Radii.button).background(colors.accentCrimson),
            contentAlignment = Alignment.Center,
        ) {
            Text(badgeGlyph, style = L5RTheme.type.heading2.copy(color = colors.paperLight))
        }
    }
}

@Composable
private fun RollTypeControl(selected: RollType, onSelect: (RollType) -> Unit) {
    SegmentedControl(
        options = listOf(
            Segment(RollType.SKILL, "Abilità"),
            Segment(RollType.TRAIT, "Tratto"),
            Segment(RollType.RING, "Anello"),
            Segment(RollType.SPELL, "Incant."),
        ),
        selected = selected,
        onSelect = onSelect,
    )
}

@Composable
private fun ModeControl(selected: RollMode, onSelect: (RollMode) -> Unit) {
    SegmentedControl(
        options = listOf(
            Segment(RollMode.OPEN, "Aperto"),
            Segment(RollMode.TN, "Con TN"),
        ),
        selected = selected,
        onSelect = onSelect,
    )
}

@Composable
private fun SkilledControl(skilled: Boolean, onSelect: (Boolean) -> Unit) {
    // Uses the muted-ink fill to set it apart from the crimson mode toggle (design §Skilled).
    SegmentedControl(
        options = listOf(Segment(true, "Esperto"), Segment(false, "Inesperto")),
        selected = skilled,
        onSelect = onSelect,
        activeBg = L5RTheme.colors.inkMuted,
    )
}

@Composable
private fun NoteLine(effSkilled: Boolean, emphasis: Boolean) {
    val text = when {
        emphasis -> "Enfasi — gli 1 vengono ritirati una volta."
        !effSkilled -> "Tiro inesperto — i 10 non esplodono e non puoi dichiarare Raise."
        else -> "Notazione Roll & Keep — XkY: tira X dadi, tieni gli Y migliori. I 10 esplodono."
    }
    Text(
        text,
        style = L5RTheme.type.caption.copy(color = L5RTheme.colors.inkMuted),
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun HeroNotation(state: DiceUiState) {
    val colors = L5RTheme.colors
    val cfg = state.config
    val eff = state.effective
    val header = when (cfg.rollType) {
        RollType.SKILL -> "Tiro di abilità"
        RollType.TRAIT -> "Tiro di tratto"
        RollType.RING -> "Tiro di anello"
        RollType.SPELL -> "Tiro di incantesimo"
    }
    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.s2),
    ) {
        Text(header.uppercase(), style = DiceType.rollHeader.copy(color = colors.accentGold))
        Text(eff.notation, style = DiceType.notation.copy(color = colors.accentGold))

        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s2)) {
            if (cfg.mode == RollMode.TN) {
                StatePill("TN ${eff.effTn}", fg = colors.accentCrimson)
            }
            if (eff.effRaises > 0) StatePill("${eff.effRaises} RAISE", fg = colors.accentBlue)
            if (!eff.effSkilled) StatePill("INESPERTO", fg = colors.inkMuted)
            if (cfg.voidSpent) {
                StatePill(if (eff.promoted) "VUOTO +1k1 → ESPERTO" else "VUOTO +1k1", fg = colors.ringVoid)
            }
            if (eff.emphasis) StatePill("ENFASI", fg = colors.accentGoldLight)
        }
    }
}

@Composable
private fun VoidCard(spent: Boolean, promotes: Boolean, onToggle: () -> Unit) {
    val colors = L5RTheme.colors
    Column(
        Modifier
            .fillMaxWidth()
            .clip(Radii.card)
            .background(colors.ringVoid.copy(alpha = 0.06f))
            .border(1.dp, colors.parchmentBorder, Radii.card)
            .padding(Spacing.s3),
        verticalArrangement = Arrangement.spacedBy(Spacing.s2),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SectionLabel("Punto del Vuoto")
            Text("1 per tiro", style = L5RTheme.type.caption.copy(color = colors.inkFaint))
        }
        ToggleBar(
            label = "✦ Spendi — +1k1",
            active = spent,
            onClick = onToggle,
            activeBg = colors.accentGold,
        )
        if (promotes) {
            Text(
                "Il tiro inesperto diventa esperto (grado 1)",
                style = L5RTheme.type.captionItalic.copy(color = colors.accentGold),
            )
        }
    }
}

@Composable
private fun TnRow(state: DiceUiState, viewModel: DiceViewModel) {
    val colors = L5RTheme.colors
    val cfg = state.config
    val eff = state.effective
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.s3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ConfigTile("TN base", "${cfg.tn}") { viewModel.openKeypad(KeypadField.TN) }

        if (eff.effSkilled) {
            Column(
                Modifier
                    .weight(1f)
                    .clip(Radii.button)
                    .background(colors.whiteWash)
                    .border(1.dp, colors.parchmentBorder, Radii.button)
                    .padding(Spacing.s2),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Spacing.s1),
            ) {
                SectionLabel("Raise → TN ${eff.effTn}")
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.s3),
                ) {
                    StepperButton("−", enabled = cfg.raises > 0) { viewModel.setRaises(cfg.raises - 1) }
                    Text("${cfg.raises}", style = DiceType.configValue.copy(color = colors.ink))
                    StepperButton("+", enabled = cfg.raises < 10) { viewModel.setRaises(cfg.raises + 1) }
                }
            }
        } else {
            Box(
                Modifier
                    .weight(1f)
                    .clip(Radii.button)
                    .border(1.dp, colors.parchmentBorder.copy(alpha = 0.6f), Radii.button)
                    .padding(Spacing.s3),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "Non disponibili sui tiri inesperti",
                    style = L5RTheme.type.captionItalic.copy(color = colors.inkFaint),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun RollButton(rolling: Boolean, onRoll: () -> Unit) {
    val colors = L5RTheme.colors
    Box(
        Modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(Radii.button)
            .background(if (rolling) colors.accentCrimsonDark else colors.accentCrimson)
            .then(if (rolling) Modifier else Modifier.clickable(onClick = onRoll)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            if (rolling) "⚁  Lancio…" else "⚀  Tira i dadi",
            style = DiceType.rollButton.copy(color = colors.paperLight),
        )
    }
}

/** A signed bonus for display: `+5`, `-3`, or `0`. */
private fun signed(bonus: Int): String = when {
    bonus > 0 -> "+$bonus"
    else -> "$bonus"
}
