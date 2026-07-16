package com.quantumos.shell.ui

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.BatteryManager
import android.os.SystemClock
import com.quantumos.appshell.SettingsStore
import com.quantumos.core.BootLifecycleState
import com.quantumos.core.DeploymentRegion
import com.quantumos.core.QuantumStateEngine
import com.quantumos.core.QuarkParser
import com.quantumos.core.QuarkReflexPosture
import com.quantumos.quarkbrain.QuarkBrainProvider
import com.quantumos.quarkbrain.QuarkOnDeviceBrain
import com.quantumos.shell.ai.QuarkVoiceEngine
import com.quantumos.shell.ai.SherpaKokoroVoiceEngine
import com.quantumos.shell.ai.VoiceEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.quantumos.shell.ai.VoiceModelProvisioner

/*
 * QuantumRuntime — the process-level home of the ONE QuantumStateEngine (M5).
 *
 * Why this exists: the QUARK Assistant View is a separate Activity reached from the floating
 * trigger (M4), while the launcher chrome lives in LauncherActivity. For the four reused rail
 * actions to behave identically to their M3 originals, and for phosphor hue + Stealth to carry over
 * between the two surfaces, BOTH must read and mutate the SAME engine — not a per-Activity copy.
 * So the engine, the scripted-brain parser, the telemetry poll, and the connectivity label all live
 * here on an application-scoped coroutine scope that outlives any single Activity.
 *
 * (Window-level side effects — Stealth's screenBrightness — are still per-window and must be
 * re-applied in each Activity that wants them; see QuarkAssistantActivity. State carries over; the
 * window attribute does not. This is exactly the gap the M5 brief Step 7 flags.)
 */
object QuantumRuntime {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    // Ship default = DELIBERATE; the persisted Boot Pace (loaded in boot()) overrides this before the
    // cold-boot sequence actually runs. The old hardcoded SNAPPY was a dev-only convenience (M6 Step 2).
    val engine = QuantumStateEngine(appScope)
    val parser = QuarkParser(engine)

    // M6 Step 4 — procedural sound, app-scoped so cues sound regardless of which Activity is foreground.
    private val sound = SoundEngine()

    // UI-only connectivity label (transport type isn't part of core state) — shared so STATUS and
    // the assistant read one source.
    private val _connectivity = MutableStateFlow(ConnectivityInfo())
    val connectivity: StateFlow<ConnectivityInfo> = _connectivity.asStateFlow()

    private var appContext: Context? = null
    private var booted = false
    private var telemetryStarted = false
    private var audioStarted = false

    // QUARK's on-device brain — production default (QUARK Brain Promotion, decision 88). The ONE
    // process-wide instance lives in QuarkBrainProvider (:quark-brain) so :files' AiAssistBridge
    // reads/drives the exact same brain as this launcher's Assistant View, not a private copy.
    // appContext is set by boot() before any Activity can call onDeviceBrain().
    fun onDeviceBrain(): QuarkOnDeviceBrain = QuarkBrainProvider.onDeviceBrain(
        requireNotNull(appContext) { "QuantumRuntime.boot() must be called before onDeviceBrain()" }
    )

    // ── Kill switch (brief §4) ────────────────────────────────────────────────────────────────
    // The old Phase 1 debug toggle, retained as a hidden emergency fallback: forces the free-text
    // loop back onto the Scripted-Line Library even if the on-device brain is loaded and healthy.
    // Not persisted across process death — matches every prior debug-scaffolding toggle in this repo
    // (deliberate: a stuck kill switch should not survive a restart, so a fresh process always tries
    // the real brain again unless the Operator re-engages it).
    private val _killSwitchActive = MutableStateFlow(false)
    val killSwitchActive: StateFlow<Boolean> = _killSwitchActive.asStateFlow()
    fun toggleKillSwitch() { _killSwitchActive.value = !_killSwitchActive.value }

    // ── Voice (QUARK Brain Promotion §4 — production default; Phase 2a/2b built + hardware-confirmed
    // this pipeline already) ──────────────────────────────────────────────────────────────────
    // voiceEnabled now defaults ON — voice fires on real replies same as text, gated only by Stealth
    // (decision 38) and the kill switch's own scripted-only fallback still gets spoken too (the
    // rollback path shouldn't also go mute). The engine is built eagerly at boot() (initVoiceIfEnabled)
    // rather than waiting for a manual toggle.
    private val _voiceEnabled = MutableStateFlow(true)
    val voiceEnabled: StateFlow<Boolean> = _voiceEnabled.asStateFlow()

    private var _voiceEngine: VoiceEngine? = null
    private var voiceObserverStarted = false

    // Which voice identity to build. QUARK_H2 is her locked, Fold-6-confirmed custom voice (Phase 2b
    // "VOICE LOCKED", closed 2026-07-04) — now the production default. PLACEHOLDER (Android TTS) is
    // the automatic fallback buildVoiceEngine() already applies whenever the H2 sherpa-onnx model
    // isn't present on this device yet (voice model acquisition is still a manual import — see
    // BUILD_LOG judgment calls), so the voice loop never goes mute either way.
    enum class VoiceIdentity { PLACEHOLDER, QUARK_H2 }
    private val _voiceIdentity = MutableStateFlow(VoiceIdentity.QUARK_H2)
    val voiceIdentity: StateFlow<VoiceIdentity> = _voiceIdentity.asStateFlow()

    /** Select the voice identity and rebuild the engine so the change takes effect immediately. */
    fun setVoiceIdentity(identity: VoiceIdentity) {
        if (_voiceIdentity.value == identity) return
        _voiceIdentity.value = identity
        rebuildVoiceEngine()
    }

    /** Build the engine for the current identity, falling back to the placeholder if unavailable. */
    private fun buildVoiceEngine(ctx: Context): VoiceEngine {
        val engine: VoiceEngine =
            if (_voiceIdentity.value == VoiceIdentity.QUARK_H2 && SherpaKokoroVoiceEngine.isSupported(ctx))
                SherpaKokoroVoiceEngine(ctx)
            else
                QuarkVoiceEngine(ctx)
        engine.warmUp()   // hide any cold cost inside the reactive beat
        return engine
    }

    /**
     * Rebuild the voice engine after a change (voice identity flip, model just provisioned).
     * Builds — and so warms — the engine for the current identity immediately, regardless of
     * whether voice is currently switched on: the Director's usual flow picks `VOICE-ID: QUARK-H2`
     * a beat before flipping `VOICE: ON`, and the model-load cost is the single biggest chunk of
     * the reported cold-start latency, so paying it as early as possible (while they're still
     * poking at the debug menu) is what actually shrinks perceived latency — waiting for the ON
     * toggle just relocates the same cost to the worst possible moment, right before the first
     * line speaks. The observer (what makes speech audible) still only starts if voice is enabled.
     */
    fun rebuildVoiceEngine() {
        val ctx = appContext ?: return
        _voiceEngine?.shutdown()
        _voiceEngine = buildVoiceEngine(ctx)
        if (_voiceEnabled.value) startVoiceObserver()
    }

    // Status line for the QUARK-H2 model import (debug UI). Empty when idle.
    private val _voiceModelStatus = MutableStateFlow("")
    val voiceModelStatus: StateFlow<String> = _voiceModelStatus.asStateFlow()

    /**
     * Import a sherpa Kokoro model tarball (`*.tar.bz2`) the Operator picked, extract it on IO, and
     * rebuild the engine so QUARK-H2 goes live. Failures are surfaced in [voiceModelStatus], never
     * crash. See VoiceModelProvisioner / voice/quark-phase2b/HANDOFF.md.
     */
    fun importVoiceModel(context: Context, uri: Uri) {
        appScope.launch {
            _voiceModelStatus.value = "EXTRACTING…"
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(uri)?.use {
                        VoiceModelProvisioner.importModelTarBz2(context, it)
                    } ?: false
                }.getOrDefault(false)
            }
            _voiceModelStatus.value = if (ok) "MODEL READY" else "IMPORT FAILED"
            if (ok) rebuildVoiceEngine()
        }
    }

    fun toggleVoice() {
        _voiceEnabled.value = !_voiceEnabled.value
        if (_voiceEnabled.value) ensureVoiceEngineRunning()
    }

    // Build the engine (if needed) and start the observer — shared by toggleVoice()'s enable branch
    // and boot()'s eager init now that voice defaults ON. The engine may already exist —
    // rebuildVoiceEngine() (identity pick, model import) builds+warms eagerly regardless of this
    // call, precisely so the load cost is paid before this moment rather than here. Build only if
    // nothing exists yet; always (re)start the observer — it's idempotent, guarded by
    // voiceObserverStarted.
    private fun ensureVoiceEngineRunning() {
        val ctx = requireNotNull(appContext) { "QuantumRuntime.boot() must be called first" }
        if (_voiceEngine == null) {
            _voiceEngine = buildVoiceEngine(ctx)
        }
        startVoiceObserver()
    }

    /*
     * Stop any in-flight TTS utterance without settling posture. Call on Activity close so
     * a mid-sentence STOW doesn't leave orphaned audio after the view is gone. The voice
     * observer remains live; the next spoken line will start fresh.
     */
    fun stopCurrentSpeech() {
        _voiceEngine?.stop()
    }

    /*
     * Observe the conversation log and speak each new entry once it settles into its result
     * posture. Sequences after the non-verbal chirp (≈280 ms gap) so they never overlap.
     * Respects both the voice sub-toggle and the Stealth mute gate (decision 38). On completion,
     * dispatches IDLE so the reactive presence returns to rest — matching the brief's rule that
     * audio finishes → settle back to Idle.
     *
     * Safety: crisis entries (showCrisisResource == true) are never spoken — the resource line is
     * plain UI text shown beneath QUARK's words, and speaking it would undermine its careful tone.
     */
    private fun startVoiceObserver() {
        if (voiceObserverStarted) return
        voiceObserverStarted = true
        appScope.launch {
            var prevSize = engine.conversationLog.value.size   // skip entries already in the log
            engine.conversationLog.collect { log ->
                if (log.size <= prevSize) { prevSize = log.size; return@collect }
                val entry = log[prevSize]           // the one new entry (quarkSay adds one at a time)
                prevSize = log.size

                if (entry.showCrisisResource) return@collect    // safety rule — never speak crisis
                if (!_voiceEnabled.value) return@collect        // sub-toggle gate
                if (engine.masterState.value.environment.isStealthMode) return@collect  // Stealth

                // Brief gap to let the non-verbal chirp finish before speech begins (decision 45).
                delay(280)

                val callTime = System.currentTimeMillis()
                var audioStartTime = 0L
                _voiceEngine?.speak(
                    text = entry.line,
                    onStart = { t -> audioStartTime = t },
                    onDone = {
                        // Report latency to the LOG channel so the Director can read it.
                        val startLatencyMs = if (audioStartTime > 0) audioStartTime - callTime else -1
                        val playbackMs = System.currentTimeMillis() -
                            (if (audioStartTime > 0) audioStartTime else callTime)
                        engine.appendSystemLog(
                            "VOICE: TTS_START ${startLatencyMs}ms · PLAYBACK ${playbackMs}ms"
                        )
                        // Settle the reactive presence back to Idle once audio finishes.
                        engine.dispatchQuarkReflex("VOICE_DONE", QuarkReflexPosture.IDLE, "", null)
                    }
                )
            }
        }
    }
    // ──────────────────────────────────────────────────────────────────────────────────────────

    fun boot(context: Context) {
        appContext = context.applicationContext
        if (booted) return
        booted = true

        // Apply persisted settings BEFORE the cold-boot sequence so the chosen pace takes effect this
        // boot, the region is right from the first frame (M6 Step 0/2), and the phosphor hue reads
        // from CONFIG's durable store (SIGNAL + CONFIG Task Brief §3) instead of always booting green.
        appContext?.let { ctx ->
            engine.setBootPace(SettingsStore.loadBootPace(ctx))
            engine.setDeploymentRegion(SettingsStore.loadRegion(ctx))
            engine.updateEnvironmentProfile { it.copy(activeHue = SettingsStore.loadPhosphorHue(ctx)) }
        }

        startAudioPlayback()

        // Voice now defaults ON (QUARK Brain Promotion §4) — build + warm the engine here rather than
        // waiting for a manual toggle, same "pay the cold cost early" reasoning rebuildVoiceEngine()
        // already documents for the identity-pick path.
        if (_voiceEnabled.value) ensureVoiceEngineRunning()

        // Speak the §6 canon online line the moment cold boot reaches QUARK_ONLINE (live slots). Launch
        // the watcher BEFORE executeColdBootSequence so it can't miss the transient state.
        appScope.launch {
            engine.masterState.first { it.bootLifecycle == BootLifecycleState.QUARK_ONLINE }
            parser.speakOnline()
        }

        engine.executeColdBootSequence()
    }

    // Collect the engine's audio-cue stream and synthesise each token (gated by Stealth). Idempotent.
    private fun startAudioPlayback() {
        if (audioStarted) return
        audioStarted = true
        appScope.launch {
            engine.audioCueStream.collect { token ->
                sound.play(token, engine.masterState.value.environment.isStealthMode)
            }
        }
    }

    // Direct cue for pure-UI feedback (keypad tick, select clunk) — still routed through the same
    // Stealth gate as the stream so muting stays consistent. Optional gain (0..1): the gear-reel's
    // detent click is the first caller to use it (Launcher Restructure Phase 2 ratchet feel).
    fun playCue(token: String, gain: Float = 1f) =
        sound.play(token, engine.masterState.value.environment.isStealthMode, gain)

    // ---------- M3/M6 phosphor — cycle (Vitality panel's quick action) + persist in one place ----------
    // CONFIG (SIGNAL + CONFIG Task Brief) is the durable source of truth for this same setting; both
    // write through this one store so they can't drift into two independent values (brief §3).
    fun cyclePhosphorHue() {
        engine.cyclePhosphorHue()
        appContext?.let { SettingsStore.savePhosphorHue(it, engine.masterState.value.environment.activeHue) }
    }

    /*
     * CONFIG is a docked library module and cannot reach this live engine directly (it would be a
     * circular dependency — :app depends on :config to launch it) — see the App Shell Integration
     * BUILD_LOG note on AiAssistBridge for the same constraint. So CONFIG writes phosphor/region/
     * boot-pace straight to the shared SettingsStore, and the launcher re-reads it here on ON_RESUME
     * (the same "granted outside the app, re-detected on return" pattern as the M4 overlay
     * permission), applying anything CONFIG changed to the live engine — including firing QUARK's
     * existing region-switch acknowledgement line so a CONFIG-driven region change still speaks and
     * logs exactly like the old STATUS-row toggle did.
     */
    fun resyncPersistedSettings() {
        val ctx = appContext ?: return
        val persistedHue = SettingsStore.loadPhosphorHue(ctx)
        if (persistedHue != engine.masterState.value.environment.activeHue) {
            engine.updateEnvironmentProfile { it.copy(activeHue = persistedHue) }
        }
        val persistedRegion: DeploymentRegion = SettingsStore.loadRegion(ctx)
        if (persistedRegion != engine.masterState.value.deploymentRegion) {
            engine.setDeploymentRegion(persistedRegion)
            parser.speakRegionSwitched(persistedRegion)
        }
        val persistedPace = SettingsStore.loadBootPace(ctx)
        if (persistedPace != engine.masterState.value.bootPace) {
            engine.setBootPace(persistedPace)   // takes effect next cold boot; harmless to set now
        }
    }

    /*
     * Real telemetry poll (moved here from the ViewModel at M5 so it keeps feeding the shared engine
     * regardless of which Activity is foreground). One loop on the app scope, NOT a per-recomposition
     * read; feeds the existing engine.incomingTelemetryUpdate(...) seam so readiness stays one source
     * of truth, with the UI-only transport label riding alongside.
     */
    fun startTelemetry(context: Context) {
        if (telemetryStarted) return
        telemetryStarted = true
        val appContext = context.applicationContext
        appScope.launch {
            while (isActive) {
                val battery = readBattery(appContext)
                val conn = readConnectivity(appContext)
                val uptimeMs = SystemClock.elapsedRealtime()
                val signal = signalTier(conn)
                engine.incomingTelemetryUpdate(
                    battery.percent, battery.charging, uptimeMs, signal, battery.tempCelsius
                )
                _connectivity.value = conn
                delay(3000L) // functional reactive state, not an animation loop
            }
        }
    }

    private data class BatteryReading(val percent: Int, val charging: Boolean, val tempCelsius: Float)

    private fun readBattery(context: Context): BatteryReading {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val percent = if (level >= 0 && scale > 0) (level * 100) / scale else 0
        val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL
        val rawTemp = intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) ?: -1
        val tempCelsius = if (rawTemp > 0) rawTemp / 10f else 25.0f
        return BatteryReading(percent, charging, tempCelsius)
    }

    private fun readConnectivity(context: Context): ConnectivityInfo {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return ConnectivityInfo(false, "OFFLINE")
        val caps = cm.activeNetwork?.let { cm.getNetworkCapabilities(it) }
            ?: return ConnectivityInfo(false, "OFFLINE")
        val online = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        val transport = when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WI-FI"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "CELLULAR"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "ETHERNET"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> "VPN"
            else -> "UNKNOWN"
        }
        return if (online) ConnectivityInfo(true, transport) else ConnectivityInfo(false, "OFFLINE")
    }

    private fun signalTier(conn: ConnectivityInfo): Int = when {
        !conn.connected -> 0
        conn.transport == "WI-FI" || conn.transport == "ETHERNET" -> 4
        conn.transport == "CELLULAR" || conn.transport == "VPN" -> 2
        else -> 1
    }
}
