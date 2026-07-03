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
