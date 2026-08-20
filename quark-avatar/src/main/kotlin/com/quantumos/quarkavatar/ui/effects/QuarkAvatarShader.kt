package com.quantumos.quarkavatar.ui.effects

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize

/*
 * QUARK avatar overlay shader (Phase 4b, art/quark-avatar/PRODUCTION_LOG.md) -- the real-time AGSL
 * half of the locked hybrid render path (pre-rendered Blender frames + live GPU tinting). Applied on
 * top of the bundled posture-library PNGs (res/drawable-nodpi), which bake QUARK's four physical
 * materials as fixed, real-world colors (Director-approved phosphor-guardrail exception) and the
 * emissive accent (spine conduit + headband) as pure baked GREEN -- this shader keys that green
 * region and retints it live from PhosphorHueRuntime, per the locked "only the accent is hue-driven"
 * resolution. GPU shader with a cheap fallback (house-style hard rule): if RuntimeShader construction
 * fails, or on a pre-API-33 surface, the modifier is a no-op and the plain (unshaded) bundled PNG
 * still renders -- never a crash or a blank screen.
 *
 * Uniforms are set once per recomposition, not per-frame -- no `time` uniform, matching "static at
 * rest / zero idle redraw." Speaking's ripple-ring VFX is deliberately NOT part of this shader (see
 * SpeakingRippleOverlay in QuarkAvatarScreen.kt) -- it's geometric ring-drawing, better suited to a
 * Compose Canvas overlay (the same technique QuarkMascot.kt already uses for HAPPY/SCAN), keeping
 * this shader scoped to true pixel-level work: edge detection and color-keyed retinting.
 */
private const val QUARK_AVATAR_SHADER_SRC = """
    uniform shader content;
    uniform float2 resolution;
    uniform float3 accentColor;
    uniform float rimStrength;
    uniform float stealthDim;

    half4 main(float2 coord) {
        half4 src = content.eval(coord);
        if (src.a <= 0.0) {
            return src;
        }
        float3 rgb = float3(src.rgb);

        // Accent retint: the posture-library bake marks the emissive accent as pure green (0,1,0),
        // shaded by the render lighting -- key on "green clearly dominant over red and blue" rather
        // than exact equality, so shaded/darker accent pixels are still caught, not just the flattest
        // highlight. Scale the live accentColor by the baked pixel's own green value to preserve that
        // shading instead of flattening it to one flat color.
        bool isAccentGreen = rgb.g > rgb.r * 2.0 && rgb.g > rgb.b * 2.0 && rgb.g > 0.05;
        if (isAccentGreen) {
            rgb = accentColor * rgb.g;
        }

        // Rim/edge glow: a 4-tap alpha gradient detects proximity to the silhouette edge and adds an
        // internal rim-light there -- bounded by src.a > 0 (the early-out above), so the glow never
        // bleeds past the PNG's own raster bounds.
        float aE = content.eval(coord + float2(1.5, 0.0)).a;
        float aW = content.eval(coord - float2(1.5, 0.0)).a;
        float aN = content.eval(coord + float2(0.0, 1.5)).a;
        float aS = content.eval(coord - float2(0.0, 1.5)).a;
        float rim = clamp((abs(aE - aW) + abs(aN - aS)) * 2.0, 0.0, 1.0) * rimStrength;
        rgb += rim * float3(1.0, 1.0, 1.0) * 0.35;

        // Stealth dim: flat brightness multiply, saturation unchanged -- matches
        // QuantumStateEngine.toggleStealthMode()'s own doc comment.
        rgb *= stealthDim;

        return half4(half3(rgb), src.a);
    }
"""

/**
 * Applies the QUARK avatar overlay shader (accent retint + rim glow + Stealth dim) to the modified
 * content. [accentColor] should be the live phosphor hue for retint-eligible postures, or the fixed
 * `Phosphor.Warn` for Alert (Alert's bake is never actually retinted since it has no green accent
 * pixels to key on, but passing a consistent color keeps the call site simple). [rimStrength] 0f
 * disables the rim entirely; [stealthDim] 1f is normal brightness, ~0.35f is Stealth-dimmed.
 */
fun Modifier.quarkAvatarEffect(
    accentColor: Color,
    rimStrength: Float = 0.6f,
    stealthDim: Float = 1f
): Modifier = composed {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return@composed this
    val shader = remember { runCatching { RuntimeShader(QUARK_AVATAR_SHADER_SRC) }.getOrNull() }
        ?: return@composed this // compilation failed -> cheap fallback: plain unshaded image, never a crash
    var size by remember { mutableStateOf(IntSize.Zero) }
    this
        .onSizeChanged { size = it }
        .graphicsLayer {
            // Explicit offscreen compositing -- no prior art in this repo proves a RenderEffect
            // samples a transparent-background Image's real alpha correctly by default; this is the
            // safe, explicit choice rather than an assumption. Confirm on-device (no silhouette
            // fringing) before treating this as settled -- see PRODUCTION_LOG's open questions.
            compositingStrategy = CompositingStrategy.Offscreen
            if (size.width > 0 && size.height > 0) {
                shader.setFloatUniform("resolution", size.width.toFloat(), size.height.toFloat())
                shader.setFloatUniform("accentColor", accentColor.red, accentColor.green, accentColor.blue)
                shader.setFloatUniform("rimStrength", rimStrength)
                shader.setFloatUniform("stealthDim", stealthDim)
                renderEffect = RenderEffect
                    .createRuntimeShaderEffect(shader, "content")
                    .asComposeRenderEffect()
            }
        }
}
