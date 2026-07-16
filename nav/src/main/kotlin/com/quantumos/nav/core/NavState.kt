package com.quantumos.nav.core

import com.quantumos.core.PhosphorHue

/*
 * QuantumOS Nav — pure logic layer. No Android/UI dependency so it runs in a plain JVM unit test
 * (matches the launcher's com.quantumos.core convention: logic separated from Compose, unit-tested).
 *
 * App Shell Integration (Phase 3): PhosphorHue now comes from the shared :core module instead of a
 * locally duplicated enum -- one source for the hue cycle, shared by the launcher and every docked
 * module.
 */

data class NavCoordinates(val latitude: Double, val longitude: Double)

/** A named sector preset (Bible-style field terminology, not a generic "favorite"). */
data class SectorPreset(val label: String, val coordinates: NavCoordinates, val zoom: Double = 14.0)

object SectorPresets {
    // JAPAN overview first (broad theatre view), then the Japan area sectors, then the two
    // regional sectors that were already in the field set. The overview zooms out; cities zoom in.
    val ALL = listOf(
        SectorPreset("JAPAN", NavCoordinates(37.5, 137.0), zoom = 4.5),
        SectorPreset("TOKYO", NavCoordinates(35.6762, 139.6503)),
        SectorPreset("YOKOHAMA", NavCoordinates(35.4437, 139.6380)),
        SectorPreset("NAGOYA", NavCoordinates(35.1815, 136.9066)),
        SectorPreset("KYOTO", NavCoordinates(35.0116, 135.7681)),
        SectorPreset("OSAKA", NavCoordinates(34.6937, 135.5023)),
        SectorPreset("HIROSHIMA", NavCoordinates(34.3853, 132.4553)),
        SectorPreset("FUKUOKA", NavCoordinates(33.5904, 130.4017)),
        SectorPreset("SENDAI", NavCoordinates(38.2682, 140.8694)),
        SectorPreset("SAPPORO", NavCoordinates(43.0618, 141.3545)),
        SectorPreset("NAHA", NavCoordinates(26.2124, 127.6809)),
        SectorPreset("HONG KONG", NavCoordinates(22.3193, 114.1694)),
        SectorPreset("MACAU", NavCoordinates(22.1987, 113.5439)),
    )
}

/** The initial broad sector shown on a cold boot of the app, before the Operator warps anywhere. */
val DEFAULT_SECTOR = NavCoordinates(35.6762, 139.6503)

sealed class CoordinateParseResult {
    data class Valid(val coordinates: NavCoordinates) : CoordinateParseResult()
    data object Empty : CoordinateParseResult()
    data object OutOfRange : CoordinateParseResult()
}

/**
 * Parses and range-checks raw lat/lng text entry. Pulled out of the Activity so it's a plain,
 * unit-testable function rather than logic buried in a click listener.
 */
fun parseCoordinates(latText: String, lngText: String): CoordinateParseResult {
    val lat = latText.trim()
    val lng = lngText.trim()
    if (lat.isEmpty() || lng.isEmpty()) return CoordinateParseResult.Empty

    val latVal = lat.toDoubleOrNull()
    val lngVal = lng.toDoubleOrNull()
    if (latVal == null || lngVal == null || latVal !in -90.0..90.0 || lngVal !in -180.0..180.0) {
        return CoordinateParseResult.OutOfRange
    }
    return CoordinateParseResult.Valid(NavCoordinates(latVal, lngVal))
}

/**
 * Discrete hop waypoints between two coordinates — the "stepped, not interpolated" warp motion
 * (house style: mechanical over silky). [steps] intermediate hops plus the final destination;
 * the camera jumps to each in turn rather than tweening smoothly between them.
 */
fun stepWaypoints(from: NavCoordinates, to: NavCoordinates, steps: Int = 4): List<NavCoordinates> {
    require(steps >= 1) { "steps must be >= 1" }
    return (1..steps).map { step ->
        val fraction = step.toDouble() / steps
        NavCoordinates(
            latitude = from.latitude + (to.latitude - from.latitude) * fraction,
            longitude = from.longitude + (to.longitude - from.longitude) * fraction,
        )
    }
}

/** Cycles GREEN -> AMBER -> CYAN -> GREEN, mirroring the launcher's phosphor-cycle action. */
fun PhosphorHue.next(): PhosphorHue = when (this) {
    PhosphorHue.GREEN -> PhosphorHue.AMBER
    PhosphorHue.AMBER -> PhosphorHue.CYAN
    PhosphorHue.CYAN -> PhosphorHue.GREEN
}
