package com.quantumos.shell.ai

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import java.io.File
import java.nio.FloatBuffer
import java.nio.LongBuffer
import java.util.concurrent.Executors

/**
 * Phase 2b voice engine — QUARK's own custom voice (candidate "QUARK-H2") on a Kokoro ONNX runtime.
 *
 * This is the on-device integration *scaffold* for the locked voice. It carries the full pipeline —
 * phonemize → tokenize → ONNX inference → AudioTrack playback — against the real kokoro-v1.0 graph:
 *
 *     inputs : tokens int64[1, seq]   style float[1, 256]   speed float[1]
 *     output : audio  float[audio_length]  @ 24 kHz
 *
 * Two pieces are deliberately left as marked seams because they need the Fold 6 / build assets:
 *   1. The model weights (`kokoro-v1.0.onnx`, ~325 MB) are NOT bundled — they are fetched to
 *      filesDir at first run (same pattern as the Phase 1 Gemma model's PICK-FILE acquisition).
 *   2. On-device phonemization ([Phonemizer]) — the audition used espeak-ng on the build machine;
 *      the default [UnavailablePhonemizer] returns null, so until a G2P is wired this engine reports
 *      UNAVAILABLE and the runtime falls back to the Phase 2a placeholder. Nothing goes mute.
 *
 * The voice itself — the QUARK-H2 blend — is already owned and locked: its embedding ships as the
 * asset `quark_voice/quark_voice_H2.f32` (see voice/quark-phase2b/ for the recipe). Only the runtime
 * plumbing above is pending hardware.
 */
class KokoroVoiceEngine(
    context: Context,
    private val phonemizer: Phonemizer = UnavailablePhonemizer,
) : VoiceEngine {

    private val appContext = context.applicationContext

    private val _readyState = MutableStateFlow(VoiceReadyState.INITIALIZING)
    override val readyState: StateFlow<VoiceReadyState> = _readyState.asStateFlow()
    override val isReady get() = _readyState.value == VoiceReadyState.READY

    // All ONNX + audio work runs on one dedicated thread: inference is CPU-heavy and AudioTrack
    // writes block, and serialising keeps a fresh utterance from racing a still-playing one.
    private val worker = Executors.newSingleThreadExecutor()

    private var env: OrtEnvironment? = null
    private var session: OrtSession? = null
    private var vocab: Map<Char, Long> = emptyMap()
    private var styleRows: Array<FloatArray> = emptyArray()   // [510][256] — one row per phoneme count
    @Volatile private var track: AudioTrack? = null
    @Volatile private var currentId = 0L

    init {
        worker.execute { initialise() }
    }

    /** Load vocab + embedding from assets and open the ONNX session. Any failure → UNAVAILABLE. */
    private fun initialise() {
        try {
            vocab = loadVocab()
            styleRows = loadStyleRows()
            val modelFile = modelPath()
            if (!modelFile.exists()) {
                // Voice is owned; only the (large) weights aren't fetched yet. Not an error.
                _readyState.value = VoiceReadyState.UNAVAILABLE
                return
            }
            val e = OrtEnvironment.getEnvironment()
            val opts = OrtSession.SessionOptions().apply {
                setIntraOpNumThreads(2)   // conservative on a phone; tune during the Fold 6 pass
            }
            session = e.createSession(modelFile.absolutePath, opts)
            env = e

            // A phonemizer is required to produce real tokens. Without one we can open the session
            // but can't speak, so stay UNAVAILABLE rather than advertise a voice we can't render.
            _readyState.value =
                if (phonemizer.phonemize("test") != null) VoiceReadyState.READY
                else VoiceReadyState.UNAVAILABLE
        } catch (t: Throwable) {
            _readyState.value = VoiceReadyState.UNAVAILABLE
        }
    }

    override fun warmUp() {
        worker.execute {
            if (!isReady) return@execute
            try {
                // Throwaway inference so the first real line pays no cold cost (hides in the Scan beat).
                runInference(longArrayOf(0, 0), styleRow(0))
            } catch (_: Throwable) { /* warming is best-effort */ }
        }
    }

    override fun speak(text: String, onStart: (Long) -> Unit, onDone: () -> Unit) {
        if (!isReady) { onDone(); return }
        val id = System.nanoTime()
        currentId = id
        worker.execute {
            try {
                val phonemes = phonemizer.phonemize(text)
                if (phonemes == null) { onDone(); return@execute }

                val ids = phonemes.mapNotNull { vocab[it] }              // phoneme chars → token ids
                val styleRow = styleRow(ids.size)                        // Kokoro indexes style by count
                val tokens = LongArray(ids.size + 2)                     // 0-padded bookends
                for (i in ids.indices) tokens[i + 1] = ids[i]

                val audio = runInference(tokens, styleRow)
                if (currentId != id) return@execute                     // superseded by a newer line
                playBlocking(audio, id, onStart)
            } catch (_: Throwable) {
                // never leave the reactive posture stuck
            } finally {
                if (currentId == id) onDone()
            }
        }
    }

    /** Run the kokoro graph for one utterance and return the raw 24 kHz mono float PCM. */
    private fun runInference(tokens: LongArray, style: FloatArray): FloatArray {
        val e = env ?: return FloatArray(0)
        val s = session ?: return FloatArray(0)
        val tokenT = OnnxTensor.createTensor(e, LongBuffer.wrap(tokens), longArrayOf(1, tokens.size.toLong()))
        val styleT = OnnxTensor.createTensor(e, FloatBuffer.wrap(style), longArrayOf(1, STYLE_DIM.toLong()))
        val speedT = OnnxTensor.createTensor(e, FloatBuffer.wrap(floatArrayOf(SPEED)), longArrayOf(1))
        try {
            s.run(mapOf("tokens" to tokenT, "style" to styleT, "speed" to speedT)).use { out ->
                return out.get("audio").get().value as FloatArray
            }
        } finally {
            tokenT.close(); styleT.close(); speedT.close()
        }
    }

    /** Stream [audio] to a fresh AudioTrack; fire [onStart] the instant playback begins. */
    private fun playBlocking(audio: FloatArray, id: Long, onStart: (Long) -> Unit) {
        if (audio.isEmpty()) return
        val minBuf = AudioTrack.getMinBufferSize(
            SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_FLOAT
        ).coerceAtLeast(audio.size * 4)
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
                    .setSampleRate(SAMPLE_RATE)
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
        while (off < audio.size && currentId == id) {
            val n = t.write(audio, off, audio.size - off, AudioTrack.WRITE_BLOCKING)
            if (n <= 0) break
            off += n
        }
        if (currentId == id) {
            t.stop()   // blocks until the buffered tail drains, so onDone lines up with real end
        }
        t.release()
        if (track === t) track = null
    }

    /** Style row for a phoneme [count], clamped into the pack's [0, 509] range. */
    private fun styleRow(count: Int): FloatArray {
        if (styleRows.isEmpty()) return FloatArray(256)
        return styleRows[count.coerceIn(0, styleRows.size - 1)]
    }

    private fun loadVocab(): Map<Char, Long> {
        val json = appContext.assets.open("$ASSET_DIR/kokoro_vocab.json")
            .bufferedReader().use { it.readText() }
        val obj = JSONObject(json)
        val map = HashMap<Char, Long>(obj.length())
        for (key in obj.keys()) {
            if (key.isNotEmpty()) map[key[0]] = obj.getLong(key)
        }
        return map
    }

    /** Read the owned QUARK-H2 embedding asset: 510×256 little-endian float32. */
    private fun loadStyleRows(): Array<FloatArray> {
        val bytes = appContext.assets.open("$ASSET_DIR/quark_voice_H2.f32").use { it.readBytes() }
        val fb = java.nio.ByteBuffer.wrap(bytes).order(java.nio.ByteOrder.LITTLE_ENDIAN).asFloatBuffer()
        return Array(STYLE_BUCKETS) { r ->
            FloatArray(STYLE_DIM) { c -> fb.get(r * STYLE_DIM + c) }
        }
    }

    /** Weights live in filesDir (fetched, not bundled) — see class doc + voice/quark-phase2b/README. */
    private fun modelPath(): File = modelPath(appContext)

    override fun stop() {
        currentId = 0L
        track?.let { runCatching { it.pause(); it.flush(); it.release() } }
        track = null
    }

    override fun shutdown() {
        stop()
        worker.execute {
            runCatching { session?.close() }
            session = null
            env = null
        }
        worker.shutdown()
        _readyState.value = VoiceReadyState.UNAVAILABLE
    }

    companion object {
        private const val ASSET_DIR = "quark_voice"
        private const val MODEL_FILE = "kokoro-v1.0.onnx"
        private const val SAMPLE_RATE = 24_000
        private const val STYLE_BUCKETS = 510
        private const val STYLE_DIM = 256
        private const val SPEED = 1.02f          // locked QUARK-H2 pace (see voice/quark-phase2b/)

        private fun modelPath(context: Context): File =
            File(context.applicationContext.filesDir, "$ASSET_DIR/$MODEL_FILE")

        /**
         * Race-free precondition for choosing this engine over the placeholder: both the fetched
         * model weights and an on-device [phonemizer] must be present. With the default
         * [UnavailablePhonemizer] this is always false, so QUARK_H2 cleanly falls back until the
         * Fold 6 integration lands the model + a G2P.
         */
        fun isSupported(context: Context, phonemizer: Phonemizer = UnavailablePhonemizer): Boolean =
            modelPath(context).exists() && phonemizer.phonemize("test") != null
    }
}
