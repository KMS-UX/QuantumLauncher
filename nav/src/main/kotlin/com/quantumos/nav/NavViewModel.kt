package com.quantumos.nav

import androidx.lifecycle.ViewModel
import com.quantumos.core.PhosphorHue
import com.quantumos.nav.core.CoordinateParseResult
import com.quantumos.nav.core.DEFAULT_SECTOR
import com.quantumos.nav.core.NavCoordinates
import com.quantumos.nav.core.SectorPreset
import com.quantumos.nav.core.next
import com.quantumos.nav.core.parseCoordinates
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** GPS locator readout — three discrete states, terse status microcopy (never a chatty sentence). */
sealed class GpsReadout {
    data object Scanning : GpsReadout()
    data class Fix(val latitude: Double, val longitude: Double, val altitudeMeters: Double) : GpsReadout()
    data object PermissionDenied : GpsReadout()
}

/** A warp request the map consumes. [id] increments so a repeat warp to the same coords still fires. */
data class WarpRequest(val id: Long, val destination: NavCoordinates, val zoom: Double)

/*
 * QuantumOS Nav — UI-state holder. Survives fold/unfold/rotate (platform rule: config-surviving state
 * lives in a ViewModel, not composition). Holds only the small nav-local state; the actual parse/step
 * logic lives in com.quantumos.nav.core so it's unit-tested without Android.
 */
class NavViewModel : ViewModel() {

    private val _activeHue = MutableStateFlow(PhosphorHue.GREEN)
    val activeHue: StateFlow<PhosphorHue> = _activeHue.asStateFlow()

    private val _gps = MutableStateFlow<GpsReadout>(GpsReadout.Scanning)
    val gps: StateFlow<GpsReadout> = _gps.asStateFlow()

    // Transient terse status line (e.g. "INVALID COORDINATES"), shown in-surface instead of a Toast.
    private val _statusLine = MutableStateFlow<String?>(null)
    val statusLine: StateFlow<String?> = _statusLine.asStateFlow()

    private val _warp = MutableStateFlow<WarpRequest?>(null)
    val warp: StateFlow<WarpRequest?> = _warp.asStateFlow()

    private var warpCounter = 0L

    fun cyclePhosphor() { _activeHue.value = _activeHue.value.next() }

    fun onGpsFix(lat: Double, lng: Double, alt: Double) { _gps.value = GpsReadout.Fix(lat, lng, alt) }
    fun onGpsPermissionDenied() { _gps.value = GpsReadout.PermissionDenied }

    /** Fire the initial broad-sector warp on cold boot (called once the map style is ready). */
    fun warpToDefaultSector() { requestWarp(DEFAULT_SECTOR, zoom = 9.0) }

    fun warpToPreset(preset: SectorPreset) {
        _statusLine.value = null
        requestWarp(preset.coordinates, zoom = preset.zoom)
    }

    /**
     * Return-to-current-location. Warps to the latest GPS fix if we have one; otherwise reports a
     * terse status rather than moving the camera (no fix to move to yet, or signal denied).
     */
    fun warpToCurrentLocation() {
        when (val fix = _gps.value) {
            is GpsReadout.Fix -> {
                _statusLine.value = null
                requestWarp(NavCoordinates(fix.latitude, fix.longitude), zoom = 15.0)
            }
            GpsReadout.Scanning -> _statusLine.value = "ACQUIRING FIX…"
            GpsReadout.PermissionDenied -> _statusLine.value = "LOCATION DENIED"
        }
    }

    /** Validate typed entry; on success warp, on failure surface a terse in-surface status line. */
    fun warpToEntry(latText: String, lngText: String) {
        when (val result = parseCoordinates(latText, lngText)) {
            is CoordinateParseResult.Valid -> {
                _statusLine.value = null
                requestWarp(result.coordinates, zoom = 14.0)
            }
            CoordinateParseResult.Empty -> _statusLine.value = "AWAITING COORDS"
            CoordinateParseResult.OutOfRange -> _statusLine.value = "INVALID COORDINATES"
        }
    }

    private fun requestWarp(destination: NavCoordinates, zoom: Double) {
        _warp.value = WarpRequest(id = warpCounter++, destination = destination, zoom = zoom)
    }
}
