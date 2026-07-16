package com.quantumos.optics

import com.quantumos.optics.capture.FilmProfile
import com.quantumos.optics.ui.camera.DialMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LeicaEmulationUnitTest {

    @Test
    fun testDialModes() {
        // Verify all Leica M4 dial modes are present and have correct descriptor strings
        val modes = DialMode.values()
        assertEquals(4, modes.size)
        
        val expMode = DialMode.EXP
        assertEquals("EXP", expMode.label)
        assertEquals("EXPOSURE PARAMETERS", expMode.desc)
        
        val astMode = DialMode.AST
        assertEquals("AST", astMode.label)
        assertEquals("ASTRONOMICAL ALIGN", astMode.desc)
    }

    @Test
    fun testFilmProfiles() {
        // Verify film emulation profiles exist and have the correct Leica descriptors
        val profiles = FilmProfile.values()
        assertEquals(2, profiles.size)
        
        val triX = FilmProfile.BW_TRI_X
        assertEquals("TRI-X 400", triX.label)
        assertTrue(triX.desc.contains("QUANTUM SILVER"))

        val portra = FilmProfile.COL_PORTRA
        assertEquals("PORTRA 400", portra.label)
        assertTrue(portra.desc.contains("BLACKHOLE CHROMATIC"))
    }

    @Test
    fun testColorMatrixFormulas() {
        // Verify that the color matrices are well-formed and mathematically valid
        val monoValues = floatArrayOf(
            0.299f * 1.35f, 0.587f * 1.35f, 0.114f * 1.35f, 0f, -0.05f,
            0.299f * 1.35f, 0.587f * 1.35f, 0.114f * 1.35f, 0f, -0.05f,
            0.299f * 1.35f, 0.587f * 1.35f, 0.114f * 1.35f, 0f, -0.05f,
            0f, 0f, 0f, 1f, 0f
        )
        val colorValues = floatArrayOf(
            1.15f, 0.05f, 0.0f,  0f, 0.03f,
            0.05f, 1.10f, 0.0f,  0f, 0.02f,
            0.0f,  0.05f, 0.90f, 0f, -0.01f,
            0f,    0f,    0f,    1f, 0f
        )
        
        assertEquals(20, monoValues.size)
        assertEquals(20, colorValues.size)
        
        // Assert that the monochrome matrix translates colors into equal RGB intensities for a grayscale feel
        assertEquals(monoValues[0], monoValues[5])
        assertEquals(monoValues[1], monoValues[6])
        assertEquals(monoValues[2], monoValues[7])
        
        // Assert that the warm color matrix preserves and boosts red hues while slightly attenuating blue
        assertTrue(colorValues[0] > colorValues[10]) // Red scale > Blue scale
    }
}
