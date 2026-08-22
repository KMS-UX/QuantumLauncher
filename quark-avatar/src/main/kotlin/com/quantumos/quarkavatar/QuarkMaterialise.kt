package com.quantumos.quarkavatar

/*
 * B1 -- the materialise transition. QUARK does not simply exist; the projection SCANS HER IN.
 *
 * Why this is house-style clean where the AMBIENT loop is a departure: this is **reactive**. It runs
 * because something happened -- the surface was opened, or a state was invoked -- and then it stops
 * dead. `frameAt(1f)` is the static presentation exactly, so once the transition has settled the
 * screen is pixel-identical to a build with no transition at all and the "zero idle redraw"
 * guarantee is unaffected.
 *
 * The emitter is at the BOTTOM EDGE and off-screen, which is the Director's call and the only one
 * that works with this art: the plates are cut at mid-thigh, so there are no feet to stand on a
 * drawn base, and a drawn base was already rejected once for reading as a disc floating in the
 * frame. An emitter below the frame edge implies the apparatus without ever drawing it.
 *
 * The sequence, in one pass:
 *
 *   0.00 -- 0.12   the emitter STRIKES: a hard bloom at the bottom edge, faster up than down
 *   0.08 -- 0.78   a scan band travels from the bottom edge up past the top of the frame
 *   0.12 -- 0.82   the figure RESOLVES behind the band, revealed bottom-up with a soft edge
 *   0.10 -- 0.90   the figure fades in from nothing to full
 *   0.72 -- 1.00   the emitter falls back to its resting level
 *
 * Kept as pure data with no Compose types so the timing is unit-testable without a device -- the
 * one thing on this track that has repeatedly needed proving is that an effect actually settles.
 */
data class MaterialiseFrame(
    /** Extra emitter glow at the bottom edge, 0 at rest and 1 at the strike's peak. */
    val emitterBoost: Float,
    /**
     * Scan-band centre as a fraction of viewport height: 1 is the bottom edge, 0 the top, and it
     * runs past both so the band enters and leaves cleanly. Negative means already gone.
     */
    val bandY: Float,
    /**
     * How much of the figure is still hidden, as a fraction of its height measured from the TOP.
     * 1 = nothing revealed yet, 0 = fully revealed.
     */
    val hidden: Float,
    /** Opacity of the figure itself. */
    val figureAlpha: Float,
) {
    /** True once nothing is left to draw differently -- the caller can skip every effect. */
    val isSettled: Boolean
        get() = emitterBoost <= 0f && hidden <= 0f && figureAlpha >= 1f && bandY < BAND_GONE
}

/** The static presentation: what the screen looks like when no transition is running. */
val MATERIALISE_SETTLED = MaterialiseFrame(
    emitterBoost = 0f, bandY = -1f, hidden = 0f, figureAlpha = 1f,
)

private const val BAND_GONE = -0.2f

fun materialiseFrameAt(progress: Float): MaterialiseFrame {
    val p = progress.coerceIn(0f, 1f)
    if (p >= 1f) return MATERIALISE_SETTLED

    // The strike is deliberately asymmetric -- fast up, slow down. A projector's emitter reaching
    // power is an event; falling back to idle is not.
    val strike = when {
        p < 0.12f -> ease(span(p, 0f, 0.12f))
        p < 0.72f -> 1f
        else -> 1f - ease(span(p, 0.72f, 1f))
    }

    val band = 1f - span(p, 0.08f, 0.78f) * 1.25f
    val hidden = 1f - ease(span(p, 0.12f, 0.82f))
    val alpha = ease(span(p, 0.10f, 0.90f))

    return MaterialiseFrame(
        emitterBoost = strike,
        // Once the band is past the top there is nothing to draw; clamping it here keeps the
        // "is it gone" test in one place rather than at every call site.
        bandY = if (band < BAND_GONE) -1f else band,
        hidden = hidden.coerceIn(0f, 1f),
        figureAlpha = alpha.coerceIn(0f, 1f),
    )
}

/** Position of [p] within [from]..[to], clamped to 0..1. */
private fun span(p: Float, from: Float, to: Float): Float =
    ((p - from) / (to - from)).coerceIn(0f, 1f)

/** Smoothstep. Stepped motion is the house rule for STATE changes; a single scan-in is a move. */
private fun ease(t: Float): Float = t * t * (3f - 2f * t)
