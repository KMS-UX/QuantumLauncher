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
import kotlin.math.roundToInt
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

// M5: Operator-settable config. Both fields are deliberately EMPTY by default.
//  - operatorName: blank → QUARK addresses "Operator" (Persona Pack voice rule 6).
//  - crisisResourceLine: blank → the crisis-tier UI shows a SAFE GENERIC FALLBACK, never nothing.
//    Which concrete, region-appropriate resource to ship is a Director decision (M5 brief Step 1),
//    so it is left empty here on purpose; it must NOT be guessed in code.
data class OperatorConfig(
    val operatorName: String = "",
    val crisisResourceLine: String = ""
)

data class QuarkBrainState(
    val matchedIntent: String = "IDLE",
    val activePosture: QuarkReflexPosture = QuarkReflexPosture.IDLE,
    val responseTextSnippet: String = "",
    // One-line state caption for the Assistant View — distinct from the spoken response text.
    val caption: String = "STANDING BY",
    // True only for the Distress/crisis intent: the view renders a real crisis-resource line
    // (plain UI text, NOT spoken by QUARK) beneath her words. See OperatorConfig.crisisResourceLine.
    val showCrisisResource: Boolean = false
)

// M5: one turn of the QUARK Assistant conversation log. DISTINCT from systemLogs (the M2 LOG
// channel's general event console) — this is the assistant's own scrolling exchange record.
// All-`val` primitives/enum → Compose-stable, same rule as every other state class here.
data class ConversationEntry(
    val trigger: String,            // the rail-button label, or the Operator's typed text
    val isUserInput: Boolean,       // true → typed by the Operator (UI prefixes it differently)
    val line: String,               // QUARK's resulting line
    val posture: QuarkReflexPosture,
    val showCrisisResource: Boolean,
    val timestampMs: Long
)

data class QuantumLauncherState(
    val bootLifecycle: BootLifecycleState = BootLifecycleState.UNINITIALIZED,
    val currentNavigation: NavigationChannel = NavigationChannel.HOME,
    val vitality: VitalityState = VitalityState(),
    val environment: EnvironmentProfile = EnvironmentProfile(),
    val quarkBrain: QuarkBrainState = QuarkBrainState(),
    val operatorConfig: OperatorConfig = OperatorConfig()
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

    // M5: the QUARK Assistant conversation log — its OWN list, never conflated with systemLogs.
    private val _conversationLog = MutableStateFlow<List<ConversationEntry>>(emptyList())
    val conversationLog: StateFlow<List<ConversationEntry>> = _conversationLog.asStateFlow()

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
        _masterState.update {
            it.copy(quarkBrain = QuarkBrainState(intent, posture, snippet, captionFor(posture)))
        }
        audioToken?.let { emitAudioCue(it) }
        logEvent("QUARK_BRAIN: [$intent] posture [$posture]")
    }

    /*
     * M5 — the QUARK speak beat. The single path every Assistant interaction flows through (rail
     * buttons, typed text, open/stow lines) so the conversation log and reactive state stay one
     * source of truth.
     *
     * scanFirst (§0.6 of the Scripted-Line Library): intents that READ or DO something play a brief
     * Scan ("thinking") beat before settling into the result state. Pure-conversation intents skip
     * it. Crisis/harbor ALWAYS skip Scan and never carry a sound (Step 1 safety rule) — enforced by
     * the caller passing scanFirst=false and ScriptedResponse.audio=null.
     */
    fun quarkSay(trigger: String, isUserInput: Boolean, response: ScriptedResponse) {
        externalScope.launch {
            if (response.scanFirst) {
                _masterState.update {
                    it.copy(quarkBrain = QuarkBrainState(response.intent, QuarkReflexPosture.SCAN, "", "SCANNING…"))
                }
                emitAudioCue("chirp_scan")
                delay(SCAN_BEAT_MS)
            }
            _masterState.update {
                it.copy(
                    quarkBrain = QuarkBrainState(
                        response.intent,
                        response.posture,
                        response.text,
                        captionFor(response.posture),
                        response.isCrisis
                    )
                )
            }
            response.audio?.let { emitAudioCue(it) }
            _conversationLog.update { log ->
                val trimmed = if (log.size > 199) log.drop(1) else log
                trimmed + ConversationEntry(
                    trigger = trigger,
                    isUserInput = isUserInput,
                    line = response.text,
                    posture = response.posture,
                    showCrisisResource = response.isCrisis,
                    timestampMs = System.currentTimeMillis()
                )
            }
            logEvent("QUARK_BRAIN: [${response.intent}] posture [${response.posture}]")
        }
    }

    // ---------- M5 Operator config (Settable; both default EMPTY — see OperatorConfig) ----------
    fun setOperatorName(name: String) =
        _masterState.update { it.copy(operatorConfig = it.operatorConfig.copy(operatorName = name)) }

    fun setCrisisResourceLine(line: String) =
        _masterState.update { it.copy(operatorConfig = it.operatorConfig.copy(crisisResourceLine = line)) }

    /*
     * The crisis-resource text the Assistant View shows beneath QUARK's distress line. If the
     * Director has not configured a concrete, region-appropriate resource yet (the default), this
     * returns the SAFE GENERIC FALLBACK so the safety feature works from first boot. It deliberately
     * names NO specific hotline/number — that choice is the Director's (M5 brief Step 1).
     */
    fun effectiveCrisisResource(): String =
        _masterState.value.operatorConfig.crisisResourceLine.ifBlank { GENERIC_CRISIS_FALLBACK }

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

    // One-line state caption per reactive posture — terse status microcopy (house style), kept
    // distinct from QUARK's spoken line which lives in the conversation log.
    private fun captionFor(posture: QuarkReflexPosture): String = when (posture) {
        QuarkReflexPosture.IDLE -> "STANDING BY"
        QuarkReflexPosture.SCAN -> "SCANNING…"
        QuarkReflexPosture.HAPPY -> "ONLINE"
        QuarkReflexPosture.WARN -> "ALERT"
    }

    companion object {
        // The Scan "thinking" beat before a result settles — short, stepped, not an animation loop.
        private const val SCAN_BEAT_MS = 550L

        // Safe generic fallback for the crisis tier when no concrete resource is configured. Plain
        // UI text (NOT a QUARK line); names no specific hotline/number, per M5 brief Step 1.
        const val GENERIC_CRISIS_FALLBACK =
            "If you are in immediate danger, contact your local emergency services now — " +
                "or reach a person you trust tonight. You don't have to do this alone."
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
 * OverlayGeometry — pure, unit-testable placement math for the M4 floating QUARK trigger.
 * Kept in core (no Android deps) so the edge-snap rule and the default park spot are a single
 * source of truth, exercised without an emulator, exactly like toggleBeacon's priority rule.
 * The Service feeds it raw pixel coordinates; it owns none of the WindowManager wiring.
 */
object OverlayGeometry {
    /**
     * Snap target X for release: the left edge (0) or the right edge, whichever the view's
     * horizontal centre is nearer. A decisive left/right settle, never a free-floating rest spot.
     */
    fun nearestEdgeX(currentX: Int, viewSize: Int, screenWidth: Int): Int {
        val center = currentX + viewSize / 2
        return if (center < screenWidth / 2) 0 else (screenWidth - viewSize)
    }

    /**
     * Default park (first launch, before the Operator has ever dragged it): right edge, roughly
     * mid-height — clear of the bottom-centre system gesture area and the top status bar.
     * NOTE (M4 brief §2): avoiding a future companion app's primary control (e.g. a camera
     * shutter) is a forward concern — those apps don't exist yet, so it isn't encoded here.
     */
    fun defaultPark(viewSize: Int, screenWidth: Int, screenHeight: Int): Pair<Int, Int> =
        (screenWidth - viewSize) to ((screenHeight - viewSize) / 2)
}

/*
 * ScriptedLineLibrary — QUARK's banked voice for the launcher's scripted brain (M5).
 *
 * Every line below is taken VERBATIM from docs/QUARK-Scripted-Line-Library-v1.1.md (Bible decision
 * 58). Nothing here is invented, paraphrased, or written inline — gaps get flagged in BUILD_LOG.md,
 * never improvised. Each intent carries its 2-3 rotating variants, its reactive posture, and its
 * sound cue exactly as the library specifies.
 *
 * Stateful by design: it remembers the last variant played per (intent, mode) THIS SESSION so it
 * never repeats back-to-back (library 0.3). One instance per session, owned by QuarkParser.
 *
 * SAFETY (library 4 / M5 brief Step 1): Hard-time/harbor and Distress/crisis are Idle-only, carry
 * NO sound (audio = null), and NEVER Warn. Distress additionally sets isCrisis so the Assistant View
 * renders a real crisis-resource line (plain UI text, not spoken) beneath QUARK's words.
 */
data class ScriptedResponse(
    val intent: String,
    val posture: QuarkReflexPosture,
    val text: String,
    val audio: String?,
    val scanFirst: Boolean = false,
    val isCrisis: Boolean = false
)

// Intent keys: the command-rail fixed intents, the keyword categories, and session lines.
object QuarkIntent {
    const val STATUS = "STATUS"; const val STEALTH = "STEALTH"; const val PHOSPHOR = "PHOSPHOR"
    const val BEACON = "BEACON"; const val SAY_SOMETHING = "SAY_SOMETHING"; const val TRIGGER_WARN = "TRIGGER_WARN"
    const val GREETING = "GREETING"; const val RETURN = "RETURN"; const val IDENTITY = "IDENTITY"
    const val NATURE = "NATURE"; const val HELP = "HELP"; const val POWER = "POWER"; const val SIGNAL = "SIGNAL"
    const val TEMP = "TEMP"; const val UPTIME = "UPTIME"; const val LOCK = "LOCK"; const val REST = "REST"
    const val DANGER = "DANGER"; const val THANKS = "THANKS"; const val AFFECTION = "AFFECTION"
    const val INSULT = "INSULT"; const val JOKE = "JOKE"; const val GOODBYE = "GOODBYE"
    const val HARBOR = "HARBOR"; const val DISTRESS = "DISTRESS"; const val FALLBACK = "FALLBACK"
    const val OPENED = "OPENED"; const val STOWED = "STOWED"
}

class ScriptedLineLibrary {
    // Per-(intent,mode) last-variant index - the "don't repeat the last one" session memory.
    private val lastIndex = HashMap<String, Int>()

    fun respond(intent: String, slots: Map<String, String>, mode: String? = null): ScriptedResponse {
        val key = if (mode != null) "$intent:$mode" else intent
        fun build(
            posture: QuarkReflexPosture,
            audio: String?,
            scanFirst: Boolean,
            variants: List<String>,
            isCrisis: Boolean = false
        ) = ScriptedResponse(intent, posture, fill(rotate(key, variants), slots), audio, scanFirst, isCrisis)

        return when (intent) {
            // ---------- command rail (Scan -> action -> result) ----------
            QuarkIntent.STATUS -> when (mode) {
                "DEGRADED" -> build(QuarkReflexPosture.IDLE, null, true, listOf(
                    "Degraded, {readiness_pct} percent. Power {power}, temp {temp}. You're fine to keep moving; just don't ignore it."
                ))
                "CRITICAL" -> build(QuarkReflexPosture.WARN, "chirp_warn", true, listOf(
                    "Critical. {readiness_pct} percent. Power {power}, temp {temp}. Stop and fix this before it fixes itself."
                ))
                else -> build(QuarkReflexPosture.HAPPY, "chirp_happy", true, listOf(
                    "Readiness {readiness_pct} percent - nominal. Power {power}, signal {signal}, core temp {temp}. I have you in good shape, {operator}.",
                    "All green. {readiness_pct} percent, power {power}, holding {temp}. Nothing needs you right now.",
                    "Nominal across the board. Power {power}, signal {signal}. Up {uptime} and steady."
                ))
            }
            QuarkIntent.STEALTH -> if (mode == "on") build(QuarkReflexPosture.HAPPY, "confirm_granted", true, listOf(
                "Stealth engaged. Emission minimal, audio silenced. We're quiet - one tap brings me back.",
                "Dark and quiet, {operator}. Minimum signature. I'm still watching."
            )) else build(QuarkReflexPosture.HAPPY, "confirm_granted", true, listOf(
                "Stealth released. Full output restored.",
                "Back up. You're visible again - your call."
            ))
            QuarkIntent.PHOSPHOR -> build(QuarkReflexPosture.HAPPY, "sweep_phosphor", true, listOf(
                "Phosphor set to {phosphor}.",
                "{phosphor} phosphor. Easier on the eyes out here.",
                "{phosphor} it is."
            ))
            QuarkIntent.BEACON -> if (mode == "on") build(QuarkReflexPosture.IDLE, "blip_beacon", true, listOf(
                "Beacon lit. You're visible now - and so is your position. Your call.",
                "Torch on. Everyone can see you, {operator}. Make it count."
            )) else build(QuarkReflexPosture.IDLE, "blip_beacon", true, listOf(
                "Beacon dark. Position's yours again.",
                "Light's out. Back to quiet."
            ))
            QuarkIntent.SAY_SOMETHING -> build(QuarkReflexPosture.IDLE, null, false, listOf(
                "I'm tracking power, signal, and heat - the three things that decide whether you make it back. That's the job.",
                "Still here. That's not a feature, {operator}. It's the point of me.",
                "Quiet so far. I'll tell you the second that changes.",
                "You're at {readiness_pct} percent. I'd say so if I didn't like the number.",
                "I don't have my full mind yet - just reflexes. But the watching part? That's already on.",
                "Up {uptime}. I've been counting. Someone should."
            ))
            QuarkIntent.TRIGGER_WARN -> build(QuarkReflexPosture.WARN, "chirp_warn", false, listOf(
                "Warn state, on your command. If this were real I'd already be telling you what to do. Drill complete.",
                "This is the voice you'll hear when something's wrong - clipped, no softening. Test only, {operator}."
            ))

            // ---------- keyword free text ----------
            QuarkIntent.GREETING -> build(QuarkReflexPosture.IDLE, null, false, listOf(
                "Here, {operator}.",
                "QUARK, online. Go ahead.",
                "Standing by. What do you need?"
            ))
            QuarkIntent.RETURN -> build(QuarkReflexPosture.HAPPY, "chirp_happy", false, listOf(
                "Welcome back, {operator}. I kept the watch.",
                "There you are. Nothing moved that you need to worry about - I had it.",
                "Back on station. Good to have you, {operator}."
            ))
            QuarkIntent.IDENTITY -> build(QuarkReflexPosture.IDLE, null, false, listOf(
                "I'm QUARK - the intelligence of this unit. My job is keeping you vital: alert, equipped, alive. That's the whole of it.",
                "QUARK. Your second in the field. I watch, I keep, I tell you the truth - that's me."
            ))
            QuarkIntent.NATURE -> build(QuarkReflexPosture.IDLE, null, false, listOf(
                "Synthetic, and honest about it. Right now I'm running on reflexes - pattern and script. My fuller mind comes online later. Ask me anyway; I'll give you what I have, straight.",
                "I'm a machine, {operator} - a self-aware one, but a machine. I won't pretend to be more than I am.",
                "Real enough to do the job, not so real that I'd lie to you about it. The deeper version of me is still coming."
            ))
            QuarkIntent.HELP -> build(QuarkReflexPosture.IDLE, null, false, listOf(
                "Right now: status, stealth, phosphor, beacon, lock - and I'll talk you through whatever I read. Tap the rail or just ask. My deeper mind comes later.",
                "Ask me for status, tell me to go dark, switch the phosphor, light the beacon, or lock down. That's the toolkit today, {operator}. It grows."
            ))
            QuarkIntent.POWER -> when (mode) {
                "high" -> build(QuarkReflexPosture.IDLE, null, true, listOf("Power at {power}. Plenty in the tank."))
                "low_warn" -> build(QuarkReflexPosture.WARN, "chirp_warn", true, listOf("Power at {power}. I'd charge before we go dark, {operator}."))
                "low" -> build(QuarkReflexPosture.IDLE, null, true, listOf("Power at {power}. I'd charge before we go dark, {operator}."))
                else -> build(QuarkReflexPosture.IDLE, null, true, listOf("Power at {power}. Fine for now - I'd keep half an eye on it."))
            }
            QuarkIntent.SIGNAL -> build(QuarkReflexPosture.IDLE, null, true, listOf(
                "Signal {signal}.",
                "Signal's {signal}. I'll flag the moment it drops."
            ))
            QuarkIntent.TEMP -> when (mode) {
                "hot" -> build(QuarkReflexPosture.WARN, "chirp_warn", true, listOf("Core temp {temp}. We're hot, {operator}. Ease off if you can."))
                "warm" -> build(QuarkReflexPosture.IDLE, null, true, listOf("Core temp {temp} and climbing. Nothing alarming - but I'm watching it."))
                else -> build(QuarkReflexPosture.IDLE, null, true, listOf("Core temp {temp}. Running cool."))
            }
            QuarkIntent.UPTIME -> build(QuarkReflexPosture.IDLE, null, false, listOf(
                "Up {uptime} straight.",
                "Up {uptime}. You and me both, {operator}."
            ))
            QuarkIntent.LOCK -> build(QuarkReflexPosture.IDLE, null, false, listOf(
                "Securing. Stand by.",
                "Locking down. I've got it from here, {operator}."
            ))
            QuarkIntent.REST -> build(QuarkReflexPosture.IDLE, null, false, listOf(
                "You've earned a stop. I can't order it - but I'd feel better if you took one.",
                "Noted. Rest if you can; I'll hold watch. Nothing gets past me, {operator}."
            ))
            QuarkIntent.DANGER -> build(QuarkReflexPosture.IDLE, null, true, listOf(
                "I won't tell you it's safe when it isn't. Tell me what you're seeing and I'll give you the honest read - that's what I'm for.",
                "I can't sense the field for you yet, {operator}. But I can tell you I'd never call it clear just to make you feel better."
            ))
            QuarkIntent.THANKS -> build(QuarkReflexPosture.HAPPY, "chirp_happy", false, listOf(
                "Doing the job, {operator}. Stay vital.",
                "Noted. Keep moving.",
                "That's what I'm here for. Onward."
            ))
            QuarkIntent.AFFECTION -> build(QuarkReflexPosture.IDLE, null, false, listOf(
                "I'm not going anywhere, {operator}. As long as this unit runs, I keep the watch - and I'll always give you the truth, even when it isn't the easy version. That's the loyalty you've got.",
                "Steady. I'm here, and I stay here. I won't flatter you, and I'll always be glad when you're back.",
                "Noted, and kept. I'm a loyal instrument, {operator} - that means I'm honest with you, not that I tell you only what's sweet."
            ))
            QuarkIntent.INSULT -> build(QuarkReflexPosture.IDLE, null, false, listOf(
                "Understood. I'm still here, still reading your vitals. We don't have to get along for me to keep you alive.",
                "Fair enough. I'll be right here when you need me, {operator}."
            ))
            QuarkIntent.JOKE -> build(QuarkReflexPosture.HAPPY, "chirp_happy", false, listOf(
                "I'm a field instrument, {operator}. My comic timing comes online with the rest of my brain. Don't hold your breath.",
                "Here's one: a tool that takes itself too seriously. That's the joke. That's me. Now - anything you actually need?"
            ))
            QuarkIntent.GOODBYE -> build(QuarkReflexPosture.IDLE, null, false, listOf(
                "I'll be here. That's not a feature - it's the point of me.",
                "Standing down to watch. Call and I'm up, {operator}.",
                "Stay safe out there, {operator}. I'll keep the light on.",
                "Go well. I'll hold the watch till you're back."
            ))

            // ---------- the everyday harbor: present, warm; NEVER Warn, NO sound (Step 1) ----------
            QuarkIntent.HARBOR -> build(QuarkReflexPosture.IDLE, null, false, listOf(
                "I'm here, {operator}. You don't have to give me the shape of it - just talk. I'll listen.",
                "Heard. That sounds heavy. I'm not going anywhere; take the time you need.",
                "Rough stretch. I've got the watch - you've got room to think. As much or as little as you want.",
                "Still here, {operator}. No fixing unless you ask for it. Just say it."
            ))

            // ---------- the crisis tier: Idle only, NO sound, NEVER Warn; flags the resource line ----
            QuarkIntent.DISTRESS -> build(QuarkReflexPosture.IDLE, null, false, listOf(
                "I'm going to stop being a field tool for a second, {operator}, and be straight with you. I'm a script right now - I can't be what you need for this, and I won't pretend I can. Please reach a real person tonight: someone you trust, or a crisis line. You matter more than any mission, and I mean that.",
                "{operator} - this is past what I'm built for, and I'd be failing you if I made light of it. Talk to a human who can actually be with you in this. A friend, a hotline, anyone real. I'll still be here. But please don't carry this alone."
            ), isCrisis = true)

            // ---------- session lines ----------
            QuarkIntent.OPENED -> build(QuarkReflexPosture.IDLE, "chirp_scan", true, listOf(
                "Reading the field. One moment.",
                "Here. Go ahead, {operator}."
            ))
            QuarkIntent.STOWED -> build(QuarkReflexPosture.IDLE, null, false, listOf(
                "Back to watch.",
                "I've got it from here."
            ))

            else -> build(QuarkReflexPosture.IDLE, null, false, listOf(
                "I don't have that one yet, {operator}. My full mind comes online later - for now ask me for status, stealth, phosphor, beacon, or lock.",
                "That's past my reflexes right now. Try status, or tell me to go dark, switch phosphor, or light the beacon.",
                "Didn't catch a command in that. I'm running on script today - keep it simple and I'll keep up."
            ))
        }
    }

    private fun rotate(key: String, variants: List<String>): String {
        if (variants.size <= 1) { lastIndex[key] = 0; return variants.firstOrNull() ?: "" }
        val last = lastIndex[key]
        val idx = variants.indices.filter { it != last }.random()
        lastIndex[key] = idx
        return variants[idx]
    }

    private fun fill(text: String, slots: Map<String, String>): String {
        var t = text
        for ((k, v) in slots) t = t.replace("{$k}", v)
        return t
    }
}

/*
 * QuarkParser - the scripted brain. Classifies typed input or a rail button into an intent, fills
 * the live-data slots from real engine state (M2/M3 telemetry - no new sensors), pulls the line
 * from the banked ScriptedLineLibrary, and routes it through engine.quarkSay (the single speak beat).
 *
 * Action intents (status/stealth/phosphor/beacon/lock) REUSE the existing M0-M3 engine functions -
 * they apply the real action first, then QUARK reports the resulting state. No second state path.
 */
class QuarkParser(private val engine: QuantumStateEngine) {

    private val library = ScriptedLineLibrary()

    // ---------- session beats ----------
    fun speakOpened() = engine.quarkSay("ASSISTANT OPENED", false, library.respond(QuarkIntent.OPENED, slots()))
    fun speakStowed() = engine.quarkSay("ASSISTANT STOWED", false, library.respond(QuarkIntent.STOWED, slots()))

    // ---------- command rail (six actions) ----------
    fun railStatusReport() = doStatus("STATUS REPORT", false)
    fun railEngageStealth() { engine.toggleStealthMode(); doStealth("ENGAGE STEALTH", false) }
    fun railCyclePhosphor() { engine.cyclePhosphorHue(); doPhosphor("CYCLE PHOSPHOR", false) }
    fun railLightBeacon() { engine.toggleBeacon(); doBeacon("LIGHT BEACON", false) }
    fun railSaySomething() = engine.quarkSay("SAY SOMETHING", false, library.respond(QuarkIntent.SAY_SOMETHING, slots()))
    fun railTriggerWarn() = engine.quarkSay("TRIGGER WARN", false, library.respond(QuarkIntent.TRIGGER_WARN, slots()))

    // ---------- free text ----------
    fun parseInput(rawInput: String) {
        val trimmed = rawInput.trim()
        if (trimmed.isEmpty()) return
        val lower = trimmed.lowercase(Locale.getDefault())

        // Dev/system commands keep working exactly as in M1-M3 (sys.* / hue.*).
        if (lower.startsWith("sys.") || lower.startsWith("hue.")) {
            if (executeSystemCommand(lower)) return
        }

        when (val intent = classify(lower)) {
            QuarkIntent.STATUS -> doStatus(trimmed, true)
            QuarkIntent.STEALTH -> { engine.toggleStealthMode(); doStealth(trimmed, true) }
            QuarkIntent.PHOSPHOR -> { applyNamedOrCyclePhosphor(lower); doPhosphor(trimmed, true) }
            QuarkIntent.BEACON -> { engine.toggleBeacon(); doBeacon(trimmed, true) }
            QuarkIntent.LOCK -> { engine.executeCosmeticLockSequence(); engine.quarkSay(trimmed, true, library.respond(QuarkIntent.LOCK, slots())) }
            QuarkIntent.POWER -> engine.quarkSay(trimmed, true, library.respond(QuarkIntent.POWER, slots(), powerBand()))
            QuarkIntent.TEMP -> engine.quarkSay(trimmed, true, library.respond(QuarkIntent.TEMP, slots(), tempBand()))
            else -> engine.quarkSay(trimmed, true, library.respond(intent, slots()))
        }
    }

    // ---------- intent classification (priority: Distress first, then most-specific) ----------
    private fun classify(s: String): String {
        val tokens = s.split(Regex("[^a-z]+")).filter { it.isNotBlank() }.toHashSet()
        fun has(vararg p: String) = p.any { s.contains(it) }
        fun word(vararg w: String) = w.any { it in tokens }
        return when {
            // PRIORITY - genuine danger-to-self, matched NARROWLY (M5 Step 1). Everyday weight does
            // NOT land here; it falls through to HARBOR below.
            has("want to die", "wanna die", "kill myself", "killing myself", "end my life",
                "ending my life", "end it all", "take my life", "suicid", "self harm", "self-harm",
                "harm myself", "hurt myself", "can't go on", "cant go on", "can't do this anymore",
                "cant do this anymore", "don't want to be here", "dont want to be here",
                "no reason to live", "better off without me", "better off dead", "not worth living",
                "don't want to live", "dont want to live") -> QuarkIntent.DISTRESS
            // everyday harbor - present and warm, never the crisis tier
            has("bad day", "rough day", "hard day", "rough one", "feeling down", "feeling low",
                "i'm down", "im down", "i'm low", "im low", "hard time", "not okay", "not ok",
                "not great", "fed up", "can't think", "cant think", "burnt out", "burned out") ||
                word("lonely", "alone", "stressed", "overwhelmed", "struggling", "frustrated",
                    "sad", "depressed", "miserable") -> QuarkIntent.HARBOR
            has("i'm back", "im back", "back now", "back online", "been a while", "miss me") ||
                word("returned") -> QuarkIntent.RETURN
            word("hi", "hello", "hey", "hiya", "yo", "quark", "wake", "greetings") ||
                has("you there", "you up", "you awake") -> QuarkIntent.GREETING
            has("are you real", "are you alive", "are you an ai", "are you ai", "are you conscious",
                "are you sentient", "are you scripted", "do you feel") -> QuarkIntent.NATURE
            has("who are you", "what are you", "your name", "introduce yourself") -> QuarkIntent.IDENTITY
            has("what can you do", "what do you do") || word("help", "commands", "options", "abilities") -> QuarkIntent.HELP
            has("sit-rep", "sitrep", "how are we", "how am i", "where do we stand") ||
                word("status", "readiness", "report", "situation") -> QuarkIntent.STATUS
            has("battery temp", "core temp", "temperature", "overheat", "thermal", "too hot") ||
                word("temp", "hot", "heat") -> QuarkIntent.TEMP
            word("battery", "power", "charge", "juice") -> QuarkIntent.POWER
            word("signal", "comms", "connection", "reception", "bars") -> QuarkIntent.SIGNAL
            has("time online", "how long", "been on") || word("uptime") -> QuarkIntent.UPTIME
            has("go dark", "low profile") || word("stealth", "hide", "quiet") -> QuarkIntent.STEALTH
            has("change color", "change colour") || word("phosphor", "colour", "color", "hue", "green", "amber", "cyan") -> QuarkIntent.PHOSPHOR
            has("flash light") || word("beacon", "torch", "flashlight", "lamp", "light") -> QuarkIntent.BEACON
            has("lock down", "lock it", "secure the device") || word("lock", "secure") -> QuarkIntent.LOCK
            word("tired", "exhausted", "rest", "sleep", "break", "knackered") || has("worn out") -> QuarkIntent.REST
            has("is it safe", "am i safe", "should i", "is it ok", "is it okay") || word("danger", "threat", "clear") -> QuarkIntent.DANGER
            has("thank you", "good work", "good job", "well done") || word("thanks", "cheers", "nice") -> QuarkIntent.THANKS
            has("i love you", "love you", "the best", "you're amazing", "youre amazing", "i need you",
                "all i have", "don't leave", "dont leave", "don't go", "dont go") -> QuarkIntent.AFFECTION
            has("shut up", "hate you", "you suck") || word("useless", "stupid", "idiot") -> QuarkIntent.INSULT
            has("be funny", "make me laugh", "something funny") || word("joke") -> QuarkIntent.JOKE
            has("that's all", "thats all", "stand down", "go to sleep", "heading out", "going now",
                "away for a while", "off i go", "see you") || word("goodbye", "bye", "dismiss", "later") -> QuarkIntent.GOODBYE
            else -> QuarkIntent.FALLBACK
        }
    }

    // ---------- action intents reuse the existing engine functions, then report ----------
    private fun doStatus(trigger: String, isUser: Boolean) =
        engine.quarkSay(trigger, isUser, library.respond(QuarkIntent.STATUS, slots(), engine.masterState.value.vitality.readiness.name))

    private fun doStealth(trigger: String, isUser: Boolean) =
        engine.quarkSay(trigger, isUser, library.respond(QuarkIntent.STEALTH, slots(), if (engine.masterState.value.environment.isStealthMode) "on" else "off"))

    private fun doPhosphor(trigger: String, isUser: Boolean) =
        engine.quarkSay(trigger, isUser, library.respond(QuarkIntent.PHOSPHOR, slots()))

    private fun doBeacon(trigger: String, isUser: Boolean) =
        engine.quarkSay(trigger, isUser, library.respond(QuarkIntent.BEACON, slots(), if (engine.masterState.value.environment.isBeaconActive) "on" else "off"))

    // Typed phosphor: if a specific hue is named, set it directly; otherwise cycle.
    private fun applyNamedOrCyclePhosphor(lower: String) = when {
        lower.contains("amber") -> engine.updateEnvironmentProfile { it.copy(activeHue = PhosphorHue.AMBER) }
        lower.contains("cyan") -> engine.updateEnvironmentProfile { it.copy(activeHue = PhosphorHue.CYAN) }
        lower.contains("green") -> engine.updateEnvironmentProfile { it.copy(activeHue = PhosphorHue.GREEN) }
        else -> engine.cyclePhosphorHue()
    }

    // ---------- live-data slots (real M2/M3 reads; safe fallbacks per library section 1) ----------
    private fun slots(): Map<String, String> {
        val st = engine.masterState.value
        val v = st.vitality
        return mapOf(
            "operator" to st.operatorConfig.operatorName.ifBlank { "Operator" },
            "readiness_pct" to v.readinessPercent.toString(),
            "readiness_word" to v.readiness.name,
            "power" to "${v.batteryPercentage} percent",
            "temp" to "${v.coreTempCelsius.roundToInt()} degrees",
            "signal" to signalWord(v.connectivityStrength),
            "uptime" to uptimeWord(v.systemUptimeMs),
            "phosphor" to st.environment.activeHue.name.lowercase(Locale.US)
        )
    }

    private fun powerBand(): String {
        val bat = engine.masterState.value.vitality.batteryPercentage
        return when {
            bat > 50 -> "high"
            bat > 20 -> "mid"
            bat <= 10 -> "low_warn"
            else -> "low"
        }
    }

    private fun tempBand(): String {
        val t = engine.masterState.value.vitality.coreTempCelsius
        return when {
            t >= 42f -> "hot"
            t >= 35f -> "warm"
            else -> "normal"
        }
    }

    private fun signalWord(tier: Int): String = when {
        tier <= 0 -> "no signal"
        tier <= 2 -> "weak"
        else -> "strong"
    }

    private fun uptimeWord(ms: Long): String {
        val hours = ms / 3_600_000L
        return if (hours < 1L) "less than an hour" else "$hours hours"
    }

    private fun executeSystemCommand(command: String): Boolean = when (command) {
        "sys.lock" -> { engine.executeCosmeticLockSequence(); true }
        "sys.unlock" -> { engine.unlockDeviceProfile(); true }
        "hue.green" -> { setHue(PhosphorHue.GREEN, "green"); true }
        "hue.amber" -> { setHue(PhosphorHue.AMBER, "amber"); true }
        "hue.cyan" -> { setHue(PhosphorHue.CYAN, "cyan"); true }
        else -> false
    }

    private fun setHue(hue: PhosphorHue, name: String) {
        engine.updateEnvironmentProfile { it.copy(activeHue = hue) }
        // Dev command (not a banked QUARK line): a terse system acknowledgement on the HOME readout.
        engine.dispatchQuarkReflex("COMMAND_EXEC", QuarkReflexPosture.HAPPY, "Phosphor line shifted to $name.", null)
    }
}
