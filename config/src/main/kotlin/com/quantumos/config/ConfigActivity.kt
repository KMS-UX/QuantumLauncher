package com.quantumos.config

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.quantumos.appshell.engageFieldUnitDisplay
import com.quantumos.appshell.hideSystemBars
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quantumos.appshell.Phosphor
import com.quantumos.config.ui.ConfigScreen
import com.quantumos.config.ui.components.AppShell

/*
 * ConfigActivity -- docked into the launcher's shared App Shell (SIGNAL + CONFIG Task Brief). Built
 * natively in this monorepo from the first commit, per the brief's §0. Launched internally by the
 * launcher's CONFIG instrument tile via a plain Intent (same task, no NEW_TASK/CLEAR_TOP). No
 * BackHandler is added here -- the Shell owns back once docked, so the system/predictive back gesture
 * simply finishes this Activity and returns to the still-live LauncherActivity on HOME.
 *
 * Unlike every other docked module, CONFIG's phosphor hue is NOT a throwaway local `remember` --
 * it's read live from ConfigViewModel, which is itself seeded from (and writes straight back to) the
 * shared SettingsStore. CONFIG's whole purpose is to BE the durable setting, so its own chrome tracks
 * it directly rather than keeping a second, disposable copy.
 */
class ConfigActivity : ComponentActivity() {

    private val viewModel: ConfigViewModel by viewModels()

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
            val hue by viewModel.phosphorHue.collectAsStateWithLifecycle()
            val bootPace by viewModel.bootPace.collectAsStateWithLifecycle()
            val region by viewModel.region.collectAsStateWithLifecycle()

            val themeColor = Phosphor.bright(hue)
            val themeColorDim = Phosphor.dim(hue)

            AppShell(
                title = "Config",
                themeColor = themeColor,
                onReturnHome = { finish() }
            ) { padding ->
                ConfigScreen(
                    phosphorHue = hue,
                    bootPace = bootPace,
                    region = region,
                    themeColor = themeColor,
                    themeColorDim = themeColorDim,
                    onCyclePhosphor = viewModel::cyclePhosphorHue,
                    onCycleBootPace = viewModel::cycleBootPace,
                    onCycleRegion = viewModel::cycleDeploymentRegion,
                    onOpenAvatarPreview = {
                        // Provisional dev-preview entry point (Phase 4b) -- setClassName avoids a
                        // Gradle dependency edge from :config onto :quark-avatar; both modules live
                        // in the same process/APK so this resolves at runtime. See
                        // art/quark-avatar/PRODUCTION_LOG.md for why this is temporary, not the real
                        // navigation decision for where "QUARK Core App" lives.
                        startActivity(Intent().setClassName(packageName, "com.quantumos.quarkavatar.QuarkAvatarActivity"))
                    },
                    contentPadding = padding
                )
            }
        }
    }
}
