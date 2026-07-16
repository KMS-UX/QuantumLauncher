package com.quantumos.audio

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sin

data class AudioTrackInfo(
    val title: String,
    val file: File,
    val isSynthetic: Boolean,
    val durationText: String
)

/*
 * AUDIO's real domain logic -- ported from the standalone repo's AudioEngine.kt (Core Apps Fix-Pass,
 * Decision 86). MediaRecorder/MediaPlayer wiring and the live-amplitude polling loop are kept as-is
 * (they were already correctly gated on the recording/playing flags in the source app). Everything
 * that used to reach into the standalone app's own QuantumState.addLog / SoundSynthesizer is dropped:
 * this class emits plain log lines on its own SharedFlow instead, and carries no sound dependency (no
 * shared SoundEngine is reachable from a docked library module yet -- same category of gap as the
 * AiAssistBridge placeholder in :core; not attempted here).
 *
 * Owned by AudioViewModel (constructed with applicationContext + viewModelScope) rather than kept as
 * a process-wide singleton object, so its lifetime matches the ViewModel's and there is nothing to
 * leak across Activity recreation.
 *
 * "PLEASE STANDBY" real-wait rule: MediaRecorder/MediaPlayer prepare()+start() are blocking hardware
 * calls. Both startRecording() and playTrack() flip isPreparing to true before doing that blocking
 * work on Dispatchers.IO, so the screen can show the real PleaseStandbyCard for the real
 * hardware-allocation wait -- not a fake artificial delay, and not the old app's PIN-lock misuse of
 * that card.
 */
class AudioEngine(
    private val appContext: Context,
    private val scope: CoroutineScope
) {
    private var mediaPlayer: MediaPlayer? = null
    private var mediaRecorder: MediaRecorder? = null
    private var recordingFile: File? = null

    private val _logEvents = MutableSharedFlow<String>(replay = 0, extraBufferCapacity = 16)
    val logEvents: SharedFlow<String> = _logEvents.asSharedFlow()

    // Player state
    private val _trackList = MutableStateFlow<List<AudioTrackInfo>>(emptyList())
    val trackList: StateFlow<List<AudioTrackInfo>> = _trackList.asStateFlow()

    private val _currentTrack = MutableStateFlow<AudioTrackInfo?>(null)
    val currentTrack: StateFlow<AudioTrackInfo?> = _currentTrack.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _isPreparingPlayback = MutableStateFlow(false)
    val isPreparingPlayback: StateFlow<Boolean> = _isPreparingPlayback.asStateFlow()

    private val _playbackProgress = MutableStateFlow(0f)
    val playbackProgress: StateFlow<Float> = _playbackProgress.asStateFlow()

    private val _playbackTime = MutableStateFlow("00:00")
    val playbackTime: StateFlow<String> = _playbackTime.asStateFlow()

    private val _playbackDuration = MutableStateFlow("00:00")
    val playbackDuration: StateFlow<String> = _playbackDuration.asStateFlow()

    private val _playbackSpectrum = MutableStateFlow<List<Float>>(List(16) { 0.1f })
    val playbackSpectrum: StateFlow<List<Float>> = _playbackSpectrum.asStateFlow()

    // Recorder state
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _isPreparingRecorder = MutableStateFlow(false)
    val isPreparingRecorder: StateFlow<Boolean> = _isPreparingRecorder.asStateFlow()

    private val _recordingDurationTenths = MutableStateFlow(0)
    val recordingDurationTenths: StateFlow<Int> = _recordingDurationTenths.asStateFlow()

    private val _recordingTime = MutableStateFlow("00:00")
    val recordingTime: StateFlow<String> = _recordingTime.asStateFlow()

    // Live mic amplitude -- ONLY updated while actually recording (gated in startRecorderPolling
    // below). Never drifts on its own; RecorderScreen's oscilloscope reads this stream directly.
    private val _liveAmplitude = MutableStateFlow(0f)
    val liveAmplitude: StateFlow<Float> = _liveAmplitude.asStateFlow()

    private var playerPollJob: Job? = null
    private var recorderPollJob: Job? = null

    fun initTracks() {
        scope.launch(Dispatchers.IO) {
            val syntheticDir = File(appContext.filesDir, "synthetic")
            if (!syntheticDir.exists()) syntheticDir.mkdirs()

            val log1 = File(syntheticDir, "field_log_01.wav")
            if (!log1.exists()) generateMelodyWav(log1, SAMPLE_RATE, 8.0)

            val log2 = File(syntheticDir, "cosmic_static.wav")
            if (!log2.exists()) generateStaticWav(log2, SAMPLE_RATE, 10.0)

            val log3 = File(syntheticDir, "quark_diag.wav")
            if (!log3.exists()) generateDiagWav(log3, SAMPLE_RATE, 6.0)

            refreshTrackListInternal()
        }
    }

    fun refreshTrackList() {
        scope.launch(Dispatchers.IO) { refreshTrackListInternal() }
    }

    private fun refreshTrackListInternal() {
        val syntheticDir = File(appContext.filesDir, "synthetic")
        val recordingsDir = File(appContext.filesDir, "recordings")
        val importedDir = File(appContext.filesDir, "imported")
        if (!recordingsDir.exists()) recordingsDir.mkdirs()
        if (!importedDir.exists()) importedDir.mkdirs()

        val list = mutableListOf<AudioTrackInfo>()

        if (syntheticDir.exists()) {
            syntheticDir.listFiles()?.sortedBy { it.name }?.forEach { file ->
                val title = when (file.name) {
                    "field_log_01.wav" -> "FIELD_LOG_01.wav"
                    "cosmic_static.wav" -> "COSMIC_STATIC.wav"
                    "quark_diag.wav" -> "QUARK_DIAGNOSTICS.wav"
                    else -> file.name.uppercase()
                }
                list.add(AudioTrackInfo(title, file, true, durationTextFor(file)))
            }
        }
        if (recordingsDir.exists()) {
            recordingsDir.listFiles()?.sortedByDescending { it.lastModified() }?.forEach { file ->
                list.add(AudioTrackInfo(file.name.uppercase(), file, false, durationTextFor(file)))
            }
        }
        if (importedDir.exists()) {
            importedDir.listFiles()?.sortedByDescending { it.lastModified() }?.forEach { file ->
                list.add(AudioTrackInfo(file.name.uppercase(), file, false, durationTextFor(file)))
            }
        }

        _trackList.value = list
        if (_currentTrack.value == null && list.isNotEmpty()) {
            _currentTrack.value = list.first()
        }
    }

    fun importAudioFile(uri: Uri, fileName: String) {
        scope.launch(Dispatchers.IO) {
            try {
                val importedDir = File(appContext.filesDir, "imported")
                if (!importedDir.exists()) importedDir.mkdirs()
                val destFile = File(importedDir, fileName)
                appContext.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(destFile).use { output -> input.copyTo(output) }
                }
                log("SYS: IMPORTED AUDIO -> $fileName")
                refreshTrackListInternal()
            } catch (e: Exception) {
                log("ERR: IMPORT FAILED -> ${e.localizedMessage}")
            }
        }
    }

    private fun durationTextFor(file: File): String {
        if (file.name == "field_log_01.wav") return "00:08"
        if (file.name == "cosmic_static.wav") return "00:10"
        if (file.name == "quark_diag.wav") return "00:06"
        val mp = MediaPlayer()
        return try {
            mp.setDataSource(file.absolutePath)
            mp.prepare()
            val text = AudioTimeFormat.millisToClock(mp.duration.toLong())
            mp.release()
            text
        } catch (e: Exception) {
            mp.release()
            "00:04"
        }
    }

    // --- PLAYBACK ---

    fun selectTrack(track: AudioTrackInfo) {
        stopPlayback()
        _currentTrack.value = track
        log("PLAYER: LOADED TRACK -> ${track.title}")
    }

    fun playTrack(track: AudioTrackInfo) {
        scope.launch {
            stopPlaybackInternal()
            _currentTrack.value = track
            _isPreparingPlayback.value = true

            val prepared = withContext(Dispatchers.IO) {
                runCatching {
                    MediaPlayer().apply {
                        setDataSource(track.file.absolutePath)
                        prepare()
                    }
                }.getOrNull()
            }

            _isPreparingPlayback.value = false
            if (prepared == null) {
                log("ERR: PLAYBACK FAILED FOR ${track.title}")
                return@launch
            }

            mediaPlayer = prepared
            prepared.setOnCompletionListener {
                stopPlayback()
                log("PLAYER: COMPLETED ${track.title}")
            }
            prepared.start()
            _isPlaying.value = true
            _playbackDuration.value = AudioTimeFormat.millisToClock(prepared.duration.toLong())
            log("PLAYER: PLAYING ${track.title}")
            startPlayerPolling()
        }
    }

    fun togglePlayback() {
        val track = _currentTrack.value ?: return
        if (_isPlaying.value) {
            mediaPlayer?.pause()
            _isPlaying.value = false
            log("PLAYER: PAUSED ${track.title}")
        } else if (mediaPlayer != null) {
            mediaPlayer?.start()
            _isPlaying.value = true
            log("PLAYER: RESUMED ${track.title}")
            startPlayerPolling()
        } else {
            playTrack(track)
        }
    }

    fun stopPlayback() {
        scope.launch { stopPlaybackInternal() }
    }

    private fun stopPlaybackInternal() {
        playerPollJob?.cancel()
        mediaPlayer?.let {
            runCatching { if (it.isPlaying) it.stop() }
            it.release()
        }
        mediaPlayer = null
        _isPlaying.value = false
        _playbackProgress.value = 0f
        _playbackTime.value = "00:00"
        _playbackSpectrum.value = List(16) { 0.1f }
    }

    private fun startPlayerPolling() {
        playerPollJob?.cancel()
        playerPollJob = scope.launch {
            while (_isPlaying.value) {
                mediaPlayer?.let { mp ->
                    try {
                        val current = mp.currentPosition
                        val total = mp.duration
                        if (total > 0) {
                            _playbackProgress.value = current.toFloat() / total
                            _playbackTime.value = AudioTimeFormat.millisToClock(current.toLong())
                        }
                        // Vintage frequency-bar simulation -- correctly gated already (this loop only
                        // runs while _isPlaying is true; it stops the instant playback stops).
                        _playbackSpectrum.value = List(16) { index ->
                            val base = sin(System.currentTimeMillis().toDouble() / 150.0 + index).toFloat()
                            val noise = (Math.random().toFloat() * 0.3f)
                            (0.3f + base * 0.3f + noise).coerceIn(0.05f, 1.0f)
                        }
                    } catch (e: Exception) {
                        // ignore transient state races
                    }
                }
                delay(100)
            }
        }
    }

    // --- RECORDER ---

    fun startRecording() {
        if (_isRecording.value || _isPreparingRecorder.value) return
        scope.launch {
            stopPlaybackInternal()
            _isPreparingRecorder.value = true

            val recordingsDir = File(appContext.filesDir, "recordings")
            val result = withContext(Dispatchers.IO) {
                if (!recordingsDir.exists()) recordingsDir.mkdirs()
                val count = (recordingsDir.listFiles()?.size ?: 0) + 1
                val target = File(recordingsDir, "field_log_${"%03d".format(count)}.mp4")
                runCatching {
                    val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        MediaRecorder(appContext)
                    } else {
                        @Suppress("DEPRECATION")
                        MediaRecorder()
                    }
                    recorder.apply {
                        setAudioSource(MediaRecorder.AudioSource.MIC)
                        setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                        setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                        setOutputFile(target.absolutePath)
                        prepare()
                        start()
                    }
                    recorder to target
                }.getOrNull()
            }

            _isPreparingRecorder.value = false
            if (result == null) {
                log("ERR: RECORDER HARDWARE ALLOCATION FAILED")
                return@launch
            }

            val (recorder, target) = result
            mediaRecorder = recorder
            recordingFile = target
            _isRecording.value = true
            _recordingDurationTenths.value = 0
            _recordingTime.value = "00:00"
            log("RECORDER: MIC ACTIVE -> WRITING ${target.name.uppercase()}")
            startRecorderPolling()
        }
    }

    fun stopRecording() {
        if (!_isRecording.value) return
        scope.launch {
            recorderPollJob?.cancel()
            val name = recordingFile?.name?.uppercase() ?: "FILE"
            withContext(Dispatchers.IO) {
                runCatching {
                    mediaRecorder?.stop()
                    mediaRecorder?.release()
                }
            }
            mediaRecorder = null
            _isRecording.value = false
            _liveAmplitude.value = 0f
            log("RECORDER: FLUSHED BUFFER -> COMMITTED $name")
            refreshTrackListInternal()
        }
    }

    // Correctly gated already in the source app -- ported as-is. The `while (_isRecording.value)`
    // guard is the entire contract: the instant recording stops, this loop exits and nothing keeps
    // pushing values into _liveAmplitude. No idle-drift branch exists here or in RecorderScreen.
    private fun startRecorderPolling() {
        recorderPollJob?.cancel()
        recorderPollJob = scope.launch {
            while (_isRecording.value) {
                delay(100)
                _recordingDurationTenths.value += 1
                _recordingTime.value = AudioTimeFormat.tenthsToClock(_recordingDurationTenths.value)
                mediaRecorder?.let { mr ->
                    try {
                        val maxAmp = mr.maxAmplitude
                        _liveAmplitude.value = (maxAmp.toFloat() / 32767f).coerceIn(0f, 1f)
                    } catch (e: Exception) {
                        // ignore transient state races
                    }
                }
            }
        }
    }

    // --- synthetic demo content (pure math -> WAV, no external deps) ---

    private fun writeWavHeader(out: FileOutputStream, totalAudioLen: Int, totalDataLen: Int, sampleRate: Int) {
        val header = ByteArray(44)
        val byteRate = sampleRate * 2

        header[0] = 'R'.code.toByte(); header[1] = 'I'.code.toByte(); header[2] = 'F'.code.toByte(); header[3] = 'F'.code.toByte()
        header[4] = (totalDataLen and 0xff).toByte()
        header[5] = ((totalDataLen shr 8) and 0xff).toByte()
        header[6] = ((totalDataLen shr 16) and 0xff).toByte()
        header[7] = ((totalDataLen shr 24) and 0xff).toByte()
        header[8] = 'W'.code.toByte(); header[9] = 'A'.code.toByte(); header[10] = 'V'.code.toByte(); header[11] = 'E'.code.toByte()
        header[12] = 'f'.code.toByte(); header[13] = 'm'.code.toByte(); header[14] = 't'.code.toByte(); header[15] = ' '.code.toByte()
        header[16] = 16; header[17] = 0; header[18] = 0; header[19] = 0
        header[20] = 1; header[21] = 0
        header[22] = 1; header[23] = 0
        header[24] = (sampleRate and 0xff).toByte()
        header[25] = ((sampleRate shr 8) and 0xff).toByte()
        header[26] = ((sampleRate shr 16) and 0xff).toByte()
        header[27] = ((sampleRate shr 24) and 0xff).toByte()
        header[28] = (byteRate and 0xff).toByte()
        header[29] = ((byteRate shr 8) and 0xff).toByte()
        header[30] = ((byteRate shr 16) and 0xff).toByte()
        header[31] = ((byteRate shr 24) and 0xff).toByte()
        header[32] = 2; header[33] = 0
        header[34] = 16; header[35] = 0
        header[36] = 'd'.code.toByte(); header[37] = 'a'.code.toByte(); header[38] = 't'.code.toByte(); header[39] = 'a'.code.toByte()
        header[40] = (totalAudioLen and 0xff).toByte()
        header[41] = ((totalAudioLen shr 8) and 0xff).toByte()
        header[42] = ((totalAudioLen shr 16) and 0xff).toByte()
        header[43] = ((totalAudioLen shr 24) and 0xff).toByte()

        out.write(header, 0, 44)
    }

    private fun generateMelodyWav(file: File, sampleRate: Int, duration: Double) {
        val totalSamples = (sampleRate * duration).toInt()
        val totalAudioLen = totalSamples * 2
        val totalDataLen = totalAudioLen + 36
        try {
            FileOutputStream(file).use { fos ->
                writeWavHeader(fos, totalAudioLen, totalDataLen, sampleRate)
                val pcmBuffer = ByteBuffer.allocate(totalSamples * 2).order(ByteOrder.LITTLE_ENDIAN)
                val notes = doubleArrayOf(329.63, 392.00, 440.00, 523.25, 587.33, 659.25, 783.99, 880.00)
                for (i in 0 until totalSamples) {
                    val t = i.toDouble() / sampleRate
                    val noteIdx = ((t / 0.3).toInt() % notes.size)
                    val baseFreq = notes[noteIdx]
                    val angle = 2.0 * Math.PI * baseFreq * t
                    val primaryWave = sin(angle)
                    val subHarmonic = sin(2.0 * Math.PI * (baseFreq * 0.5) * t) * 0.4
                    val thirdHarmonic = sin(2.0 * Math.PI * (baseFreq * 3.0) * t) * 0.15
                    val noteTime = t % 0.3
                    val envelope = kotlin.math.exp(-8.0 * noteTime)
                    val combined = (primaryWave + subHarmonic + thirdHarmonic) * 0.3 * envelope
                    pcmBuffer.putShort((combined * 32767.0).toInt().coerceIn(-32768, 32767).toShort())
                }
                fos.write(pcmBuffer.array())
            }
        } catch (e: Exception) {
            // best-effort synthetic demo content; a failure here just leaves the track list shorter
        }
    }

    private fun generateStaticWav(file: File, sampleRate: Int, duration: Double) {
        val totalSamples = (sampleRate * duration).toInt()
        val totalAudioLen = totalSamples * 2
        val totalDataLen = totalAudioLen + 36
        try {
            FileOutputStream(file).use { fos ->
                writeWavHeader(fos, totalAudioLen, totalDataLen, sampleRate)
                val pcmBuffer = ByteBuffer.allocate(totalSamples * 2).order(ByteOrder.LITTLE_ENDIAN)
                for (i in 0 until totalSamples) {
                    val t = i.toDouble() / sampleRate
                    val rawNoise = (Math.random() * 2.0 - 1.0)
                    val cosmicWind = 0.5 + 0.3 * sin(2.0 * Math.PI * 0.2 * t) + 0.2 * sin(2.0 * Math.PI * 3.0 * t)
                    val combined = rawNoise * 0.08 * cosmicWind
                    pcmBuffer.putShort((combined * 32767.0).toInt().coerceIn(-32768, 32767).toShort())
                }
                fos.write(pcmBuffer.array())
            }
        } catch (e: Exception) {
            // best-effort
        }
    }

    private fun generateDiagWav(file: File, sampleRate: Int, duration: Double) {
        val totalSamples = (sampleRate * duration).toInt()
        val totalAudioLen = totalSamples * 2
        val totalDataLen = totalAudioLen + 36
        try {
            FileOutputStream(file).use { fos ->
                writeWavHeader(fos, totalAudioLen, totalDataLen, sampleRate)
                val pcmBuffer = ByteBuffer.allocate(totalSamples * 2).order(ByteOrder.LITTLE_ENDIAN)
                for (i in 0 until totalSamples) {
                    val t = i.toDouble() / sampleRate
                    val pulseCycle = t % 0.5
                    val sweepFreq = if (pulseCycle < 0.15) 1200.0 - (1000.0 * (pulseCycle / 0.15)) else 440.0
                    val primaryWave = sin(2.0 * Math.PI * sweepFreq * t)
                    val envelope = if (pulseCycle < 0.2) kotlin.math.exp(-6.0 * pulseCycle) else 0.0
                    val tickCycle = t % 0.1
                    val tickWave = if (tickCycle < 0.01) {
                        sin(2.0 * Math.PI * 2000.0 * t) * kotlin.math.exp(-300.0 * tickCycle) * 0.2
                    } else 0.0
                    val combined = (primaryWave * 0.2 * envelope) + tickWave
                    pcmBuffer.putShort((combined * 32767.0).toInt().coerceIn(-32768, 32767).toShort())
                }
                fos.write(pcmBuffer.array())
            }
        } catch (e: Exception) {
            // best-effort
        }
    }

    private fun log(message: String) {
        _logEvents.tryEmit(message)
    }

    companion object {
        private const val SAMPLE_RATE = 22050
    }
}
