package com.l5rcm.companion.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.l5rcm.companion.ui.dice.DiceScreen
import com.l5rcm.companion.ui.dice.DicePreset
import com.l5rcm.companion.ui.imports.ImportRouter
import com.l5rcm.companion.ui.imports.qr.QrScanScreen
import com.l5rcm.companion.ui.library.LibraryScreen
import com.l5rcm.companion.ui.theme.L5RTheme

object Routes {
    const val MAIN = "main"
    const val LIBRARY = "library"
    const val QR_SCAN = "qr_scan"
    const val DICE = "dice"
}

/** Top-level navigation. The clan accent follows the loaded character. */
@Composable
fun AppNav(viewModel: AppViewModel = hiltViewModel()) {
    val characterState by viewModel.character.collectAsStateWithLifecycle()
    val clanId = (characterState as? CharacterUiState.Ready)?.view?.clanId

    L5RTheme(clanId = clanId) {
        val navController = rememberNavController()
        // Stable lambdas to avoid recreating on each recomposition.
        val openLibrary = remember { { navController.navigate(Routes.LIBRARY); Unit } }
        val scanQr = remember { { navController.navigate(Routes.QR_SCAN); Unit } }
        val openDice = remember { { navController.navigate(Routes.DICE); Unit } }
        // A Combat-tab roll: stash the pre-filled config, then open the shared dice screen.
        val combatRoll = remember {
            { preset: DicePreset -> viewModel.prepareRoll(preset); navController.navigate(Routes.DICE) }
        }

        NavHost(navController = navController, startDestination = Routes.MAIN) {
            composable(Routes.MAIN) {
                ImportRouter(
                    state = characterState,
                    viewModel = viewModel,
                    onOpenLibrary = openLibrary,
                    onScanQr = scanQr,
                    onOpenDice = openDice,
                    onCombatRoll = combatRoll,
                )
            }
            composable(Routes.DICE) {
                val view = (characterState as? CharacterUiState.Ready)?.view
                val subtitle = view?.let {
                    listOfNotNull(it.clanName, "Rank ${it.insightRank}", it.schoolName).joinToString(" · ")
                }.orEmpty()
                val preset by viewModel.pendingDice.collectAsStateWithLifecycle()
                val combat by viewModel.combat.collectAsStateWithLifecycle()
                DiceScreen(
                    characterName = view?.name.orEmpty(),
                    characterSubtitle = subtitle,
                    onBack = { navController.popBackStack() },
                    preset = preset,
                    onPresetConsumed = viewModel::consumePendingDice,
                    // Only context rolls (driven by a preset) tie the Void toggle to the tracker.
                    voidAvailable = if (preset != null) combat?.voidCurrent else null,
                    onVoidSpent = viewModel::spendVoid,
                )
            }
            composable(Routes.LIBRARY) {
                LibraryScreen(
                    viewModel = viewModel,
                    onBack = {
                        navController.popBackStack()
                        viewModel.retryPending()
                    },
                )
            }
            composable(Routes.QR_SCAN) {
                QrScanScreen(
                    onResult = { json ->
                        navController.popBackStack()
                        viewModel.importCharacterFromJson(json)
                    },
                    onCancel = { navController.popBackStack() },
                )
            }
        }
    }
}
