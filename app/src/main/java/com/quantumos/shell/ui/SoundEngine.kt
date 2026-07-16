package com.quantumos.shell.ui

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import com.quantumos.core.SoundCue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.sin
import kotlin.random.Random

/*
 * QuantumOS — SoundEngine (M6 Step 4). The audio twin of the motion language: short, functional,
 * PROCEDURALLY SYNTHESISED cues — relays, mechanical keys, phosphor beeps, CRT-era tones — the same
 * spirit as the Web-Audio synthesis in the HTML prototypes. NOT professionally produced audio files;
 * that's a future identity/polish refinement, explicitly out of scope (brief Step 4 / hard stop).
 *
 * Every audio token the engine + scripted library emit since M0 finally produces a distinct, audible
 * cue here. Nothing is a placeholder beep standing in for all of them — each signature has its own
 * synthesis recipe. No cinematic swells, no orchestral pads (house style).
 *
 * Stealth gate: while Stealth is engaged the app's own SFX are muted (the M3/M5 isStealthMode flag
 * finally has teeth) — EXCEPT the two stealth transition cues themselves, which ARE the sound of
 * going dark / coming back and must be heard.
 */
class SoundEngine {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // gain (0..1, default full): Launcher Restructure Phase 2 — the gear-reel's detent click scales
    // its own loudness with spin speed (ratchet feel, Build Brief §Phase 2). Every other call site
    // keeps the default and is unaffected.
    fun play(token: String, stealth: Boolean, gain: Float = 1f) {
        if (stealth && token != SoundCue.STEALTH_DOWN && token != SoundCue.STEALTH_UP) return
        scope.launch {
            val samples = runCatching { synth(token) }.getOrNull() ?: return@launch
            runCatching { blast(applyGain(samples, gain)) }
        }
    }

    private fun applyGain(samples: ShortArray, gain: Float): ShortArray {
        if (gain >= 0.999f) return samples
        val g = gain.coerceIn(0f, 1f)
        return ShortArray(samples.size) { (samples[it] * g).toInt().toShort() }
    }

    // ---------- the cue bank: one recipe per token ----------
    private fun synth(token: String): ShortArray? {
        val t = Track(durMsFor(token))
        when (token) {
            // Boot = warm power-up sweep ("system alive").
            SoundCue.POWER_ON_FLASH -> {
                t.tone(0, 220, 60.0, 520.0, amp = 0.5, wave = Wave.SAW, atk = 2, rel = 60)
                t.tone(0, 220, 120.0, 1040.0, amp = 0.18, wave = Wave.SINE, atk = 2, rel = 60)
            }
            SoundCue.BOOT_SWEEP -> {
                t.tone(0, 480, 196.0, 880.0, amp = 0.42, wave = Wave.SINE, atk = 8, rel = 90)
                t.tone(0, 480, 98.0, 440.0, amp = 0.20, wave = Wave.SAW, atk = 8, rel = 90)
                t.tone(360, 120, 1320.0, 1320.0, amp = 0.14, wave = Wave.SINE, atk = 6, rel = 80)
            }
            // Keypad / boot-step = tight high relay tick.
            SoundCue.KEY_TICK -> {
                t.tone(0, 18, 2100.0, 1700.0, amp = 0.4, wave = Wave.SQUARE, atk = 1, rel = 10)
                t.tone(0, 10, 140.0, 80.0, amp = 0.25, wave = Wave.NOISE, atk = 0, rel = 8)
            }
            // UI-select = short low clunk.
            SoundCue.UI_CLUNK -> {
                t.tone(0, 70, 180.0, 110.0, amp = 0.4, wave = Wave.SQUARE, atk = 1, rel = 50)
            }
            // Access Denied = harsh low buzz, tremolo'd (shares QUARK's Warn language).
            SoundCue.BUZZ_DENIED -> {
                t.tone(0, 300, 110.0, 96.0, amp = 0.42, wave = Wave.SQUARE, atk = 2, rel = 30, tremoloHz = 22.0)
            }
            // Access Granted = crisp two-note + soft sub.
            SoundCue.CONFIRM_GRANTED -> {
                t.tone(0, 110, 660.0, 660.0, amp = 0.34, wave = Wave.SINE, atk = 3, rel = 30)
                t.tone(110, 150, 990.0, 990.0, amp = 0.34, wave = Wave.SINE, atk = 3, rel = 60)
                t.tone(0, 260, 110.0, 110.0, amp = 0.16, wave = Wave.SINE, atk = 6, rel = 80)
            }
            // Phosphor retune = quick shimmering sweep.
            SoundCue.SWEEP_PHOSPHOR -> {
                t.tone(0, 160, 420.0, 1200.0, amp = 0.30, wave = Wave.SINE, atk = 4, rel = 60)
                t.tone(0, 160, 840.0, 2400.0, amp = 0.10, wave = Wave.SINE, atk = 4, rel = 60)
            }
            SoundCue.STEALTH_DOWN -> t.tone(0, 260, 760.0, 150.0, amp = 0.34, wave = Wave.SINE, atk = 4, rel = 90)
            SoundCue.STEALTH_UP -> t.tone(0, 260, 150.0, 760.0, amp = 0.34, wave = Wave.SINE, atk = 4, rel = 70)
            // Beacon = three short warn-blips.
            SoundCue.BLIP_BEACON -> {
                t.tone(0, 60, 1000.0, 1000.0, amp = 0.34, wave = Wave.SQUARE, atk = 2, rel = 20)
                t.tone(120, 60, 1000.0, 1000.0, amp = 0.34, wave = Wave.SQUARE, atk = 2, rel = 20)
                t.tone(240, 60, 1000.0, 1000.0, amp = 0.34, wave = Wave.SQUARE, atk = 2, rel = 20)
            }
            // Device-secured = a low latch (thunk + click).
            SoundCue.DEVICE_SECURED -> {
                t.tone(0, 110, 200.0, 120.0, amp = 0.40, wave = Wave.SQUARE, atk = 1, rel = 50)
                t.tone(90, 40, 1600.0, 1400.0, amp = 0.26, wave = Wave.SQUARE, atk = 1, rel = 30)
            }
            // PLEASE STANDBY = soft processing pulse.
            SoundCue.STANDBY_PULSE -> {
                t.tone(0, 280, 330.0, 330.0, amp = 0.28, wave = Wave.SINE, atk = 6, rel = 80, tremoloHz = 9.0)
            }
            // QUARK non-verbal chirps — wordless, distinct from her spoken voice.
            SoundCue.CHIRP_SCAN -> t.tone(0, 150, 600.0, 1050.0, amp = 0.30, wave = Wave.SINE, atk = 4, rel = 50)
            SoundCue.CHIRP_HAPPY -> {
                t.tone(0, 90, 880.0, 880.0, amp = 0.30, wave = Wave.SINE, atk = 3, rel = 30)
                t.tone(90, 130, 1320.0, 1320.0, amp = 0.30, wave = Wave.SINE, atk = 3, rel = 50)
                t.tone(150, 90, 1980.0, 1980.0, amp = 0.12, wave = Wave.SINE, atk = 2, rel = 60) // sparkle
            }
            SoundCue.CHIRP_WARN -> {
                t.tone(0, 120, 300.0, 280.0, amp = 0.38, wave = Wave.SQUARE, atk = 2, rel = 30, tremoloHz = 20.0)
                t.tone(130, 120, 240.0, 220.0, amp = 0.38, wave = Wave.SQUARE, atk = 2, rel = 40, tremoloHz = 20.0)
            }
            // APPS page-step click (v5 nav buttons) — a tight mechanical click as the page seats.
            SoundCue.REEL_DETENT -> {
                t.tone(0, 14, 1200.0, 800.0, amp = 0.34, wave = Wave.SQUARE, atk = 1, rel = 10)
                t.tone(0, 10, 130.0, 80.0, amp = 0.18, wave = Wave.NOISE, atk = 0, rel = 6)
            }
            // Legacy token aliases (kept so older emit sites still sound through one bank).
            "SND_POWER_UP_SWEEP" -> return synth(SoundCue.BOOT_SWEEP)
            "SND_SECURING_BEAT" -> return synth(SoundCue.DEVICE_SECURED)
            else -> return null
        }
        return t.toShorts()
    }

    private fun durMsFor(token: String): Int = when (token) {
        SoundCue.BOOT_SWEEP -> 500
        SoundCue.CONFIRM_GRANTED, SoundCue.STANDBY_PULSE -> 300
        SoundCue.BLIP_BEACON, SoundCue.BUZZ_DENIED, SoundCue.STEALTH_DOWN, SoundCue.STEALTH_UP -> 320
        SoundCue.CHIRP_HAPPY, SoundCue.CHIRP_WARN -> 260
        SoundCue.POWER_ON_FLASH, SoundCue.SWEEP_PHOSPHOR, SoundCue.CHIRP_SCAN -> 240
        SoundCue.DEVICE_SECURED -> 160
        SoundCue.KEY_TICK, SoundCue.UI_CLUNK -> 90
        SoundCue.REEL_DETENT -> 14
        else -> 300
    }

    // ---------- playback ----------
    private fun blast(samples: ShortArray) {
        if (samples.isEmpty()) return
        val track = AudioTrack(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
            AudioFormat.Builder()
                .setSampleRate(SR)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build(),
            samples.size * 2,
            AudioTrack.MODE_STATIC,
            AudioManager.AUDIO_SESSION_ID_GENERATE
        )
        if (track.state != AudioTrack.STATE_INITIALIZED) {
            runCatching { track.release() }
            return
        }
        track.write(samples, 0, samples.size)
        track.setNotificationMarkerPosition(samples.size)
        track.setPlaybackPositionUpdateListener(object : AudioTrack.OnPlaybackPositionUpdateListener {
            override fun onMarkerReached(t: AudioTrack?) { runCatching { t?.stop(); t?.release() } }
            override fun onPeriodicNotification(t: AudioTrack?) {}
        })
        track.play()
    }

    // ---------- synthesis primitives ----------
    private enum class Wave { SINE, SQUARE, SAW, NOISE }

    private inner class Track(durMs: Int) {
        val data = DoubleArray((SR.toLong() * durMs / 1000).toInt().coerceAtLeast(1))

        fun tone(
            atMs: Int, durMs: Int, f0: Double, f1: Double,
            amp: Double, wave: Wave, atk: Int, rel: Int, tremoloHz: Double = 0.0
        ) {
            val start = SR * atMs / 1000
            val n = (SR * durMs / 1000).coerceAtLeast(1)
            val atkN = (SR * atk / 1000).coerceAtLeast(1)
            val relN = (SR * rel / 1000).coerceAtLeast(1)
            var phase = 0.0
            for (i in 0 until n) {
                val idx = start + i
                if (idx >= data.size) break
                val frac = if (n > 1) i.toDouble() / (n - 1) else 0.0
                val f = f0 + (f1 - f0) * frac
                phase += 2.0 * PI * f / SR
                val raw = when (wave) {
                    Wave.SINE -> sin(phase)
                    Wave.SQUARE -> if (sin(phase) >= 0.0) 1.0 else -1.0
                    Wave.SAW -> { val p = phase / (2.0 * PI); 2.0 * (p - floor(p + 0.5)) }
                    Wave.NOISE -> Random.nextDouble(-1.0, 1.0)
                }
                var env = when {
                    i < atkN -> i.toDouble() / atkN
                    i > n - relN -> (n - i).toDouble() / relN
                    else -> 1.0
                }.coerceIn(0.0, 1.0)
                if (tremoloHz > 0.0) env *= 0.55 + 0.45 * sin(2.0 * PI * tremoloHz * i / SR)
                data[idx] += raw * amp * env
            }
        }

        fun toShorts(): ShortArray {
            var peak = 0.0
            for (v in data) peak = max(peak, abs(v))
            // Limit if we clipped; never boost a quiet cue louder than authored.
            val gain = if (peak > 0.95) 0.95 / peak else 1.0
            val out = ShortArray(data.size)
            for (i in data.indices) {
                val v = (data[i] * gain).coerceIn(-1.0, 1.0)
                out[i] = (v * 32767.0).toInt().toShort()
            }
            return out
        }
    }

    companion object {
        private const val SR = 22050
    }
}
