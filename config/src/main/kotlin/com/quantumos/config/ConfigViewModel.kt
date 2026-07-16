package com.quantumos.config

import android.app.Application
import androidx.lifecycle.AndroidViewModel
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
 * the same constraint elsewhere in this repo). So every value here is read from -- and every change
 * written straight back to -- the shared :app-shell SettingsStore, the SAME store QuantumRuntime uses.
 * That store is the single durable source of truth (brief §3): the launcher re-reads it on its own
 * ON_RESUME (QuantumRuntime.resyncPersistedSettings()) so a change made here is picked up the moment
 * HOME is next shown, without CONFIG ever touching the engine.
 *
 * CONFIG's own on-screen phosphor hue is seeded from this same store (not a throwaway local
 * `remember`, unlike the other docked modules' known pre-existing gap) -- this module's whole reason
 * to exist is to BE the durable setting, so its own chrome should read it, not a second local copy.
 */
class ConfigViewModel(application: Application) : AndroidViewModel(application) {

    private val ctx get() = getApplication<Application>()

    private val _phosphorHue = MutableStateFlow(SettingsStore.loadPhosphorHue(ctx))
    val phosphorHue: StateFlow<PhosphorHue> = _phosphorHue.asStateFlow()

    private val _bootPace = MutableStateFlow(SettingsStore.loadBootPace(ctx))
    val bootPace: StateFlow<BootPace> = _bootPace.asStateFlow()

    private val _region = MutableStateFlow(SettingsStore.loadRegion(ctx))
    val region: StateFlow<DeploymentRegion> = _region.asStateFlow()

    fun cyclePhosphorHue() {
        val next = when (_phosphorHue.value) {
            PhosphorHue.GREEN -> PhosphorHue.AMBER
            PhosphorHue.AMBER -> PhosphorHue.CYAN
            PhosphorHue.CYAN -> PhosphorHue.GREEN
        }
        _phosphorHue.value = next
        SettingsStore.savePhosphorHue(ctx, next)
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
