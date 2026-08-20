package com.quantumos.quarkavatar.ui

import androidx.compose.animation.core.EaseOutQuad
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quantumos.appshell.Fonts
import com.quantumos.appshell.Phosphor
import com.quantumos.core.PhosphorHue
import com.quantumos.quarkavatar.DemoPosture
import com.quantumos.quarkavatar.R
import com.quantumos.quarkavatar.ui.effects.quarkAvatarEffect

/*
 * QUARK AVATAR dev-preview screen (Phase 4b) -- displays the bundled posture-library frame with the
 * shader applied, plus a control strip to see every state combination on-device. Posture/Speaking/
 * Stealth are LOCAL demo toggles only (not wired to the real QuantumStateEngine -- see
 * PRODUCTION_LOG's non-goals, blocked on a cross-module circular-dependency issue). HUE is the one
 * exception: it drives the real, already-safe-to-reach PhosphorHueRuntime.
 */
@Composable
fun QuarkAvatarScreen(
    posture: DemoPosture,
    themeHue: PhosphorHue,
    themeColor: Color,
    themeColorDim: Color,
    speakingPreview: Boolean,
    stealthPreview: Boolean,
    onCyclePosture: () -> Unit,
    onCycleHue: () -> Unit,
    onToggleSpeaking: () -> Unit,
    onToggleStealth: () -> Unit,
    contentPadding: PaddingValues
) {
    val (drawableRes, accentIsLive) = when (posture) {
        DemoPosture.NEUTRAL -> R.drawable.posture_relaxed_idle_green to true
        DemoPosture.ALERT -> R.drawable.posture_relaxed_idle_alert_red to false
        DemoPosture.THINKING -> R.drawable.posture_thinking_green to true
    }
    // Alert's bake is fixed --warn red, never live-tinted -- enforced structurally here, not by
    // convention: the shader is only ever handed the live hue when accentIsLive is true.
    val accentColor = if (accentIsLive) themeColor else Phosphor.Warn

    Box(Modifier.fillMaxSize().padding(contentPadding), contentAlignment = Alignment.Center) {
        Box(Modifier.fillMaxWidth(0.8f), contentAlignment = Alignment.Center) {
            Image(
                painter = painterResource(drawableRes),
                contentDescription = "QUARK avatar -- $posture",
                modifier = Modifier
                    .fillMaxWidth()
                    .quarkAvatarEffect(
                        accentColor = accentColor,
                        rimStrength = 0.6f,
                        stealthDim = if (stealthPreview) 0.35f else 1f
                    )
            )
            if (speakingPreview) {
                SpeakingRippleOverlay(color = accentColor)
            }
        }

        Column(
            Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp).fillMaxWidth().padding(horizontal = 16.dp)
        ) {
            DemoControlRow("POSTURE", posture.name, themeColor, themeColorDim, onCyclePosture)
            DemoControlRow("HUE", themeHue.name, themeColor, themeColorDim, onCycleHue)
            DemoControlRow("SPEAKING RIPPLE", if (speakingPreview) "ON" else "OFF", themeColor, themeColorDim, onToggleSpeaking)
            DemoControlRow("STEALTH DIM", if (stealthPreview) "ON" else "OFF", themeColor, themeColorDim, onToggleStealth)
        }
    }
}

// Terse, tap-to-cycle/toggle row -- same visual language as CONFIG's SettingCycleRow, without a
// house-icon glyph (this is a temporary dev surface, not a real settings row).
@Composable
private fun DemoControlRow(label: String, value: String, color: Color, dimColor: Color, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 6.dp)
    ) {
        Text(label.padEnd(18), color = dimColor, fontFamily = Fonts.ChakraPetch, fontSize = 12.sp)
        Text(": $value", color = color, fontFamily = Fonts.ChakraPetch, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

// Speaking's ripple-ring VFX -- a plain Compose Canvas overlay, not part of the AGSL shader (see
// QuarkAvatarShader.kt's doc comment for why). Mirrors QuarkMascot.kt's HAPPY/SCAN ring technique
// exactly (rememberInfiniteTransition, radius 1.0->2.4 / alpha 1->0, EaseOutQuad, Restart) -- reused,
// not reinvented. Only exists in composition while Speaking preview is toggled on (zero idle redraw
// when off).
@Composable
private fun SpeakingRippleOverlay(color: Color) {
    val transition = rememberInfiniteTransition(label = "quark_avatar_speaking")
    val ringR by transition.animateFloat(
        initialValue = 1.0f, targetValue = 2.4f,
        animationSpec = infiniteRepeatable(tween(1000, easing = EaseOutQuad), RepeatMode.Restart),
        label = "ringR"
    )
    val ringA by transition.animateFloat(
        initialValue = 1.0f, targetValue = 0.0f,
        animationSpec = infiniteRepeatable(tween(1000, easing = EaseOutQuad), RepeatMode.Restart),
        label = "ringA"
    )
    Canvas(Modifier.fillMaxSize()) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val baseRadius = size.minDimension * 0.28f
        drawCircle(
            color = color.copy(alpha = ringA * 0.35f),
            radius = baseRadius * ringR,
            center = center,
            style = Stroke(width = 2.dp.toPx())
        )
    }
}
