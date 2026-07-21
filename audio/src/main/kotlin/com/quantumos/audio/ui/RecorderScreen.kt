package com.quantumos.audio.ui

import android.Manifest
import androidx.compose.animation.core.EaseInOutQuad
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import com.quantumos.appshell.Fonts
import com.quantumos.appshell.Glyph
import com.quantumos.appshell.Phosphor
import com.quantumos.appshell.PleaseStandbyCard
import com.quantumos.appshell.QuantumIcon
import com.quantumos.audio.AudioViewModel

/*
 * RECORDER -- AUDIO's default/primary screen (matches the source app's already-correct
 * ActiveChannel.RECORDER default). Ported from the standalone repo's RecorderScreen.kt with the
 * Core Apps Fix-Pass's #1 required fix applied: the fake idle-drift oscilloscope loop
 * (`while (!isRecording) { ...sine wave... delay(80) }`) is deleted outright. The waveform is now
 * recording-data-driven ONLY -- see the single LaunchedEffect below.
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun RecorderScreen(
    viewModel: AudioViewModel,
    themeColor: Color,
    dimColor: Color
) {
    val isRecordingState by viewModel.engine.isRecording.collectAsState()
    val isPreparingState by viewModel.engine.isPreparingRecorder.collectAsState()
    val recordingTimeState by viewModel.engine.recordingTime.collectAsState()
    val trackListState by viewModel.engine.trackList.collectAsState()

    val recordPermission = rememberPermissionState(Manifest.permission.RECORD_AUDIO) { granted ->
        if (granted) viewModel.startRecording() else viewModel.recordDenied()
    }

    // Oscilloscope trace memory -- the ONLY writer of this list is the effect below, and it is
    // gated strictly on isRecordingState. No idle branch exists: when recording stops, the effect's
    // key flips, the collecting coroutine is cancelled, and the trace freezes exactly where it is.
    val oscilloscopePoints = remember { mutableStateListOf<Float>() }

    LaunchedEffect(isRecordingState) {
        if (isRecordingState) {
            oscilloscopePoints.clear()
            viewModel.engine.liveAmplitude.collect { amp ->
                oscilloscopePoints.add(amp)
                if (oscilloscopePoints.size > 80) oscilloscopePoints.removeAt(0)
            }
        }
        // else: do nothing. No resting sine-wave, no idle timer -- zero idle redraw.
    }

    // REC indicator pulse -- only ticks while actually recording (gated the same way point 5's
    // vinyl-spin fix requires for the Player screen's turntable: no animation object exists when
    // there is nothing to animate).
    val pulseAlpha: Float
    if (isRecordingState) {
        val pulseTransition = rememberInfiniteTransition(label = "rec_pulse")
        val alpha by pulseTransition.animateFloat(
            initialValue = 0.3f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(600, easing = EaseInOutQuad),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulse"
        )
        pulseAlpha = alpha
    } else {
        pulseAlpha = 1f
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp)
        ) {
            // --- OSCILLOSCOPE CRT REC SCREEN ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .border(width = 1.dp, color = themeColor, shape = RoundedCornerShape(4.dp))
                    .background(Phosphor.Crt)
                    .padding(6.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val width = size.width
                    val height = size.height
                    val midY = height / 2f

                    val gridAlpha = 0.12f
                    drawLine(
                        color = themeColor.copy(alpha = gridAlpha),
                        start = Offset(0f, midY),
                        end = Offset(width, midY),
                        strokeWidth = 1.dp.toPx()
                    )
                    val gridCols = 8
                    val colWidth = width / gridCols
                    for (i in 1 until gridCols) {
                        drawLine(
                            color = themeColor.copy(alpha = gridAlpha),
                            start = Offset(i * colWidth, 0f),
                            end = Offset(i * colWidth, height),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    if (oscilloscopePoints.isNotEmpty()) {
                        val dx = width / 80f
                        val path = Path()
                        val mirrorPath = Path()
                        oscilloscopePoints.forEachIndexed { i, amp ->
                            val x = i * dx
                            val y = midY - (amp * (height * 0.45f))
                            val yMirror = midY + (amp * (height * 0.45f))
                            if (i == 0) {
                                path.moveTo(x, y)
                                mirrorPath.moveTo(x, yMirror)
                            } else {
                                path.lineTo(x, y)
                                mirrorPath.lineTo(x, yMirror)
                            }
                        }
                        drawPath(path = path, color = themeColor, style = Stroke(width = 2.dp.toPx()))
                        drawPath(
                            path = mirrorPath,
                            color = themeColor.copy(alpha = 0.4f),
                            style = Stroke(width = 1.dp.toPx())
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (isRecordingState) "▲ REC ACTIVE" else "■ STANDBY",
                        color = if (isRecordingState) Phosphor.Warn.copy(alpha = pulseAlpha) else themeColor,
                        fontSize = 10.sp,
                        fontFamily = Fonts.ChakraPetch,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "BUFFER: $recordingTimeState",
                        color = themeColor,
                        fontSize = 10.sp,
                        fontFamily = Fonts.ChakraPetch,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!isRecordingState) {
                    Button(
                        onClick = {
                            if (recordPermission.status.isGranted) {
                                viewModel.startRecording()
                            } else {
                                recordPermission.launchPermissionRequest()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Phosphor.Warn.copy(alpha = 0.15f)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .size(width = 150.dp, height = 56.dp)
                            .border(width = 2.dp, color = Phosphor.Warn, shape = RoundedCornerShape(8.dp))
                    ) {
                        QuantumIcon(Glyph.Record, tint = Phosphor.Warn, size = 16.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "RECORD",
                            color = Phosphor.Warn,
                            fontSize = 14.sp,
                            fontFamily = Fonts.ChakraPetch,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Button(
                        onClick = { viewModel.stopRecording() },
                        colors = ButtonDefaults.buttonColors(containerColor = themeColor.copy(alpha = 0.15f)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .size(width = 150.dp, height = 56.dp)
                            .border(width = 2.dp, color = themeColor, shape = RoundedCornerShape(8.dp))
                    ) {
                        QuantumIcon(Glyph.Stop, tint = themeColor, size = 16.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "STOP",
                            color = themeColor,
                            fontSize = 14.sp,
                            fontFamily = Fonts.ChakraPetch,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "FIELD CAPTURE BUFFER HISTORY",
                color = themeColor,
                fontSize = 11.sp,
                fontFamily = Fonts.ChakraPetch,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .border(width = 1.dp, color = themeColor.copy(alpha = 0.3f), shape = RoundedCornerShape(4.dp))
                    .background(Phosphor.Crt)
                    .padding(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val recordings = trackListState.filter { !it.isSynthetic }
                if (recordings.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                QuantumIcon(Glyph.Mic, tint = themeColor.copy(alpha = 0.3f), size = 36.dp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "CAPTURE BUFFER EMPTY",
                                    color = themeColor.copy(alpha = 0.4f),
                                    fontSize = 11.sp,
                                    fontFamily = Fonts.ChakraPetch
                                )
                                Text(
                                    text = "No logged transmissions recorded yet.",
                                    color = themeColor.copy(alpha = 0.3f),
                                    fontSize = 9.sp,
                                    fontFamily = Fonts.ChakraPetch,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                    }
                } else {
                    items(recordings) { fileInfo ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                QuantumIcon(Glyph.Mic, tint = themeColor, size = 16.dp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = fileInfo.title,
                                        color = themeColor,
                                        fontSize = 12.sp,
                                        fontFamily = Fonts.ChakraPetch,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "SECURE LOG CH-MIC",
                                        color = themeColor.copy(alpha = 0.5f),
                                        fontSize = 9.sp,
                                        fontFamily = Fonts.ChakraPetch
                                    )
                                }
                            }
                            Text(
                                text = fileInfo.durationText,
                                color = themeColor,
                                fontSize = 11.sp,
                                fontFamily = Fonts.ChakraPetch,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        HorizontalDivider(color = themeColor.copy(alpha = 0.1f))
                    }
                }
            }
        }

        // Real async wait: mic hardware allocation (MediaRecorder.prepare()+start() on Dispatchers.IO).
        if (isPreparingState) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Phosphor.Crt.copy(alpha = 0.92f)),
                contentAlignment = Alignment.Center
            ) {
                PleaseStandbyCard(
                    subline = "ALLOCATING MIC HARDWARE",
                    color = themeColor,
                    dimColor = dimColor,
                    font = Fonts.ChakraPetch
                )
            }
        }
    }
}
