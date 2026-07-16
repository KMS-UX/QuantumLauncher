package com.quantumos.quarkbrain

import com.quantumos.core.AiAssistBridge
import com.quantumos.core.AiAssistResult

/*
 * QuarkAiAssistBridge — the real AiAssistBridge implementation (QUARK Brain Promotion Task Brief,
 * decision 88). Interface shape is unchanged from NotYetWiredAiAssistBridge (:core) — this is a
 * one-line swap at every call site, exactly as the Core Apps Fix-Pass's own placeholder comment
 * promised. Wraps the ONE shared [QuarkOnDeviceBrain] instance (see [QuarkBrainProvider]) so every
 * caller — the QUARK Assistant View, FILES' DECRYPT AI / co-pilot chat — reads and drives the same
 * brain, not a private copy.
 *
 * Deliberately does NOT trigger a weights download on a bare ask() call: acquiring ~2.6 GB is an
 * explicit, consenting action that belongs to the Assistant View's acquisition panel (brief §3), not
 * something a background chat call from a docked module should kick off silently. If the weights are
 * already on disk but just not loaded into this process yet, loading (~10s, no network) is cheap
 * enough to pay here — the caller's own "please standby" beat covers it.
 */
class QuarkAiAssistBridge(private val brain: QuarkOnDeviceBrain) : AiAssistBridge {
    override suspend fun ask(prompt: String): AiAssistResult {
        if (!brain.isLoaded) {
            if (!brain.isPresent) return AiAssistResult.Unavailable(unavailableReason(brain.state.value))
            val loaded = brain.loadModel()
            if (!loaded) return AiAssistResult.Unavailable(unavailableReason(brain.state.value))
        }
        val reply = brain.reply(prompt)
        // QuarkOnDeviceBrain.reply() reports its own failures as a "[ERR: ...]" string rather than
        // throwing — surface those as Unavailable instead of a fabricated Answer.
        return if (reply.startsWith("[ERR:")) AiAssistResult.Unavailable(reply) else AiAssistResult.Answer(reply)
    }

    private fun unavailableReason(state: BrainReadyState): String = when (state) {
        is BrainReadyState.Idle ->
            "QUARK BRAIN NOT ACQUIRED — open the QUARK assistant and acquire her neural weights first."
        is BrainReadyState.Downloading -> "QUARK BRAIN ACQUIRING WEIGHTS — try again shortly."
        is BrainReadyState.NoNetwork -> "QUARK BRAIN OFFLINE — no network to acquire weights."
        is BrainReadyState.Err -> "QUARK BRAIN ERROR — ${state.message}"
        is BrainReadyState.Loading, is BrainReadyState.Downloaded -> "QUARK BRAIN LOADING — try again shortly."
        is BrainReadyState.Loaded -> "QUARK BRAIN LOAD FAILED — try again."
    }
}
