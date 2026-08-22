package com.quantumos.quarkavatar

/*
 * B3 -- what SPEAKING looks like.
 *
 * What it replaced: a stroked Compose circle expanding from the centre of the screen, inherited from
 * the pre-rendered-frames path. It spoke none of the housing's language -- it was not a projection
 * artefact, it had no relationship to the emitter or the column, and it was the only round thing on
 * the surface.
 *
 * What it is now: **the emitter works harder while she talks.** The apparatus at the bottom edge
 * burns with her cadence and the column brightens with it, so the projector is visibly under load.
 * That keeps the Phase 13 principle intact -- the APPARATUS moves, QUARK does not -- and it needs no
 * new drawn element, because B1 already established the emitter as where the machine is.
 *
 * The cadence is a **stepped table, not a sine**. A sine reads as a smooth ambient throb, which is
 * both the wrong feeling and against the house rule that motion is "stepped, not interpolated". A
 * fixed table of levels held for equal slices reads as a machine keying a signal: irregular enough
 * to be speech, mechanical enough to be this OS. It is also deterministic, so it is testable and it
 * looks the same on every run.
 */

// Thirteen levels, held in turn. Thirteen because a prime number of steps against the animation's
// own loop keeps the pattern from settling into an audible-looking rhythm. The values are hand-set
// rather than random: peaks and near-silences in the proportion speech actually has, never resting
// at zero because a projector carrying a voice is never fully off.
private val CADENCE = floatArrayOf(
    0.92f, 0.34f, 0.78f, 1.00f, 0.22f, 0.61f, 0.97f,
    0.30f, 0.83f, 0.45f, 1.00f, 0.26f, 0.70f,
)

/**
 * How long one full pass through the cadence table takes.
 *
 * Slowed 1.4x from 1300 ms on the Director's call after seeing it on device — at the original rate
 * the emitter read as agitated rather than as a projector carrying a voice.
 */
const val SPEAK_CYCLE_MS = 1820

/**
 * The emitter's burn level at [phase] through the cadence loop, in 0..1.
 *
 * [phase] wraps, so a caller can drive it from a plain looping animation without normalising.
 */
fun speakingCadenceAt(phase: Float): Float {
    val wrapped = phase - kotlin.math.floor(phase)
    val i = (wrapped * CADENCE.size).toInt().coerceIn(0, CADENCE.size - 1)
    return CADENCE[i]
}

/** The number of discrete steps in one loop -- exposed so tests do not hard-code it. */
val speakingCadenceSteps: Int get() = CADENCE.size
