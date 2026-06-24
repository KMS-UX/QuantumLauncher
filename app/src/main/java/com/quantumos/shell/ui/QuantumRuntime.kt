package com.quantumos.shell.ui

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.SystemClock
import com.quantumos.core.BootPace
import com.quantumos.core.QuantumStateEngine
import com.quantumos.core.QuarkParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/*
 * QuantumRuntime — the process-level home of the ONE QuantumStateEngine (M5).
 *
 * Why this exists: the QUARK Assistant View is a separate Activity reached from the floating
 * trigger (M4), while the launcher chrome lives in LauncherActivity. For the four reused rail
 * actions to behave identically to their M3 originals, and for phosphor hue + Stealth to carry over
 * between the two surfaces, BOTH must read and mutate the SAME engine — not a per-Activity copy.
 * So the engine, the scripted-brain parser, the telemetry poll, and the connectivity label all live
 * here on an application-scoped coroutine scope that outlives any single Activity.
 *
 * (Window-level side effects — Stealth's screenBrightness — are still per-window and must be
 * re-applied in each Activity that wants them; see QuarkAssistantActivity. State carries over; the
 * window attribute does not. This is exactly the gap the M5 brief Step 7 flags.)
 */
object QuantumRuntime {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val engine = QuantumStateEngine(appScope, BootPace.SNAPPY) // SNAPPY dev boot; ship = DELIBERATE
    val parser = QuarkParser(engine)

    // UI-only connectivity label (transport type isn't part of core state) — shared so STATUS and
    // the assistant read one source.
    private val _connectivity = MutableStateFlow(ConnectivityInfo())
    val connectivity: StateFlow<ConnectivityInfo> = _connectivity.asStateFlow()

    private var booted = false
    private var telemetryStarted = false

    fun boot() {
        if (booted) return
        booted = true
        engine.executeColdBootSequence()
    }

    /*
     * Real telemetry poll (moved here from the ViewModel at M5 so it keeps feeding the shared engine
     * regardless of which Activity is foreground). One loop on the app scope, NOT a per-recomposition
     * read; feeds the existing engine.incomingTelemetryUpdate(...) seam so readiness stays one source
     * of truth, with the UI-only transport label riding alongside.
     */
    fun startTelemetry(context: Context) {
        if (telemetryStarted) return
        telemetryStarted = true
        val appContext = context.applicationContext
        appScope.launch {
            while (isActive) {
                val battery = readBattery(appContext)
                val conn = readConnectivity(appContext)
                val uptimeMs = SystemClock.elapsedRealtime()
                val signal = signalTier(conn)
                engine.incomingTelemetryUpdate(
                    battery.percent, battery.charging, uptimeMs, signal, battery.tempCelsius
                )
                _connectivity.value = conn
                delay(3000L) // functional reactive state, not an animation loop
            }
        }
    }

    private data class BatteryReading(val percent: Int, val charging: Boolean, val tempCelsius: Float)

    private fun readBattery(context: Context): BatteryReading {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val percent = if (level >= 0 && scale > 0) (level * 100) / scale else 0
        val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL
        val rawTemp = intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) ?: -1
        val tempCelsius = if (rawTemp > 0) rawTemp / 10f else 25.0f
        return BatteryReading(percent, charging, tempCelsius)
    }

    private fun readConnectivity(context: Context): ConnectivityInfo {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return ConnectivityInfo(false, "OFFLINE")
        val caps = cm.activeNetwork?.let { cm.getNetworkCapabilities(it) }
            ?: return ConnectivityInfo(false, "OFFLINE")
        val online = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        val transport = when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WI-FI"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "CELLULAR"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "ETHERNET"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> "VPN"
            else -> "UNKNOWN"
        }
        return if (online) ConnectivityInfo(true, transport) else ConnectivityInfo(false, "OFFLINE")
    }

    private fun signalTier(conn: ConnectivityInfo): Int = when {
        !conn.connected -> 0
        conn.transport == "WI-FI" || conn.transport == "ETHERNET" -> 4
        conn.transport == "CELLULAR" || conn.transport == "VPN" -> 2
        else -> 1
    }
}
