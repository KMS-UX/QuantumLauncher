package com.quantumos.shell.ai

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/*
 * Phase 2a voice engine — wraps Android's built-in TextToSpeech for the text→audio pipeline.
 *
 * Android TTS runs fully offline on Android 13+ (Google's neural voice is pre-installed) with
 * sub-100ms start latency, making it the ideal "prove the plumbing cheaply" placeholder. It
 * satisfies Phase 2a's requirements (on-device, offline, no network, real latency headroom) while
 * the ONNX/Piper→Kokoro voice swap in Phase 2b replaces the timbre, not the pipeline.
 *
 * Pitch/rate are dialled slightly toward the EDI register as a stand-in direction. Phase 2b's
 * KokoroVoiceEngine implements the same VoiceEngine contract, so the swap needs no caller change.
 *
 * Thread safety: speak() is called from a coroutine; TTS callbacks arrive on TTS's internal
 * thread. The `gate` monitor keeps the pending-utterance state consistent.
 */
class QuarkVoiceEngine(context: Context) : VoiceEngine {

    private val appContext = context.applicationContext

    private val _readyState = MutableStateFlow(VoiceReadyState.INITIALIZING)
    override val readyState: StateFlow<VoiceReadyState> = _readyState.asStateFlow()
    override val isReady get() = _readyState.value == VoiceReadyState.READY

    private var tts: TextToSpeech? = null

    // Single in-flight utterance tracked by ID. gate ensures speak() and TTS callbacks
    // see a consistent state even across threads.
    private val gate = Object()
    private var pendingId = ""
    private var pendingOnStart: ((Long) -> Unit)? = null
    private var pendingOnDone: (() -> Unit)? = null

    init {
        tts = TextToSpeech(appContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        val cb = synchronized(gate) {
                            if (utteranceId == pendingId) pendingOnStart else null
                        }
                        cb?.invoke(System.currentTimeMillis())
                    }

                    override fun onDone(utteranceId: String?) {
                        val cb = synchronized(gate) {
                            if (utteranceId == pendingId) {
                                pendingId = ""; pendingOnDone
                            } else null
                        }
                        cb?.invoke()
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        // Settle on error so posture doesn't stick.
                        val cb = synchronized(gate) {
                            if (utteranceId == pendingId) {
                                pendingId = ""; pendingOnDone
                            } else null
                        }
                        cb?.invoke()
                    }
                })
                tts?.language = Locale.US
                tts?.setPitch(0.88f)       // EDI-register direction: slightly lower, deliberate
                tts?.setSpeechRate(0.92f)  // measured field-tool pace
                _readyState.value = VoiceReadyState.READY
            } else {
                _readyState.value = VoiceReadyState.UNAVAILABLE
            }
        }
    }

    /*
     * Speak [text]. Calls [onStart] with the wall-clock time when TTS begins producing audio
     * (useful for start-latency measurement), then [onDone] when audio playback completes.
     * If the engine is not ready, [onDone] is called immediately so callers can always assume
     * the settle callback fires — even on error or before initialization.
     * QUEUE_FLUSH replaces any currently-playing utterance.
     */
    override fun speak(text: String, onStart: (Long) -> Unit, onDone: () -> Unit) {
        if (!isReady) { onDone(); return }
        val id = System.nanoTime().toString()
        synchronized(gate) {
            pendingId = id
            pendingOnStart = onStart
            pendingOnDone = onDone
        }
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, id)
    }

    /*
     * Stop any in-flight utterance without firing the settle callback. Used on a clean close
     * (e.g. Activity finish) where the posture settle would be spurious.
     */
    override fun stop() {
        synchronized(gate) {
            pendingId = ""
            pendingOnStart = null
            pendingOnDone = null
        }
        tts?.stop()
    }

    override fun shutdown() {
        stop()
        tts?.shutdown()
        tts = null
        _readyState.value = VoiceReadyState.UNAVAILABLE
    }
}
