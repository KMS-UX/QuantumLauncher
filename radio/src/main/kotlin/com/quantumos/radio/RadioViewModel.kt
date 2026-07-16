package com.quantumos.radio

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs

/*
 * RadioViewModel -- ported from the standalone repo's com.example.radio.RadioViewModel (Core Apps
 * Fix-Pass, Decision 86). Tuning/band/preset/reception/volume/stealth logic carries over unchanged --
 * it was already correct. DELETED per the Director ruling (not ported, not optional):
 *  - phosphorTheme / cyclePhosphorTheme: the docked module now takes its active phosphor hue from a
 *    local PhosphorHue selection in RadioActivity (matching how Optics/Nav do it), not a private
 *    PhosphorTheme enum duplicating :core's PhosphorHue.
 *  - isDecoding / decodeLog / decodedResult / customInput / isDecoderPaneOpen / setCustomInput /
 *    setDecoderPaneOpen / startSignalDecoding(): the whole cryptographic signal-decoder feature.
 *    RADIO is a pure content-receiver; see docs/future-signal/radio-decoder.md for where the original
 *    implementation still lives (untouched, in the standalone rollback repo).
 *
 * State that must survive fold/unfold/rotate lives here in a ViewModel, per this repo's platform rule
 * (CLAUDE.md), not in composition.
 */

enum class RadioBand {
    FM, AM, WX
}

data class RadioPreset(
    val frequency: String,
    val name: String,
    val description: String,
    val isEncrypted: Boolean = false
)

class RadioViewModel : ViewModel() {
    // Current Band (FM, AM, WX)
    private val _currentBand = MutableStateFlow(RadioBand.FM)
    val currentBand: StateFlow<RadioBand> = _currentBand.asStateFlow()

    // Current Frequency (Double represented as String for smooth tuning and display)
    private val _frequency = MutableStateFlow("94.5")
    val frequency: StateFlow<String> = _frequency.asStateFlow()

    // Mechanical Volume level (0 - 100)
    private val _volume = MutableStateFlow(65)
    val volume: StateFlow<Int> = _volume.asStateFlow()

    // Reception Strength (0 - 100), computed dynamically based on tuning proximity to presets
    private val _reception = MutableStateFlow(100)
    val reception: StateFlow<Int> = _reception.asStateFlow()

    // Stealth Mode (dims screen, mutes audio, maintains color saturation)
    private val _stealthMode = MutableStateFlow(false)
    val stealthMode: StateFlow<Boolean> = _stealthMode.asStateFlow()

    // Preset list per band with post-apocalyptic used-future flavor. Carries over as-is (already
    // correct per the earlier audit). isEncrypted is a content descriptor only -- it flags a station
    // as a known-encrypted transmission RADIO can tune to but not decode; it does not imply RADIO
    // itself performs any decoding (that capability was removed -- see radio-decoder.md).
    val fmPresets = listOf(
        RadioPreset("90.1", "RANGER HQ", "Tactical battlefield updates", false),
        RadioPreset("94.5", "GLOW AMBIENT", "Eerie pre-war radio broadcast", false),
        RadioPreset("101.3", "APOCALYPSE NEWS", "Independent survivalist bulletins", false),
        RadioPreset("107.9", "CRYPTIC BEACON", "Encrypted digital anomalous telemetry", true)
    )

    val amPresets = listOf(
        RadioPreset("640", "AUTOMATED MORSE", "Self-regulating emergency beacon", false),
        RadioPreset("1120", "BUNKER ALPHA", "Pre-war automated SOS distress loop", true),
        RadioPreset("1540", "MERCHANT HUB", "Post-apocalyptic barter & supply loop", false)
    )

    val wxPresets = listOf(
        RadioPreset("162.40", "WX-1 ATMO-ALERT", "Radiation levels and cloud metrics", false),
        RadioPreset("162.47", "WX-2 CHEM-WARN", "Tox-cloud vectors and chemical rain", false)
    )

    init {
        updateReception()
    }

    fun selectBand(band: RadioBand) {
        _currentBand.value = band
        // Set fallback start frequency for the band
        val defaultFreq = when (band) {
            RadioBand.FM -> "94.5"
            RadioBand.AM -> "640"
            RadioBand.WX -> "162.40"
        }
        _frequency.value = defaultFreq
        updateReception()
    }

    fun tuneFrequency(newFreq: String) {
        _frequency.value = newFreq
        updateReception()
    }

    fun setVolume(vol: Int) {
        _volume.value = vol.coerceIn(0, 100)
    }

    fun toggleStealthMode() {
        _stealthMode.value = !_stealthMode.value
    }

    /**
     * Compute reception strength dynamically. Shows 100% near presets, falling off
     * toward white static noise as the user tunes away from clear carriers.
     */
    private fun updateReception() {
        val current = _frequency.value.toDoubleOrNull() ?: return
        val presets = when (_currentBand.value) {
            RadioBand.FM -> fmPresets
            RadioBand.AM -> amPresets
            RadioBand.WX -> wxPresets
        }

        // Find distance to closest station in current band
        var minDiff = Double.MAX_VALUE
        for (preset in presets) {
            val pf = preset.frequency.toDoubleOrNull() ?: continue
            val diff = abs(current - pf)
            if (diff < minDiff) {
                minDiff = diff
            }
        }

        // Define sensitivity range depending on the band spacing
        val falloffRange = when (_currentBand.value) {
            RadioBand.FM -> 1.5   // MHz Range
            RadioBand.AM -> 120.0  // kHz Range
            RadioBand.WX -> 0.15  // MHz Range
        }

        val quality = (100.0 * (1.0 - (minDiff / falloffRange))).coerceIn(0.0, 100.0)
        _reception.value = quality.toInt()
    }
}
