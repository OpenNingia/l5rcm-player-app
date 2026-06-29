package com.l5rcm.companion.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.l5rcm.companion.data.catalog.CatalogEntry
import com.l5rcm.companion.data.repository.InstalledPack
import com.l5rcm.companion.ui.AppViewModel
import com.l5rcm.companion.ui.theme.L5RTheme
import com.l5rcm.companion.ui.theme.Radii
import com.l5rcm.companion.ui.theme.Spacing
import com.l5rcm.companion.ui.widgets.OrnateDivider
import com.l5rcm.companion.ui.widgets.RicePaperOverlay
import com.l5rcm.companion.ui.widgets.SheetPanel

/** Datapack library: install from the official repo, enable/disable, delete. */
@Composable
fun LibraryScreen(viewModel: AppViewModel, onBack: () -> Unit) {
    val state by viewModel.library.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.loadCatalog() }

    Box(Modifier.fillMaxSize().background(L5RTheme.colors.paper)) {
        RicePaperOverlay(Modifier.fillMaxSize())
        Column(Modifier.fillMaxSize().padding(Spacing.s4)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("DATAPACK LIBRARY", style = L5RTheme.type.display.copy(color = L5RTheme.colors.ink))
                TextButton(onClick = onBack) {
                    Text("DONE", style = L5RTheme.type.label.copy(color = L5RTheme.colors.accentCrimson))
                }
            }
            OrnateDivider()

            state.error?.let {
                Text(
                    it,
                    style = L5RTheme.type.caption.copy(color = L5RTheme.colors.error),
                    modifier = Modifier.padding(bottom = Spacing.s2),
                )
            }

            LazyColumn(
                Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(Spacing.s4),
            ) {
                if (state.installed.isNotEmpty()) {
                    item {
                        SheetPanel("Installed") {
                            state.installed.forEach { pack ->
                                InstalledRow(
                                    pack = pack,
                                    onToggle = { viewModel.setPackEnabled(pack.id, it) },
                                    onRemove = { viewModel.removePack(pack.id) },
                                )
                            }
                        }
                    }
                }

                item {
                    SheetPanel("Available") {
                        when {
                            state.loadingCatalog -> Box(
                                Modifier.fillMaxWidth().padding(Spacing.s4),
                                contentAlignment = Alignment.Center,
                            ) { CircularProgressIndicator(color = L5RTheme.colors.accentCrimson) }

                            state.catalog.isEmpty() -> Text(
                                "No datapacks available. Pull to refresh once you're online.",
                                style = L5RTheme.type.body.copy(color = L5RTheme.colors.inkMuted),
                            )
                        }
                    }
                }

                items(state.catalog, key = { it.name }) { entry ->
                    CatalogRow(
                        entry = entry,
                        installed = state.installed.any { it.id.equals(entry.coreId, ignoreCase = true) },
                        installing = state.installingPackName == entry.name,
                        onInstall = { viewModel.install(entry) },
                    )
                }
            }
        }
    }
}

/** Heuristic pack id from a catalog asset name, for the "installed" check. */
private val CatalogEntry.coreId: String
    get() = name.substringBefore('-').removeSuffix("_pack").lowercase()

@Composable
private fun InstalledRow(pack: InstalledPack, onToggle: (Boolean) -> Unit, onRemove: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = Spacing.s1),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(Modifier.weight(1f)) {
            Text(pack.id, style = L5RTheme.type.body.copy(color = L5RTheme.colors.ink))
            if (pack.version.isNotBlank()) {
                Text("v${pack.version}", style = L5RTheme.type.caption.copy(color = L5RTheme.colors.inkFaint))
            }
        }
        Switch(
            checked = pack.enabled,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(checkedTrackColor = L5RTheme.colors.accentGold),
        )
        TextButton(onClick = onRemove) {
            Text("REMOVE", style = L5RTheme.type.caption.copy(color = L5RTheme.colors.error))
        }
    }
}

@Composable
private fun CatalogRow(
    entry: CatalogEntry,
    installed: Boolean,
    installing: Boolean,
    onInstall: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(L5RTheme.colors.paperLight, Radii.input)
            .padding(Spacing.s3),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(Modifier.weight(1f)) {
            Text(entry.name, style = L5RTheme.type.body.copy(color = L5RTheme.colors.ink))
            if (entry.version.isNotBlank()) {
                Text("v${entry.version}", style = L5RTheme.type.caption.copy(color = L5RTheme.colors.inkFaint))
            }
        }
        when {
            installing -> CircularProgressIndicator(
                color = L5RTheme.colors.accentCrimson,
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
            )
            installed -> Text("INSTALLED", style = L5RTheme.type.caption.copy(color = L5RTheme.colors.success))
            else -> OutlinedButton(onClick = onInstall, shape = Radii.button) {
                Text("INSTALL", style = L5RTheme.type.label.copy(color = L5RTheme.colors.accentCrimson))
            }
        }
    }
}
