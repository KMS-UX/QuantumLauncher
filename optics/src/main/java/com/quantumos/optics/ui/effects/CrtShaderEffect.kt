package com.quantumos.optics.ui.effects

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize

/**
 * Phosphor CRT screen treatment implemented as a GPU AGSL shader (`RuntimeShader`,
 * API 33+ -- this app's floor since minSdk is 33), never a CPU draw-loop.
 *
 * Applies, in a single pass over the composited content:
 *  - horizontal scanlines (darken every other pixel row),
 *  - a radial vignette (darken toward the corners),
 *  - a subtle low-frequency flicker driven by the [time] uniform,
 * all additively tinted by [themeColor] at low opacity.
 */
private const val CRT_SHADER_SRC =
  """
    uniform shader content;
    uniform float time;
    uniform float3 phosphorColor;
    uniform float2 resolution;

    half4 main(float2 coord) {
        half4 srcColor = content.eval(coord);

        // 1. Horizontal scanlines: darken every other pixel row.
        float lineSpacing = 3.0;
        float scanPhase = fract(coord.y / lineSpacing);
        float scanline = scanPhase < 0.5 ? 1.0 : 0.86;

        // 2. Radial vignette: darken toward the corners.
        float2 center = resolution * 0.5;
        float maxDist = max(length(center), 1.0);
        float dist = length(coord - center) / maxDist;
        float vignette = 1.0 - smoothstep(0.55, 1.05, dist) * 0.55;

        // 3. Subtle low-frequency flicker, modulated by the stepped `time` uniform.
        float flicker = 0.97 + 0.03 * sin(time);

        float effectMultiplier = scanline * vignette * flicker;

        half3 tinted = srcColor.rgb * effectMultiplier + half3(phosphorColor) * 0.05;
        return half4(tinted, srcColor.a);
    }
"""

/**
 * Layers the phosphor CRT GPU shader (scanlines + vignette + flicker, tinted by
 * [themeColor]) over the content this modifier is applied to. [time] should come from
 * a slow, stepped counter (not a smooth per-frame animation) -- see the `LaunchedEffect`
 * driving `time` alongside this modifier's call site.
 */
@Composable
fun Modifier.crtPhosphorEffect(themeColor: Color, time: Float): Modifier {
  val shader = remember { RuntimeShader(CRT_SHADER_SRC) }
  var layerSize by remember { mutableStateOf(IntSize.Zero) }

  return this.onSizeChanged { layerSize = it }.graphicsLayer {
    shader.setFloatUniform("time", time)
    shader.setFloatUniform("phosphorColor", themeColor.red, themeColor.green, themeColor.blue)
    shader.setFloatUniform(
      "resolution",
      layerSize.width.toFloat().coerceAtLeast(1f),
      layerSize.height.toFloat().coerceAtLeast(1f),
    )
    renderEffect = RenderEffect.createRuntimeShaderEffect(shader, "content").asComposeRenderEffect()
  }
}
