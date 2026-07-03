package com.quantumos.shell.ai

import android.content.Context
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import java.io.File
import java.io.InputStream
import java.io.RandomAccessFile

/**
 * Provisions the on-device assets [SherpaKokoroVoiceEngine] needs, and reports what's present.
 *
 * The large sherpa Kokoro model dir (`model.onnx`, `tokens.txt`, `espeak-ng-data/`, `voices.bin`)
 * is NOT bundled: the Director downloads the sherpa `kokoro-multi-lang-v1_0` tarball on-device and
 * imports it via [importModelTarBz2], which extracts it here. Until that lands, [status].modelReady
 * is false and the engine falls back to the placeholder. See voice/quark-phase2b/HANDOFF.md.
 *
 * **Why the H2 voice is a patched copy of the model's own `voices.bin`, not a standalone file:**
 * sherpa's kokoro model bakes a fixed speaker count (`n_speakers`, read from the ONNX metadata) and
 * fatally `_Exit()`s — an uncatchable process kill, not a Kotlin exception — if the `voices` file's
 * float count doesn't equal `n_speakers × 510 × 256` exactly (verified against sherpa's C++,
 * `offline-tts-kokoro-model.cc`). Our owned H2 embedding is only *one* speaker's worth, so handing
 * it to sherpa as-is crashes the whole app the instant the model loads it. The fix: take the
 * tarball's own `voices.bin` (the exact size that model expects) and overwrite one speaker slot's
 * bytes with our H2 embedding, so the file size still matches and `sid` picks H2 out of the pack.
 */
object VoiceModelProvisioner {

    private const val ROOT = "quark_voice"
    private const val MODEL_SUBDIR = "kokoro"
    private const val MODEL_FILE = "model.onnx"
    private const val TOKENS_FILE = "tokens.txt"
    private const val DATA_DIR = "espeak-ng-data"
    private const val ORIGINAL_VOICES_FILE = "voices.bin"          // shipped in the tarball, untouched
    private const val PATCHED_VOICES_FILE = "voices-quark-h2.bin"  // original, with one slot = H2
    private const val H2_ASSET = "$ROOT/quark_voice_H2.f32"        // 510x256 float32, ONE speaker's block

    private const val STYLE_ROWS = 510
    private const val STYLE_DIM = 256
    private const val BYTES_PER_FLOAT = 4
    private const val BLOCK_BYTES = STYLE_ROWS * STYLE_DIM * BYTES_PER_FLOAT   // 522_240 — one speaker

    data class ModelStatus(
        val modelReady: Boolean,
        val modelPath: String,
        val tokensPath: String,
        val dataDirPath: String,
        val missing: List<String>,
    )

    /** The patched voices file plus the speaker id (`sid`) at which QUARK-H2 lives inside it. */
    data class VoicesResult(val file: File, val sid: Int)

    private fun modelDir(context: Context): File =
        File(context.applicationContext.filesDir, "$ROOT/$MODEL_SUBDIR")

    fun status(context: Context): ModelStatus {
        val dir = modelDir(context)
        val model = File(dir, MODEL_FILE)
        val tokens = File(dir, TOKENS_FILE)
        val data = File(dir, DATA_DIR)
        val voices = File(dir, ORIGINAL_VOICES_FILE)
        val missing = buildList {
            if (!model.exists()) add(MODEL_FILE)
            if (!tokens.exists()) add(TOKENS_FILE)
            if (!data.isDirectory) add("$DATA_DIR/")
            if (!voices.exists() || voices.length() < BLOCK_BYTES) add(ORIGINAL_VOICES_FILE)
        }
        return ModelStatus(
            modelReady = missing.isEmpty(),
            modelPath = model.absolutePath,
            tokensPath = tokens.absolutePath,
            dataDirPath = data.absolutePath,
            missing = missing,
        )
    }

    /**
     * Build (or rebuild) sherpa's `voices` file: a byte-for-byte copy of the model's own
     * `voices.bin` — so the total float count still matches what the model demands — with the
     * **last** speaker slot overwritten by our owned H2 embedding. Always regenerates from the
     * pristine original so a previous bad/partial patch never lingers. Returns null if the
     * original voices.bin isn't present yet, or is smaller than one speaker's block (corrupt).
     */
    fun ensureVoicesFile(context: Context): VoicesResult? {
        val dir = modelDir(context)
        val original = File(dir, ORIGINAL_VOICES_FILE)
        if (!original.exists()) return null
        val totalBytes = original.length()
        if (totalBytes < BLOCK_BYTES || totalBytes % BLOCK_BYTES != 0L) return null   // corrupt/partial
        val numSpeakers = (totalBytes / BLOCK_BYTES).toInt()
        val h2Sid = numSpeakers - 1

        val patched = File(dir, PATCHED_VOICES_FILE)
        return try {
            original.copyTo(patched, overwrite = true)
            val h2Bytes = context.applicationContext.assets.open(H2_ASSET).use { it.readBytes() }
            if (h2Bytes.size.toLong() != BLOCK_BYTES.toLong()) return null   // asset corrupt/wrong size
            RandomAccessFile(patched, "rw").use { raf ->
                raf.seek(h2Sid.toLong() * BLOCK_BYTES)
                raf.write(h2Bytes)
            }
            VoicesResult(patched, h2Sid)
        } catch (_: Throwable) {
            null
        }
    }

    /**
     * Extract a sherpa Kokoro `*.tar.bz2` (as an [InputStream] from a picked/downloaded file) into
     * the model dir, stripping the archive's leading directory so files land at a stable path.
     * Returns true if the resulting dir satisfies [status].modelReady.
     */
    fun importModelTarBz2(context: Context, input: InputStream): Boolean {
        val dir = modelDir(context).apply { mkdirs() }
        TarArchiveInputStream(BZip2CompressorInputStream(input.buffered())).use { tar ->
            var entry = tar.nextEntry
            while (entry != null) {
                val stripped = entry.name.substringAfter('/', entry.name)  // drop top-level dir
                if (stripped.isNotEmpty()) {
                    val target = File(dir, stripped)
                    if (!target.canonicalPath.startsWith(dir.canonicalPath + File.separator)) {
                        throw SecurityException("Zip-slip guard: ${entry.name}")  // path-traversal guard
                    }
                    if (entry.isDirectory) {
                        target.mkdirs()
                    } else {
                        target.parentFile?.mkdirs()
                        target.outputStream().use { tar.copyTo(it) }
                    }
                }
                entry = tar.nextEntry
            }
        }
        return status(context).modelReady
    }
}
