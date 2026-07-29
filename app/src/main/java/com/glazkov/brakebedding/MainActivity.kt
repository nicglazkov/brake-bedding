package com.glazkov.brakebedding

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.glazkov.brakebedding.ui.EditorViewModel
import com.glazkov.brakebedding.ui.RunViewModel
import com.glazkov.brakebedding.ui.SettingsViewModel
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

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BrakeBeddingTheme {
                BrakeBeddingApp()
            }
        }
    }
}

@Composable
private fun BrakeBeddingApp() {
    val navController = rememberNavController()
    val runViewModel: RunViewModel = viewModel(factory = RunViewModel.Factory)
    val settingsViewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory)

    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { runViewModel.refreshPermissionState() }

    NavHost(navController = navController, startDestination = Routes.RUN) {
        composable(Routes.RUN) {
            val state by runViewModel.state.collectAsStateWithLifecycle()
            RunScreen(
                state = state,
                onStart = runViewModel::start,
                onPause = runViewModel::pause,
                onResume = runViewModel::resume,
                onStop = runViewModel::stop,
                onSkipStage = runViewModel::skipStage,
                onEditProcedure = { navController.navigate(Routes.EDITOR) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onOpenHelp = { navController.navigate(Routes.HELP) },
                onRequestPermission = {
                    permissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                        ),
                    )
                },
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
                appVersion = BuildConfig.VERSION_NAME,
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
