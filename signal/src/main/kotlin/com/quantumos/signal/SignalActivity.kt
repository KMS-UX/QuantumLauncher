package com.quantumos.signal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.quantumos.appshell.Phosphor
import com.quantumos.core.PhosphorHue
import com.quantumos.signal.ui.SignalScreen
import com.quantumos.signal.ui.components.AppShell

/*
 * SignalActivity -- docked into the launcher's shared App Shell (SIGNAL + CONFIG Task Brief). Built
 * natively in this monorepo from the first commit, per the brief's §0: real CLAUDE.md canon, the
 * real :app-shell module, no separate AI-Studio generation session to drift from house style.
 * Launched internally by the launcher's SIGNAL instrument tile via a plain Intent (same task, no
 * NEW_TASK/CLEAR_TOP). No BackHandler is added here -- the Shell owns back once docked, so the
 * system/predictive back gesture simply finishes this Activity and returns to the still-live
 * LauncherActivity on HOME. The "◄ HOME" line in AppShell's header is the same return path, made
 * explicit and tappable.
 *
 * Active phosphor hue is local, per-module state (PhosphorHue, defaulting GREEN) -- matching how
 * every other docked module does it, the same known pre-existing phosphor-sync gap the brief
 * explicitly keeps out of scope (§4).
 */
class SignalActivity : ComponentActivity() {

    private val viewModel: SignalViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            var themeHue by remember { mutableStateOf(PhosphorHue.GREEN) }
            val themeColor = Phosphor.bright(themeHue)
            val themeColorDim = Phosphor.dim(themeHue)

            AppShell(
                title = "Signal",
                themeColor = themeColor,
                onReturnHome = { finish() }
            ) { padding ->
                SignalScreen(
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
