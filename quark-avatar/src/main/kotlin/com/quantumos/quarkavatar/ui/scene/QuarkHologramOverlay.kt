package com.quantumos.quarkavatar.ui.scene

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.quantumos.quarkavatar.MATERIALISE_SETTLED
import com.quantumos.quarkavatar.MaterialiseFrame
import com.quantumos.quarkavatar.SPEAK_CYCLE_MS
import com.quantumos.quarkavatar.speakingCadenceAt

/*
 * The projection housing QUARK stands in -- a Gatebox/Mora-style holographic column, drawn in the
 * active phosphor over whatever the SceneView beneath is rendering.
 *
 * **QUARK herself is static. The PROJECTION is not.** That distinction is what lets this have
 * ambient life without breaking the house rule that QUARK is "static at rest (zero idle redraw)"
 * and that her motion is reactive, not ambient. Nothing here animates the character: what moves is
 * the apparatus she is being projected by -- a slow scan sweep travelling up the column and a faint
 * flicker in the column, which is what a projector does whether or not the thing it is projecting
 * is doing anything. She stays perfectly still inside it.
 *
 * It is still a running animation and it still costs battery on an always-visible surface, so it is
 * opt-in behind an AMBIENT control rather than simply switched on. With `ambient = false` every
 * element below is static and the composable does not recompose at all.
 *
 * The MATERIALISE transition (B1) also draws here -- the emitter strike at the bottom edge and the
 * scan band QUARK resolves behind. That one is reactive rather than ambient: it runs once, on entry
 * or on request, and settles to exactly this static state.
 *
 * Composition, bottom to top:
 *   - a soft column of light rising from the floor line the figure stands on
 *   - fixed scanlines across the whole surface
 *   - CRT falloff: content fades to black at the edges, no drawn bezel (house style)
 *
 * Drawn as plain Compose geometry rather than AGSL because it is painted once and never animates;
 * the shader budget belongs to things that actually change.
 */
@Composable
fun QuarkHologramOverlay(
    phosphor: Color,
    ambient: Boolean,
    /**
     * How hard the emitter burns at rest, 0 normally and 1 under an alert state.
     *
     * WARN's housing red was measured as very nearly INVISIBLE after Phase 15 removed the floor
     * pool: driving `phosphor` to `--warn` moved the housing strip's R-B by 1.08 out of 255, because
     * the column alone is drawn at alpha 0.016-0.10 and there was no longer a bright element for the
     * colour to land on. Phase 13's "unmistakable at a glance" had been measured WITH the pool.
     *
     * This gives the colour something to burn on again -- and reuses B1's emitter bloom to do it, so
     * the alert is a projection artefact rather than the drawn disc that got removed for floating in
     * the middle of the frame. It sits at the bottom edge, where the apparatus is.
     */
    alert: Float = 0f,
    /**
     * How hard QUARK is speaking, 0..1. Drives the emitter's cadence -- see QuarkSpeaking.kt for why
     * the apparatus carries this rather than a ring drawn over the character.
     */
    speaking: Float = 0f,
    /**
     * How far the projection is powered down for Stealth, 0 normal and 1 fully dark.
     *
     * B3. Stealth was a flat 0.35 multiply on the plate's colour matrix and nothing else -- the
     * housing carried on burning at full strength around a dimmed figure, which is the opposite of
     * what "going dark" looks like. Now the APPARATUS powers down: the column collapses toward the
     * emitter and everything dims, leaving only an ember at the bottom edge so the unit still reads
     * as alive rather than off.
     */
    stealth: Float = 0f,
    materialise: MaterialiseFrame = MATERIALISE_SETTLED,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "quark_projection")
    // Position of the scan sweep, 0 at the floor and 1 at the top of the column. Slow on purpose:
    // a fast sweep reads as a loading bar, and this is meant to read as an idle carrier signal.
    val sweep by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(SWEEP_PERIOD_MS, easing = LinearEasing), RepeatMode.Restart,
        ),
        label = "sweep",
    )
    // Emitter flicker. Deliberately shallow -- a projector that is struggling reads as broken
    // hardware, and QUARK's whole job is to look like something that is working.
    val flicker by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1f - FLICKER_DEPTH,
        animationSpec = infiniteRepeatable(
            tween(FLICKER_PERIOD_MS, easing = LinearEasing), RepeatMode.Reverse,
        ),
        label = "flicker",
    )
    // The materialise STRIKE rides on top of whatever the emitter is otherwise doing, so an
    // ambient flicker and a scan-in do not fight over the same value.
    // Speaking cadence. Only runs while she is actually speaking, so a silent screen still has zero
    // idle redraw.
    val speakPhase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(SPEAK_CYCLE_MS, easing = LinearEasing), RepeatMode.Restart,
        ),
        label = "speak_cadence",
    )
    val speakBurn = if (speaking > 0f) speaking * speakingCadenceAt(speakPhase) else 0f

    // Stealth dims everything the housing draws and collapses the column toward the emitter.
    val powered = 1f - stealth * STEALTH_DIM_DEPTH
    val emitter = (if (ambient) flicker else 1f) *
        (1f + materialise.emitterBoost * STRIKE_GAIN) *
        (1f + speakBurn * SPEAK_GAIN) * powered

    Canvas(modifier) {
        val w = size.width
        val h = size.height
        // The figure is framed standing on this line -- see FLOOR_FRACTION in QuarkSceneView.
        val floorY = h * FLOOR_FRACTION

        // --- projection column: a wedge of light widening from the floor line upward -------------
        // Drawn with BOTH a vertical and a horizontal falloff. The first version used a plain
        // vertical gradient inside a rect, and its left and right edges showed on device as two
        // hard vertical seams -- a visible box around the figure, which is exactly the drawn bezel
        // the house style forbids.
        val columnTop = h * (0.10f + stealth * STEALTH_COLLAPSE)
        val columnHalf = w * 0.34f
        val steps = 28
        for (i in 0 until steps) {
            val t = i / (steps - 1f)
            val x0 = w / 2f - columnHalf * (0.35f + 0.65f * t)
            val x1 = w / 2f + columnHalf * (0.35f + 0.65f * t)
            val y0 = columnTop + (floorY - columnTop) * t
            val y1 = columnTop + (floorY - columnTop) * ((i + 1) / (steps - 1f))
            drawRect(
                brush = Brush.horizontalGradient(
                    0f to Color.Transparent,
                    0.5f to phosphor.copy(alpha = (0.016f + 0.10f * t * t) * emitter),
                    1f to Color.Transparent,
                    startX = x0, endX = x1,
                ),
                topLeft = Offset(x0, y0),
                size = Size(x1 - x0, (y1 - y0).coerceAtLeast(1f)),
            )
        }

        // --- floor pool: REMOVED (Director, Phase 15) --------------------------------------------
        // A radial-gradient oval plus a hard bright core line used to sit on FLOOR_FRACTION so the
        // figure read as PROJECTED INTO the housing rather than pasted onto the background. On
        // device it did not read as an emitter: at this aspect it read as a bright cyan disc
        // hanging in mid-screen -- a "laser disc" -- and it competed with QUARK for attention.
        // The column below her base carries the projection read on its own; FLOOR_FRACTION is
        // still the line the figure is framed standing on, it just is not drawn any more.

        // --- materialise: the scan band she resolves behind ---------------------------------------
        if (materialise.bandY >= 0f) {
            val bandH = h * MATERIALISE_BAND_HEIGHT
            val centre = h * materialise.bandY
            drawRect(
                brush = Brush.verticalGradient(
                    0f to Color.Transparent,
                    0.5f to phosphor.copy(alpha = MATERIALISE_BAND_ALPHA),
                    1f to Color.Transparent,
                    startY = centre - bandH / 2f,
                    endY = centre + bandH / 2f,
                ),
                topLeft = Offset(0f, centre - bandH / 2f),
                size = Size(w, bandH),
            )
        }

        // --- ambient scan sweep: a soft band of brighter phosphor travelling up the column -----
        if (ambient) {
            val bandH = h * SWEEP_HEIGHT
            val y0 = floorY - (floorY - columnTop + bandH) * sweep
            drawRect(
                brush = Brush.verticalGradient(
                    0f to Color.Transparent,
                    0.5f to phosphor.copy(alpha = SWEEP_ALPHA),
                    1f to Color.Transparent,
                    startY = y0,
                    endY = y0 + bandH,
                ),
                topLeft = Offset(w * 0.10f, y0),
                size = Size(w * 0.80f, bandH),
            )
        }

        // --- scanlines ---------------------------------------------------------------------------
        val step = SCANLINE_SPACING.toPx()
        var y = 0f
        while (y < h) {
            drawRect(
                color = Color.Black.copy(alpha = SCANLINE_ALPHA),
                topLeft = Offset(0f, y),
                size = Size(w, step * 0.5f),
            )
            y += step
        }

        // --- CRT falloff: fade to black at every edge, no drawn bezel ----------------------------
        // The bottom fade is DEEPER than the top. It is doing a second job now: QUARK's base runs
        // off the bottom edge, and this is what turns that from a crop into a dissolve. A projection
        // fades out where the emitter's cone runs out; it does not stop at a hard line.
        val fade = h * 0.16f
        val bottomFade = h * BOTTOM_FALLOFF
        drawRect(
            brush = Brush.verticalGradient(
                0f to Color.Black, 1f to Color.Transparent, startY = 0f, endY = fade,
            ),
        )
        drawRect(
            brush = Brush.verticalGradient(
                0f to Color.Transparent, 1f to Color.Black, startY = h - bottomFade, endY = h,
            ),
        )
        val sideFade = w * 0.14f
        drawRect(
            brush = Brush.horizontalGradient(
                0f to Color.Black, 1f to Color.Transparent, startX = 0f, endX = sideFade,
            ),
        )
        drawRect(
            brush = Brush.horizontalGradient(
                0f to Color.Transparent, 1f to Color.Black, startX = w - sideFade, endX = w,
            ),
        )

        // --- the emitter, drawn LAST ---------------------------------------------------------------
        // AFTER the CRT falloff, and that ordering is the whole point. The falloff paints black
        // over the bottom 26% of the frame and the emitter's bloom occupies the bottom 22% -- drawn
        // before it, the vignette erased the emitter almost entirely. Measured: with SPEAKING on,
        // the housing corners sat at 1.02x their silent value, which is nothing.
        //
        // It is also right rather than merely convenient. The falloff exists to fade CONTENT to
        // black at the edges; the emitter IS the edge -- it is the apparatus, not something being
        // projected -- so it has no business being vignetted by it.
        // Off-screen by design (see QuarkMaterialise). What is drawn is only the BLOOM the emitter
        // throws back up into the frame, which is why there is no ellipse and nothing that can read
        // as a disc floating in the middle of the screen -- the mistake the floor pool made.
        // The same bloom carries two things: B1's strike, and an alert state's resting burn. They
        // take the STRONGER rather than summing, so WARN during a materialise does not clip to a
        // white bar.
        // Four things can burn the emitter. They take the STRONGEST rather than summing, so a WARN
        // heard during a materialise while she is talking does not clip to a white bar.
        val bloomLevel = maxOf(
            materialise.emitterBoost,
            alert * ALERT_EMITTER,
            speakBurn * SPEAK_EMITTER,
            // The ember. Powered down is not off -- the unit is still running, quietly.
            stealth * STEALTH_EMBER,
        )
        if (bloomLevel > 0f) {
            val bloom = h * EMITTER_BLOOM
            drawRect(
                brush = Brush.verticalGradient(
                    0f to Color.Transparent,
                    1f to phosphor.copy(alpha = EMITTER_ALPHA * bloomLevel),
                    startY = h - bloom,
                    endY = h,
                ),
                topLeft = Offset(0f, h - bloom),
                size = Size(w, bloom),
            )
        }

    }
}

// Where the projection column terminates, as a fraction of viewport height. This used to be 0.82 --
// the line the figure's base stood ON, back when the base was visible. It is 1.0 now: QUARK is
// bottom-anchored and overhangs the frame, so there is no floor line to stand on and the column runs
// all the way to the edge with her. Keeping it at 0.82 would have drawn the column stopping short of
// her, which is the "cropped picture" read the new framing exists to remove.
const val FLOOR_FRACTION = 1.0f

// Measured against a face, not a silhouette: at 3.dp / 0.22 alpha the scanlines read as heavy
// black banding that destroyed the eyelashes and lip detail the head-crop splat resolves. They
// have to be present enough to say CRT and light enough to leave the art intact.
private val SCANLINE_SPACING = 4.dp
private const val SCANLINE_ALPHA = 0.10f

// ~7 s per pass. Slow enough to read as a carrier signal rather than as progress.
private const val SWEEP_PERIOD_MS = 7000
private const val SWEEP_HEIGHT = 0.16f
private const val SWEEP_ALPHA = 0.07f

// Deeper than the 0.16 used on the other three edges -- it has to dissolve QUARK's overhanging base,
// not just vignette an empty corner.
private const val BOTTOM_FALLOFF = 0.26f

// B1 materialise. The strike multiplies the emitter rather than replacing it, so at the peak the
// column is 2.6x its resting brightness -- an event, clearly, without clipping to white.
private const val STRIKE_GAIN = 1.6f

// B3 speaking. Much gentler than the strike: this runs for as long as she talks, where the strike is
// a single event, and a column that swings 2.6x while she speaks is a strobe.
private const val SPEAK_GAIN = 0.45f
private const val SPEAK_EMITTER = 0.55f

// B3 stealth. Dimmed, not off -- the house style calls Stealth "dimmed", and the ember is what keeps
// it reading as a unit running dark rather than a unit switched off.
private const val STEALTH_DIM_DEPTH = 0.80f
private const val STEALTH_COLLAPSE = 0.55f
// Tuned AFTER the emitter moved past the CRT falloff. At 0.10 it had been half-buried by the
// vignette; un-vignetted, the same value left Stealth at 72% of normal brightness -- brighter than
// it has any business being for a unit running dark. 0.035 is a presence, not a light.
private const val STEALTH_EMBER = 0.035f

// How hard the emitter burns under an alert, relative to B1's strike. Below the strike on purpose:
// the strike is a momentary event and this is a state QUARK can sit in, so it has to be unmistakable
// without being painful to look at.
private const val ALERT_EMITTER = 0.75f

// The bloom the off-screen emitter throws back into the bottom of the frame.
private const val EMITTER_BLOOM = 0.22f
private const val EMITTER_ALPHA = 0.42f

// The scan band is TALLER and BRIGHTER than the ambient sweep: the ambient one has to read as an
// idle carrier you barely notice, this one has to read as the thing that is building her.
private const val MATERIALISE_BAND_HEIGHT = 0.10f
private const val MATERIALISE_BAND_ALPHA = 0.30f

// A 4% dip over ~2.6 s -- present if you look for it, invisible if you do not.
private const val FLICKER_PERIOD_MS = 2600
private const val FLICKER_DEPTH = 0.04f
