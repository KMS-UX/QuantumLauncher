package com.quantumos.signal

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.quantumos.core.FieldDecodeResult
import com.quantumos.core.FieldDecoder
import com.quantumos.core.SignalLevels
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/*
 * SignalViewModel -- SIGNAL's state (Task Brief §2). Pure state holder: the real Android platform
 * listeners live in SignalSensors, registered/unregistered by the Composable's own lifecycle so they
 * only run while this screen is actually open (zero idle poll, acceptance §7). This class never
 * touches a system service directly -- same split as AUDIO's AudioViewModel/AudioEngine.
 */
data class GaugeReading(
    val bars: Int = 0,
    val label: String = "--",
    val permissionGranted: Boolean = true
)

data class SignalUiState(
    val cellular: GaugeReading = GaugeReading(label = "NO SIGNAL"),
    val wifi: GaugeReading = GaugeReading(label = "OFFLINE"),
    val gps: GaugeReading = GaugeReading(label = "NO FIX"),
    val bluetooth: GaugeReading = GaugeReading(label = "OFF"),
    val sparkline: List<Int> = emptyList(),
    val isScanning: Boolean = false,
    val decoderInput: String = "",
    val isDecoding: Boolean = false,
    val decodeResult: FieldDecodeResult? = null
)

class SignalViewModel(application: Application) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(SignalUiState())
    val state: StateFlow<SignalUiState> = _state.asStateFlow()

    // ---------- gauge updates -- called only from real, event-driven platform callbacks ----------

    fun onCellularReading(level: Int, label: String) {
        _state.update { it.copy(cellular = GaugeReading(SignalLevels.cellularBars(level), label)) }
        recordSparklinePoint()
    }

    fun onCellularPermissionDenied() {
        _state.update { it.copy(cellular = GaugeReading(0, "ACCESS DENIED", permissionGranted = false)) }
    }

    fun onWifiReading(rssiDbm: Int?, label: String) {
        val bars = rssiDbm?.let { SignalLevels.wifiBars(it) } ?: 0
        _state.update { it.copy(wifi = GaugeReading(bars, label)) }
        recordSparklinePoint()
    }

    fun onGpsReading(satellitesUsed: Int, satellitesVisible: Int) {
        val bars = SignalLevels.gpsBars(satellitesUsed, satellitesVisible)
        val label = if (satellitesVisible <= 0) "NO FIX" else "$satellitesUsed/$satellitesVisible SATS"
        _state.update { it.copy(gps = GaugeReading(bars, label)) }
        recordSparklinePoint()
    }

    fun onGpsPermissionDenied() {
        _state.update { it.copy(gps = GaugeReading(0, "ACCESS DENIED", permissionGranted = false)) }
    }

    fun onBluetoothReading(adapterOn: Boolean, bondedCount: Int, connectedCount: Int) {
        val bars = SignalLevels.bluetoothBars(adapterOn, bondedCount, connectedCount)
        val label = when {
            !adapterOn -> "OFF"
            connectedCount > 0 -> "CONNECTED"
            bondedCount > 0 -> "PAIRED, IDLE"
            else -> "ON, NO PAIR"
        }
        _state.update { it.copy(bluetooth = GaugeReading(bars, label)) }
        recordSparklinePoint()
    }

    fun onBluetoothPermissionDenied() {
        _state.update { it.copy(bluetooth = GaugeReading(0, "ACCESS DENIED", permissionGranted = false)) }
    }

    // Rolling trace of overall link posture -- event-driven only (fired from the gauge updates
    // above and from RUN SCAN below), never a timer (Task Brief §2's sparkline requirement).
    private fun recordSparklinePoint() {
        val s = _state.value
        val bars = listOf(s.cellular.bars, s.wifi.bars, s.gps.bars, s.bluetooth.bars)
        val compositePercent = ((bars.average() / 4.0) * 100).toInt().coerceIn(0, 100)
        _state.update { it.copy(sparkline = (it.sparkline + compositePercent).takeLast(SPARKLINE_CAPACITY)) }
    }

    // ---------- RUN SCAN -- an explicit, user-triggered sweep, never continuous background scanning ----------
    fun runScan() {
        if (_state.value.isScanning) return
        viewModelScope.launch {
            _state.update { it.copy(isScanning = true) }
            delay(SCAN_BEAT_MS)          // one bounded stepped beat, never a loop
            recordSparklinePoint()       // force a fresh trace point even if nothing else changed
            _state.update { it.copy(isScanning = false) }
        }
    }

    // ---------- offline field decoder (Task Brief §2 decoder seed feature -- no AI, no network) ----------
    fun updateDecoderInput(text: String) = _state.update { it.copy(decoderInput = text) }

    fun runDecode() {
        if (_state.value.isDecoding) return
        val raw = _state.value.decoderInput
        viewModelScope.launch {
            _state.update { it.copy(isDecoding = true, decodeResult = null) }
            delay(DECODE_BEAT_MS)        // one bounded stepped beat, mirrors :comms's own terminal
            _state.update { it.copy(isDecoding = false, decodeResult = FieldDecoder.decode(raw)) }
        }
    }

    companion object {
        private const val SPARKLINE_CAPACITY = 40
        private const val SCAN_BEAT_MS = 900L
        private const val DECODE_BEAT_MS = 500L
    }
}
