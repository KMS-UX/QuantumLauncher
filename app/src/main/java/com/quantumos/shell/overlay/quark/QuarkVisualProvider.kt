package com.quantumos.shell.overlay.quark

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.quantumos.core.QuantumLauncherState

/*
 * QuarkVisualProvider — the seam a future baked-Blender library swaps behind (Phase 3), per the
 * QUARK Core App Integration Spec §2a. Takes the whole shared state (not just posture/hue) so a
 * future implementation can react to anything without changing this interface, and renders
 * whatever it wants into the given box. [isSpeaking] is the only speech signal that actually
 * exists today (QuantumRuntime has no amplitude/viseme stream) — implementations get a coarse
 * start/stop boolean, not fabricated timing data.
 */
interface QuarkVisualProvider {
    @Composable
    fun RenderPresence(
        state: QuantumLauncherState,
        isSpeaking: Boolean,
        modifier: Modifier
    )
}
