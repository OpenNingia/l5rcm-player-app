package com.l5rcm.companion.ui.sheet

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.material3.rememberDrawerState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.l5rcm.companion.data.session.SessionNote
import com.l5rcm.companion.domain.model.CharacterView
import com.l5rcm.companion.domain.model.Ring
import com.l5rcm.companion.domain.rules.Stance
import com.l5rcm.companion.ui.CombatUiState
import com.l5rcm.companion.ui.dice.DicePreset
import com.l5rcm.companion.ui.theme.L5RTheme
import com.l5rcm.companion.ui.theme.Layout
import com.l5rcm.companion.ui.theme.Spacing
import com.l5rcm.companion.ui.widgets.RicePaperOverlay
import kotlinx.coroutines.launch

/**
 * The character sheet. Portrait-first (docs §4.4 compact form): a slim paper-dark
 * top bar with a hamburger that opens a left-edge drawer of [SheetSection]s; the
 * content area renders the selected section.
 */
@Composable
fun SheetScreen(
    view: CharacterView,
    combat: CombatUiState?,
    onDamage: (Int) -> Unit,
    onHeal: (Int) -> Unit,
    onRest: () -> Unit,
    onResetWounds: () -> Unit,
    onSetStance: (Stance) -> Unit,
    onSpendVoid: () -> Unit,
    onRegainVoid: () -> Unit,
    onEquipWeapon: (String?) -> Unit,
    onToggleArmor: () -> Unit,
    onSetFullDefenseTotal: (Int) -> Unit,
    onSpellSlot: (Ring, Int) -> Unit,
    onCombatRoll: (DicePreset) -> Unit,
    notes: List<SessionNote>,
    onSaveNote: (Long, Long, String) -> Unit,
    onDeleteNote: (Long) -> Unit,
    onOpenLibrary: () -> Unit,
    onImport: () -> Unit,
    onScanQr: () -> Unit,
    onOpenDice: () -> Unit,
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    // rememberSaveable so the selected tab survives navigating out to the dice roller / library and back.
    var section by rememberSaveable { mutableStateOf(SheetSection.CHARACTER) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = L5RTheme.colors.paperDark,
                modifier = Modifier.width(Layout.drawerMaxWidth),
            ) {
                DrawerHeader(view)
                SheetSection.entries.forEach { s ->
                    NavRow(
                        section = s,
                        selected = s == section,
                        onClick = {
                            section = s
                            scope.launch { drawerState.close() }
                        },
                    )
                }
                ExtraNavRow("Dice Roller", "賽") { scope.launch { drawerState.close() }; onOpenDice() }
                ExtraNavRow("Datapack Library", "蔵") { scope.launch { drawerState.close() }; onOpenLibrary() }
                // Import another character — file or QR. Re-importing the same character (same uuid)
                // refreshes the sheet from a desktop edit and keeps its session overlay.
                ExtraNavRow("Import .l5r file", "替") { scope.launch { drawerState.close() }; onImport() }
                ExtraNavRow("Scan QR code", "掃") { scope.launch { drawerState.close() }; onScanQr() }
            }
        },
    ) {
        Box(Modifier.fillMaxSize().background(L5RTheme.colors.paper)) {
            RicePaperOverlay(Modifier.fillMaxSize())
            Column(Modifier.fillMaxSize()) {
                TopBar(
                    title = view.name.ifBlank { "Character" },
                    onMenu = { scope.launch { drawerState.open() } },
                )
                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = Spacing.s5, vertical = Spacing.s5),
                    verticalArrangement = Arrangement.spacedBy(Spacing.s4),
                ) {
                    SectionContent(
                        section = section,
                        view = view,
                        combat = combat,
                        onDamage = onDamage,
                        onHeal = onHeal,
                        onRest = onRest,
                        onResetWounds = onResetWounds,
                        onSetStance = onSetStance,
                        onSpendVoid = onSpendVoid,
                        onRegainVoid = onRegainVoid,
                        onEquipWeapon = onEquipWeapon,
                        onToggleArmor = onToggleArmor,
                        onSetFullDefenseTotal = onSetFullDefenseTotal,
                        onSpellSlot = onSpellSlot,
                        onCombatRoll = onCombatRoll,
                        notes = notes,
                        onSaveNote = onSaveNote,
                        onDeleteNote = onDeleteNote,
                    )
                }
            }
        }
    }
}

@Composable
private fun TopBar(title: String, onMenu: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(L5RTheme.colors.paperDark)
            // edge-to-edge is on: keep the bar's content clear of the status bar and
            // the front-camera cutout (otherwise the hamburger lands under the lens).
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
            "☰",
            style = L5RTheme.type.display.copy(color = L5RTheme.colors.ink),
            modifier = Modifier.clickable(onClick = onMenu).padding(end = Spacing.s4),
        )
        Text(title, style = L5RTheme.type.heading1.copy(color = L5RTheme.colors.ink))
    }
}

@Composable
private fun DrawerHeader(view: CharacterView) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(L5RTheme.colors.accentPrimary.copy(alpha = 0.14f))
            .padding(Spacing.s4),
    ) {
        Text(view.name.ifBlank { "Character" }, style = L5RTheme.type.display.copy(color = L5RTheme.colors.ink))
        val sub = listOfNotNull(view.clanName, view.familyName).joinToString(" · ")
        if (sub.isNotBlank()) {
            Text(sub, style = L5RTheme.type.caption.copy(color = L5RTheme.colors.inkMuted))
        }
        view.schoolName?.let {
            Text(it, style = L5RTheme.type.captionItalic.copy(color = L5RTheme.colors.inkMuted))
        }
    }
}

@Composable
private fun NavRow(section: SheetSection, selected: Boolean, onClick: () -> Unit) {
    val colors = L5RTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .height(Layout.navRowHeight)
            .background(if (selected) colors.paper else androidx.compose.ui.graphics.Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.s4),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (selected) {
            Box(Modifier.width(3.dp).fillMaxHeight().background(colors.accentCrimson))
        }
        Text(section.kanji, style = L5RTheme.type.heading2.copy(color = if (selected) colors.accentCrimson else colors.inkMuted))
        Text(
            section.title.uppercase(),
            style = L5RTheme.type.label.copy(color = if (selected) colors.accentCrimson else colors.inkMuted),
            modifier = Modifier.padding(start = Spacing.s3),
        )
    }
}

@Composable
private fun ExtraNavRow(label: String, kanji: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(Layout.navRowHeight)
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.s4),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(kanji, style = L5RTheme.type.heading2.copy(color = L5RTheme.colors.inkFaint))
        Text(
            label.uppercase(),
            style = L5RTheme.type.label.copy(color = L5RTheme.colors.inkMuted),
            modifier = Modifier.padding(start = Spacing.s3),
        )
    }
}
