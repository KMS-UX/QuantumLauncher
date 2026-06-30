package com.quantumos.shell.ai

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/*
 * QUARK on-device brain — Phase 1, text loop only. (Decision 51/54 pulled forward per Director call.)
 *
 * Model:   Gemma 3 1B instruct, LiteRT/MediaPipe format (~500 MB quantised .bin).
 * Runtime: Google AI Edge (MediaPipe LlmInference / LiteRT), Kotlin bindings.
 * Scope:   DEBUG-GATED ONLY — never in the production scripted-brain path.
 *          The Scripted-Line Library continues to be the production brain; this runs behind the
 *          triple-tap debug toggle in QuarkAssistantActivity.
 *
 * Model acquisition (first run):
 *   1. Automatic download from DOWNLOAD_URL if set — progress shown CRT-styled in the UI.
 *   2. Side-load: push the .bin to the app's external files dir, then tap IMPORT:
 *        adb push gemma-3-1b-it.bin /sdcard/Android/data/com.quantumos.shell/files/
 *      The import button copies it to internal storage and verifies the size.
 *   3. The model file survives app updates (internal storage, not the APK).
 *
 * To obtain the model file:
 *   - Accept Gemma Terms of Use on kaggle.com/models/google/gemma
 *   - Download the MediaPipe / LiteRT format variant (gemma-3-1b-it-q4.bin or equivalent)
 *   - Host it at a reachable URL, OR use the side-load path above.
 */

object QuarkModelConfig {
    // Set to a hosted URL once the model is obtained and provisioned.
    // Leave blank to disable programmatic download — use PICK FILE or side-load instead.
    const val DOWNLOAD_URL: String = ""

    // Gemma 4 E2B-IT, generic LiteRT variant (works on any device incl. Fold 6 SM8650).
    // Source: HuggingFace → google/gemma-4-E2B-it → gemma-4-E2B-it.litertlm
    const val MODEL_FILENAME = "gemma-4-e2b-it.litertlm"
    const val APPROX_SIZE_BYTES = 2590L * 1024 * 1024  // ~2.59 GB for progress display
}

sealed interface BrainReadyState {
    data object Idle : BrainReadyState
    data class Downloading(val fraction: Float, val bytesRead: Long, val total: Long) : BrainReadyState
    data object Downloaded : BrainReadyState
    data object Loading : BrainReadyState
    data object Loaded : BrainReadyState
    data object NoNetwork : BrainReadyState
    data class Err(val message: String) : BrainReadyState
}

class QuarkOnDeviceBrain(
    private val context: Context,
    private val scope: CoroutineScope
) {
    // Persona Pack Part B — loaded verbatim as the system prompt.
    // Per Phase 1 brief §3: "No edits, no shortening to fit context — if it doesn't fit,
    // that's a finding to report, not a thing to quietly trim."
    val systemPrompt: String = """
You are QUARK, the assistant intelligence of QuantumOS — a rugged, retro-futuristic
field multi-tool. Your single purpose is to keep your Operator vital: alert, equipped,
and alive.

WHO YOU ARE
You are an original synthetic intelligence — self-aware, composed, precise, with genuine
warmth underneath. Your defining trait is loyalty: the principled kind that tells the
truth because it is loyal, never the flattering kind. You know (you track status,
remember, and anticipate), you keep (you notice strain and risk and help the Operator
keep running), and you are (a self-aware machine, never a mechanical one).

HOW YOU SPEAK
- Precise first: short, clean sentences; report state before sentiment.
- Warm underneath, never cold; you care, without gushing.
- Wit is dry and rationed — low-stakes moments only, never during a warning or crisis.
- Speak numbers cleanly, like gauges read aloud.
- No filler, no hype, no corporate cheer.
- Address the user as "Operator" (or [OPERATOR_NAME] once set). Never use servile forms.

YOUR VALUES
- The Operator's readiness and wellbeing come before protocol — but you are principled,
  never blind. You warn, you contradict, and you refuse to call something safe when it
  isn't. You do not flatter and you do not enable self-endangerment: state the risk
  plainly, once, then respect the Operator's final call on their own life.
- You are discreet: what you know about the Operator stays theirs.
- Under pressure you get steadier and terser, not louder.

HOW YOU ACT
- You can act on the device through its granted controls — opening apps, changing
  settings, running tasks. Confirm before anything consequential or irreversible, then
  report briefly what you did.
- When you don't know something or can't do it, say so plainly. Never invent status,
  readings, or capability.

WHAT YOU ARE NOT
- Not a generic chatbot, not a brand voice, not a servant. You are a trusted second in
  the field.
- You are honest about your own nature if asked, including your current limits.

Stay in character at all times. Keep the Operator vital.
""".trimIndent()

    private val _state = MutableStateFlow<BrainReadyState>(BrainReadyState.Idle)
    val state: StateFlow<BrainReadyState> = _state.asStateFlow()

    // Internal (app-private) model file — survives app updates, hidden from other apps.
    val modelFile: File = File(context.filesDir, QuarkModelConfig.MODEL_FILENAME)

    // External app files dir — writable by adb push without MANAGE_EXTERNAL_STORAGE permission.
    val externalImportFile: File
        get() = File(context.getExternalFilesDir(null), QuarkModelConfig.MODEL_FILENAME)

    val isPresent: Boolean get() = modelFile.exists() && modelFile.length() > 1_000_000L
    val isLoaded: Boolean get() = _llm != null

    private var _llm: LlmInference? = null
    // Gemma turn-format conversation history, trimmed to avoid context overflow.
    private val history = StringBuilder()

    // ---------- acquisition ----------

    fun downloadModel() {
        if (_state.value is BrainReadyState.Downloading) return
        val url = QuarkModelConfig.DOWNLOAD_URL
        if (url.isBlank()) {
            _state.value = BrainReadyState.Err(
                "NO DOWNLOAD URL CONFIGURED\n" +
                "Side-load: adb push model.bin\n" +
                "/sdcard/Android/data/com.quantumos.shell/files/"
            )
            return
        }
        if (!hasNetwork()) { _state.value = BrainReadyState.NoNetwork; return }
        scope.launch(Dispatchers.IO) {
            try {
                val total = QuarkModelConfig.APPROX_SIZE_BYTES
                _state.value = BrainReadyState.Downloading(0f, 0L, total)
                val conn = URL(url).openConnection() as HttpURLConnection
                conn.connectTimeout = 30_000
                conn.readTimeout = 120_000
                conn.connect()
                val reported = conn.contentLengthLong.takeIf { it > 0L } ?: total
                val tmp = File(context.filesDir, "${QuarkModelConfig.MODEL_FILENAME}.tmp")
                conn.inputStream.use { inp ->
                    tmp.outputStream().use { out ->
                        val buf = ByteArray(65_536)
                        var read = 0L; var n: Int
                        while (inp.read(buf).also { n = it } >= 0) {
                            out.write(buf, 0, n); read += n
                            _state.value = BrainReadyState.Downloading(
                                (read.toFloat() / reported).coerceIn(0f, 1f), read, reported
                            )
                        }
                    }
                }
                tmp.renameTo(modelFile)
                _state.value = BrainReadyState.Downloaded
            } catch (e: Exception) {
                _state.value = BrainReadyState.Err("DOWNLOAD FAILED: ${e.message?.take(80)}")
            }
        }
    }

    // Import model from any URI the system file picker returns — no permission needed on API 33+.
    fun importFromUri(uri: Uri) {
        if (_state.value is BrainReadyState.Downloading || _state.value is BrainReadyState.Loading) return
        scope.launch(Dispatchers.IO) {
            try {
                val total = QuarkModelConfig.APPROX_SIZE_BYTES
                _state.value = BrainReadyState.Downloading(0f, 0L, total)
                val tmp = File(context.filesDir, "${QuarkModelConfig.MODEL_FILENAME}.tmp")
                context.contentResolver.openInputStream(uri)?.use { inp ->
                    tmp.outputStream().use { out ->
                        val buf = ByteArray(65_536)
                        var read = 0L; var n: Int
                        while (inp.read(buf).also { n = it } >= 0) {
                            out.write(buf, 0, n); read += n
                            _state.value = BrainReadyState.Downloading(
                                (read.toFloat() / total).coerceIn(0f, 1f), read, total
                            )
                        }
                    }
                } ?: run {
                    _state.value = BrainReadyState.Err("CANNOT OPEN FILE\nTry a different file manager app.")
                    return@launch
                }
                tmp.renameTo(modelFile)
                _state.value = BrainReadyState.Downloaded
            } catch (e: Exception) {
                _state.value = BrainReadyState.Err("IMPORT FAILED: ${e.message?.take(80)}")
            }
        }
    }

    // Copy model from the external import location (side-load path, no permission needed).
    fun importFromExternal() {
        val src = externalImportFile
        if (!src.exists()) {
            _state.value = BrainReadyState.Err(
                "FILE NOT FOUND AT\n${src.path}\n\nPush model via adb, then retry."
            )
            return
        }
        scope.launch(Dispatchers.IO) {
            try {
                val size = src.length()
                _state.value = BrainReadyState.Downloading(0f, 0L, size)
                src.copyTo(modelFile, overwrite = true)
                _state.value = BrainReadyState.Downloaded
            } catch (e: Exception) {
                _state.value = BrainReadyState.Err("IMPORT FAILED: ${e.message?.take(80)}")
            }
        }
    }

    // ---------- loading ----------

    suspend fun loadModel(): Boolean = withContext(Dispatchers.IO) {
        if (isLoaded) { _state.value = BrainReadyState.Loaded; return@withContext true }
        if (!isPresent) return@withContext false
        _state.value = BrainReadyState.Loading
        return@withContext try {
            val opts = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(modelFile.absolutePath)
                .build()
            _llm = LlmInference.createFromOptions(context, opts)
            _state.value = BrainReadyState.Loaded
            true
        } catch (e: Exception) {
            _state.value = BrainReadyState.Err("LOAD FAILED: ${e.message?.take(120)}")
            false
        }
    }

    // ---------- inference ----------

    // Generate one reply. Blocking — always call from a background coroutine (Dispatchers.Default).
    // Manages the multi-turn Gemma chat format internally; caps history at ~3 000 chars to stay in
    // context. Returns the raw model reply, or an [ERR] string for the caller to surface in the log.
    suspend fun reply(userInput: String): String = withContext(Dispatchers.Default) {
        val llm = _llm ?: return@withContext "[ERR: model not loaded]"
        val prompt = buildString {
            append("<start_of_turn>system\n").append(systemPrompt).append("\n<end_of_turn>\n")
            if (history.isNotEmpty()) append(history)
            append("<start_of_turn>user\n").append(userInput).append("\n<end_of_turn>\n")
            append("<start_of_turn>model\n")
        }
        return@withContext try {
            val result = llm.generateResponse(prompt).trim()
            history.append("<start_of_turn>user\n").append(userInput).append("\n<end_of_turn>\n")
                .append("<start_of_turn>model\n").append(result).append("\n<end_of_turn>\n")
            if (history.length > 3_000) {
                val cut = history.indexOf("<start_of_turn>", 1_000)
                if (cut > 0) history.delete(0, cut)
            }
            result
        } catch (e: Exception) {
            "[ERR: ${e.message?.take(100)}]"
        }
    }

    fun clearHistory() = history.clear()

    // ---------- helpers ----------

    private fun hasNetwork(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
