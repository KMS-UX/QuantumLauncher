package com.quantumos.audio.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quantumos.appshell.Fonts
import com.quantumos.appshell.Phosphor
import com.quantumos.audio.AudioViewModel
import com.quantumos.core.QuarkReflexPosture
import kotlinx.coroutines.launch

/*
 * QUARK -- AUDIO's own local reactive-mascot tab (a small per-app flavor feature, distinct from the
 * launcher's own M5 QUARK Assistant View which is out of scope for this docked module). Ported from
 * the standalone repo's QuarkScreen.kt.
 *
 * The command grid drops the source app's "ENGAGE STEALTH" button: that toggled a LOCAL, purely
 * cosmetic flag with no real effect (unlike the launcher's real system-wide Stealth, which actually
 * dims emission + mutes SFX). Keeping a fake control with that name here risked misleading the
 * Operator into thinking it did something real. Flagging this as a scope call, not locking it
 * silently -- happy to wire it back in if the Director wants a cosmetic-only toggle kept for parity.
 */
@Composable
fun QuarkScreen(
    viewModel: AudioViewModel,
    themeColor: Color,
    dimColor: Color,
    onCycleHue: () -> Unit
) {
    val quarkState by viewModel.quarkPosture.collectAsState()
    val quarkText by viewModel.quarkText.collectAsState()
    val isRecordingState by viewModel.engine.isRecording.collectAsState()
    val isPlayingState by viewModel.engine.isPlaying.collectAsState()

    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    LaunchedEffect(quarkText) {
        scope.launch { scrollState.animateScrollTo(scrollState.maxValue) }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().height(180.dp),
            contentAlignment = Alignment.Center
        ) {
            QuarkMascot(quarkState = quarkState, themeColor = themeColor) {
                viewModel.setQuark(QuarkReflexPosture.HAPPY, "Operator contact registered, physical touch receptors functional.")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(90.dp)
                .border(width = 1.dp, color = themeColor, shape = RoundedCornerShape(4.dp))
                .background(Phosphor.Crt)
                .padding(10.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize().verticalScroll(scrollState)) {
                Text(
                    text = "QUARK LOCAL REFLEX MODULE",
                    color = themeColor.copy(alpha = 0.5f),
                    fontSize = 9.sp,
                    fontFamily = Fonts.ChakraPetch,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "QUARK > $quarkText",
                    color = if (quarkState == QuarkReflexPosture.WARN) Phosphor.Warn else themeColor,
                    fontSize = 11.sp,
                    fontFamily = Fonts.ChakraPetch,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 15.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "FIELD MANUAL COMMAND OVERRIDES",
            color = themeColor,
            fontSize = 11.sp,
            fontFamily = Fonts.ChakraPetch,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Start).padding(bottom = 8.dp)
        )

        Column(modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.fillMaxWidth()) {
                CommandButton(
                    modifier = Modifier.weight(1f),
                    label = "STATUS REPORT",
                    themeColor = themeColor
                ) {
                    val recState = if (isRecordingState) "RECORDER ACTIVE" else "RECORDER STANDBY"
                    val playState = if (isPlayingState) "PLAYBACK ACTIVE" else "PLAYBACK STANDBY"
                    viewModel.setQuark(QuarkReflexPosture.SCAN, "$recState. $playState. Core module nominal, Operator.")
                }
                Spacer(modifier = Modifier.width(8.dp))
                CommandButton(
                    modifier = Modifier.weight(1f),
                    label = "SAY SOMETHING",
                    themeColor = themeColor
                ) {
                    val lines = listOf(
                        "Listening for active acoustic envelopes, Operator.",
                        "Core telemetry nominal on this channel.",
                        "The used future demands absolute focus. Stay alert.",
                        "I'm designed to keep you vital. Let's monitor the sector."
                    )
                    viewModel.setQuark(QuarkReflexPosture.HAPPY, lines.random())
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            CommandButton(
                modifier = Modifier.fillMaxWidth(),
                label = "CYCLE PHOSPHOR",
                themeColor = themeColor
            ) {
                onCycleHue()
                viewModel.setQuark(QuarkReflexPosture.SCAN, "Recalibrating retina matrices, Operator.")
            }

            Spacer(modifier = Modifier.height(8.dp))

            CommandButton(
                modifier = Modifier.fillMaxWidth(),
                label = "TRIGGER WARN (TEST)",
                themeColor = Phosphor.Warn,
                outlineColor = Phosphor.Warn
            ) {
                viewModel.setQuark(QuarkReflexPosture.WARN, "TEST ALERT: this is the voice you'll hear when something's wrong. Drill complete.")
            }
        }
    }
}

@Composable
fun CommandButton(
    modifier: Modifier = Modifier,
    label: String,
    themeColor: Color,
    outlineColor: Color = themeColor,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(4.dp),
        modifier = modifier.height(44.dp).border(width = 1.dp, color = outlineColor, shape = RoundedCornerShape(4.dp))
    ) {
        Text(
            text = label,
            color = themeColor,
            fontSize = 11.sp,
            fontFamily = Fonts.ChakraPetch,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}
