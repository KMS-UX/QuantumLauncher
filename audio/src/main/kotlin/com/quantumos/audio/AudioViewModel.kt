package com.quantumos.audio

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.quantumos.core.QuarkReflexPosture
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// AUDIO's own internal navigation model -- five in-app screens (RECORDER default per the source
// app's already-correct ActiveChannel.RECORDER default/back-routing, preserved here). Distinct from
// the launcher's HOME/APPS/STATUS/LOG channel strip -- that's launcher-only chrome; a docked module
// is free to have its own internal screens (House Style skill).
enum class AudioChannel(val displayName: String) {
    RECORDER("RECORDER"),
    PLAYER("PLAYER"),
    QUARK("QUARK"),
    CONFIG("CONFIG"),
    LOG("LOG")
}

/*
 * AUDIO's ViewModel -- state that must survive fold/unfold/rotate lives here (platform rule), not in
 * composition. Owns the AudioEngine instance (recorder/player), the local QUARK reactive posture +
 * caption (a small per-app flavor feature, distinct from the launcher's own M5 QUARK Assistant View --
 * out of scope for this module), the operator call sign, and the in-app system log.
 *
 * No sound cues are wired: there is no shared SoundEngine reachable from a docked library module yet
 * (same category of gap as :core's AiAssistBridge placeholder) -- not attempted here.
 */
class AudioViewModel(application: Application) : AndroidViewModel(application) {

    val engine = AudioEngine(application.applicationContext, viewModelScope)

    private val _channel = MutableStateFlow(AudioChannel.RECORDER)
    val channel: StateFlow<AudioChannel> = _channel.asStateFlow()

    private val _operatorName = MutableStateFlow("")
    val operatorName: StateFlow<String> = _operatorName.asStateFlow()

    private val _quarkPosture = MutableStateFlow(QuarkReflexPosture.IDLE)
    val quarkPosture: StateFlow<QuarkReflexPosture> = _quarkPosture.asStateFlow()

    private val _quarkText = MutableStateFlow(
        "Monitoring active audio frequencies, Operator. Speaker diagnostics nominal."
    )
    val quarkText: StateFlow<String> = _quarkText.asStateFlow()

    private val _systemLogs = MutableStateFlow<List<String>>(emptyList())
    val systemLogs: StateFlow<List<String>> = _systemLogs.asStateFlow()

    private var quarkResetJob: Job? = null

    init {
        engine.initTracks()
        viewModelScope.launch {
            engine.logEvents.collect { message -> addLog(message) }
        }
        addLog("AUDIO: MODULE ONLINE. RECORDER CHANNEL DEFAULT.")
    }

    fun setChannel(target: AudioChannel) {
        _channel.value = target
        addLog("NAV: CHANNEL -> ${target.displayName}")
    }

    fun setOperatorName(name: String) {
        _operatorName.value = name
    }

    // The single path every local QUARK reaction flows through -- auto-settles back to IDLE after a
    // beat, same behavior the source app had, so the tab doesn't get stuck mid-reaction.
    fun setQuark(posture: QuarkReflexPosture, text: String) {
        _quarkPosture.value = posture
        _quarkText.value = text
        addLog("QUARK: ${posture.name} | \"$text\"")
        quarkResetJob?.cancel()
        if (posture != QuarkReflexPosture.IDLE) {
            quarkResetJob = viewModelScope.launch {
                delay(3500)
                _quarkPosture.value = QuarkReflexPosture.IDLE
            }
        }
    }

    fun addLog(message: String) {
        _systemLogs.update { logs ->
            val trimmed = if (logs.size > 199) logs.drop(1) else logs
            trimmed + "[${System.currentTimeMillis()}] $message"
        }
    }

    fun clearLogs() {
        _systemLogs.value = listOf("[${System.currentTimeMillis()}] LOG CONSOLE PURGED.")
    }

    // --- recorder / player pass-throughs (screens talk to the ViewModel, not the engine directly) ---

    fun startRecording() {
        engine.startRecording()
        setQuark(QuarkReflexPosture.SCAN, "Envelope active, Operator. Capturing sound frequencies.")
    }

    fun stopRecording() {
        engine.stopRecording()
        setQuark(QuarkReflexPosture.HAPPY, "Audio buffer successfully committed to field recordings, Operator.")
    }

    fun recordDenied() {
        setQuark(QuarkReflexPosture.WARN, "ACCESS DENIED: microphone feed is blocked, Operator.")
    }

    fun playTrack(track: AudioTrackInfo) {
        engine.playTrack(track)
        setQuark(QuarkReflexPosture.HAPPY, "Synthesizing wave playback of ${track.title}, Operator.")
    }

    fun togglePlayback() {
        val wasPlaying = engine.isPlaying.value
        engine.togglePlayback()
        setQuark(
            if (wasPlaying) QuarkReflexPosture.IDLE else QuarkReflexPosture.HAPPY,
            if (wasPlaying) "Suspended audio thread." else "Resuming synthesiser wave output, Operator."
        )
    }

    fun stopPlayback() {
        engine.stopPlayback()
        setQuark(QuarkReflexPosture.IDLE, "Flushed synthesiser wave output.")
    }

    fun importAudioFile(uri: Uri, fileName: String) {
        engine.importAudioFile(uri, fileName)
    }
}
