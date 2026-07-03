package com.quantumos.shell.ai

import android.content.Context
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import java.io.File
import java.io.InputStream

/**
 * Provisions the on-device assets [SherpaKokoroVoiceEngine] needs, and reports what's present.
 *
 * The custom voice is owned and small — the H2 embedding ships as an app asset and is copied into
 * sherpa's `voices` slot automatically. The large sherpa Kokoro model dir (`model.onnx`,
 * `tokens.txt`, `espeak-ng-data/`) is NOT bundled: the Director downloads the sherpa
 * `kokoro-multi-lang-v1_0` tarball on-device and imports it via [importModelTarBz2], which extracts
 * it here. Until that lands, [status].modelReady is false and the engine falls back to the
 * placeholder. See voice/quark-phase2b/HANDOFF.md.
 */
object VoiceModelProvisioner {

    private const val ROOT = "quark_voice"
    private const val MODEL_SUBDIR = "kokoro"
    private const val MODEL_FILE = "model.onnx"
    private const val TOKENS_FILE = "tokens.txt"
    private const val DATA_DIR = "espeak-ng-data"
    private const val VOICES_FILE = "voices-quark-h2.bin"        // our H2 embedding, sherpa format
    private const val H2_ASSET = "$ROOT/quark_voice_H2.f32"      // 510x256 float32 == 1-speaker voices.bin

    data class ModelStatus(
        val modelReady: Boolean,
        val modelPath: String,
        val tokensPath: String,
        val dataDirPath: String,
        val missing: List<String>,
    )

    private fun modelDir(context: Context): File =
        File(context.applicationContext.filesDir, "$ROOT/$MODEL_SUBDIR")

    fun status(context: Context): ModelStatus {
        val dir = modelDir(context)
        val model = File(dir, MODEL_FILE)
        val tokens = File(dir, TOKENS_FILE)
        val data = File(dir, DATA_DIR)
        val missing = buildList {
            if (!model.exists()) add(MODEL_FILE)
            if (!tokens.exists()) add(TOKENS_FILE)
            if (!data.isDirectory) add("$DATA_DIR/")
        }
        return ModelStatus(
            modelReady = missing.isEmpty(),
            modelPath = model.absolutePath,
            tokensPath = tokens.absolutePath,
            dataDirPath = data.absolutePath,
            missing = missing,
        )
    }

    /** Copy the bundled H2 embedding into sherpa's voices slot (once). Returns the file, or null. */
    fun ensureVoicesFile(context: Context): File? {
        val dir = modelDir(context).apply { mkdirs() }
        val out = File(dir, VOICES_FILE)
        if (out.exists() && out.length() > 0) return out
        return try {
            context.applicationContext.assets.open(H2_ASSET).use { input ->
                out.outputStream().use { input.copyTo(it) }
            }
            out
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
