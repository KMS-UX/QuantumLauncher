package com.quantumos.audio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.quantumos.appshell.engageFieldUnitDisplay
import com.quantumos.appshell.hideSystemBars
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.quantumos.appshell.Phosphor
import com.quantumos.appshell.PhosphorHueRuntime
import com.quantumos.audio.ui.RecorderScreen
import com.quantumos.audio.ui.PlayerScreen
import com.quantumos.audio.ui.QuarkScreen
import com.quantumos.audio.ui.ConfigScreen
import com.quantumos.audio.ui.LogScreen
import com.quantumos.audio.ui.components.AppShell
import com.quantumos.audio.ui.components.ChannelTabs

/*
 * AudioActivity -- docked into the launcher's shared App Shell (Core Apps Fix-Pass, Decision 86).
 * Launched internally by the launcher's AUDIO instrument tile via a plain Intent (same task, no
 * NEW_TASK/CLEAR_TOP). No BackHandler is added here -- the Shell owns back once docked, so the
 * system/predictive back gesture simply finishes this Activity and returns to the still-live
 * LauncherActivity on HOME. The "◄ HOME" line in AppShell's header is the same return path, made
 * explicit and tappable.
 *
 * themeHue now reads PhosphorHueRuntime (Core Apps Polish Pass, Item 2) -- the one process-wide live
 * source of truth every docked module + CONFIG + the launcher shares, replacing the old per-module
 * `remember { mutableStateOf(PhosphorHue.GREEN) }` that never saw a hue change made anywhere else.
 *
 * The Recorder screen is the default/primary screen (AudioChannel.RECORDER default in AudioViewModel,
 * matching the source app's already-correct default + back-routing).
 */
class AudioActivity : ComponentActivity() {

    private val viewModel: AudioViewModel by viewModels()

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
            val dimColor = Phosphor.dim(themeHue)

            val onCycleHue: () -> Unit = {
                PhosphorHueRuntime.cycleHue(context)
                viewModel.addLog("AUDIO: PHOSPHOR LINE -> ${PhosphorHueRuntime.activeHue.value.name}")
            }

            AppShell(
                title = "Audio",
                themeColor = themeColor,
                onReturnHome = { finish() }
            ) { padding ->
                val activeChannel by viewModel.channel.collectAsState()
                val isRecordingState by viewModel.engine.isRecording.collectAsState()

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    ChannelTabs(
                        current = activeChannel,
                        themeColor = themeColor,
                        dimColor = dimColor,
                        isRecording = isRecordingState,
                        onSelect = { viewModel.setChannel(it) }
                    )
                    Box(Modifier.weight(1f).fillMaxSize()) {
                        when (activeChannel) {
                            AudioChannel.RECORDER -> RecorderScreen(
                                viewModel = viewModel,
                                themeColor = themeColor,
                                dimColor = dimColor
                            )
                            AudioChannel.PLAYER -> PlayerScreen(
                                viewModel = viewModel,
                                themeColor = themeColor,
                                dimColor = dimColor
                            )
                            AudioChannel.QUARK -> QuarkScreen(
                                viewModel = viewModel,
                                themeColor = themeColor,
                                dimColor = dimColor,
                                onCycleHue = onCycleHue
                            )
                            AudioChannel.CONFIG -> ConfigScreen(
                                viewModel = viewModel,
                                themeColor = themeColor,
                                dimColor = dimColor,
                                activeHueName = themeHue.name,
                                onCycleHue = onCycleHue
                            )
                            AudioChannel.LOG -> LogScreen(
                                viewModel = viewModel,
                                themeColor = themeColor,
                                dimColor = dimColor
                            )
                        }
                    }
                }
            }
        }
    }
}
