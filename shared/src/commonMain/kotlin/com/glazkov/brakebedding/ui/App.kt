package com.glazkov.brakebedding.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.glazkov.brakebedding.platform.appVersionName
import com.glazkov.brakebedding.ui.screens.EditorScreen
import com.glazkov.brakebedding.ui.screens.HelpScreen
import com.glazkov.brakebedding.ui.screens.RunScreen
import com.glazkov.brakebedding.ui.screens.SettingsScreen
import com.glazkov.brakebedding.ui.theme.BrakeBeddingTheme

private object Routes {
    const val RUN = "run"
    const val EDITOR = "editor"
    const val SETTINGS = "settings"
    const val HELP = "help"
}

/** The root of the app. Each platform entry point shows this composable. */
@Composable
fun BrakeBeddingApp() {
    BrakeBeddingTheme {
        val navController = rememberNavController()
        val runViewModel: RunViewModel = viewModel(factory = RunViewModel.Factory)
        val settingsViewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory)
        val actions = rememberPlatformActions(onPermissionResult = runViewModel::refreshPermissionState)

        NavHost(navController = navController, startDestination = Routes.RUN) {
            composable(Routes.RUN) {
                val state by runViewModel.state.collectAsStateWithLifecycle()
                RunScreen(
                    state = state,
                    onStart = { actions.startRun(runViewModel::start) },
                    onPause = runViewModel::pause,
                    onResume = runViewModel::resume,
                    onStop = runViewModel::stop,
                    onSkipStage = runViewModel::skipStage,
                    onEditProcedure = { navController.navigate(Routes.EDITOR) },
                    onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                    onOpenHelp = { navController.navigate(Routes.HELP) },
                    onRequestPermission = actions::requestLocationPermission,
                    onRefreshPermissionState = runViewModel::refreshPermissionState,
                )
            }

            composable(Routes.EDITOR) {
                val editorViewModel: EditorViewModel = viewModel(factory = EditorViewModel.Factory)
                val state by editorViewModel.state.collectAsStateWithLifecycle()
                EditorScreen(
                    state = state,
                    onBack = { navController.popBackStack() },
                    onRename = editorViewModel::rename,
                    onUpsert = editorViewModel::upsert,
                    onRemove = editorViewModel::remove,
                    onUndoRemove = editorViewModel::undoRemove,
                    onMove = editorViewModel::move,
                    onApplyPreset = editorViewModel::applyPreset,
                )
            }

            composable(Routes.SETTINGS) {
                val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
                SettingsScreen(
                    settings = settings,
                    appVersion = appVersionName(),
                    onBack = { navController.popBackStack() },
                    onUnitSystem = settingsViewModel::setUnitSystem,
                    onVoiceCues = settingsViewModel::setVoiceCues,
                    onHapticCues = settingsViewModel::setHapticCues,
                    onKeepScreenOn = settingsViewModel::setKeepScreenOn,
                )
            }

            composable(Routes.HELP) {
                HelpScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
