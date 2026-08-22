package com.quantumos.shell.ai

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.k2fsa.sherpa.onnx.GenerationConfig
import com.k2fsa.sherpa.onnx.OfflineTts
import com.k2fsa.sherpa.onnx.OfflineTtsConfig
import com.k2fsa.sherpa.onnx.OfflineTtsKokoroModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsModelConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.Executors

/**
 * Phase 2b voice engine — QUARK's own custom voice (QUARK-H2) via the sherpa-onnx Kokoro runtime.
 *
 * sherpa-onnx runs the Kokoro model AND does espeak-ng phonemization internally, which is why it —
 * not a raw onnxruntime session — is the on-device engine: it dissolves the hard on-device G2P
 * problem entirely (the audition used espeak-ng on the build machine; sherpa carries espeak-ng-data
 * on the device). See voice/quark-phase2b/HANDOFF.md for the full rationale and provisioning steps.
 *
 * How QUARK-H2 plays instead of a stock speaker: sherpa's kokoro model bakes a fixed speaker count
 * into its ONNX metadata and demands a `voices.bin` with exactly that many `[510,256]` float32
 * blocks — handing it a differently-sized file is a **fatal, uncatchable native `_Exit()`**, not a
 * Kotlin exception (verified against sherpa's C++, `offline-tts-kokoro-model.cc`). So we don't hand
 * it our lone H2 block directly: [VoiceModelProvisioner] patches a *copy of the model's own*
 * `voices.bin` — same total size, one slot overwritten with our owned H2 embedding — and reports
 * back the `sid` that slot lives at ([h2Sid]).
 *
 * Two things must be present on-device for this to go READY (else it reports UNAVAILABLE and the
 * runtime falls back to the Phase 2a placeholder — the voice loop never goes mute):
 *   1. The sherpa Kokoro model dir in filesDir (`model.onnx`, `tokens.txt`, `espeak-ng-data/`,
 *      `voices.bin`), provisioned by [VoiceModelProvisioner]. Not bundled (~large); fetched
 *      on-device.
 *   2. The sherpa native libs (`libsherpa-onnx-jni.so`) in the APK's jniLibs — added from the
 *      pinned v1.13.2 android release tarball at build time (see HANDOFF.md). Their absence is
 *      caught here as an UnsatisfiedLinkError → UNAVAILABLE, so a build without them still runs.
 */
class SherpaKokoroVoiceEngine(context: Context) : VoiceEngine {

    private val appContext = context.applicationContext

    private val _readyState = MutableStateFlow(VoiceReadyState.INITIALIZING)
    override val readyState: StateFlow<VoiceReadyState> = _readyState.asStateFlow()
    override val isReady get() = _readyState.value == VoiceReadyState.READY

    override val engineLabel = "QUARK-H2"

    // Why this engine last went quiet (see VoiceEngine.lastFault). Written from the worker thread,
    // read from the runtime's voice observer on Main — @Volatile, not a lock: it is a diagnostic
    // string, and a stale read costs a slightly-late LOG line, nothing more.
    @Volatile private var _lastFault: String = ""
    override val lastFault get() = _lastFault

    private fun fault(what: String, t: Throwable? = null) {
        _lastFault = if (t == null) what
        else "$what // ${t.javaClass.simpleName}: ${t.message.orEmpty().take(120)}"
    }

    private val worker = Executors.newSingleThreadExecutor()
    private var tts: OfflineTts? = null
    private var sampleRate = 24_000
    private var h2Sid = 0   // set in initialise() from VoiceModelProvisioner's patched voices file
    @Volatile private var track: AudioTrack? = null
    @Volatile private var currentId = 0L

    // Gain the last utterance needed to reach speaking level (see levelise()). Reported in the LOG
    // so a "still too quiet" report can be answered with a number instead of another guess.
    @Volatile private var lastGain = 1f
    override val lastLevelInfo get() = "GAIN " + String.format("%.1f", lastGain) + "x"

    init {
        worker.execute { initialise() }
    }

    private fun initialise() {
        try {
            val status = VoiceModelProvisioner.status(appContext)
            // The patched voices file is the model's own voices.bin with one slot overwritten by our
            // owned H2 embedding — required so its float count still matches what the model's ONNX
            // metadata demands (a mismatch there is a fatal, uncatchable native exit, not a Kotlin
            // exception — see VoiceModelProvisioner's doc comment).
            val voices = VoiceModelProvisioner.ensureVoicesFile(appContext)
            if (!status.modelReady || voices == null) {
                // Two distinct causes that used to look identical from outside: the model dir is
                // incomplete, or it is complete but voices.bin could not be patched (wrong size,
                // missing H2 asset, unwritable filesDir).
                fault(
                    if (!status.modelReady) "MODEL INCOMPLETE // ${status.missing.joinToString(" ")}"
                    else "VOICES PATCH FAILED"
                )
                _readyState.value = VoiceReadyState.UNAVAILABLE
                return
            }
            h2Sid = voices.sid
            val config = OfflineTtsConfig(
                model = OfflineTtsModelConfig(
                    kokoro = OfflineTtsKokoroModelConfig(
                        model = status.modelPath,
                        voices = voices.file.absolutePath,
                        tokens = status.tokensPath,
                        dataDir = status.dataDirPath,
                        lang = "en-us",
                    ),
                    // sherpa's own Kokoro examples use 4 threads (vs. 2 default) since the model
                    // benefits from more parallelism than most sherpa models; the Fold 6's SoC has
                    // cores to spare. Confirmed via Fold 6 latency test to shrink inference time
                    // (2026-07-04 Director test: ~7000ms cold @ 2 threads).
                    numThreads = 4,
                    debug = false,
                    provider = "cpu",
                ),
            )
            // Touching OfflineTts triggers System.loadLibrary("sherpa-onnx-jni"); if the native libs
            // aren't in the APK this throws and we fall back rather than crash.
            val engine = OfflineTts(assetManager = null, config = config)
            sampleRate = engine.sampleRate()
            tts = engine
            _lastFault = ""
            _readyState.value = VoiceReadyState.READY
        } catch (t: Throwable) {
            // The big one: UnsatisfiedLinkError here means the sherpa JNI libs never made it into
            // the APK, which presents on-device as "H2 selected, model imported, still not her
            // voice" — indistinguishable by ear from a dozen other causes until now.
            fault("ENGINE INIT FAILED", t)
            _readyState.value = VoiceReadyState.UNAVAILABLE
        }
    }

    override fun warmUp() {
        worker.execute {
            if (!isReady) return@execute
            // Throwaway synth so the first real line pays no cold cost (hides in the Scan beat).
            runCatching { tts?.generateWithConfig(".", GenerationConfig(sid = h2Sid, speed = SPEED)) }
        }
    }

    override fun speak(text: String, onStart: (Long) -> Unit, onDone: () -> Unit) {
        if (!isReady) {
            // A mute return. Keep whatever initialise() recorded if it is still the live reason;
            // otherwise say plainly that the engine was not ready when the line came in.
            if (_lastFault.isEmpty()) fault("NOT READY // ${_readyState.value}")
            onDone(); return
        }
        val id = System.nanoTime()
        currentId = id
        worker.execute {
            try {
                val audio = tts?.generateWithConfig(text, GenerationConfig(sid = h2Sid, speed = SPEED))
                if (audio == null) { fault("SYNTH RETURNED NOTHING"); return@execute }
                if (audio.samples.isEmpty()) { fault("SYNTH RETURNED 0 SAMPLES"); return@execute }
                if (currentId != id) return@execute            // superseded by a newer line
                playBlocking(audio.samples, audio.sampleRate, id, onStart)
            } catch (t: Throwable) {
                // Still swallowed — a failed line must never crash the field unit or leave the
                // reactive posture stuck — but no longer swallowed *silently*.
                fault("SYNTH FAILED", t)
            } finally {
                if (currentId == id) onDone()
            }
        }
    }

    /*
     * Bring one utterance up to a consistent speaking level, in place.
     *
     * Why this is needed at all (Fold 6, 2026-08-22): QUARK-H2 came out at roughly 0.3x the loudness
     * of the Android TTS placeholder. It is not a routing or stream-volume difference — both land on
     * the same output — it is the samples themselves: Kokoro renders well below full scale, and we
     * were writing its floats to the track verbatim while Android's TTS engine does its own levelling
     * on the way out. So we do the levelling it does not.
     *
     * RMS-targeted, not a fixed multiplier: a constant gain would fix this one model and break on the
     * next voice, and would still leave loud and quiet lines uneven. Target an RMS typical of speech,
     * then clamp so the loudest peak still lands under full scale — so it never clips, which on a
     * voice reads as harsh crackle and would be a worse defect than being quiet. MAX_GAIN keeps near-
     * silence (a breath, a trailing consonant) from being amplified into hiss.
     *
     * Returns the gain actually applied, for the LOG line — so "is it still quiet" is answerable with
     * a number instead of by ear.
     */
    private fun levelise(samples: FloatArray): Float {
        var peak = 0f
        var sumSquares = 0.0
        for (s in samples) {
            val a = kotlin.math.abs(s)
            if (a > peak) peak = a
            sumSquares += (s.toDouble() * s.toDouble())
        }
        if (peak < 1e-4f) return 1f                      // effectively silence — leave it alone
        val rms = kotlin.math.sqrt(sumSquares / samples.size).toFloat()
        if (rms < 1e-5f) return 1f
        val wanted = TARGET_RMS / rms
        val ceiling = PEAK_CEILING / peak                // never let the loudest sample clip
        val gain = kotlin.math.min(kotlin.math.min(wanted, ceiling), MAX_GAIN)
        if (gain in 0.99f..1.01f) return 1f              // already at level — skip the pass entirely
        for (i in samples.indices) samples[i] = samples[i] * gain
        return gain
    }

    private fun playBlocking(samples: FloatArray, sr: Int, id: Long, onStart: (Long) -> Unit) {
        if (samples.isEmpty()) return
        lastGain = levelise(samples)
        val minBuf = AudioTrack.getMinBufferSize(
            sr, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_FLOAT
        ).coerceAtLeast(samples.size * 4)
        val t = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANT)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                    .setSampleRate(sr)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(minBuf)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
        track = t
        try {
            t.play()
            onStart(System.currentTimeMillis())
            var off = 0
            while (off < samples.size && currentId == id) {
                val n = t.write(samples, off, samples.size - off, AudioTrack.WRITE_BLOCKING)
                if (n <= 0) break
                off += n
            }
            // WRITE_BLOCKING only blocks while the internal buffer is full — with the buffer sized to
            // hold the whole clip (above), the loop above returns almost instantly after one big memcpy,
            // well before the hardware has actually rendered any of it. AudioTrack.stop() on a streaming
            // track halts immediately rather than draining the buffer, so calling it right after the
            // write loop cut playback off within a millisecond of starting. Poll playbackHeadPosition
            // (frames actually rendered) until it reaches what we wrote — mono, so frames == samples —
            // with a generous timeout as a safety net against a device that never reports completion.
            if (currentId == id) {
                val deadline = System.currentTimeMillis() + (samples.size * 1000L / sr) + 2_000L
                while (currentId == id &&
                    t.playbackHeadPosition < off &&
                    System.currentTimeMillis() < deadline
                ) {
                    Thread.sleep(20)
                }
            }
        } catch (t2: Throwable) {
            fault("PLAYBACK FAILED", t2)
        } finally {
            // The track is created, used, and destroyed on the worker thread and nowhere else — see
            // stop() below for why that ownership rule matters. `finally` so a throw anywhere above
            // can never leak an AudioTrack: the platform caps how many a process may hold open, and
            // a leak per utterance would eventually silence the voice permanently with no error.
            runCatching { t.stop() }
            runCatching { t.release() }
            if (track === t) track = null
        }
    }

    /*
     * Signal-only stop. This is called from the main thread (QuantumRuntime.stopCurrentSpeech, on
     * every QUARK close/STOW) while the worker thread may be mid-write or mid-poll on the SAME
     * AudioTrack — so the old implementation's release() here was a use-after-release race against
     * playBlocking's own `t.playbackHeadPosition` / `t.stop()` / `t.release()`, i.e. a double free.
     *
     * Fix: main thread only signals (currentId) and pauses+flushes, which is what stopping actually
     * needs to achieve — audio goes quiet immediately. Destruction stays with the thread that owns
     * the track, in playBlocking's finally. One owner, no race.
     */
    override fun stop() {
        currentId = 0L
        runCatching { track?.pause(); track?.flush() }
    }

    override fun shutdown() {
        stop()
        worker.execute {
            runCatching { tts?.release() }
            tts = null
        }
        worker.shutdown()
        _readyState.value = VoiceReadyState.UNAVAILABLE
    }

    companion object {
        private const val SPEED = 1.02f   // locked QUARK-H2 pace (see voice/quark-phase2b/)

        // Output levelling — see levelise(). TARGET_RMS is a normal speech level for float PCM;
        // PEAK_CEILING leaves ~0.3 dB of headroom so the loudest sample never clips; MAX_GAIN caps
        // how far a quiet clip may be lifted so silence is not amplified into hiss.
        private const val TARGET_RMS = 0.14f
        private const val PEAK_CEILING = 0.97f
        private const val MAX_GAIN = 8f

        /**
         * Race-free precondition for choosing this engine over the placeholder: the sherpa Kokoro
         * model dir is provisioned. (Native-lib presence can only be known by trying to construct,
         * which init handles — a missing lib degrades to UNAVAILABLE, not a crash.)
         */
        fun isSupported(context: Context): Boolean =
            VoiceModelProvisioner.status(context).modelReady
    }
}
