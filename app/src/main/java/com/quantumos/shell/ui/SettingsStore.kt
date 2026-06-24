package com.quantumos.shell.ui

import android.content.Context
import com.quantumos.core.BootPace
import com.quantumos.core.DeploymentRegion

/*
 * QuantumOS — persistent settings (M6). The first place a setting needs to survive a real cold boot,
 * so the bar is deliberately low: plain SharedPreferences, no DataStore/Room (brief Step 0 — "simple
 * local storage is enough"). Both the Deployment Region and the Boot Pace persist through one small
 * mechanism here rather than two.
 *
 * Pure key/value over enum names; unknown/missing values fall back to the shipped defaults
 * (Japan, Deliberate) so a first boot — or a corrupted pref — is always safe.
 */
object SettingsStore {
    private const val PREFS = "quantumos_settings"
    private const val KEY_REGION = "deployment_region"
    private const val KEY_BOOT_PACE = "boot_pace"

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun loadRegion(context: Context): DeploymentRegion =
        runCatching { DeploymentRegion.valueOf(prefs(context).getString(KEY_REGION, null) ?: "") }
            .getOrDefault(DeploymentRegion.JAPAN)

    fun loadBootPace(context: Context): BootPace =
        runCatching { BootPace.valueOf(prefs(context).getString(KEY_BOOT_PACE, null) ?: "") }
            .getOrDefault(BootPace.DELIBERATE)

    fun saveRegion(context: Context, region: DeploymentRegion) {
        prefs(context).edit().putString(KEY_REGION, region.name).apply()
    }

    fun saveBootPace(context: Context, pace: BootPace) {
        prefs(context).edit().putString(KEY_BOOT_PACE, pace.name).apply()
    }
}
