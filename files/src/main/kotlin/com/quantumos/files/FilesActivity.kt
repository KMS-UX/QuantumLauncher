package com.quantumos.files

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.quantumos.appshell.Phosphor
import com.quantumos.files.ui.components.AppShell
import com.quantumos.files.ui.screens.FilesScreen
import com.quantumos.files.viewmodel.FileExplorerViewModel

/*
 * FilesActivity -- docked into the launcher's shared App Shell (Core Apps Fix-Pass, Decision 86).
 * Launched internally by the launcher's FILES instrument tile via a plain Intent (same task, no
 * NEW_TASK/CLEAR_TOP). No BackHandler is added here -- the Shell owns back once docked, so the
 * system/predictive back gesture simply finishes this Activity and returns to the still-live
 * LauncherActivity on HOME. The "◄ HOME" line in AppShell's header is the same return path, made
 * explicit and tappable. Mirrors OpticsActivity's structure.
 */
class FilesActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            val viewModel: FileExplorerViewModel = viewModel()

            // The ViewModel owns activeHue (default GREEN) -- this docked module's hue is not yet
            // synced with the launcher's live selection (known, pre-existing limitation shared with
            // Optics/Nav). Phosphor.bright/dim is the single token source; never hardcode hues here.
            val currentThemeColor = Phosphor.bright(viewModel.activeHue)
            val currentThemeColorDim = Phosphor.dim(viewModel.activeHue)

            AppShell(
                title = "Files",
                themeColor = currentThemeColor,
                onReturnHome = { finish() }
            ) { contentPadding ->
                FilesScreen(
                    viewModel = viewModel,
                    themeColor = currentThemeColor,
                    dimColor = currentThemeColorDim,
                    contentPadding = contentPadding
                )
            }
        }
    }
}
