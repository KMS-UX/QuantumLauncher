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
 * How QUARK-H2 plays instead of a stock speaker: sherpa's `voices.bin` is float32
 * `[num_speakers, 510, 256]` and `sid` selects the speaker block (verified against sherpa's C++).
 * Our locked H2 embedding is exactly one such block, so we point `voices` at it and use `sid = 0` —
 * QUARK-H2 is speaker 0, no stock voice involved.
 *
 * Two things must be present on-device for this to go READY (else it reports UNAVAILABLE and the
 * runtime falls back to the Phase 2a placeholder — the voice loop never goes mute):
 *   1. The sherpa Kokoro model dir in filesDir (`model.onnx`, `tokens.txt`, `espeak-ng-data/`),
 *      provisioned by [VoiceModelProvisioner]. Not bundled (~large); fetched on-device.
 *   2. The sherpa native libs (`libsherpa-onnx-jni.so`) in the APK's jniLibs — added from the
 *      pinned v1.13.2 android release tarball at build time (see HANDOFF.md). Their absence is
 *      caught here as an UnsatisfiedLinkError → UNAVAILABLE, so a build without them still runs.
 */
class SherpaKokoroVoiceEngine(context: Context) : VoiceEngine {

    private val appContext = context.applicationContext

    private val _readyState = MutableStateFlow(VoiceReadyState.INITIALIZING)
    override val readyState: StateFlow<VoiceReadyState> = _readyState.asStateFlow()
    override val isReady get() = _readyState.value == VoiceReadyState.READY

    private val worker = Executors.newSingleThreadExecutor()
    private var tts: OfflineTts? = null
    private var sampleRate = 24_000
    @Volatile private var track: AudioTrack? = null
    @Volatile private var currentId = 0L

    init {
        worker.execute { initialise() }
    }

    private fun initialise() {
        try {
            // The owned H2 embedding ships as an asset; copy it into place as sherpa's voices file.
            val voices = VoiceModelProvisioner.ensureVoicesFile(appContext)
            val status = VoiceModelProvisioner.status(appContext)
            if (!status.modelReady || voices == null) {
                _readyState.value = VoiceReadyState.UNAVAILABLE
                return
            }
            val config = OfflineTtsConfig(
                model = OfflineTtsModelConfig(
                    kokoro = OfflineTtsKokoroModelConfig(
                        model = status.modelPath,
                        voices = voices.absolutePath,
                        tokens = status.tokensPath,
                        dataDir = status.dataDirPath,
                        lang = "en-us",
                    ),
                    numThreads = 2,   // tune during the Fold 6 latency pass
                    debug = false,
                    provider = "cpu",
                ),
            )
            // Touching OfflineTts triggers System.loadLibrary("sherpa-onnx-jni"); if the native libs
            // aren't in the APK this throws and we fall back rather than crash.
            val engine = OfflineTts(assetManager = null, config = config)
            sampleRate = engine.sampleRate()
            tts = engine
            _readyState.value = VoiceReadyState.READY
        } catch (t: Throwable) {
            _readyState.value = VoiceReadyState.UNAVAILABLE
        }
    }

    override fun warmUp() {
        worker.execute {
            if (!isReady) return@execute
            // Throwaway synth so the first real line pays no cold cost (hides in the Scan beat).
            runCatching { tts?.generateWithConfig(".", GenerationConfig(sid = 0, speed = SPEED)) }
        }
    }

    override fun speak(text: String, onStart: (Long) -> Unit, onDone: () -> Unit) {
        if (!isReady) { onDone(); return }
        val id = System.nanoTime()
        currentId = id
        worker.execute {
            try {
                val audio = tts?.generateWithConfig(text, GenerationConfig(sid = 0, speed = SPEED))
                    ?: return@execute
                if (currentId != id) return@execute            // superseded by a newer line
                playBlocking(audio.samples, audio.sampleRate, id, onStart)
            } catch (_: Throwable) {
                // never leave the reactive posture stuck
            } finally {
                if (currentId == id) onDone()
            }
        }
    }

    private fun playBlocking(samples: FloatArray, sr: Int, id: Long, onStart: (Long) -> Unit) {
        if (samples.isEmpty()) return
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
        t.play()
        onStart(System.currentTimeMillis())
        var off = 0
        while (off < samples.size && currentId == id) {
            val n = t.write(samples, off, samples.size - off, AudioTrack.WRITE_BLOCKING)
            if (n <= 0) break
            off += n
        }
        if (currentId == id) t.stop()   // blocks until the buffered tail drains
        t.release()
        if (track === t) track = null
    }

    override fun stop() {
        currentId = 0L
        track?.let { runCatching { it.pause(); it.flush(); it.release() } }
        track = null
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

        /**
         * Race-free precondition for choosing this engine over the placeholder: the sherpa Kokoro
         * model dir is provisioned. (Native-lib presence can only be known by trying to construct,
         * which init handles — a missing lib degrades to UNAVAILABLE, not a crash.)
         */
        fun isSupported(context: Context): Boolean =
            VoiceModelProvisioner.status(context).modelReady
    }
}
