package com.quantumos.shell.overlay.quark

import androidx.compose.animation.core.EaseInOutQuad
import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import com.quantumos.appshell.Phosphor
import com.quantumos.appshell.crtShader
import com.quantumos.core.QuantumLauncherState
import com.quantumos.core.QuarkReflexPosture

/*
 * QuarkHologramProvider — the Phase 1 (AI Studio) placeholder hologram, ported and rewired against
 * the real state/token surfaces per the Phase 2b Extract & Rewire brief:
 *  - Reads real Phosphor.bright/dim/Warn (no invented hex, no WarnDim — Warn stands in for its own
 *    "dim" pairing rather than deriving a new red constant, per the original Integration Spec).
 *  - CRT texture comes from the shared crtShader() AGSL RenderEffect (with its own cheap fallback),
 *    not a CPU draw-loop — same mechanism every other QuantumOS surface uses.
 *  - Doesn't read Stealth at all: Stealth's screen/SFX/voice reactions already live at their real,
 *    independent call sites (QuarkAssistantActivity's window dim, SoundEngine, QuantumRuntime's
 *    voice observer). This renderer only reacts to [isAnimating] — when Stealth silences voice,
 *    isSpeaking is simply false and animation stops on its own; no fourth Stealth behaviour here.
 *  - [isSpeaking] is a coarse boolean (see QuarkVisualProvider) — the "mouth" is a fixed pulse tied
 *    to the same discrete glow tween used elsewhere, not a fabricated amplitude/viseme stream.
 */
class QuarkHologramProvider : QuarkVisualProvider {

    @Composable
    override fun RenderPresence(
        state: QuantumLauncherState,
        isSpeaking: Boolean,
        modifier: Modifier
    ) {
        val posture = state.quarkBrain.activePosture
        val hue = state.environment.activeHue

        // Dynamic phosphor tint — re-derived every recomposition, never cached. Warn stands in for
        // both the bright and "dim" role in its own state; no second red constant.
        val phosphorColor = if (posture == QuarkReflexPosture.WARN) Phosphor.Warn else Phosphor.bright(hue)

        val baseAlpha = 0.45f
        val rimAlpha = 0.80f

        // Gated animation: zero-idle-redraw at rest, matching the existing QuarkPresence discipline.
        val isAnimating = posture != QuarkReflexPosture.IDLE || isSpeaking

        var scanLineY by remember { mutableFloatStateOf(0f) }
        var glowPulse by remember { mutableFloatStateOf(1f) }

        if (isAnimating) {
            val infiniteTransition = rememberInfiniteTransition(label = "HoloAnimations")

            if (posture == QuarkReflexPosture.SCAN) {
                val sweep by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(2500, easing = EaseInOutSine),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "ScanSweep"
                )
                scanLineY = sweep
            } else {
                scanLineY = 0f
            }

            // Same discrete pulse serves HAPPY's glow beat and the coarse "speaking" mouth pulse —
            // one honest tween, not two competing animations.
            if (posture == QuarkReflexPosture.HAPPY || isSpeaking) {
                val glow by infiniteTransition.animateFloat(
                    initialValue = 0.85f,
                    targetValue = 1.15f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1200, easing = EaseInOutQuad),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "GlowPulse"
                )
                glowPulse = glow
            } else {
                glowPulse = 1f
            }
        } else {
            scanLineY = 0f
            glowPulse = 1f
        }

        Canvas(modifier = modifier.crtShader()) {
            val w = size.width
            val h = size.height
            val cx = w / 2f
            val cy = h / 2f

            val scale = (w.coerceAtMost(h) / 450f).coerceIn(0.5f, 2.5f)

            // Humanoid-bust silhouette: head dome, neck, chest — the Phase 1 visual bar (translucency,
            // rim-glow, soft edge-fade) cleared per the Integration Spec §2a.
            val headPath = Path().apply {
                moveTo(cx - 70f * scale, cy - 100f * scale)
                cubicTo(
                    cx - 70f * scale, cy - 160f * scale,
                    cx + 70f * scale, cy - 160f * scale,
                    cx + 70f * scale, cy - 100f * scale
                )
                lineTo(cx + 60f * scale, cy + 20f * scale)
                cubicTo(
                    cx + 50f * scale, cy + 90f * scale,
                    cx - 50f * scale, cy + 90f * scale,
                    cx - 60f * scale, cy + 20f * scale
                )
                close()
            }
            val neckPath = Path().apply {
                moveTo(cx - 30f * scale, cy + 70f * scale)
                lineTo(cx - 40f * scale, cy + 150f * scale)
                lineTo(cx + 40f * scale, cy + 150f * scale)
                lineTo(cx + 30f * scale, cy + 70f * scale)
                close()
            }
            val chestPath = Path().apply {
                moveTo(cx - 40f * scale, cy + 150f * scale)
                cubicTo(
                    cx - 100f * scale, cy + 180f * scale,
                    cx - 120f * scale, cy + 240f * scale,
                    cx - 120f * scale, cy + 250f * scale
                )
                lineTo(cx + 120f * scale, cy + 250f * scale)
                cubicTo(
                    cx + 120f * scale, cy + 240f * scale,
                    cx + 100f * scale, cy + 180f * scale,
                    cx + 40f * scale, cy + 150f * scale
                )
                close()
            }

            // Base projector platform / anchor rings.
            val anchorY = cy + 240f * scale
            val anchorRadius = 130f * scale
            drawOval(
                color = phosphorColor.copy(alpha = baseAlpha * 0.4f),
                topLeft = Offset(cx - anchorRadius, anchorY - 15f * scale),
                size = Size(anchorRadius * 2, 30f * scale),
                style = Stroke(width = 2f)
            )
            drawOval(
                color = phosphorColor.copy(alpha = baseAlpha * 0.15f),
                topLeft = Offset(cx - anchorRadius * 1.3f, anchorY - 22f * scale),
                size = Size(anchorRadius * 2.6f, 44f * scale),
                style = Stroke(width = 1f)
            )

            // Core luminance base beams.
            val beamBrush = Brush.verticalGradient(
                colors = listOf(phosphorColor.copy(alpha = baseAlpha * 0.5f), Color.Transparent),
                startY = anchorY,
                endY = cy - 80f * scale
            )
            drawRect(
                brush = beamBrush,
                topLeft = Offset(cx - 45f * scale, cy - 80f * scale),
                size = Size(90f * scale, anchorY - (cy - 80f * scale)),
                blendMode = BlendMode.Screen
            )

            // Holographic translucency fill.
            val fillBrush = Brush.verticalGradient(
                colors = listOf(phosphorColor.copy(alpha = baseAlpha * 1.1f), phosphorColor.copy(alpha = baseAlpha * 0.1f)),
                startY = cy - 140f * scale,
                endY = cy + 220f * scale
            )
            drawPath(path = chestPath, brush = fillBrush)
            drawPath(path = neckPath, brush = fillBrush)
            drawPath(path = headPath, brush = fillBrush)

            // Rim-glow overlay — decaying strokes, the "edge-fade" the visual bar asks for.
            val strokes = listOf(6f to 0.2f, 3f to 0.5f, 1f to 1.0f)
            strokes.forEach { (width, alphaMultiplier) ->
                val finalAlpha = rimAlpha * alphaMultiplier * glowPulse
                drawPath(path = chestPath, color = phosphorColor.copy(alpha = finalAlpha), style = Stroke(width = width * scale))
                drawPath(path = neckPath, color = phosphorColor.copy(alpha = finalAlpha), style = Stroke(width = width * scale))
                drawPath(path = headPath, color = phosphorColor.copy(alpha = finalAlpha), style = Stroke(width = width * scale))
            }

            drawLine(
                color = phosphorColor.copy(alpha = rimAlpha * 0.6f),
                start = Offset(cx, cy + 70f * scale),
                end = Offset(cx, cy + 150f * scale),
                strokeWidth = 3f * scale
            )

            val earRadius = 18f * scale
            drawCircle(
                color = phosphorColor.copy(alpha = rimAlpha),
                radius = earRadius,
                center = Offset(cx - 68f * scale, cy - 80f * scale),
                style = Stroke(width = 2f * scale)
            )
            drawCircle(
                color = phosphorColor.copy(alpha = rimAlpha),
                radius = earRadius,
                center = Offset(cx + 68f * scale, cy - 80f * scale),
                style = Stroke(width = 2f * scale)
            )

            val crownY = cy - 110f * scale
            drawLine(
                color = phosphorColor.copy(alpha = rimAlpha),
                start = Offset(cx - 68f * scale, crownY),
                end = Offset(cx + 68f * scale, crownY),
                strokeWidth = 4f * scale
            )
            drawCircle(
                color = phosphorColor.copy(alpha = rimAlpha * glowPulse),
                radius = 6f * scale,
                center = Offset(cx, crownY)
            )

            val emblemY = cy + 190f * scale
            drawLine(
                color = phosphorColor.copy(alpha = rimAlpha * 0.4f),
                start = Offset(cx - 30f * scale, emblemY),
                end = Offset(cx + 30f * scale, emblemY),
                strokeWidth = 2f * scale
            )
            drawCircle(
                color = phosphorColor.copy(alpha = rimAlpha),
                radius = 4f * scale,
                center = Offset(cx, emblemY),
                style = Stroke(width = 1.5f * scale)
            )

            // Face, per reactive posture — the four locked states, same semantics as QuarkPresence.
            when (posture) {
                QuarkReflexPosture.IDLE -> {
                    drawLine(
                        color = phosphorColor.copy(alpha = rimAlpha * 0.7f),
                        start = Offset(cx - 32f * scale, cy - 30f * scale),
                        end = Offset(cx - 12f * scale, cy - 30f * scale),
                        strokeWidth = 2f * scale
                    )
                    drawLine(
                        color = phosphorColor.copy(alpha = rimAlpha * 0.7f),
                        start = Offset(cx + 12f * scale, cy - 30f * scale),
                        end = Offset(cx + 32f * scale, cy - 30f * scale),
                        strokeWidth = 2f * scale
                    )
                }
                QuarkReflexPosture.SCAN -> {
                    drawCircle(
                        color = phosphorColor.copy(alpha = rimAlpha),
                        radius = 8f * scale,
                        center = Offset(cx - 22f * scale, cy - 30f * scale),
                        style = Stroke(width = 1.5f * scale)
                    )
                    drawCircle(
                        color = phosphorColor.copy(alpha = rimAlpha),
                        radius = 8f * scale,
                        center = Offset(cx + 22f * scale, cy - 30f * scale),
                        style = Stroke(width = 1.5f * scale)
                    )
                    drawLine(
                        color = phosphorColor.copy(alpha = rimAlpha * 0.5f),
                        start = Offset(cx - 45f * scale, cy - 30f * scale),
                        end = Offset(cx + 45f * scale, cy - 30f * scale),
                        strokeWidth = 1f * scale
                    )
                }
                QuarkReflexPosture.HAPPY -> {
                    val eyeRadius = 10f * scale
                    drawArc(
                        color = phosphorColor.copy(alpha = rimAlpha),
                        startAngle = 200f, sweepAngle = 140f, useCenter = false,
                        topLeft = Offset(cx - 32f * scale, cy - 38f * scale),
                        size = Size(eyeRadius * 2, eyeRadius * 1.5f),
                        style = Stroke(width = 2.5f * scale)
                    )
                    drawArc(
                        color = phosphorColor.copy(alpha = rimAlpha),
                        startAngle = 200f, sweepAngle = 140f, useCenter = false,
                        topLeft = Offset(cx + 12f * scale, cy - 38f * scale),
                        size = Size(eyeRadius * 2, eyeRadius * 1.5f),
                        style = Stroke(width = 2.5f * scale)
                    )
                }
                QuarkReflexPosture.WARN -> {
                    drawLine(
                        color = phosphorColor.copy(alpha = rimAlpha),
                        start = Offset(cx - 30f * scale, cy - 34f * scale),
                        end = Offset(cx - 14f * scale, cy - 24f * scale),
                        strokeWidth = 3f * scale
                    )
                    drawLine(
                        color = phosphorColor.copy(alpha = rimAlpha),
                        start = Offset(cx + 30f * scale, cy - 34f * scale),
                        end = Offset(cx + 14f * scale, cy - 24f * scale),
                        strokeWidth = 3f * scale
                    )
                }
            }

            // Mouth: a coarse "vocalizing" pulse while isSpeaking (driven by glowPulse, not a
            // fabricated amplitude stream), otherwise a static contour per posture.
            val mouthY = cy + 15f * scale
            if (isSpeaking) {
                val mouthHalfWidth = 14f * scale * glowPulse
                drawLine(
                    color = phosphorColor,
                    start = Offset(cx - mouthHalfWidth, mouthY),
                    end = Offset(cx + mouthHalfWidth, mouthY),
                    strokeWidth = 3f * scale
                )
            } else if (posture == QuarkReflexPosture.HAPPY) {
                drawArc(
                    color = phosphorColor.copy(alpha = rimAlpha),
                    startAngle = 20f, sweepAngle = 140f, useCenter = false,
                    topLeft = Offset(cx - 15f * scale, mouthY - 5f * scale),
                    size = Size(30f * scale, 15f * scale),
                    style = Stroke(width = 2f * scale)
                )
            } else {
                drawLine(
                    color = phosphorColor.copy(alpha = rimAlpha * 0.6f),
                    start = Offset(cx - 12f * scale, mouthY),
                    end = Offset(cx + 12f * scale, mouthY),
                    strokeWidth = 2f * scale
                )
            }

            // Active scanning laser sweep — a functional per-state indicator, not decorative CRT
            // texture (that comes from crtShader() on the outer modifier).
            if (posture == QuarkReflexPosture.SCAN) {
                val laserY = cy - 140f * scale + (280f * scale * scanLineY)
                drawLine(
                    color = phosphorColor.copy(alpha = rimAlpha * 0.9f),
                    start = Offset(cx - 100f * scale, laserY),
                    end = Offset(cx + 100f * scale, laserY),
                    strokeWidth = 2.5f * scale
                )
                drawLine(
                    color = phosphorColor.copy(alpha = rimAlpha * 0.25f),
                    start = Offset(cx - 120f * scale, laserY),
                    end = Offset(cx + 120f * scale, laserY),
                    strokeWidth = 7f * scale
                )
            }
        }
    }
}
