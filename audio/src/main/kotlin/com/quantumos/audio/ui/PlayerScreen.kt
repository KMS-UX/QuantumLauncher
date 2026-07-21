package com.quantumos.audio.ui

import androidx.compose.animation.core.EaseInOutQuad
import androidx.compose.animation.core.EaseOutQuad
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quantumos.appshell.Fonts
import com.quantumos.appshell.Glyph
import com.quantumos.appshell.Phosphor
import com.quantumos.appshell.PleaseStandbyCard
import com.quantumos.appshell.QuantumIcon
import com.quantumos.audio.AudioViewModel

/*
 * PLAYER -- AUDIO's secondary screen (field recorder first, per the module identity; player second).
 * Ported from the standalone repo's PlayerScreen.kt. Fix-pass point 5: the turntable's vinyl-spin
 * `rememberInfiniteTransition` now only exists while `isPlaying` is true -- previously the transition
 * object ran continuously in the background even though the rendered angle was already correctly
 * frozen to 0f when idle. Palette: no material grays for the vinyl chrome -- everything is drawn in
 * the active phosphor + its dim pair on Phosphor.Crt, matching the rest of the docked modules.
 */
@Composable
fun PlayerScreen(
    viewModel: AudioViewModel,
    themeColor: Color,
    dimColor: Color
) {
    val trackListState by viewModel.engine.trackList.collectAsState()
    val currentTrackState by viewModel.engine.currentTrack.collectAsState()
    val isPlayingState by viewModel.engine.isPlaying.collectAsState()
    val isPreparingState by viewModel.engine.isPreparingPlayback.collectAsState()
    val progressState by viewModel.engine.playbackProgress.collectAsState()
    val timeState by viewModel.engine.playbackTime.collectAsState()
    val durationState by viewModel.engine.playbackDuration.collectAsState()
    val spectrumState by viewModel.engine.playbackSpectrum.collectAsState()

    var viewMode by remember { mutableStateOf("SPECTRUM") }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp)
        ) {
            // --- SPECTRUM / TURNTABLE VISUALIZER CRT SCREEN ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .border(width = 1.dp, color = themeColor, shape = RoundedCornerShape(4.dp))
                    .background(Phosphor.Crt)
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "DECK: " + if (isPlayingState) "▲ PLAYING" else "■ STANDBY",
                            color = themeColor,
                            fontSize = 10.sp,
                            fontFamily = Fonts.ChakraPetch,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            modifier = Modifier
                                .clickable { viewMode = if (viewMode == "SPECTRUM") "TURNTABLE" else "SPECTRUM" }
                                .background(themeColor.copy(alpha = 0.08f))
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "MODE: $viewMode ⇅",
                                color = themeColor,
                                fontSize = 9.sp,
                                fontFamily = Fonts.ChakraPetch,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (viewMode == "SPECTRUM") {
                        Row(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            spectrumState.forEach { barHeight ->
                                Column(
                                    modifier = Modifier.weight(1f).fillMaxHeight().padding(horizontal = 2.dp),
                                    verticalArrangement = Arrangement.Bottom
                                ) {
                                    val segments = 8
                                    for (s in 0 until segments) {
                                        val isActive = (segments - s) <= (barHeight * segments)
                                        val segmentColor = if (isActive) themeColor else themeColor.copy(alpha = 0.08f)
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .weight(1f)
                                                .padding(vertical = 1.dp)
                                                .background(segmentColor)
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                            TurntableVisualizer(themeColor = themeColor, dimColor = dimColor, isPlaying = isPlayingState)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // --- MECHANICAL TIMELINE PROGRESS SLIDER ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = timeState, color = themeColor, fontSize = 11.sp, fontFamily = Fonts.ChakraPetch, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.weight(1f).padding(horizontal = 10.dp).height(6.dp)
                ) {
                    val progressSegments = 30
                    for (i in 0 until progressSegments) {
                        val active = i < (progressState * progressSegments)
                        val barColor = if (active) themeColor else themeColor.copy(alpha = 0.15f)
                        Box(
                            modifier = Modifier.weight(1f).fillMaxHeight().padding(horizontal = 0.5.dp).background(barColor)
                        )
                    }
                }
                Text(text = durationState, color = themeColor, fontSize = 11.sp, fontFamily = Fonts.ChakraPetch, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(14.dp))

            // --- ACTIVE TAPE HEADER DETAIL ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(width = 1.dp, color = themeColor.copy(alpha = 0.4f), shape = RoundedCornerShape(4.dp))
                    .background(themeColor.copy(alpha = 0.04f))
                    .padding(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "CURRENT REEL LOG:",
                            color = themeColor.copy(alpha = 0.6f),
                            fontSize = 9.sp,
                            fontFamily = Fonts.ChakraPetch
                        )
                        Text(
                            text = currentTrackState?.title ?: "NO TRACK LOADED",
                            color = themeColor,
                            fontSize = 14.sp,
                            fontFamily = Fonts.ChakraPetch,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = { viewModel.togglePlayback() },
                            modifier = Modifier
                                .size(44.dp)
                                .border(width = 1.dp, color = themeColor, shape = RoundedCornerShape(50))
                                .background(Phosphor.Crt)
                        ) {
                            QuantumIcon(
                                glyph = if (isPlayingState) Glyph.Pause else Glyph.Play,
                                tint = themeColor,
                                size = 18.dp
                            )
                        }
                        IconButton(
                            onClick = { viewModel.stopPlayback() },
                            modifier = Modifier
                                .size(44.dp)
                                .border(width = 1.dp, color = themeColor, shape = RoundedCornerShape(50))
                                .background(Phosphor.Crt)
                        ) {
                            QuantumIcon(Glyph.Stop, tint = themeColor, size = 16.dp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SECURE AUDIO REGISTRY",
                    color = themeColor,
                    fontSize = 11.sp,
                    fontFamily = Fonts.ChakraPetch,
                    fontWeight = FontWeight.Bold
                )
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .border(width = 1.dp, color = themeColor.copy(alpha = 0.3f), shape = RoundedCornerShape(4.dp))
                    .background(Phosphor.Crt)
                    .padding(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(trackListState) { track ->
                    val isSelected = currentTrackState?.file?.absolutePath == track.file.absolutePath
                    val itemBg = if (isSelected) themeColor.copy(alpha = 0.12f) else Color.Transparent

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(itemBg)
                            .clickable { viewModel.playTrack(track) }
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                            QuantumIcon(
                                glyph = Glyph.Play,
                                tint = if (isSelected) themeColor else dimColor,
                                size = 14.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = track.title,
                                    color = if (isSelected) themeColor else themeColor.copy(alpha = 0.8f),
                                    fontSize = 12.sp,
                                    fontFamily = Fonts.ChakraPetch,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = when {
                                        track.isSynthetic -> "SYNTHETIC CORE TRANS"
                                        track.file.absolutePath.contains("/imported/") -> "IMPORTED FILE TRANS"
                                        else -> "MIC FIELD RECORD"
                                    },
                                    color = if (isSelected) themeColor.copy(alpha = 0.7f) else themeColor.copy(alpha = 0.4f),
                                    fontSize = 8.sp,
                                    fontFamily = Fonts.ChakraPetch
                                )
                            }
                        }
                        Text(
                            text = track.durationText,
                            color = if (isSelected) themeColor else themeColor.copy(alpha = 0.6f),
                            fontSize = 11.sp,
                            fontFamily = Fonts.ChakraPetch,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                    HorizontalDivider(color = themeColor.copy(alpha = 0.08f))
                }
            }
        }

        // Real async wait: MediaPlayer.prepare() on Dispatchers.IO before playback starts.
        if (isPreparingState) {
            Box(
                modifier = Modifier.fillMaxSize().background(Phosphor.Crt.copy(alpha = 0.92f)),
                contentAlignment = Alignment.Center
            ) {
                PleaseStandbyCard(
                    subline = "PREPARING PLAYBACK DECK",
                    color = themeColor,
                    dimColor = dimColor,
                    font = Fonts.ChakraPetch
                )
            }
        }
    }
}

@Composable
fun TurntableVisualizer(
    themeColor: Color,
    dimColor: Color,
    isPlaying: Boolean
) {
    // Vinyl-spin fix-pass point 5: the infinite transition object itself only exists while playing --
    // the rendered angle isn't just clamped to 0f, the ticking animation is never created when idle.
    val currentAngle: Float
    if (isPlaying) {
        val vinylTransition = rememberInfiniteTransition(label = "vinyl")
        val angle by vinylTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(animation = tween(2500, easing = LinearEasing), repeatMode = RepeatMode.Restart),
            label = "vinyl_rotation"
        )
        currentAngle = angle
    } else {
        currentAngle = 0f
    }

    // A single-shot settle, not an infinite loop -- fine to run unconditionally (it reaches its
    // target and stops).
    val toneArmAngle by animateFloatAsState(
        targetValue = if (isPlaying) 22f else 0f,
        animationSpec = tween(1000, easing = EaseInOutQuad),
        label = "tonearm"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val centerPlatterX = width * 0.42f
        val centerPlatterY = height * 0.5f
        val platterRadius = height * 0.44f

        drawCircle(color = Phosphor.Crt, radius = platterRadius + 4f, center = Offset(centerPlatterX, centerPlatterY))
        drawCircle(
            color = themeColor.copy(alpha = 0.3f),
            radius = platterRadius + 4f,
            center = Offset(centerPlatterX, centerPlatterY),
            style = Stroke(width = 2f)
        )

        rotate(degrees = currentAngle, pivot = Offset(centerPlatterX, centerPlatterY)) {
            drawCircle(color = Phosphor.Crt, radius = platterRadius, center = Offset(centerPlatterX, centerPlatterY))

            val grooveCount = 8
            for (i in 1..grooveCount) {
                drawCircle(
                    color = themeColor.copy(alpha = 0.12f),
                    radius = platterRadius * (0.3f + 0.6f * (i.toFloat() / grooveCount)),
                    center = Offset(centerPlatterX, centerPlatterY),
                    style = Stroke(width = 1f)
                )
            }

            drawLine(
                color = themeColor.copy(alpha = 0.06f),
                start = Offset(centerPlatterX - platterRadius, centerPlatterY - platterRadius),
                end = Offset(centerPlatterX + platterRadius, centerPlatterY + platterRadius),
                strokeWidth = 10f
            )
            drawLine(
                color = themeColor.copy(alpha = 0.06f),
                start = Offset(centerPlatterX - platterRadius, centerPlatterY + platterRadius),
                end = Offset(centerPlatterX + platterRadius, centerPlatterY - platterRadius),
                strokeWidth = 10f
            )

            drawCircle(color = themeColor.copy(alpha = 0.8f), radius = platterRadius * 0.28f, center = Offset(centerPlatterX, centerPlatterY))
            drawCircle(
                color = Phosphor.Crt,
                radius = platterRadius * 0.24f,
                center = Offset(centerPlatterX, centerPlatterY),
                style = Stroke(width = 1f)
            )
            drawCircle(
                color = dimColor.copy(alpha = 0.5f),
                radius = platterRadius * 0.15f,
                center = Offset(centerPlatterX, centerPlatterY),
                style = Stroke(width = 2f)
            )
        }

        drawCircle(color = themeColor, radius = 6f, center = Offset(centerPlatterX, centerPlatterY))
        drawCircle(color = Phosphor.Crt, radius = 2f, center = Offset(centerPlatterX, centerPlatterY))

        val controlX = width * 0.88f
        val sliderYStart = height * 0.25f
        val sliderYEnd = height * 0.75f
        drawLine(
            color = themeColor.copy(alpha = 0.3f),
            start = Offset(controlX, sliderYStart),
            end = Offset(controlX, sliderYEnd),
            strokeWidth = 3f
        )
        val knobY = if (isPlaying) sliderYStart + (sliderYEnd - sliderYStart) * 0.6f else sliderYStart + (sliderYEnd - sliderYStart) * 0.3f
        drawRect(color = themeColor, topLeft = Offset(controlX - 8f, knobY - 5f), size = Size(16f, 10f))

        val pivotX = width * 0.76f
        val pivotY = height * 0.22f
        drawCircle(color = Phosphor.Crt, radius = 16f, center = Offset(pivotX, pivotY))
        drawCircle(color = themeColor.copy(alpha = 0.5f), radius = 16f, center = Offset(pivotX, pivotY), style = Stroke(width = 1.5f))
        drawCircle(color = Phosphor.Crt, radius = 8f, center = Offset(pivotX, pivotY))

        rotate(degrees = toneArmAngle, pivot = Offset(pivotX, pivotY)) {
            val shaftEndX = pivotX - 45f
            val shaftEndY = pivotY + 70f
            drawLine(color = themeColor.copy(alpha = 0.8f), start = Offset(pivotX, pivotY), end = Offset(shaftEndX, shaftEndY), strokeWidth = 3.5f)
            drawLine(color = themeColor.copy(alpha = 0.4f), start = Offset(pivotX, pivotY), end = Offset(shaftEndX, shaftEndY), strokeWidth = 1f)

            val headshellEndX = shaftEndX - 18f
            val headshellEndY = shaftEndY + 18f
            drawLine(color = dimColor, start = Offset(shaftEndX, shaftEndY), end = Offset(headshellEndX, headshellEndY), strokeWidth = 5f)

            // Needle tip -- kept in-palette (bright phosphor), not the alert-red the source app used
            // decoratively here: --warn is alerts/access-denied ONLY, never decorative (House Style).
            drawRect(color = themeColor, topLeft = Offset(headshellEndX - 3f, headshellEndY - 3f), size = Size(6f, 6f))
        }
    }
}
