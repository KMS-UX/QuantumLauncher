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
 * emissive accent (spine conduit + headband) -- this shader keys that accent region and retints it
 * live from PhosphorHueRuntime, per the locked "only the accent is hue-driven" resolution. GPU shader
 * with a cheap fallback (house-style hard rule): if RuntimeShader construction fails, or on a
 * pre-API-33 surface, the modifier is a no-op and the plain (unshaded) bundled PNG still renders --
 * never a crash or a blank screen.
 *
 * Both the background-key and the accent-key constants below are measured, not assumed -- checked
 * against the actual bundled PNGs on an emulator, not eyeballed from the Blender script's material
 * parameters:
 *  - The three posture PNGs are NOT alpha-matted despite PRODUCTION_LOG's original description --
 *    their alpha channel is uniformly 255 (opaque) everywhere, including the studio backdrop, which
 *    is instead a flat, uniform (58,67,58)/255 fill (measured identical at 8 sample points across all
 *    3 files). A real alpha-matted re-render is Blender-pipeline work, out of this session's scope --
 *    this shader synthesizes its own subject mask at runtime by keying that flat background color
 *    instead, which also makes the rim-glow's edge detection meaningful (it was previously computing
 *    a gradient over a uniformly-255 channel, and could never detect an edge at all).
 *  - The emissive accent's real baked color is a pale, near-white green cast (e.g. rgb ~200,227,198),
 *    not a saturated pure green -- an emissive material at high strength blooms/washes out under the
 *    renderer's tone mapping. The original "G > 2x R and B" key assumed pure (0,1,0) and could never
 *    fire on the real data (mathematically impossible once R exceeds ~127). Recalibrated to the
 *    measured dominance: background/body pixels sit at G-max(R,B) in [0,9]/255, the accent region
 *    (a 66x272px band matching the spine conduit's real screen position, confirmed spatially, not
 *    just by color) sits at [25,45]/255 -- the smoothstep band below targets that gap.
 */
private const val QUARK_AVATAR_SHADER_SRC = """
    uniform shader content;
    uniform float2 resolution;
    uniform float3 accentColor;
    uniform float rimStrength;
    uniform float stealthDim;

    const float3 kBgColor = float3(0.227, 0.263, 0.227);

    float subjectMask(float2 coord) {
        float3 c = float3(content.eval(coord).rgb);
        float d = length(c - kBgColor);
        return smoothstep(0.03, 0.10, d);
    }

    half4 main(float2 coord) {
        half4 src = content.eval(coord);
        float mask = subjectMask(coord);
        if (mask <= 0.0) {
            return half4(0.0, 0.0, 0.0, 0.0);
        }
        float3 rgb = float3(src.rgb);

        // Accent retint: key on the measured pale-green dominance band, not an assumed pure (0,1,0).
        // Scale the live accentColor by the baked pixel's own green value to preserve whatever
        // shading/falloff the render already has instead of flattening it to one flat color.
        float greenness = rgb.g - max(rgb.r, rgb.b);
        float accentT = smoothstep(0.07, 0.14, greenness);
        rgb = mix(rgb, accentColor * rgb.g, accentT);

        // Rim/edge glow: a 4-tap gradient over the synthesized mask (not the source alpha channel,
        // which is useless here) detects proximity to the silhouette edge and adds an internal
        // rim-light there. First pass used a 1.5px offset / 0.35 strength and was confirmed
        // on-device to be too subtle to read at all (checked by rendering and looking, not assumed --
        // see PRODUCTION_LOG) -- widened to a ~6px band with a brighter additive term.
        float mE = subjectMask(coord + float2(6.0, 0.0));
        float mW = subjectMask(coord - float2(6.0, 0.0));
        float mN = subjectMask(coord + float2(0.0, 6.0));
        float mS = subjectMask(coord - float2(0.0, 6.0));
        float rim = clamp((abs(mE - mW) + abs(mN - mS)), 0.0, 1.0) * rimStrength;
        rgb += rim * float3(1.0, 1.0, 1.0) * 0.9;

        // Stealth dim: flat brightness multiply, saturation unchanged -- matches
        // QuantumStateEngine.toggleStealthMode()'s own doc comment.
        rgb *= stealthDim;

        return half4(half3(rgb) * mask, mask);
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
