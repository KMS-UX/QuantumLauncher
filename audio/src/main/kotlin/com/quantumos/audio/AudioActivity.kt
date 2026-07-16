package com.quantumos.audio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.quantumos.appshell.Phosphor
import com.quantumos.audio.ui.RecorderScreen
import com.quantumos.audio.ui.PlayerScreen
import com.quantumos.audio.ui.QuarkScreen
import com.quantumos.audio.ui.ConfigScreen
import com.quantumos.audio.ui.LogScreen
import com.quantumos.audio.ui.components.AppShell
import com.quantumos.audio.ui.components.ChannelTabs
import com.quantumos.core.PhosphorHue

/*
 * AudioActivity -- docked into the launcher's shared App Shell (Core Apps Fix-Pass, Decision 86).
 * Launched internally by the launcher's AUDIO instrument tile via a plain Intent (same task, no
 * NEW_TASK/CLEAR_TOP). No BackHandler is added here -- the Shell owns back once docked, so the
 * system/predictive back gesture simply finishes this Activity and returns to the still-live
 * LauncherActivity on HOME. The "◄ HOME" line in AppShell's header is the same return path, made
 * explicit and tappable.
 *
 * themeHue is a plain composition-level `remember`, not persisted, matching Optics/Nav's existing
 * pattern for docked modules (a known, pre-existing limitation the Director has already accepted for
 * the other docked modules -- not something to fix in this session).
 *
 * The Recorder screen is the default/primary screen (AudioChannel.RECORDER default in AudioViewModel,
 * matching the source app's already-correct default + back-routing).
 */
class AudioActivity : ComponentActivity() {

    private val viewModel: AudioViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            var themeHue by remember { mutableStateOf(PhosphorHue.GREEN) }
            val themeColor = Phosphor.bright(themeHue)
            val dimColor = Phosphor.dim(themeHue)

            val onCycleHue: () -> Unit = {
                themeHue = when (themeHue) {
                    PhosphorHue.GREEN -> PhosphorHue.AMBER
                    PhosphorHue.AMBER -> PhosphorHue.CYAN
                    PhosphorHue.CYAN -> PhosphorHue.GREEN
                }
                viewModel.addLog("AUDIO: PHOSPHOR LINE -> ${themeHue.name}")
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
