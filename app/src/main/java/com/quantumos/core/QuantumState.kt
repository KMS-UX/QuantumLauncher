package com.quantumos.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.random.Random

/*
 * QuantumOS — CORE STATE + PARSER (pure Kotlin, NO UI / NO Compose deps).
 * Lives in com.quantumos.core so it unit-tests with zero emulator (runbook Step 4).
 * All state classes are all-`val` primitives/enums → Compose infers them as STABLE
 * automatically, which is what gives us "static at rest / skip recomposition."
 * (Explicit @Immutable is optional and would only require a compose-runtime dep.)
 */

// ---------- enums ----------
enum class BootLifecycleState {
    UNINITIALIZED, CRT_POWER_ON, STEP_CORE, STEP_PHOSPHOR_DRIVER,
    STEP_SENSOR_ARRAY, STEP_BIOMETRICS, STEP_QUARK, QUARK_ONLINE,
    WORDMARK_STAMP, PLEASE_STANDBY, ACTIVE, DEVICE_SECURED
}

// Ship default = DELIBERATE per Build Bible decision 59; SNAPPY is for the dev sim.
enum class BootPace(val stepDurationMs: Long) { SNAPPY(100L), DELIBERATE(600L) }

enum class NavigationChannel { HOME, APPS, STATUS, LOG }
enum class PhosphorHue { GREEN, AMBER, CYAN }
enum class SystemReadiness { NOMINAL, DEGRADED, CRITICAL }
enum class QuarkReflexPosture { IDLE, SCAN, HAPPY, WARN }

// ---------- state ----------
data class VitalityState(
    val batteryPercentage: Int = 100,
    val isCharging: Boolean = false,
    val systemUptimeMs: Long = 0L,
    val connectivityStrength: Int = 4,   // coarse tier 0..4 (offline=0, cellular≈2, wifi=4)
    val coreTempCelsius: Float = 28.5f,
    val readiness: SystemReadiness = SystemReadiness.NOMINAL
) {
    // Composite readiness headline for the M3 Vitality panel (Zone 1). A "% feel" derived from the
    // same three inputs the readiness word uses — power, signal, thermal headroom — averaged.
    // Pure/derived so it stays a single source of truth and is unit-testable.
    val readinessPercent: Int
        get() {
            val power = batteryPercentage.coerceIn(0, 100)
            val signal = connectivityStrength.coerceIn(0, 4) * 25
            val thermalHeadroom = (((50f - coreTempCelsius) / 25f) * 100f).coerceIn(0f, 100f).toInt()
            return (power + signal + thermalHeadroom) / 3
        }
}

data class EnvironmentProfile(
    val activeHue: PhosphorHue = PhosphorHue.GREEN,
    val isStealthMode: Boolean = false,
    val isBeaconActive: Boolean = false,
    val isSystemLocked: Boolean = false
)

data class QuarkBrainState(
    val matchedIntent: String = "IDLE",
    val activePosture: QuarkReflexPosture = QuarkReflexPosture.IDLE,
    val responseTextSnippet: String = ""
)

data class QuantumLauncherState(
    val bootLifecycle: BootLifecycleState = BootLifecycleState.UNINITIALIZED,
    val currentNavigation: NavigationChannel = NavigationChannel.HOME,
    val vitality: VitalityState = VitalityState(),
    val environment: EnvironmentProfile = EnvironmentProfile(),
    val quarkBrain: QuarkBrainState = QuarkBrainState()
)

// ---------- engine ----------
class QuantumStateEngine(
    private val externalScope: CoroutineScope,
    private val pace: BootPace = BootPace.DELIBERATE
) {
    private val _masterState = MutableStateFlow(QuantumLauncherState())
    val masterState: StateFlow<QuantumLauncherState> = _masterState.asStateFlow()

    private val _audioCueStream = MutableSharedFlow<String>(replay = 0)
    val audioCueStream: SharedFlow<String> = _audioCueStream.asSharedFlow()

    private val _systemLogs = MutableStateFlow<List<String>>(emptyList())
    val systemLogs: StateFlow<List<String>> = _systemLogs.asStateFlow()

    fun executeColdBootSequence() {
        // Cold-boot-only trigger (Bible decision 59).
        if (_masterState.value.bootLifecycle != BootLifecycleState.UNINITIALIZED) return
        externalScope.launch {
            logEvent("SYSTEM: Initiating cold-boot sequence.")
            updateBootLifecycle(BootLifecycleState.CRT_POWER_ON)
            delay(pace.stepDurationMs)

            val steppedLogs = listOf(
                BootLifecycleState.STEP_CORE to "CORE STATUS: SUCCESS",
                BootLifecycleState.STEP_PHOSPHOR_DRIVER to "PHOSPHOR LINE REGISTER: READY",
                BootLifecycleState.STEP_SENSOR_ARRAY to "SENSOR LAYER: ENUMERATING",
                BootLifecycleState.STEP_BIOMETRICS to "BIOMETRICS CHECK: PASS",   // was "PASSUP"
                BootLifecycleState.STEP_QUARK to "QUARK INTERACTION RAIL: COMMENCING"
            )
            for ((state, logMessage) in steppedLogs) {
                updateBootLifecycle(state)
                logEvent(logMessage)
                delay(pace.stepDurationMs)
            }

            updateBootLifecycle(BootLifecycleState.QUARK_ONLINE)
            emitAudioCue("SND_POWER_UP_SWEEP")               // Boot = power-up sweep (sound decision 44)
            logEvent("QUARK: Intelligence framework online.")
            delay(pace.stepDurationMs * 2)

            updateBootLifecycle(BootLifecycleState.WORDMARK_STAMP)
            delay(pace.stepDurationMs)
            updateBootLifecycle(BootLifecycleState.PLEASE_STANDBY)
            delay(pace.stepDurationMs)
            updateBootLifecycle(BootLifecycleState.ACTIVE)
            logEvent("SYSTEM: Launcher entry ready.")
        }
    }

    fun transitionNavigation(target: NavigationChannel) {
        if (_masterState.value.bootLifecycle != BootLifecycleState.ACTIVE) return
        _masterState.update { it.copy(currentNavigation = target) }
        logEvent("NAV_CH: Channel -> [$target]")
    }

    fun updateEnvironmentProfile(transform: (EnvironmentProfile) -> EnvironmentProfile) {
        _masterState.update { it.copy(environment = transform(it.environment)) }
    }

    // ---------- M3 Vitality-panel quick actions ----------
    // All reuse the single hue/environment mechanism above — no second state path.

    // Phosphor: cycle the active hue green → amber → cyan → green, live across the whole UI.
    fun cyclePhosphorHue() {
        val next = when (_masterState.value.environment.activeHue) {
            PhosphorHue.GREEN -> PhosphorHue.AMBER
            PhosphorHue.AMBER -> PhosphorHue.CYAN
            PhosphorHue.CYAN -> PhosphorHue.GREEN
        }
        updateEnvironmentProfile { it.copy(activeHue = next) }
        logEvent("ENV: Phosphor line shifted -> $next")
    }

    // Stealth: hard-dim the emission (screen brightness handled UI-side) + mute the app's own SFX.
    // Colour saturation is unchanged — only brightness drops. One tap toggles, fully reversible.
    fun toggleStealthMode() {
        val on = !_masterState.value.environment.isStealthMode
        updateEnvironmentProfile { it.copy(isStealthMode = on) }
        logEvent("ENV: Stealth ${if (on) "ENGAGED — emission dimmed" else "RELEASED"}")
    }

    // Beacon: toggle the real torch (UI-side) + raise the warn-red field flag on Home.
    // Designed interaction rule (M3 brief): turning Beacon ON force-drops Stealth — active
    // signalling outranks staying low-emission. Lives in core so it's unit-tested, not assumed.
    fun toggleBeacon() {
        val env = _masterState.value.environment
        val turningOn = !env.isBeaconActive
        val droppedStealth = turningOn && env.isStealthMode
        updateEnvironmentProfile {
            it.copy(
                isBeaconActive = turningOn,
                isStealthMode = if (droppedStealth) false else it.isStealthMode
            )
        }
        logEvent("ENV: Beacon ${if (turningOn) "ACTIVE — field flag raised" else "DARK"}")
        if (droppedStealth) logEvent("ENV: Stealth auto-released — Beacon takes priority.")
    }

    fun incomingTelemetryUpdate(bat: Int, chg: Boolean, upMs: Long, con: Int, temp: Float) {
        // Readiness composite = power + signal + temp (Bible §5 / glossary). Signal now included.
        val readiness = when {
            con <= 0 || temp >= 45.0f || bat <= 5  -> SystemReadiness.CRITICAL
            con <= 1 || temp >= 38.0f || bat <= 15 -> SystemReadiness.DEGRADED
            else -> SystemReadiness.NOMINAL
        }
        _masterState.update { it.copy(vitality = VitalityState(bat, chg, upMs, con, temp, readiness)) }
    }

    fun dispatchQuarkReflex(intent: String, posture: QuarkReflexPosture, snippet: String, audioToken: String?) {
        _masterState.update { it.copy(quarkBrain = QuarkBrainState(intent, posture, snippet)) }
        audioToken?.let { emitAudioCue(it) }
        logEvent("QUARK_BRAIN: [$intent] posture [$posture]")
    }

    fun executeCosmeticLockSequence() {
        // Cosmetic only — does NOT grab Device Admin (Bible decision 56). Real lockNow() arrives in kiosk.
        if (_masterState.value.bootLifecycle != BootLifecycleState.ACTIVE) return
        externalScope.launch {
            logEvent("SECURITY: Secure request acknowledged.")
            updateBootLifecycle(BootLifecycleState.PLEASE_STANDBY)
            emitAudioCue("SND_SECURING_BEAT")
            delay(500L)
            updateBootLifecycle(BootLifecycleState.DEVICE_SECURED)
            updateEnvironmentProfile { it.copy(isSystemLocked = true) }
            logEvent("SECURITY: Perimeter sealed.")
        }
    }

    fun unlockDeviceProfile() {
        if (_masterState.value.environment.isSystemLocked) {
            _masterState.update {
                it.copy(
                    bootLifecycle = BootLifecycleState.ACTIVE,
                    environment = it.environment.copy(isSystemLocked = false)
                )
            }
            logEvent("SECURITY: Unsealed. Welcome back, Operator.")
        }
    }

    private fun updateBootLifecycle(target: BootLifecycleState) {
        _masterState.update { it.copy(bootLifecycle = target) }
    }

    private fun emitAudioCue(token: String) {
        externalScope.launch { _audioCueStream.emit(token) }
    }

    private fun logEvent(message: String) {
        _systemLogs.update { logs ->
            val trimmed = if (logs.size > 149) logs.drop(1) else logs
            trimmed + "[${System.currentTimeMillis()}] $message"
        }
    }
}

/*
 * ScriptedLineLibrary — the SEAM that fixes the biggest structural issue:
 * QUARK's lines must come from the banked Scripted-Line Library v1.1 (Bible decision 58),
 * NOT be invented inline in the parser. Replace the placeholder map below by loading the
 * real v1.1 content (2-3 rotating variants + reactive state + sound cue + live-data slots).
 * Lines here are INTERIM placeholders only.
 */
data class ScriptedResponse(val posture: QuarkReflexPosture, val text: String, val audio: String?)

object ScriptedLineLibrary {
    fun respond(intent: String, slots: Map<String, String> = emptyMap()): ScriptedResponse = when (intent) {
        "VITALITY_CHECK" -> ScriptedResponse(
            QuarkReflexPosture.HAPPY,
            "System status: ${slots["readiness"] ?: "Nominal"}. Energy cells at ${slots["battery"]}%. " +
                "Core thermal at ${slots["temp"]}\u00B0C.",
            "SND_RECEIVE_DATA"
        )
        "POWER_CHECK" -> ScriptedResponse(
            QuarkReflexPosture.IDLE,
            "Power at ${slots["battery"]} percent. I'd charge soon if you're heading into the field.",
            "SND_BATTERY_LOW"
        )
        "IDENTITY_QUERY" -> ScriptedResponse(
            QuarkReflexPosture.HAPPY,
            "I am QUARK. I keep the watch, and I keep you running. That's the whole of me, Operator.",
            "SND_NEURAL_CLICK"
        )
        "COMMAND_EXEC" -> ScriptedResponse(
            QuarkReflexPosture.HAPPY, slots["ack"] ?: "Acknowledged.", null
        )
        else -> ScriptedResponse(
            QuarkReflexPosture.IDLE,
            "Standing by. I'm here — that isn't a feature, it's the point of me.",
            "SND_GENERIC_BLIP"
        )
    }
}

class QuarkParser(private val engine: QuantumStateEngine) {

    fun parseInput(rawInput: String) {
        val trimmed = rawInput.trim()
        if (trimmed.isEmpty()) return
        val input = trimmed.lowercase(Locale.getDefault())

        if (input.startsWith("sys.") || input.startsWith("hue.")) {
            if (executeSystemCommand(input)) return
        }

        val s = engine.masterState.value
        val bat = s.vitality.batteryPercentage
        val temp = String.format(Locale.US, "%.1f", s.vitality.coreTempCelsius)
        val readinessWord = when (s.vitality.readiness) {
            SystemReadiness.NOMINAL -> "Nominal"
            SystemReadiness.DEGRADED -> "Degraded"   // was hardcoded "Nominal" regardless of state
            SystemReadiness.CRITICAL -> "Critical"
        }

        val intent = when {
            input.contains("status") || input.contains("diagnostics") -> "VITALITY_CHECK"
            input.contains("power") || input.contains("battery") -> "POWER_CHECK"
            input.contains("who are you") || input.contains("identity") -> "IDENTITY_QUERY"
            else -> "UNKNOWN_FALLBACK"
        }

        val r = ScriptedLineLibrary.respond(
            intent,
            mapOf("battery" to bat.toString(), "temp" to temp, "readiness" to readinessWord)
        )
        engine.dispatchQuarkReflex(intent, r.posture, r.text, r.audio)
    }

    private fun executeSystemCommand(command: String): Boolean = when (command) {
        "sys.lock" -> { engine.executeCosmeticLockSequence(); true }
        "sys.unlock" -> { engine.unlockDeviceProfile(); true }
        "hue.green" -> { setHue(PhosphorHue.GREEN, "green"); true }
        "hue.amber" -> { setHue(PhosphorHue.AMBER, "amber"); true }
        "hue.cyan"  -> { setHue(PhosphorHue.CYAN, "cyan"); true }
        else -> false
    }

    private fun setHue(hue: PhosphorHue, name: String) {
        engine.updateEnvironmentProfile { it.copy(activeHue = hue) }
        val r = ScriptedLineLibrary.respond("COMMAND_EXEC", mapOf("ack" to "Phosphor line shifted to $name."))
        engine.dispatchQuarkReflex("COMMAND_EXEC", r.posture, r.text, r.audio)
    }
}
