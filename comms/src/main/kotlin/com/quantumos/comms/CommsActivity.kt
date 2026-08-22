package com.quantumos.comms

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.quantumos.appshell.engageFieldUnitDisplay
import com.quantumos.appshell.hideSystemBars
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.quantumos.appshell.Phosphor
import com.quantumos.appshell.PhosphorHueRuntime
import com.quantumos.comms.ui.components.AppShell
import com.quantumos.comms.ui.screens.CommsScreen

/*
 * COMMS -- docked into the launcher's shared App Shell (Core Apps Fix-Pass, Decision 86). Launched
 * internally by the launcher's COMMS instrument tile via a plain Intent (same task, no
 * NEW_TASK/CLEAR_TOP) after the stepped PLEASE STANDBY hand-off beat. No BackHandler is added here
 * -- the Shell owns back once docked, so the system/predictive back gesture simply finishes this
 * Activity and returns to the still-live LauncherActivity on HOME. The "◄ HOME" line in AppShell's
 * header is the same return path, made explicit and tappable.
 */
class CommsActivity : ComponentActivity() {
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
            // Reads PhosphorHueRuntime (Core Apps Polish Pass, Item 2) -- the one process-wide live
            // source of truth every docked module + CONFIG + the launcher shares.
            val context = LocalContext.current
            PhosphorHueRuntime.init(context)
            val hue by PhosphorHueRuntime.activeHue.collectAsState()
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
                    onCycleHue = { PhosphorHueRuntime.cycleHue(context) },
                    contentPadding = padding
                )
            }
        }
    }
}
