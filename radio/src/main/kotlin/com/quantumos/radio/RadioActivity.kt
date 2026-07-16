package com.quantumos.radio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.quantumos.appshell.Phosphor
import com.quantumos.core.PhosphorHue
import com.quantumos.radio.ui.RadioScreen
import com.quantumos.radio.ui.components.AppShell

/*
 * RadioActivity -- docked into the launcher's shared App Shell (Core Apps Fix-Pass, Decision 86).
 * Launched internally by the launcher's RADIO instrument tile via a plain Intent (same task, no
 * NEW_TASK/CLEAR_TOP). No BackHandler is added here -- the Shell owns back once docked, so the
 * system/predictive back gesture simply finishes this Activity and returns to the still-live
 * LauncherActivity on HOME. The "◄ HOME" line in AppShell's header is the same return path, made
 * explicit and tappable. Mirrors OpticsActivity's structure, simplified: RADIO has no camera/sensor
 * plumbing, so its content is a single RadioScreen composable wrapped in AppShell.
 *
 * Active phosphor hue is local, per-module state (PhosphorHue, defaulting GREEN) -- matching how
 * Optics/Nav do it, a known pre-existing limitation (not a global launcher-wide setting from a docked
 * library module) that this pass doesn't attempt to fix.
 */
class RadioActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            var themeHue by remember { mutableStateOf(PhosphorHue.GREEN) }
            val themeColor = Phosphor.bright(themeHue)
            val themeColorDim = Phosphor.dim(themeHue)

            val viewModel: RadioViewModel = viewModel()

            AppShell(
                title = "Radio",
                themeColor = themeColor,
                onReturnHome = { finish() }
            ) { padding ->
                RadioScreen(
                    viewModel = viewModel,
                    themeColor = themeColor,
                    themeColorDim = themeColorDim,
                    warnColor = Phosphor.Warn,
                    onCycleTheme = {
                        themeHue = when (themeHue) {
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
