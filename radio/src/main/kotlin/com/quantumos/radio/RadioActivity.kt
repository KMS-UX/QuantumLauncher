package com.quantumos.radio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.quantumos.appshell.engageFieldUnitDisplay
import com.quantumos.appshell.hideSystemBars
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.quantumos.appshell.Phosphor
import com.quantumos.appshell.PhosphorHueRuntime
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
 * Active phosphor hue now reads PhosphorHueRuntime (Core Apps Polish Pass, Item 2) -- the one
 * process-wide live source of truth every docked module + CONFIG + the launcher shares, replacing
 * the old per-module `remember { mutableStateOf(PhosphorHue.GREEN) }` that never saw a hue change
 * made anywhere else.
 */
class RadioActivity : ComponentActivity() {
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
            val context = LocalContext.current
            PhosphorHueRuntime.init(context)
            val themeHue by PhosphorHueRuntime.activeHue.collectAsState()
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
                    onCycleTheme = { PhosphorHueRuntime.cycleHue(context) },
                    contentPadding = padding
                )
            }
        }
    }
}
