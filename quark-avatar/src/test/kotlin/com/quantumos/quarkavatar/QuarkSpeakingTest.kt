package com.quantumos.quarkavatar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QuarkSpeakingTest {

    @Test
    fun `the cadence is stepped, not continuous`() {
        // Sample finely; a stepped signal changes value only a small number of times per loop,
        // where a sine would change on almost every sample. This is the house rule under test.
        // Sampled over the HALF-OPEN interval [0, 1): phase 1.0 is the same instant as phase 0 and
        // wrapping back to the first step would count as a 14th change in a 13-step loop.
        var changes = 0
        var previous = speakingCadenceAt(0f)
        for (i in 1 until 2000) {
            val v = speakingCadenceAt(i / 2000f)
            if (v != previous) changes++
            previous = v
        }
        assertEquals("one change per step boundary", speakingCadenceSteps - 1, changes)
    }

    @Test
    fun `it wraps rather than clamping`() {
        assertEquals(speakingCadenceAt(0.25f), speakingCadenceAt(1.25f), 1e-6f)
        assertEquals(speakingCadenceAt(0.25f), speakingCadenceAt(5.25f), 1e-6f)
        assertEquals(speakingCadenceAt(0.75f), speakingCadenceAt(-0.25f), 1e-6f)
    }

    @Test
    fun `every level is in range and the emitter never goes dark`() {
        for (i in 0..1000) {
            val v = speakingCadenceAt(i / 1000f)
            assertTrue("level $v out of range", v in 0f..1f)
            assertTrue("a projector carrying a voice is never fully off", v > 0.15f)
        }
    }

    @Test
    fun `it actually varies -- peaks and near-silences, not a flat drone`() {
        val vs = (0 until speakingCadenceSteps).map { speakingCadenceAt(it / speakingCadenceSteps.toFloat()) }
        assertTrue("must reach full burn", vs.max() >= 0.99f)
        assertTrue("must have real troughs", vs.min() <= 0.30f)
        assertTrue("must use the range", vs.max() - vs.min() > 0.6f)
    }

    @Test
    fun `the step count is prime, so the pattern does not settle into a rhythm`() {
        val n = speakingCadenceSteps
        assertTrue("step count $n must be > 1", n > 1)
        assertTrue("step count $n must be prime", (2 until n).none { n % it == 0 })
    }
}
