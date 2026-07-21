package com.quantumos.appshell

import android.content.Context
import com.quantumos.core.PhosphorHue
import com.quantumos.core.next
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/*
 * PhosphorHueRuntime — the single process-wide source of truth for the LIVE active hue (Core Apps
 * Polish Pass, Item 2). Every docked module (radio/audio/comms/signal/files/nav/optics) and CONFIG
 * previously held its own independent `remember { mutableStateOf(PhosphorHue.GREEN) }` or ViewModel
 * copy, so a change in one screen never showed up anywhere else without a restart — the exact bug
 * flagged in the brief. All of :app, :config, and the eight docked modules already depend on
 * :app-shell (it's the one module beneath all of them, same seam as Phosphor/Fonts/SettingsStore),
 * so this object — not a per-module copy — is the fix.
 *
 * Backed by the same durable SettingsStore CONFIG already used, so a cold-started module reads the
 * last persisted hue immediately (init()); a live change from ANY module updates this one in-memory
 * StateFlow, and every other module currently on screen (all in the same process — :app is the only
 * `com.android.application`, every feature module is a `com.android.library`) recomposes instantly
 * via collectAsState(), no restart required. The launcher's own engine state
 * (QuantumLauncherState.environment.activeHue) bridges to/from this in QuantumRuntime so the launcher
 * and QUARK stay on the exact same value too — see QuantumRuntime.kt.
 */
object PhosphorHueRuntime {
    private val _activeHue = MutableStateFlow(PhosphorHue.GREEN)
    val activeHue: StateFlow<PhosphorHue> = _activeHue.asStateFlow()

    private var initialized = false

    /** Seed the in-memory value from the durable store. Idempotent — safe to call from every module. */
    fun init(context: Context) {
        if (initialized) return
        initialized = true
        _activeHue.value = SettingsStore.loadPhosphorHue(context)
    }

    fun setHue(context: Context, hue: PhosphorHue) {
        if (_activeHue.value == hue) return
        _activeHue.value = hue
        SettingsStore.savePhosphorHue(context, hue)
    }

    fun cycleHue(context: Context) {
        setHue(context, _activeHue.value.next())
    }
}
