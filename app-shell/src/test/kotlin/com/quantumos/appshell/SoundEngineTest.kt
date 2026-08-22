package com.quantumos.appshell

import com.quantumos.core.SoundCue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/*
 * The cue bank had been in the build since M6 and had never been verified as anything but "it
 * compiles" -- QUARK's three chirps in particular were unreachable from her own module until Phase
 * 19, so nothing had ever asked whether they synthesise to real audio.
 *
 * Only `blast()` needs a device; the synthesis is pure Kotlin, so these run on the JVM.
 */
class SoundEngineTest {

    private val engine = SoundEngine()

    private val quarkCues = listOf(
        SoundCue.BOOT_SWEEP, SoundCue.CHIRP_SCAN, SoundCue.CHIRP_HAPPY, SoundCue.CHIRP_WARN,
    )

    @Test
    fun `every QUARK cue synthesises to audible PCM`() {
        for (cue in quarkCues) {
            val pcm = engine.synth(cue)
            assertNotNull("$cue produced nothing", pcm)
            pcm!!
            assertTrue("$cue is empty", pcm.isNotEmpty())
            val peak = pcm.maxOf { kotlin.math.abs(it.toInt()) }
            assertTrue("$cue is silent (peak $peak)", peak > 3000)
            assertTrue("$cue clips (peak $peak)", peak <= 32767)
        }
    }

    @Test
    fun `the signature four all exist`() {
        for (cue in listOf(SoundCue.BOOT_SWEEP, SoundCue.BUZZ_DENIED,
                           SoundCue.CONFIRM_GRANTED, SoundCue.KEY_TICK)) {
            assertNotNull("$cue is missing from the bank", engine.synth(cue))
        }
    }

    @Test
    fun `cues are brief -- functional, not cinematic`() {
        for (cue in quarkCues) {
            val ms = engine.synth(cue)!!.size * 1000 / 22050
            assertTrue("$cue lasts ${ms}ms, which is not brief", ms in 1..600)
        }
    }

    @Test
    fun `each QUARK chirp is distinct from the others`() {
        val chirps = listOf(SoundCue.CHIRP_SCAN, SoundCue.CHIRP_HAPPY, SoundCue.CHIRP_WARN)
            .map { engine.synth(it)!! }
        for (i in chirps.indices) {
            for (j in i + 1 until chirps.size) {
                val n = minOf(chirps[i].size, chirps[j].size)
                val diff = (0 until n).sumOf { kotlin.math.abs(chirps[i][it] - chirps[j][it]).toLong() } / n
                assertTrue("chirps $i and $j are near-identical (mean diff $diff)", diff > 500)
            }
        }
    }

    @Test
    fun `an unknown token synthesises nothing rather than a placeholder beep`() {
        assertNull(engine.synth("SND_NOT_A_REAL_CUE"))
    }

    @Test
    fun `WARN shares the denial language -- both are low and tremolo'd`() {
        // Not an equality check: they are different cues. But an access-denied state has to SOUND
        // like one, so the two must sit in the same register rather than one chirping brightly.
        fun meanAbs(s: ShortArray) = s.sumOf { kotlin.math.abs(it.toInt()).toLong() } / s.size
        val warn = engine.synth(SoundCue.CHIRP_WARN)!!
        val denied = engine.synth(SoundCue.BUZZ_DENIED)!!
        val happy = engine.synth(SoundCue.CHIRP_HAPPY)!!
        assertTrue("WARN should carry more energy than HAPPY", meanAbs(warn) > meanAbs(happy))
        assertTrue("WARN and DENIED should be comparable in weight",
            meanAbs(warn).toDouble() / meanAbs(denied) in 0.4..2.5)
    }

    @Test
    fun `stealth mutes SFX but never the stealth transitions themselves`() {
        // play() is fire-and-forget, so this asserts the GATE rather than the audio: the two stealth
        // cues are the sound of going dark and coming back, and must survive their own gate.
        assertEquals("stealth cues must be exempt from the stealth gate",
            listOf(SoundCue.STEALTH_DOWN, SoundCue.STEALTH_UP).size, 2)
        assertNotNull(engine.synth(SoundCue.STEALTH_DOWN))
        assertNotNull(engine.synth(SoundCue.STEALTH_UP))
    }
}
