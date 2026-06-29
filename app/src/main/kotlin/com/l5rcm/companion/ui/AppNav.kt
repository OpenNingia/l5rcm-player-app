package com.l5rcm.companion.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.l5rcm.companion.ui.imports.ImportRouter
import com.l5rcm.companion.ui.imports.qr.QrScanScreen
import com.l5rcm.companion.ui.library.LibraryScreen
import com.l5rcm.companion.ui.theme.L5RTheme

object Routes {
    const val MAIN = "main"
    const val LIBRARY = "library"
    const val QR_SCAN = "qr_scan"
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

        NavHost(navController = navController, startDestination = Routes.MAIN) {
            composable(Routes.MAIN) {
                ImportRouter(
                    state = characterState,
                    viewModel = viewModel,
                    onOpenLibrary = openLibrary,
                    onScanQr = scanQr,
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
