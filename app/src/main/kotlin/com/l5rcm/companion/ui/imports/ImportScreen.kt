package com.l5rcm.companion.ui.imports

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.l5rcm.companion.ui.AppViewModel
import com.l5rcm.companion.ui.CharacterUiState
import com.l5rcm.companion.ui.sheet.SheetScreen
import com.l5rcm.companion.ui.theme.L5RTheme
import com.l5rcm.companion.ui.theme.Radii
import com.l5rcm.companion.ui.theme.Spacing
import com.l5rcm.companion.ui.widgets.OrnateDivider
import com.l5rcm.companion.ui.widgets.RicePaperOverlay

/** Routes the main destination by character state, owning the SAF file picker. */
@Composable
fun ImportRouter(
    state: CharacterUiState,
    viewModel: AppViewModel,
    onOpenLibrary: () -> Unit,
    onScanQr: () -> Unit,
    onOpenDice: () -> Unit,
) {
    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> uri?.let { viewModel.importCharacter(it) } }

    val openPicker = { picker.launch(arrayOf("application/json", "application/octet-stream", "*/*")) }

    when (state) {
        is CharacterUiState.Ready ->
            SheetScreen(
                view = state.view,
                onOpenLibrary = onOpenLibrary,
                onImport = openPicker,
                onOpenDice = onOpenDice,
            )

        CharacterUiState.Loading -> LoadingScreen()

        is CharacterUiState.NeedsDatapacks ->
            MissingPacksScreen(state, onOpenLibrary = onOpenLibrary)

        is CharacterUiState.Error ->
            ImportLanding(state.message, onImport = openPicker, onScanQr = onScanQr, onOpenLibrary = onOpenLibrary)

        CharacterUiState.Empty ->
            ImportLanding(null, onImport = openPicker, onScanQr = onScanQr, onOpenLibrary = onOpenLibrary)
    }
}

@Composable
private fun ScreenBackground(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize().background(L5RTheme.colors.paper)) {
        RicePaperOverlay(Modifier.fillMaxSize())
        content()
    }
}

@Composable
private fun LoadingScreen() = ScreenBackground {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = L5RTheme.colors.accentCrimson)
    }
}

@Composable
private fun ImportLanding(
    message: String?,
    onImport: () -> Unit,
    onScanQr: () -> Unit,
    onOpenLibrary: () -> Unit,
) =
    ScreenBackground {
        Column(
            modifier = Modifier.fillMaxSize().padding(Spacing.s6).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text("L5RCM", style = L5RTheme.type.display.copy(color = L5RTheme.colors.ink))
            Text(
                "Companion",
                style = L5RTheme.type.heading2.copy(color = L5RTheme.colors.accentGold),
            )
            OrnateDivider(Modifier.padding(vertical = Spacing.s5))
            Text(
                "Import a character from the desktop app to view their sheet at the table.",
                style = L5RTheme.type.body.copy(color = L5RTheme.colors.inkMuted),
                textAlign = TextAlign.Center,
            )
            if (message != null) {
                Text(
                    message,
                    style = L5RTheme.type.caption.copy(color = L5RTheme.colors.error),
                    modifier = Modifier.padding(top = Spacing.s3),
                    textAlign = TextAlign.Center,
                )
            }
            Button(
                onClick = onImport,
                shape = Radii.button,
                colors = ButtonDefaults.buttonColors(
                    containerColor = L5RTheme.colors.accentCrimson,
                    contentColor = L5RTheme.colors.whiteWash,
                ),
                modifier = Modifier.padding(top = Spacing.s5),
            ) {
                Text("IMPORT .L5R", style = L5RTheme.type.label)
            }
            OutlinedButton(
                onClick = onScanQr,
                shape = Radii.button,
                modifier = Modifier.padding(top = Spacing.s2),
            ) {
                Text("SCAN QR CODE", style = L5RTheme.type.label.copy(color = L5RTheme.colors.ink))
            }
            OutlinedButton(
                onClick = onOpenLibrary,
                shape = Radii.button,
                modifier = Modifier.padding(top = Spacing.s2),
            ) {
                Text("DATAPACK LIBRARY", style = L5RTheme.type.label.copy(color = L5RTheme.colors.inkMuted))
            }
        }
    }

@Composable
private fun MissingPacksScreen(state: CharacterUiState.NeedsDatapacks, onOpenLibrary: () -> Unit) =
    ScreenBackground {
        Column(
            modifier = Modifier.fillMaxSize().padding(Spacing.s6).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                state.characterName.ifBlank { "Character" },
                style = L5RTheme.type.display.copy(color = L5RTheme.colors.ink),
            )
            OrnateDivider(Modifier.padding(vertical = Spacing.s4))
            Text(
                "This character needs datapacks that aren't installed yet:",
                style = L5RTheme.type.body.copy(color = L5RTheme.colors.inkMuted),
                textAlign = TextAlign.Center,
            )
            Column(Modifier.padding(top = Spacing.s3)) {
                state.missing.forEach { ref ->
                    Text(
                        "• ${ref.name.ifBlank { ref.id }}${if (ref.version.isNotBlank()) " (v${ref.version})" else ""}",
                        style = L5RTheme.type.body.copy(color = L5RTheme.colors.ink),
                    )
                }
            }
            Button(
                onClick = onOpenLibrary,
                shape = Radii.button,
                colors = ButtonDefaults.buttonColors(
                    containerColor = L5RTheme.colors.accentCrimson,
                    contentColor = L5RTheme.colors.whiteWash,
                ),
                modifier = Modifier.padding(top = Spacing.s5).width(220.dp),
            ) {
                Text("OPEN LIBRARY", style = L5RTheme.type.label)
            }
        }
    }
