package com.quantumos.core

/*
 * FieldDecoder — SIGNAL's offline decoder seed feature (SIGNAL + CONFIG Task Brief §2).
 * Ported interaction shape ONLY from the cryptographic decoder that used to live in RADIO (see
 * docs/future-signal/radio-decoder.md): custom input -> decode -> stepped terminal -> result. The
 * Gemini backend is NOT ported — this is real, local, deterministic decoding, same discipline as
 * :comms's own cipher terminal (Base64/Morse/ROT13), extended with hex. No AI, no network.
 * Pure Kotlin, no Android deps, so it unit-tests with zero emulator.
 */
enum class FieldSignalFormat { BASE64, HEX, MORSE, ROT13, UNKNOWN }

data class FieldDecodeResult(val format: FieldSignalFormat, val output: String, val success: Boolean)

object FieldDecoder {
    // Recognised payload tags — same "Tag: payload" convention as :comms's cipher terminal, so an
    // Operator who has learned one field tool's decoder already knows this one.
    private val TAGS = mapOf(
        "cipher:" to FieldSignalFormat.BASE64,
        "hex:" to FieldSignalFormat.HEX,
        "morse:" to FieldSignalFormat.MORSE,
        "rot13:" to FieldSignalFormat.ROT13
    )

    fun decode(raw: String): FieldDecodeResult {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) {
            return FieldDecodeResult(FieldSignalFormat.UNKNOWN, "NO PAYLOAD.", false)
        }
        val tagEntry = TAGS.entries.firstOrNull { trimmed.startsWith(it.key, ignoreCase = true) }
        if (tagEntry != null) {
            val payload = trimmed.substring(tagEntry.key.length).trim()
            return safeDecode(tagEntry.value, payload)
        }
        // No explicit tag: try auto-detection in a fixed priority order, first plausible match wins.
        return autoDetect(trimmed) ?: FieldDecodeResult(
            FieldSignalFormat.UNKNOWN,
            "NO PATTERN MATCH. Tag the payload (CIPHER:/HEX:/MORSE:/ROT13:) or paste a recognizable format.",
            false
        )
    }

    private fun autoDetect(payload: String): FieldDecodeResult? {
        if (payload.matches(Regex("^[.\\-/\\s]+$"))) {
            safeDecode(FieldSignalFormat.MORSE, payload).let { if (it.success) return it }
        }
        val hexCandidate = payload.replace(Regex("\\s"), "")
        if (hexCandidate.isNotEmpty() && hexCandidate.matches(Regex("^[0-9a-fA-F]+$")) && hexCandidate.length % 2 == 0) {
            safeDecode(FieldSignalFormat.HEX, payload).let { if (it.success) return it }
        }
        val base64Candidate = payload.replace(Regex("\\s"), "")
        if (base64Candidate.matches(Regex("^[A-Za-z0-9+/]+={0,2}$")) && base64Candidate.length % 4 == 0) {
            safeDecode(FieldSignalFormat.BASE64, payload).let { if (it.success) return it }
        }
        return null
    }

    private fun safeDecode(format: FieldSignalFormat, payload: String): FieldDecodeResult =
        runCatching { decodeAs(format, payload) }
            .getOrElse {
                FieldDecodeResult(format, "DECODE FAILED: payload does not match its declared format.", false)
            }

    private fun decodeAs(format: FieldSignalFormat, payload: String): FieldDecodeResult = when (format) {
        FieldSignalFormat.BASE64 -> FieldDecodeResult(format, decodeBase64(payload), true)
        FieldSignalFormat.HEX -> FieldDecodeResult(format, decodeHex(payload), true)
        FieldSignalFormat.MORSE -> FieldDecodeResult(format, decodeMorse(payload), true)
        FieldSignalFormat.ROT13 -> FieldDecodeResult(format, decodeRot13(payload), true)
        FieldSignalFormat.UNKNOWN -> FieldDecodeResult(format, "", false)
    }

    private fun decodeRot13(payload: String): String = payload.map { c ->
        when {
            c in 'a'..'z' -> 'a' + (c - 'a' + 13) % 26
            c in 'A'..'Z' -> 'A' + (c - 'A' + 13) % 26
            else -> c
        }
    }.joinToString("")

    private fun decodeHex(payload: String): String {
        val clean = payload.replace(Regex("\\s"), "")
        require(clean.length % 2 == 0) { "odd-length hex payload" }
        return clean.chunked(2).map { it.toInt(16).toChar() }.joinToString("")
    }

    private fun decodeMorse(payload: String): String =
        payload.trim().split("/").joinToString(" ") { word ->
            word.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
                .joinToString("") { MORSE_TABLE[it]?.toString() ?: "?" }
        }

    // Pure JDK Base64 (java.util, not android.util) — :core carries no Android dependency.
    private fun decodeBase64(payload: String): String {
        val clean = payload.replace(Regex("\\s"), "")
        return String(java.util.Base64.getDecoder().decode(clean), Charsets.UTF_8)
    }

    private val MORSE_TABLE: Map<String, Char> = mapOf(
        ".-" to 'A', "-..." to 'B', "-.-." to 'C', "-.." to 'D', "." to 'E',
        "..-." to 'F', "--." to 'G', "...." to 'H', ".." to 'I', ".---" to 'J',
        "-.-" to 'K', ".-.." to 'L', "--" to 'M', "-." to 'N', "---" to 'O',
        ".--." to 'P', "--.-" to 'Q', ".-." to 'R', "..." to 'S', "-" to 'T',
        "..-" to 'U', "...-" to 'V', ".--" to 'W', "-..-" to 'X', "-.--" to 'Y',
        "--.." to 'Z', "-----" to '0', ".----" to '1', "..---" to '2', "...--" to '3',
        "....-" to '4', "....." to '5', "-...." to '6', "--..." to '7',
        "---.." to '8', "----." to '9'
    )
}

/*
 * SignalLevels — pure mapping from raw device readings to the 0..4 segmented-bar scale SIGNAL's
 * gauges render (Task Brief §2's "segmented bar in the house style"). Kept here, not in :signal's
 * ViewModel, so the mapping is unit-tested with zero emulator (SESSION-PLAYBOOK's testable-seam rule).
 */
object SignalLevels {
    // Cellular: TelephonyManager's own SignalStrength.level is already 0..4 (API 23+); just clamp it
    // defensively rather than trust an out-of-range value blindly.
    fun cellularBars(level: Int): Int = level.coerceIn(0, 4)

    // Wi-Fi: RSSI (dBm) -> 0..4 bars, the same threshold ladder Android's own
    // WifiManager.calculateSignalLevel(rssi, 5) uses internally.
    fun wifiBars(rssiDbm: Int): Int {
        val thresholds = intArrayOf(-88, -77, -66, -55)
        var level = 0
        for (t in thresholds) if (rssiDbm >= t) level++
        return level.coerceIn(0, 4)
    }

    // GPS: satellites-used-in-fix vs. satellites-visible -> a coarse fix-quality bar. No visible
    // satellites at all reads as 0 (no antenna view of the sky), not merely "weak."
    fun gpsBars(satellitesUsed: Int, satellitesVisible: Int): Int = when {
        satellitesVisible <= 0 -> 0
        satellitesUsed <= 0 -> 1
        else -> {
            val ratio = satellitesUsed.toFloat() / satellitesVisible.toFloat()
            (1 + ratio.coerceIn(0f, 1f) * 3f).toInt().coerceIn(1, 4)
        }
    }

    // Bluetooth: there is no ambient RSSI without actively scanning a specific peer, so SIGNAL
    // measures adapter/connection state honestly rather than fabricating a number for it.
    fun bluetoothBars(adapterOn: Boolean, bondedCount: Int, connectedCount: Int): Int = when {
        !adapterOn -> 0
        connectedCount > 0 -> 4
        bondedCount > 0 -> 2
        else -> 1
    }
}
