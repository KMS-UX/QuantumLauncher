package com.quantumos.shell.ai

import kotlinx.coroutines.flow.StateFlow

/**
 * Lifecycle state shared by every QUARK voice engine.
 *
 * UNAVAILABLE is a first-class, non-fatal state: an engine that can't initialise (missing model,
 * no phonemizer, unsupported device) reports UNAVAILABLE and the runtime falls back to another
 * engine rather than crashing. A field tool never goes mute because one voice backend is absent.
 */
enum class VoiceReadyState { INITIALIZING, READY, UNAVAILABLE }

/**
 * The contract the voice observer in [com.quantumos.shell.ui.QuantumRuntime] speaks to. Both the
 * Phase 2a placeholder ([QuarkVoiceEngine], Android TextToSpeech) and the Phase 2b custom voice
 * ([SherpaKokoroVoiceEngine], the locked QUARK-H2 blend on sherpa-onnx) implement it, so swapping the
 * timbre never touches the caller — exactly the "replaces the engine wholesale, no API change"
 * design the Phase 2a engine was written toward.
 */
interface VoiceEngine {

    val readyState: StateFlow<VoiceReadyState>
    val isReady: Boolean

    /**
     * Short, LOG-channel-safe name of the backend actually speaking ("QUARK-H2" / "PLACEHOLDER").
     *
     * The Fold 6 pass that found this necessary: H2 with a missing/broken model does not error — it
     * silently speaks in the placeholder voice, or does not speak at all, and NOTHING on screen or
     * in the LOG says which. The debug panel's `MODEL: READY` row reports files on disk, not the
     * live engine, so it reads READY in both cases. Every voice line the runtime logs now carries
     * this, so "which voice was that" is answerable from the LOG instead of by ear.
     */
    val engineLabel: String

    /**
     * The last silent failure, in-character and short, or "" when the engine is healthy.
     *
     * Both of this pipeline's failure paths were mute by construction — a `!isReady` bail that
     * returns without a sound, and a blanket `catch (_: Throwable)` around synthesis — so a voice
     * that stopped working left no evidence anywhere, on-device or off. Engines now record why they
     * went quiet here and the runtime prints it to the LOG channel. Diagnostics only; nothing in the
     * UI branches on it, and it never turns a silent degrade into a crash.
     */
    val lastFault: String

    /**
     * Optional level readout for the last utterance (e.g. "GAIN 4.2x"), or "" when the engine does
     * no levelling of its own. Engines that render below full scale must lift themselves to a normal
     * speaking level; this reports how far they had to, so "still too quiet" is answerable with a
     * number rather than by ear. Android's TTS does its own levelling, so it has nothing to report.
     */
    val lastLevelInfo: String get() = ""

    /**
     * Warm the engine so the first real utterance is fast. Neural engines (Kokoro) pay a one-time
     * cold cost — running a throwaway inference at boot / first Scan hides that cost inside the
     * reactive beat (Phase 2a's cold-start trick, carried into 2b). No-op for engines with no cold
     * cost. Safe to call before [isReady]; implementations should defer until ready.
     */
    fun warmUp() {}

    /**
     * Speak [text]. [onStart] receives the wall-clock time (ms) audio actually begins — the anchor
     * for start-latency measurement. [onDone] fires when playback completes. If the engine is not
     * ready or errors, [onDone] is still called (immediately if need be) so the caller can always
     * assume the settle callback runs and the reactive posture never sticks.
     */
    fun speak(text: String, onStart: (Long) -> Unit, onDone: () -> Unit)

    /** Stop any in-flight utterance WITHOUT firing [onDone] — used on a clean close (Activity finish). */
    fun stop()

    fun shutdown()
}
