package com.quantumos.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FieldDecoderTest {

    @Test
    fun decodesTaggedBase64() {
        val result = FieldDecoder.decode("Cipher: SEVMTE8=")
        assertEquals(FieldSignalFormat.BASE64, result.format)
        assertEquals("HELLO", result.output)
        assertTrue(result.success)
    }

    @Test
    fun decodesTaggedRot13() {
        val result = FieldDecoder.decode("Rot13: Uryyb, Bcrengbe.")
        assertEquals(FieldSignalFormat.ROT13, result.format)
        assertEquals("Hello, Operator.", result.output)
    }

    @Test
    fun decodesTaggedHex() {
        val result = FieldDecoder.decode("Hex: 48454c4c4f")
        assertEquals(FieldSignalFormat.HEX, result.format)
        assertEquals("HELLO", result.output)
    }

    @Test
    fun decodesTaggedMorse() {
        val result = FieldDecoder.decode("Morse: .... . .-.. .-.. ---")
        assertEquals(FieldSignalFormat.MORSE, result.format)
        assertEquals("HELLO", result.output)
    }

    @Test
    fun decodesMorseWithWordSeparator() {
        val result = FieldDecoder.decode("Morse: ... --- ... / ... --- ...")
        assertEquals("SOS SOS", result.output)
    }

    @Test
    fun autoDetectsUntaggedBase64() {
        val result = FieldDecoder.decode("SEVMTE8=")
        assertEquals(FieldSignalFormat.BASE64, result.format)
        assertEquals("HELLO", result.output)
    }

    @Test
    fun autoDetectsUntaggedMorse() {
        val result = FieldDecoder.decode(".... . .-.. .-.. ---")
        assertEquals(FieldSignalFormat.MORSE, result.format)
        assertEquals("HELLO", result.output)
    }

    @Test
    fun unrecognizedPayloadFailsHonestly() {
        val result = FieldDecoder.decode("just some plain field notes")
        assertEquals(FieldSignalFormat.UNKNOWN, result.format)
        assertFalse(result.success)
    }

    @Test
    fun emptyPayloadFailsHonestly() {
        val result = FieldDecoder.decode("   ")
        assertFalse(result.success)
    }

    @Test
    fun mismatchedTagFailsWithoutCrashing() {
        // Declared HEX but the payload is not valid hex — must report failure, never throw.
        val result = FieldDecoder.decode("Hex: not-hex-at-all")
        assertFalse(result.success)
        assertEquals(FieldSignalFormat.HEX, result.format)
    }
}

class SignalLevelsTest {

    @Test
    fun cellularBarsClampsToZeroToFour() {
        assertEquals(0, SignalLevels.cellularBars(-1))
        assertEquals(0, SignalLevels.cellularBars(0))
        assertEquals(4, SignalLevels.cellularBars(4))
        assertEquals(4, SignalLevels.cellularBars(9))
    }

    @Test
    fun wifiBarsFollowsRssiThresholds() {
        assertEquals(0, SignalLevels.wifiBars(-100))
        assertEquals(1, SignalLevels.wifiBars(-85))
        assertEquals(2, SignalLevels.wifiBars(-70))
        assertEquals(3, SignalLevels.wifiBars(-60))
        assertEquals(4, SignalLevels.wifiBars(-50))
    }

    @Test
    fun gpsBarsZeroWhenNoSatellitesVisible() {
        assertEquals(0, SignalLevels.gpsBars(0, 0))
    }

    @Test
    fun gpsBarsOneWhenVisibleButNoneUsed() {
        assertEquals(1, SignalLevels.gpsBars(0, 8))
    }

    @Test
    fun gpsBarsScalesWithFixRatio() {
        assertEquals(4, SignalLevels.gpsBars(8, 8))
        assertTrue(SignalLevels.gpsBars(2, 8) in 1..4)
    }

    @Test
    fun bluetoothBarsReflectsConnectionState() {
        assertEquals(0, SignalLevels.bluetoothBars(adapterOn = false, bondedCount = 3, connectedCount = 1))
        assertEquals(1, SignalLevels.bluetoothBars(adapterOn = true, bondedCount = 0, connectedCount = 0))
        assertEquals(2, SignalLevels.bluetoothBars(adapterOn = true, bondedCount = 2, connectedCount = 0))
        assertEquals(4, SignalLevels.bluetoothBars(adapterOn = true, bondedCount = 2, connectedCount = 1))
    }
}
