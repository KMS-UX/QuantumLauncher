package com.quantumos.files

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.quantumos.appshell.engageFieldUnitDisplay
import com.quantumos.appshell.hideSystemBars
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

    // Held at Activity level (not fetched inside setContent) so onResume/onPause can reach it. The
    // uptime readout writes a Compose state every second; ticking it while FILES is not in front
    // is idle redraw in a module the Operator is not even looking at -- see startVitals().
    private val viewModel: FileExplorerViewModel by viewModels()

    override fun onResume() {
        super.onResume()
        viewModel.startVitals()
    }

    override fun onPause() {
        super.onPause()
        viewModel.stopVitals()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        // A transient reveal, a fold/unfold or coming back from another app all leave
        // the system bars showing. Re-hide whenever this window is the one in front.
        if (hasFocus) hideSystemBars()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        engageFieldUnitDisplay()
        setContent {
            // activeHue is driven by PhosphorHueRuntime, the one process-wide source of truth (Core
            // Apps Polish Pass, Item 2) -- the ViewModel collects it rather than owning it, so a hue
            // change made anywhere lands here live. Phosphor.bright/dim is the single token source;
            // never hardcode hues here.
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
