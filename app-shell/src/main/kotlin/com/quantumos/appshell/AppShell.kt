package com.quantumos.appshell

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quantumos.core.NavigationChannel
import com.quantumos.core.PhosphorHue

/*
 * QuantumOS — shared App Shell chrome (App Shell Integration, Phase 3). Extracted from the
 * launcher's LauncherUi.kt so every docked module (Optics, Nav, future companions) wears the exact
 * same phosphor tokens, CRT container, nameplate header, and PLEASE STANDBY beat rather than a
 * per-app reimplementation — "one App Shell every app inherits," per the House Style skill.
 */

// ---------- phosphor token source: one hue switch recolors everything ----------
object Phosphor {
    val GreenBright = Color(0xFF00FF00); val GreenDim = Color(0xFF00AA00)
    val AmberBright = Color(0xFFFFB000); val AmberDim = Color(0xFFA86F00)
    val CyanBright  = Color(0xFF00E5FF); val CyanDim  = Color(0xFF0090A8)
    val Warn = Color(0xFFFF3B1F)
    val Crt  = Color(0xFF020402)

    fun bright(h: PhosphorHue) = when (h) {
        PhosphorHue.GREEN -> GreenBright; PhosphorHue.AMBER -> AmberBright; PhosphorHue.CYAN -> CyanBright
    }
    fun dim(h: PhosphorHue) = when (h) {
        PhosphorHue.GREEN -> GreenDim; PhosphorHue.AMBER -> AmberDim; PhosphorHue.CYAN -> CyanDim
    }
}

@Immutable
data class TerminalConstraints(
    val containerWidth: Dp,
    val containerHeight: Dp,
    val isLetterboxed: Boolean,
    val rawWindowWidth: Dp,
    val rawWindowHeight: Dp,
    val systemBarsPadding: PaddingValues
)

/*
 * THE ONE DESIGN DECISION (Director's):
 *  - forceFixedContainer = FALSE (default): surface fills the real screen; CRT falloff frames it.
 *  - forceFixedContainer = TRUE: 9:19.5 letterbox — a deliberate "screen-in-chassis" look.
 *
 * BackHandler is owned by the calling screen (launcher's QuantumAppShell, or a docked module's own
 * top-level composable) so it can route its own nav before consuming — this container never
 * intercepts back itself.
 */
@Composable
fun QuantumOSLayoutShell(
    forceFixedContainer: Boolean = false,
    targetAspectRatio: Float = 9f / 19.5f,
    content: @Composable (TerminalConstraints) -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize().background(Color.Black)
    ) {
        val rawWidth = maxWidth
        val rawHeight = maxHeight
        if (rawWidth <= 0.dp || rawHeight <= 0.dp) return@BoxWithConstraints

        val (cw, ch) = if (!forceFixedContainer) {
            rawWidth to rawHeight
        } else {
            val ratio = rawWidth.value / rawHeight.value
            when {
                ratio > targetAspectRatio -> (rawHeight * targetAspectRatio) to rawHeight
                ratio < targetAspectRatio -> rawWidth to (rawWidth / targetAspectRatio)
                else -> rawWidth to rawHeight
            }
        }

        val constraints = TerminalConstraints(
            containerWidth = cw,
            containerHeight = ch,
            isLetterboxed = (cw != rawWidth || ch != rawHeight),
            rawWindowWidth = rawWidth,
            rawWindowHeight = rawHeight,
            systemBarsPadding = WindowInsets.systemBars.asPaddingValues()
        )

        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Box(
                Modifier
                    .width(cw)
                    .height(ch)
                    .background(Phosphor.Crt)
                    .crtShader()
            ) {
                content(constraints)
            }
        }
    }
}

/*
 * REAL CRT shader. An AGSL RuntimeShader (RenderEffect, API 33+, covered by minSdk 33) that samples
 * the rendered content and lays scanlines, a CRT-falloff vignette, and a phosphor self-glow over it
 * on the GPU — not a CPU draw-loop (design-tokens rendering rule). Sets uniforms once per size, so
 * there is no idle redraw (static at rest).
 *
 * Safety net: if shader compilation ever fails — or on a pre-33 surface that slips through — it
 * falls back automatically to the cheap non-shader overlay below.
 *
 * NOTE for map-hosting modules (Nav): a RenderEffect renders its subtree into an offscreen buffer,
 * which a GLSurfaceView/TextureView-backed map is not captured into — use crtOverlay() instead for
 * any surface that hosts an embedded native map/camera view.
 */
private const val CRT_AGSL_SHADER = """
uniform shader content;
uniform float2 resolution;
half4 main(float2 coord) {
    half4 src = content.eval(coord);
    float2 uv = coord / resolution;
    // scanlines — soft dark bands, period ~3px
    float scan = 0.88 + 0.12 * (0.5 + 0.5 * sin(coord.y * 2.094));
    // CRT falloff — content fades toward the edges
    float2 c = uv - 0.5;
    float vig = clamp(1.0 - dot(c, c) * 1.15, 0.28, 1.0);
    float f = scan * vig;
    float3 rgb = float3(src.rgb) * f;
    // phosphor self-glow — lift the bright phosphor a touch
    float3 glow = float3(src.rgb) * float3(src.rgb) * 0.22;
    return half4(half3(rgb + glow), src.a);
}
"""

fun Modifier.crtShader(): Modifier = composed {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return@composed this.crtOverlay()
    val shader = remember { runCatching { RuntimeShader(CRT_AGSL_SHADER) }.getOrNull() }
        ?: return@composed this.crtOverlay()   // compilation failed → cheap fallback (safety net)
    var size by remember { mutableStateOf(IntSize.Zero) }
    this
        .onSizeChanged { size = it }
        .graphicsLayer {
            if (size.width > 0 && size.height > 0) {
                shader.setFloatUniform("resolution", size.width.toFloat(), size.height.toFloat())
                renderEffect = RenderEffect
                    .createRuntimeShaderEffect(shader, "content")
                    .asComposeRenderEffect()
            }
        }
}

// Cheap non-shader CRT treatment — the automatic fallback if the AGSL shader can't compile, and the
// deliberate choice for any surface hosting an embedded native view (map/camera) a RenderEffect
// can't capture.
fun Modifier.crtOverlay(): Modifier = drawWithContent {
    drawContent()
    val gap = 3.dp.toPx()
    var y = 0f
    while (y < size.height) {
        drawLine(Color(0x14000000), Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
        y += gap
    }
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(Color.Transparent, Color(0xAA000000)),
            center = Offset(size.width / 2f, size.height / 2f),
            radius = size.maxDimension * 0.75f
        )
    )
}

// Opaque nameplate header with registration marks — top App Shell chrome. Every docked module's
// header rolls the Vitality shade tucks-behind rule the same way: this background is opaque, never
// letting content bleed through.
@Composable
fun NameplateHeader(channelName: String, color: Color, dimColor: Color, font: FontFamily) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Phosphor.Crt)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "QUANTUM OS",
            color = color,
            fontFamily = font,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = "// $channelName",
            color = dimColor,
            fontFamily = font,
            fontSize = 12.sp
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = "[⊕]",
            color = dimColor,
            fontFamily = font,
            fontSize = 11.sp
        )
    }
}

// Channel navigation strip below the nameplate — launcher-only chrome (HOME/APPS/STATUS/LOG), kept
// here so it stays a single source alongside the header it sits under.
@Composable
fun ChannelStrip(
    current: NavigationChannel,
    color: Color,
    dimColor: Color,
    font: FontFamily,
    onSelect: (NavigationChannel) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        NavigationChannel.entries.forEach { channel ->
            val active = channel == current
            Text(
                text = if (active) "[${channel.name}]" else " ${channel.name} ",
                color = if (active) color else dimColor,
                fontFamily = font,
                fontSize = 11.sp,
                modifier = Modifier
                    .clickable { onSelect(channel) }
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            )
        }
    }
}

// PLEASE STANDBY card — the one universal loading/transition beat (House Style: never a generic
// spinner). Public so every docked module reuses it for its own hand-off beat instead of rebuilding.
@Composable
fun PleaseStandbyCard(subline: String, color: Color, dimColor: Color, font: FontFamily) {
    androidx.compose.foundation.layout.Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("PLEASE STANDBY", color = color, fontFamily = font, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        if (subline.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Text(subline, color = dimColor, fontFamily = font, fontSize = 12.sp)
        }
    }
}

// Segmented phosphor gauge (M3 Vitality panel) — extracted here (SIGNAL + CONFIG Task Brief) so
// SIGNAL's four link-diagnostic gauges render "the same visual language as the Vitality panel's
// Signal/Power/Core Temp gauges" per the brief, instead of a per-module reimplementation. No
// Material LinearProgressIndicator anywhere in the house — phosphor segments only.
@Composable
fun SegmentedGauge(
    label: String,
    filled: Int,
    total: Int,
    value: String,
    color: Color,
    dimColor: Color,
    font: FontFamily
) {
    androidx.compose.foundation.layout.Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(Modifier.fillMaxWidth()) {
            Text(label.padEnd(10), color = dimColor, fontFamily = font, fontSize = 12.sp)
            Spacer(Modifier.weight(1f))
            Text(value, color = color, fontFamily = font, fontSize = 12.sp)
        }
        Spacer(Modifier.height(3.dp))
        Row(Modifier.fillMaxWidth()) {
            repeat(total) { i ->
                Box(
                    Modifier
                        .weight(1f)
                        .height(10.dp)
                        .background(if (i < filled) color else dimColor.copy(alpha = 0.22f))
                )
                if (i < total - 1) Spacer(Modifier.width(2.dp))
            }
        }
    }
}
