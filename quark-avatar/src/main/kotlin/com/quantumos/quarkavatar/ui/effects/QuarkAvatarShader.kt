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
 * Rendering-refinement pass (PRODUCTION_LOG.md, "Rendering refinement pass — Tier 1"): the two
 * workarounds this shader originally needed are now retired at the source instead of patched here.
 *  - The posture PNGs are now genuinely alpha-matted (`setup_render()`'s `film_transparent` was
 *    `False`; now `True`, with explicit RGBA output) -- the chroma-key `subjectMask()` that used to
 *    key a flat (58,67,58)/255 studio-backdrop color is gone; `src.a` is used directly, and the
 *    rim-glow's edge detection now runs on that real alpha discontinuity instead of a synthesized
 *    proxy for one.
 *  - The emissive accent used to bake as a pale, washed-out near-white green (measured
 *    rgb~155,218,137, dominance 63/255) because Blender 5.2 defaults to the AgX view transform,
 *    which deliberately desaturates bright emissives -- not "bloom" as originally assumed. The
 *    Blender script now sets `view_transform = 'Standard'` explicitly, and pure `(0,1,0)` bakes as
 *    a genuinely saturated green again (re-measured: accent-region G-max(R,B) now clusters at
 *    ~0.54-0.88 in the 0-1 range across ~40 sampled points, with the next-highest anywhere else in
 *    the render at 0.03 -- a wide, clean gap, not a fragile few-percent margin like the original
 *    key). The smoothstep band below targets that gap with headroom on both sides.
 *
 * Tier 3 (PRODUCTION_LOG.md, "Rendering refinement pass — Tier 3"): the two items explicitly
 * deferred out of Tier 1/2 as cosmetic-not-defect polish, now done since the rim has a real
 * physical backlight (Tier 2) to sit on top of instead of being the only source of edge definition.
 *  - Rim offset was a fixed 6px regardless of the surface's actual render size, so the rim reads
 *    thicker on a small surface and thinner on a large one instead of a consistent width. Now
 *    derived from the `resolution` uniform (which was declared but never read before this) as a
 *    fraction of render height, floored so it never vanishes on a tiny surface.
 *  - Rim detection was a symmetric 4-tap central-difference gradient straddling the silhouette
 *    edge -- roughly half its response comes from OUTSIDE the mask, which never actually reaches
 *    the screen (the shader's own `if (mask <= 0.0) return transparent` at the top discards every
 *    outside pixel before the rim term would apply to it), so half the gradient's dynamic range
 *    was wasted computing a contribution that's thrown away. Replaced with an 8-direction
 *    minimum-neighbor-mask sample: for a pixel that's already confirmed inside the mask, this asks
 *    "how close is the nearest background pixel" directly, giving the same inward-only rim using
 *    the sampling budget more efficiently.
 *  - Rim color was hardcoded white regardless of the live phosphor hue. Now mixed from the same
 *    `accentColor` uniform the accent retint already uses (mostly hue-tinted with a white bias
 *    for a glow look, not a flat wash) -- ties the rim to whatever hue/Alert-red is actually active
 *    instead of being a hue-independent white line.
 */
private const val QUARK_AVATAR_SHADER_SRC = """
    uniform shader content;
    uniform float2 resolution;
    uniform float3 accentColor;
    uniform float rimStrength;
    uniform float stealthDim;

    float subjectMask(float2 coord) {
        return float(content.eval(coord).a);
    }

    half4 main(float2 coord) {
        half4 src = content.eval(coord);
        float mask = subjectMask(coord);
        if (mask <= 0.0) {
            return half4(0.0, 0.0, 0.0, 0.0);
        }
        float3 rgb = float3(src.rgb);

        // Accent retint: key on the measured saturated-green dominance band. Re-measured after the
        // Director-specified phenotype rebuild (new body, MakeHuman assets, studio-HDRI lighting).
        // The separation is now very wide: body/armour pixels top out at 0.01 while the accent
        // spans 0.46-0.90. The previous 0.35-0.55 ramp was calibrated when the ceramic pushed some
        // body pixels to 0.22, and it would now leave the dimmest accent pixels only partially
        // tinted. Band moved to 0.12-0.28 -- an order of magnitude above the body maximum, and
        // fully saturated well before the faintest accent pixel. Scale the live accentColor by the
        // baked pixel's own green value to preserve the render's shading rather than flattening it.
        float greenness = rgb.g - max(rgb.r, rgb.b);
        float accentT = smoothstep(0.12, 0.28, greenness);
        rgb = mix(rgb, accentColor * rgb.g, accentT);

        // Rim/edge glow: for a pixel already confirmed inside the mask (the function returns above
        // otherwise), sample 8 directions around it at a resolution-derived radius and take the
        // MINIMUM mask value found -- that directly answers "how close is the nearest background
        // pixel" using every tap's full dynamic range, instead of the old symmetric central-
        // difference gradient where roughly half the response came from outside-the-mask taps that
        // could never reach the screen anyway (that pixel would have already been discarded above).
        // Radius as a fraction of render height (not a fixed pixel count) so the rim reads the same
        // relative thickness on any surface size; floored so it doesn't vanish at small sizes.
        float r = max(2.0, resolution.y * 0.0045);
        float m = 1.0;
        m = min(m, subjectMask(coord + float2( r,  0.0)));
        m = min(m, subjectMask(coord + float2(-r,  0.0)));
        m = min(m, subjectMask(coord + float2(0.0,  r)));
        m = min(m, subjectMask(coord + float2(0.0, -r)));
        float rd = r * 0.7071; // diagonal taps at the same radius, axis-scaled
        m = min(m, subjectMask(coord + float2( rd,  rd)));
        m = min(m, subjectMask(coord + float2(-rd,  rd)));
        m = min(m, subjectMask(coord + float2( rd, -rd)));
        m = min(m, subjectMask(coord + float2(-rd, -rd)));
        float rim = (1.0 - m) * rimStrength;
        // Rim tint: mostly the live accent hue (ties the glow to whatever phosphor color / Alert
        // red is actually active) with a white bias so it still reads as a glow, not a flat wash.
        rgb += rim * mix(float3(1.0, 1.0, 1.0), accentColor, 0.7) * 0.9;

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
