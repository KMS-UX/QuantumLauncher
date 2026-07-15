package com.quantumos.nav

import com.quantumos.nav.core.CoordinateParseResult
import com.quantumos.nav.core.NavCoordinates
import com.quantumos.core.PhosphorHue
import com.quantumos.nav.core.SectorPresets
import com.quantumos.nav.core.next
import com.quantumos.nav.core.parseCoordinates
import com.quantumos.nav.core.stepWaypoints
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure-logic unit tests for QuantumOS Nav — no emulator (run under `gradle test`). Replaces the old
 * template tests that asserted the wrong app_name and referenced a nonexistent Greeting composable.
 */
class NavStateTest {

    @Test
    fun `valid coordinates parse to a fix`() {
        val result = parseCoordinates("35.6762", "139.6503")
        assertTrue(result is CoordinateParseResult.Valid)
        result as CoordinateParseResult.Valid
        assertEquals(35.6762, result.coordinates.latitude, 1e-6)
        assertEquals(139.6503, result.coordinates.longitude, 1e-6)
    }

    @Test
    fun `blank entry is Empty, not a crash`() {
        assertEquals(CoordinateParseResult.Empty, parseCoordinates("", "139.6"))
        assertEquals(CoordinateParseResult.Empty, parseCoordinates("35.6", "  "))
    }

    @Test
    fun `out-of-range latitude is rejected`() {
        assertEquals(CoordinateParseResult.OutOfRange, parseCoordinates("91.0", "0.0"))
        assertEquals(CoordinateParseResult.OutOfRange, parseCoordinates("0.0", "181.0"))
    }

    @Test
    fun `non-numeric entry is rejected, not thrown`() {
        assertEquals(CoordinateParseResult.OutOfRange, parseCoordinates("north", "east"))
    }

    @Test
    fun `entry is trimmed before parsing`() {
        val result = parseCoordinates("  22.3193 ", " 114.1694 ")
        assertTrue(result is CoordinateParseResult.Valid)
    }

    @Test
    fun `stepped warp ends exactly on the destination`() {
        val from = NavCoordinates(0.0, 0.0)
        val to = NavCoordinates(10.0, 20.0)
        val hops = stepWaypoints(from, to, steps = 4)
        assertEquals(4, hops.size)
        assertEquals(to.latitude, hops.last().latitude, 1e-9)
        assertEquals(to.longitude, hops.last().longitude, 1e-9)
    }

    @Test
    fun `stepped warp advances monotonically toward the destination`() {
        val hops = stepWaypoints(NavCoordinates(0.0, 0.0), NavCoordinates(8.0, 0.0), steps = 4)
        val lats = hops.map { it.latitude }
        assertEquals(listOf(2.0, 4.0, 6.0, 8.0), lats)
    }

    @Test
    fun `phosphor cycles green to amber to cyan and back`() {
        assertEquals(PhosphorHue.AMBER, PhosphorHue.GREEN.next())
        assertEquals(PhosphorHue.CYAN, PhosphorHue.AMBER.next())
        assertEquals(PhosphorHue.GREEN, PhosphorHue.CYAN.next())
    }

    @Test
    fun `sector presets are defined with in-range coordinates and positive zoom`() {
        assertTrue(SectorPresets.ALL.isNotEmpty())
        SectorPresets.ALL.forEach {
            assertTrue(it.coordinates.latitude in -90.0..90.0)
            assertTrue(it.coordinates.longitude in -180.0..180.0)
            assertTrue(it.label.isNotBlank())
            assertTrue("zoom must be positive for ${it.label}", it.zoom > 0.0)
        }
    }

    @Test
    fun `japan area presets are present`() {
        val labels = SectorPresets.ALL.map { it.label }
        // The overview plus a spread of the Japan areas requested.
        listOf("JAPAN", "TOKYO", "OSAKA", "KYOTO", "SAPPORO", "FUKUOKA", "NAHA").forEach {
            assertTrue("expected preset $it", labels.contains(it))
        }
    }

    @Test
    fun `japan overview zooms out further than a city sector`() {
        val overview = SectorPresets.ALL.first { it.label == "JAPAN" }
        val city = SectorPresets.ALL.first { it.label == "TOKYO" }
        assertTrue(overview.zoom < city.zoom)
    }
}
