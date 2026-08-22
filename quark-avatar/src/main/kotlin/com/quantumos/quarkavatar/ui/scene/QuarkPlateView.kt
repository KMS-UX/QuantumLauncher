package com.quantumos.quarkavatar.ui.scene

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.quantumos.quarkavatar.MATERIALISE_SETTLED
import com.quantumos.quarkavatar.MaterialiseFrame

/*
 * QuarkPlateView -- the reference art shown AS THE REFERENCE ART.
 *
 * Phase 9. The Director's verdict on the Gaussian-splat path was that the reconstruction loss is
 * over tolerance, and Phase 8 established why it cannot be fixed from inside the SDK: SceneView
 * 4.22.0 renders splats isotropically, so surface detail is smeared no matter how good the source
 * is. The dead-on front plates (`reference/QUARK_HOLOGRAM_FRONT.png`,
 * `QUARK_HOLOGRAMBUST_FRONT.png`) make that whole problem optional, because the presentation the
 * Director specified -- a Gatebox/Mora projection housing, front-on, static at rest -- never needs
 * a viewing angle the plate does not already contain.
 *
 * So this path spends NOTHING on reconstruction: the art reaches the screen at exactly the fidelity
 * it was drawn at. What it gives up is parallax -- there is no depth to respond to a tilt. Whether
 * that matters is a judgement to make by looking at it next to the splat, which is why both are
 * live modes on the same screen.
 *
 * The plates are chroma-keyed by `comfy/chroma_key.py`; the magenta key separates from the blue
 * subject on R-G alone, and the measured residual spill in these two is **0.000%**.
 *
 * The PHOSPHOR TINT control drives this path through the same maths the 3D path uses in its
 * Filament colour-grading LUT -- a blend between the identity and the active phosphor applied to
 * luminance -- so both paths recolour identically and the comparison stays honest.
 */
@Composable
fun QuarkPlateView(
    plateRes: Int,
    contentDescription: String,
    phosphor: Color,
    phosphorBlend: Float,
    stealthDim: Float,
    framingScale: Float = 1f,
    baseOverhang: Float = 0.17f,
    baseHeightFill: Float = 1f,
    materialise: MaterialiseFrame = MATERIALISE_SETTLED,
    modifier: Modifier = Modifier,
) {
    val filter = remember(phosphor, phosphorBlend, stealthDim) {
        val b = phosphorBlend.coerceIn(0f, 1f)
        val d = stealthDim.coerceIn(0f, 1f)
        // Row-major 4x5: [r, g, b, a, offset]. Each output channel mixes between the identity and
        // luminance * tint. Columns 3 (alpha coefficient) and 4 (offset) are zero for the colour
        // rows -- and they have to be handled explicitly, because LUMA only has three entries and
        // the first version walked off the end of it on column 3.
        fun row(channel: Int, tint: Float) = FloatArray(5) { i ->
            if (i > 2) {
                0f
            } else {
                val identity = if (i == channel) 1f else 0f
                (identity * (1f - b) + LUMA[i] * tint * b) * d
            }
        }
        val m = FloatArray(20)
        row(0, phosphor.red).copyInto(m, 0)
        row(1, phosphor.green).copyInto(m, 5)
        row(2, phosphor.blue).copyInto(m, 10)
        // Alpha passes through untouched -- the key already did the matting.
        m[18] = 1f
        ColorFilter.colorMatrix(ColorMatrix(m))
    }

    // FRAMING: bottom-anchored, and deliberately overhanging the bottom edge.
    //
    // The plate was top-aligned, which left the bust's own cut -- the hard scalloped line where the
    // chest art simply stops -- sitting in clear space with the housing visible underneath it. That
    // reads as a cropped picture of a person, not a projection: a hologram has no reason to end in a
    // straight line halfway up a screen.
    //
    // Anchoring to the bottom and pushing BASE_OVERHANG of the plate's height past the edge means
    // the ending line is never on screen. What the Operator sees instead is QUARK continuing down
    // into the frame edge and dissolving in the CRT falloff, which is what a volumetric projection
    // does -- it fades where the emitter's cone runs out, it does not get cropped.
    // The layout box is sized to the plate's OWN aspect ratio rather than left to ContentScale.Fit.
    // Fit centres the bitmap inside whatever box it is given, so with a tall box the drawn image
    // floats in the middle of it and a bottom-anchored offset is silently eaten by the slack --
    // measured, as a 232 px shortfall on the first attempt at this framing. `aspectRatio` makes the
    // layout box and the drawn image the same rectangle, so "bottom" means the plate's bottom.
    val painter = painterResource(plateRes)
    val aspect = painter.intrinsicSize.let { if (it.height > 0f) it.width / it.height else PLATE_ASPECT }
    BoxWithConstraints(modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        // `requiredWidth`, not `width` and not `fillMaxWidth`. The scale deliberately goes ABOVE
        // 1.0, and both of the others are coerced into the incoming constraints -- measured, as a
        // FRAMING control that produced four pixel-identical screenshots because every setting was
        // silently clamped back to the viewport width. `requiredWidth` ignores the incoming max;
        // the caller clips what overflows.
        // C2 -- the fold.
        //
        // The bug: sizing purely off WIDTH assumes a tall narrow viewport. These plates are far
        // taller than they are wide, so height is the dependent variable, and on the Fold 6's INNER
        // display (~2160x1856) `maxWidth / aspect` asks for a BODY plate about **twice the screen
        // height** -- her head leaves the frame entirely and the Operator is looking at a torso.
        // Landscape does the same. Measured arithmetically, not guessed.
        //
        // The fix clamps the BASE size and then lets FRAMING scale that result, rather than clamping
        // the final size. Clamping last would have made FRAMING a dead control on the inner screen --
        // every step would hit the same ceiling and render identically, which is precisely the
        // "four pixel-identical screenshots" defect this file already warns about a few lines down.
        //
        // `baseHeightFill` is the fraction of viewport height QUARK occupies at FRAMING 100%, and it
        // is taken from what the tuned 1080x2424 screen already produces -- so on that screen the
        // clamp does not bind at any reachable setting and the approved framing is bit-for-bit
        // unchanged, while a wide viewport gets the SAME PROPORTIONS the Director already signed off
        // rather than a different composition. The trailing 0.99 is a hard backstop so no
        // combination can exceed the viewport.
        val baseHeight = minOf(maxWidth / aspect, maxHeight * baseHeightFill)
        val plateHeight = minOf(baseHeight * framingScale, maxHeight * 0.99f)
        val plateWidth = plateHeight * aspect
        Image(
            painter = painter,
            contentDescription = contentDescription,
            modifier = Modifier
                .requiredWidth(plateWidth)
                .aspectRatio(aspect)
                .offset(y = plateHeight * baseOverhang)
                .materialiseReveal(materialise),
            contentScale = ContentScale.Fit,
            colorFilter = filter,
        )
    }
}

/*
 * B1: the figure resolving, bottom-up, behind the scan band.
 *
 * The mask is a full-canvas vertical gradient composited with `DstIn`, which keeps the plate only
 * where the gradient is opaque. It has to cover the WHOLE canvas -- a blend mode only applies inside
 * the bounds of the thing being drawn, so a partial rect would leave the region above it untouched
 * instead of erased.
 *
 * `CompositingStrategy.Offscreen` is not optional here. Without it the layer has no buffer of its
 * own for `DstIn` to act on and the blend applies against whatever is already on the screen -- which
 * on this surface is the housing, so the mask would eat the projection column instead of the figure.
 *
 * When the transition has settled this adds NOTHING: no layer, no blend, no allocation. That is what
 * keeps the "zero idle redraw" guarantee intact for a screen that is static the other 99% of the
 * time.
 */
private fun Modifier.materialiseReveal(frame: MaterialiseFrame): Modifier {
    if (frame.isSettled) return this
    return this
        .graphicsLayer {
            compositingStrategy = CompositingStrategy.Offscreen
            alpha = frame.figureAlpha.coerceIn(0.001f, 1f)
        }
        .drawWithContent {
            drawContent()
            if (frame.hidden <= 0f) return@drawWithContent
            // The revealed edge, measured from the top, plus a soft band above it so the figure
            // resolves out of the dark rather than being wiped by a hard line.
            val edge = frame.hidden
            val soft = (edge - REVEAL_SOFTNESS).coerceIn(0f, 1f)
            drawRect(
                brush = Brush.verticalGradient(
                    colorStops = arrayOf(
                        0f to Color.Transparent,
                        soft to Color.Transparent,
                        edge.coerceAtLeast(soft + 0.001f) to Color.Black,
                        1f to Color.Black,
                    ),
                    startY = 0f,
                    endY = size.height,
                ),
                blendMode = BlendMode.DstIn,
            )
        }
}

// How tall the soft edge of the reveal is, as a fraction of the plate. Wide enough that the figure
// appears to condense out of the scan band rather than being uncovered by a moving shutter.
private const val REVEAL_SOFTNESS = 0.14f

private val LUMA = floatArrayOf(0.2126f, 0.7152f, 0.0722f)

// The bust plates are 779x956. Only a fallback -- the real aspect is read off the painter, because
// PLATE_BODY is a different shape and hardcoding one would silently letterbox the other.
private const val PLATE_ASPECT = 779f / 956f

// How wide QUARK is drawn in the housing, and how much of her runs off the bottom edge.
//
// The width drives everything: these plates are taller than they are wide, so at any sane width the
// height is the dependent variable, and a bust simply cannot fill a 1080x2424 frame vertically
// without cropping the hair off the sides. The empty upper region is not waste -- it is the
// projection volume the column and the scan sweep live in, which is the Gatebox read.
//
// The overhang moved to RenderMode.baseOverhang -- it is a property of the PLATE SET, not of this
// composable, because the bust and the body cut in different places. See the doc there.
