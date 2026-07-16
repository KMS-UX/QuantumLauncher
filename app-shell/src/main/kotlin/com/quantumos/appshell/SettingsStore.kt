package com.quantumos.appshell

import android.content.Context
import com.quantumos.core.BootPace
import com.quantumos.core.DeploymentRegion
import com.quantumos.core.PhosphorHue

/*
 * QuantumOS — persistent settings (M6, relocated to :app-shell by the SIGNAL + CONFIG Task Brief so
 * the docked :config module and the launcher read/write the SAME store — "the durable setting
 * source of truth," per the brief's CONFIG section, rather than a second copy). Plain
 * SharedPreferences, no DataStore/Room — the bar stays deliberately low, matching M6's original call.
 *
 * Pure key/value over enum names; unknown/missing values fall back to the shipped defaults
 * (Japan, Deliberate, Green) so a first boot — or a corrupted pref — is always safe.
 */
object SettingsStore {
    private const val PREFS = "quantumos_settings"
    private const val KEY_REGION = "deployment_region"
    private const val KEY_BOOT_PACE = "boot_pace"
    private const val KEY_PHOSPHOR_HUE = "phosphor_hue"

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun loadRegion(context: Context): DeploymentRegion =
        runCatching { DeploymentRegion.valueOf(prefs(context).getString(KEY_REGION, null) ?: "") }
            .getOrDefault(DeploymentRegion.JAPAN)

    fun loadBootPace(context: Context): BootPace =
        runCatching { BootPace.valueOf(prefs(context).getString(KEY_BOOT_PACE, null) ?: "") }
            .getOrDefault(BootPace.DELIBERATE)

    // CONFIG becomes the durable source of truth for the phosphor hue (SIGNAL + CONFIG Task Brief
    // §3) — previously this was in-memory-only in QuantumStateEngine and reset to green on relaunch
    // (a known M4 limit). Default GREEN matches the engine's own shipped default.
    fun loadPhosphorHue(context: Context): PhosphorHue =
        runCatching { PhosphorHue.valueOf(prefs(context).getString(KEY_PHOSPHOR_HUE, null) ?: "") }
            .getOrDefault(PhosphorHue.GREEN)

    fun saveRegion(context: Context, region: DeploymentRegion) {
        prefs(context).edit().putString(KEY_REGION, region.name).apply()
    }

    fun saveBootPace(context: Context, pace: BootPace) {
        prefs(context).edit().putString(KEY_BOOT_PACE, pace.name).apply()
    }

    fun savePhosphorHue(context: Context, hue: PhosphorHue) {
        prefs(context).edit().putString(KEY_PHOSPHOR_HUE, hue.name).apply()
    }
}
