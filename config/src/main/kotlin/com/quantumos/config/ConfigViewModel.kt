package com.quantumos.config

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.quantumos.appshell.PhosphorHueRuntime
import com.quantumos.appshell.SettingsStore
import com.quantumos.core.BootPace
import com.quantumos.core.DeploymentRegion
import com.quantumos.core.PhosphorHue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/*
 * ConfigViewModel -- CONFIG's state (Task Brief §3). CONFIG is a docked library module and cannot
 * reach the launcher's live QuantumStateEngine directly (it would be a circular dependency -- :app
 * depends on :config to launch it; see the App Shell Integration BUILD_LOG note on AiAssistBridge for
 * the same constraint elsewhere in this repo). So boot pace / region are read from -- and written
 * straight back to -- the shared :app-shell SettingsStore, the SAME store QuantumRuntime uses. That
 * store is the single durable source of truth (brief §3): the launcher re-reads it on its own
 * ON_RESUME (QuantumRuntime.resyncPersistedSettings()) so a change made here is picked up the moment
 * HOME is next shown, without CONFIG ever touching the engine.
 *
 * Phosphor hue is different (Core Apps Polish Pass, Item 2 fix): it no longer owns its own
 * MutableStateFlow seeded once from SettingsStore -- that was exactly the "two independent toggles"
 * drift risk the polish-pass brief flagged (a change from the Vitality panel would only reach CONFIG
 * on its next cold start). It now reads/writes PhosphorHueRuntime directly, the one process-wide live
 * StateFlow every docked module + the launcher shares -- so CONFIG's own row updates the instant
 * ANYTHING else changes the hue, and vice versa, no restart either way.
 */
class ConfigViewModel(application: Application) : AndroidViewModel(application) {

    private val ctx get() = getApplication<Application>()

    init { PhosphorHueRuntime.init(ctx) }

    val phosphorHue: StateFlow<PhosphorHue> get() = PhosphorHueRuntime.activeHue

    private val _bootPace = MutableStateFlow(SettingsStore.loadBootPace(ctx))
    val bootPace: StateFlow<BootPace> = _bootPace.asStateFlow()

    private val _region = MutableStateFlow(SettingsStore.loadRegion(ctx))
    val region: StateFlow<DeploymentRegion> = _region.asStateFlow()

    fun cyclePhosphorHue() {
        PhosphorHueRuntime.cycleHue(ctx)
    }

    fun cycleBootPace() {
        val next = when (_bootPace.value) {
            BootPace.DELIBERATE -> BootPace.SNAPPY
            BootPace.SNAPPY -> BootPace.DELIBERATE
        }
        _bootPace.value = next
        SettingsStore.saveBootPace(ctx, next)
    }

    fun cycleDeploymentRegion() {
        val next = when (_region.value) {
            DeploymentRegion.JAPAN -> DeploymentRegion.HONG_KONG
            DeploymentRegion.HONG_KONG -> DeploymentRegion.JAPAN
        }
        _region.value = next
        SettingsStore.saveRegion(ctx, next)
    }
}
