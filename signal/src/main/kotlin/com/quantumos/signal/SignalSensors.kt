package com.quantumos.signal

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.GnssStatus
import android.location.LocationManager
import android.net.wifi.WifiManager
import android.telephony.SignalStrength
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat

/*
 * SignalSensors -- registers the real, event-driven Android platform listeners behind SIGNAL's four
 * gauges (Task Brief §2). Deliberately NOT a polling loop: each source only reports on its own
 * genuine change event (signal-strength callback, RSSI/state-changed broadcast, GNSS status
 * callback, Bluetooth state/ACL broadcasts) -- "event-driven updates only (no idle polling loop)," the
 * discipline the brief calls out for the sparkline specifically, applied to every gauge here.
 *
 * Registration is owned by the calling Composable's lifecycle (start() while SIGNAL is visible,
 * stop() the moment it isn't -- see SignalScreen's DisposableEffect), so nothing runs while the
 * screen is closed: zero idle redraw/poll (acceptance §7).
 */
class SignalSensors(private val context: Context, private val viewModel: SignalViewModel) {

    private var telephonyCallback: TelephonyCallback? = null
    private var wifiReceiver: BroadcastReceiver? = null
    private var gnssCallback: GnssStatus.Callback? = null
    private var bluetoothReceiver: BroadcastReceiver? = null

    private fun granted(permission: String) =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    fun start() {
        startCellular()
        startWifi()
        startGps()
        startBluetooth()
    }

    fun stop() {
        telephonyCallback?.let { cb ->
            (context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager)
                ?.unregisterTelephonyCallback(cb)
        }
        telephonyCallback = null

        wifiReceiver?.let { runCatching { context.unregisterReceiver(it) } }
        wifiReceiver = null

        gnssCallback?.let { cb ->
            (context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager)
                ?.unregisterGnssStatusCallback(cb)
        }
        gnssCallback = null

        bluetoothReceiver?.let { runCatching { context.unregisterReceiver(it) } }
        bluetoothReceiver = null
    }

    // ---------- cellular ----------
    private fun startCellular() {
        if (!granted(Manifest.permission.READ_PHONE_STATE)) {
            viewModel.onCellularPermissionDenied()
            return
        }
        val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager ?: return
        val callback = object : TelephonyCallback(), TelephonyCallback.SignalStrengthsListener {
            override fun onSignalStrengthsChanged(signalStrength: SignalStrength) {
                viewModel.onCellularReading(signalStrength.level, cellularLabelFor(signalStrength.level))
            }
        }
        runCatching {
            tm.registerTelephonyCallback(context.mainExecutor, callback)
            telephonyCallback = callback
        }.onFailure { viewModel.onCellularPermissionDenied() }
    }

    private fun cellularLabelFor(level: Int): String = when (level) {
        0 -> "NONE"
        1 -> "POOR"
        2 -> "FAIR"
        3 -> "GOOD"
        4 -> "EXCELLENT"
        else -> "UNKNOWN"
    }

    // ---------- Wi-Fi (no dangerous permission needed) ----------
    private fun startWifi() {
        val appContext = context.applicationContext
        val wifiManager = appContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager ?: return

        fun report() {
            val enabled = runCatching { wifiManager.isWifiEnabled }.getOrDefault(false)
            val rssi = if (enabled) {
                runCatching { wifiManager.connectionInfo?.rssi }.getOrNull()?.takeIf { it != RSSI_UNKNOWN }
            } else null
            val label = when {
                !enabled -> "WI-FI OFF"
                rssi == null -> "OFFLINE"
                else -> "$rssi dBm"
            }
            viewModel.onWifiReading(rssi, label)
        }

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) = report()
        }
        val filter = IntentFilter().apply {
            addAction(WifiManager.RSSI_CHANGED_ACTION)
            addAction(WifiManager.WIFI_STATE_CHANGED_ACTION)
        }
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        wifiReceiver = receiver
        report()   // an immediate first reading rather than waiting for the next change event
    }

    // ---------- GPS ----------
    private fun startGps() {
        if (!granted(Manifest.permission.ACCESS_FINE_LOCATION)) {
            viewModel.onGpsPermissionDenied()
            return
        }
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return
        if (runCatching { !lm.isProviderEnabled(LocationManager.GPS_PROVIDER) }.getOrDefault(true)) {
            viewModel.onGpsReading(0, 0)   // reads honestly as NO FIX rather than staying blank
        }
        val callback = object : GnssStatus.Callback() {
            override fun onSatelliteStatusChanged(status: GnssStatus) {
                val visible = status.satelliteCount
                var used = 0
                for (i in 0 until visible) if (status.usedInFix(i)) used++
                viewModel.onGpsReading(used, visible)
            }
        }
        runCatching {
            lm.registerGnssStatusCallback(context.mainExecutor, callback)
            gnssCallback = callback
        }.onFailure { viewModel.onGpsPermissionDenied() }
    }

    // ---------- Bluetooth ----------
    private fun startBluetooth() {
        val adapter = (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter
        if (adapter == null) {
            viewModel.onBluetoothReading(adapterOn = false, bondedCount = 0, connectedCount = 0)
            return
        }
        if (!granted(Manifest.permission.BLUETOOTH_CONNECT)) {
            viewModel.onBluetoothPermissionDenied()
            return
        }

        fun report() {
            val on = runCatching { adapter.isEnabled }.getOrDefault(false)
            val bonded = runCatching { adapter.bondedDevices?.size ?: 0 }.getOrDefault(0)
            val connected = if (!on) 0 else CONNECTION_PROFILES.count { profile ->
                runCatching { adapter.getProfileConnectionState(profile) == BluetoothProfile.STATE_CONNECTED }
                    .getOrDefault(false)
            }
            viewModel.onBluetoothReading(on, bonded, connected)
        }

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) = report()
        }
        val filter = IntentFilter().apply {
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
        }
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        bluetoothReceiver = receiver
        report()
    }

    companion object {
        private const val RSSI_UNKNOWN = -127
        private val CONNECTION_PROFILES = intArrayOf(
            BluetoothProfile.HEADSET, BluetoothProfile.A2DP, BluetoothProfile.GATT
        )
    }
}
