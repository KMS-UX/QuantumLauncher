package com.quantumos.quarkavatar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/*
 * The one property that actually matters about the materialise, and the one this track has twice
 * had to prove rather than assert: it SETTLES. QUARK is specified as static at rest with zero idle
 * redraw, so a transition that leaves any element a hair off its resting value would quietly turn
 * the avatar into an always-animating surface.
 */
class QuarkMaterialiseTest {

    @Test
    fun `settles exactly at the end`() {
        val end = materialiseFrameAt(1f)
        assertEquals(MATERIALISE_SETTLED, end)
        assertTrue(end.isSettled)
    }

    @Test
    fun `past the end stays settled`() {
        assertEquals(MATERIALISE_SETTLED, materialiseFrameAt(1.5f))
        assertEquals(MATERIALISE_SETTLED, materialiseFrameAt(Float.MAX_VALUE))
    }

    @Test
    fun `starts fully hidden and fully transparent`() {
        val start = materialiseFrameAt(0f)
        assertEquals(1f, start.hidden, 1e-6f)
        assertEquals(0f, start.figureAlpha, 1e-6f)
        assertTrue("band must start at or below the bottom edge", start.bandY >= 1f)
        assertTrue("nothing is settled at the start", !start.isSettled)
    }

    @Test
    fun `before the start clamps to the start`() {
        assertEquals(materialiseFrameAt(0f), materialiseFrameAt(-0.5f))
    }

    @Test
    fun `the figure only ever becomes more revealed`() {
        var previousHidden = Float.MAX_VALUE
        var previousAlpha = -1f
        for (i in 0..1000) {
            val f = materialiseFrameAt(i / 1000f)
            assertTrue("hidden must not increase at p=${i / 1000f}", f.hidden <= previousHidden + 1e-5f)
            assertTrue("alpha must not decrease at p=${i / 1000f}", f.figureAlpha >= previousAlpha - 1e-5f)
            previousHidden = f.hidden
            previousAlpha = f.figureAlpha
        }
    }

    @Test
    fun `the band sweeps upward and leaves`() {
        assertTrue(materialiseFrameAt(0.05f).bandY > materialiseFrameAt(0.40f).bandY)
        assertTrue(materialiseFrameAt(0.40f).bandY > materialiseFrameAt(0.70f).bandY)
        assertTrue("band is gone by the settle", materialiseFrameAt(0.95f).bandY < 0f)
    }

    @Test
    fun `the emitter strikes fast and returns to rest`() {
        val peak = materialiseFrameAt(0.3f).emitterBoost
        assertEquals("holds at full through the sweep", 1f, peak, 1e-6f)
        assertTrue("rises faster than it falls",
            materialiseFrameAt(0.06f).emitterBoost > materialiseFrameAt(0.94f).emitterBoost)
        assertEquals("back to rest", 0f, materialiseFrameAt(1f).emitterBoost, 1e-6f)
    }

    @Test
    fun `every value stays in range throughout`() {
        for (i in 0..1000) {
            val f = materialiseFrameAt(i / 1000f)
            assertTrue(f.emitterBoost in 0f..1f)
            assertTrue(f.hidden in 0f..1f)
            assertTrue(f.figureAlpha in 0f..1f)
        }
    }
}
