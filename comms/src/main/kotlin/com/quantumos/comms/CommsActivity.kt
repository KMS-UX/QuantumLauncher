package com.quantumos.comms

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.quantumos.appshell.Phosphor
import com.quantumos.comms.ui.components.AppShell
import com.quantumos.comms.ui.screens.CommsScreen
import com.quantumos.core.PhosphorHue

/*
 * COMMS -- docked into the launcher's shared App Shell (Core Apps Fix-Pass, Decision 86). Launched
 * internally by the launcher's COMMS instrument tile via a plain Intent (same task, no
 * NEW_TASK/CLEAR_TOP) after the stepped PLEASE STANDBY hand-off beat. No BackHandler is added here
 * -- the Shell owns back once docked, so the system/predictive back gesture simply finishes this
 * Activity and returns to the still-live LauncherActivity on HOME. The "◄ HOME" line in AppShell's
 * header is the same return path, made explicit and tappable.
 */
class CommsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // Local default hue, matching the Optics/Nav/Files docking pattern -- this repo doesn't
            // yet sync a docked module's phosphor hue with the launcher's live selection (a known,
            // pre-existing limitation, not something this pass fixes).
            var hue by remember { mutableStateOf(PhosphorHue.GREEN) }
            val bright = Phosphor.bright(hue)

            val viewModel: CommsViewModel = viewModel()

            AppShell(
                title = "Comms",
                themeColor = bright,
                onReturnHome = { finish() }
            ) { padding: PaddingValues ->
                CommsScreen(
                    viewModel = viewModel,
                    hue = hue,
                    onCycleHue = {
                        hue = when (hue) {
                            PhosphorHue.GREEN -> PhosphorHue.AMBER
                            PhosphorHue.AMBER -> PhosphorHue.CYAN
                            PhosphorHue.CYAN -> PhosphorHue.GREEN
                        }
                    },
                    contentPadding = padding
                )
            }
        }
    }
}
