package com.quantumos.quarkavatar.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
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
import com.quantumos.quarkavatar.R
import com.quantumos.quarkavatar.QuarkState
import com.quantumos.quarkavatar.RenderMode
import com.quantumos.quarkavatar.ui.scene.QuarkHologramOverlay
import com.quantumos.quarkavatar.ui.scene.QuarkPlateView

/*
 * QUARK AVATAR dev-preview screen -- the native-art presentation plus a control strip to see every
 * combination on device.
 *
 * STATE / SPEAKING / STEALTH are REAL as of B4 (Phase 21). They read the one live
 * QuantumStateEngine through `QuantumStateRuntime`, the same shared seam `PhosphorHueRuntime` uses
 * for the hue, and STATE / STEALTH drive it back -- a posture raised here changes the posture for the
 * whole OS and lands in the LOG channel. SPEAKING is a readout only: QUARK speaks because she has
 * something to say, and faking it here would put a second source of truth back on the screen.
 *
 * What remains local is presentation: RENDER, FRAMING, AMBIENT, MATERIALISE, PHOSPHOR TINT. Those
 * are how the avatar is DRAWN, not what the unit is doing, and they belong to this surface.
 */
@Composable
fun QuarkAvatarScreen(
    themeHue: PhosphorHue,
    themeColor: Color,
    themeColorDim: Color,
    speakingPreview: Boolean,
    stealthPreview: Boolean,
    renderMode: RenderMode,
    phosphorBlend: Float,
    state: QuarkState,
    ambient: Boolean,
    framingScale: Float,
    replayKey: Int,
    onCycleHue: () -> Unit,
    onToggleSpeaking: () -> Unit,
    onToggleStealth: () -> Unit,
    onCycleRenderMode: () -> Unit,
    onCyclePhosphorBlend: () -> Unit,
    onCycleState: () -> Unit,
    onToggleAmbient: () -> Unit,
    onCycleFraming: () -> Unit,
    onReplayMaterialise: () -> Unit,
    contentPadding: PaddingValues
) {
    Box(Modifier.fillMaxSize().padding(contentPadding), contentAlignment = Alignment.Center) {
        // The presentation itself now lives in QuarkProjection, because `:app`'s Assistant View
        // shows the SAME thing (W2). This screen is what it always was -- that presentation plus a
        // control strip to see every combination on device -- but it no longer owns a second copy
        // of the composition that could drift from the one the OS actually shows.
        QuarkProjection(
            modifier = Modifier.fillMaxSize(),
            renderMode = renderMode,
            framingScale = framingScale,
            ambient = ambient,
            phosphorBlend = phosphorBlend,
            replayKey = replayKey,
        )

        Column(
            Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp).fillMaxWidth().padding(horizontal = 16.dp)
        ) {
            DemoControlRow("RENDER", renderMode.label, themeColor, themeColorDim, onCycleRenderMode)
            DemoControlRow("STATE", "${state.label} -- ${state.line}", themeColor, themeColorDim, onCycleState)
            DemoControlRow("FRAMING", "${(framingScale * 100).toInt()}% width", themeColor, themeColorDim, onCycleFraming)
            DemoControlRow("AMBIENT", if (ambient) "ON" else "OFF", themeColor, themeColorDim, onToggleAmbient)
            DemoControlRow("MATERIALISE", "REPLAY", themeColor, themeColorDim, onReplayMaterialise)
            DemoControlRow("PHOSPHOR TINT", "${(phosphorBlend * 100).toInt()}%", themeColor, themeColorDim, onCyclePhosphorBlend)
            DemoControlRow("HUE", themeHue.name, themeColor, themeColorDim, onCycleHue)
            // A READOUT, not a control: QUARK speaks because she has something to say, and the flag
            // is raised by the voice engine. Labelled so it does not read as a dead toggle.
            DemoControlRow(
                "SPEAKING", if (speakingPreview) "LIVE -- SPEAKING" else "LIVE -- SILENT",
                themeColor, themeColorDim, onToggleSpeaking,
            )
            DemoControlRow("STEALTH", if (stealthPreview) "ON" else "OFF", themeColor, themeColorDim, onToggleStealth)
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

