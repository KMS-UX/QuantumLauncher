package com.quantumos.appshell

import com.quantumos.core.QuantumLauncherState
import com.quantumos.core.QuantumStateEngine
import com.quantumos.core.QuarkReflexPosture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/*
 * QuantumStateRuntime — the seam that lets a DOCKED MODULE read and drive the one live
 * QuantumStateEngine (B4).
 *
 * The blocker this removes has been recorded on the QUARK track since Phase 4b: `:app` depends on
 * every docked module and never the reverse, so nothing docked could reach `QuantumRuntime.engine`.
 * QUARK's own avatar has therefore been running on LOCAL DEMO TOGGLES — a simulation of the unit
 * sitting inside the unit — while the real posture, the real stealth mode and the real speaking flag
 * went unread a few objects away.
 *
 * Worth being precise about what was actually missing, because the workplan described it as a bigger
 * job than it is: `QuantumStateEngine`, `QuantumLauncherState` and `QuarkReflexPosture` were ALWAYS
 * in `:core`, which every module already depends on. What could not be reached was the live
 * INSTANCE, which `:app` owns. So this is a publishing seam, not an extraction.
 *
 * The shape is deliberately the same as [PhosphorHueRuntime], which solved the identical problem for
 * the active hue: one process-wide object beneath everything, `:app` feeds it, docked modules read it
 * with `collectAsState()`. Same seam, same reasoning, and a second use of it confirms the pattern
 * rather than inventing a new one.
 *
 * **Before `:app` publishes, `masterState` reports defaults rather than null.** A docked module can
 * be built and previewed without a booted launcher, and every consumer would otherwise need its own
 * null branch. The actuators below are no-ops until then, for the same reason.
 */
object QuantumStateRuntime {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var engine: QuantumStateEngine? = null

    private val _masterState = MutableStateFlow(QuantumLauncherState())

    /** The live launcher state. Defaults until `:app` calls [publish] at boot. */
    val masterState: StateFlow<QuantumLauncherState> = _masterState.asStateFlow()

    /** True once `:app` has handed over its engine — i.e. the values above are real. */
    val isLive: Boolean get() = engine != null

    /**
     * Called by `:app` at boot with the one engine. Idempotent: republishing the same engine does
     * nothing, so a re-entrant boot cannot stack a second collector on the same flow.
     */
    fun publish(source: QuantumStateEngine) {
        if (engine === source) return
        engine = source
        scope.launch { source.masterState.collect { _masterState.value = it } }
    }

    // ---------- actuators ----------
    //
    // Docked modules drive the real unit through these rather than holding their own copy of the
    // state. Each is a no-op before publish(), so a module that runs standalone degrades to inert
    // controls instead of crashing.

    /** Engage or release Stealth across the whole unit. */
    fun toggleStealthMode() {
        engine?.toggleStealthMode()
    }

    /** Raise a QUARK posture. `intent` is what the LOG channel records the reason as. */
    fun dispatchReflex(
        intent: String,
        posture: QuarkReflexPosture,
        snippet: String = "",
        audioToken: String? = null,
    ) {
        engine?.dispatchQuarkReflex(intent, posture, snippet, audioToken)
    }

    /** Raise or clear QUARK's speaking flag. */
    fun setSpeaking(speaking: Boolean) {
        engine?.setSpeaking(speaking)
    }
}
