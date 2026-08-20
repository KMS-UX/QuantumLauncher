package com.quantumos.quarkavatar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.quantumos.appshell.Phosphor
import com.quantumos.appshell.PhosphorHueRuntime
import com.quantumos.quarkavatar.ui.QuarkAvatarScreen
import com.quantumos.quarkavatar.ui.components.AppShell

/*
 * QuarkAvatarActivity -- the Phase 4b AGSL overlay shader dev-preview surface (see
 * art/quark-avatar/PRODUCTION_LOG.md). Reached only via CONFIG's temporary "QUARK AVATAR -- DEV
 * PREVIEW" row (a provisional entry point, NOT the real navigation decision for where "QUARK Core
 * App" lives -- that's a Director call, out of scope here). No BackHandler -- the Shell owns back
 * once docked, same as every other docked module.
 *
 * Posture/Speaking/Stealth are LOCAL, unpersisted demo state -- NOT read from
 * QuantumRuntime.masterState. Wiring the real QuarkReflexPosture/Stealth state in requires the same
 * kind of cross-module extraction :quark-brain needed (:app depends on every docked module, never
 * the reverse, so a docked module can't reach :app's QuantumRuntime directly) -- flagged as follow-up
 * work, not attempted this pass. HUE is the one exception: it drives the real, already-safe-to-reach
 * PhosphorHueRuntime, same as every other docked module.
 */
class QuarkAvatarActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            PhosphorHueRuntime.init(context)
            val themeHue by PhosphorHueRuntime.activeHue.collectAsState()
            val themeColor = Phosphor.bright(themeHue)
            val themeColorDim = Phosphor.dim(themeHue)

            var posture by remember { mutableStateOf(DemoPosture.NEUTRAL) }
            var speakingPreview by remember { mutableStateOf(false) }
            var stealthPreview by remember { mutableStateOf(false) }

            AppShell(
                title = "Quark",
                themeColor = themeColor,
                onReturnHome = { finish() }
            ) { padding ->
                QuarkAvatarScreen(
                    posture = posture,
                    themeHue = themeHue,
                    themeColor = themeColor,
                    themeColorDim = themeColorDim,
                    speakingPreview = speakingPreview,
                    stealthPreview = stealthPreview,
                    onCyclePosture = {
                        val values = DemoPosture.entries
                        posture = values[(values.indexOf(posture) + 1) % values.size]
                    },
                    onCycleHue = { PhosphorHueRuntime.cycleHue(context) },
                    onToggleSpeaking = { speakingPreview = !speakingPreview },
                    onToggleStealth = { stealthPreview = !stealthPreview },
                    contentPadding = padding
                )
            }
        }
    }
}
