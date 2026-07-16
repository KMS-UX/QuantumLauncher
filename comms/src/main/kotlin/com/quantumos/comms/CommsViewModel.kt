package com.quantumos.comms

import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/*
 * COMMS domain model + state -- ported from the standalone QuantumComms repo's CommsViewModel
 * (Core Apps Fix-Pass, Decision 86). Real, testable Kotlin logic; the platform rule keeps this
 * state here (a ViewModel), not in composition, so it survives fold/unfold/rotate.
 *
 * What changed from the standalone app (see the comms module task brief):
 *  - No Gemini-backed AI chat persona. #HQ-TACTICAL's live AI-reply loop is gone; every channel now
 *    shows seeded/scripted callsign traffic, appended to only by the Operator's own outgoing
 *    transmissions (sendMessage) or the manual simulateIncomingTransmission() test affordance --
 *    never a live back-and-forth chat behavior.
 *  - No infinite telemetry/uptime simulator loops (house style: zero idle redraw / static at rest).
 *    Satellite links are a static seeded snapshot, not a ticking while(true) fake-drift loop.
 *  - The cipher-decryption terminal is REAL local decoding (Base64 / Morse / ROT13) instead of a
 *    call to an AI backend -- it never needed Gemini's reasoning, only format translation, so this
 *    keeps the feature genuinely functional with no network dependency at all (no INTERNET
 *    permission in comms/build.gradle.kts).
 */

enum class CipherFormat { BASE64, MORSE, ROT13 }

enum class LogType { INFO, WARNING, SECURE, ERROR }

data class Channel(
    val name: String,
    val callsign: String,
    val functionLine: String,
    // Bumped each time a new transmission lands on this channel; drives the one-shot live-pulse
    // dot in the channel list (Core Apps Fix-Pass identity fix). Never a ticking/looping value.
    val pulseTrigger: Int = 0
)

data class Transmission(
    val sender: String,
    val text: String,
    val timestamp: String,
    val isOutgoing: Boolean,
    val channelName: String
)

data class SystemLogEntry(
    val message: String,
    val timestamp: String,
    val type: LogType = LogType.INFO
)

data class SatelliteLink(
    val name: String,
    val status: String,
    val connectionDb: Int
)

data class PreloadedCipher(
    val label: String,
    val payload: String,
    val format: CipherFormat
)

data class CommsUiState(
    val selectedChannel: String = "COMMAND",
    val channels: List<Channel> = emptyList(),
    val transmissions: Map<String, List<Transmission>> = emptyMap(),
    val currentInput: String = "",
    val systemLogs: List<SystemLogEntry> = emptyList(),
    val activeBeacons: List<String> = emptyList(),
    val satellites: List<SatelliteLink> = emptyList(),
    val selectedPreloadedCipherIndex: Int = -1,
    val cipherInputText: String = "",
    val cipherOutputText: String = "",
    // A bounded, one-shot PLEASE STANDBY beat while decodeSignal runs -- real work behind a
    // stylish card (house style), not a fake spinner and not a ticking loop.
    val isDecrypting: Boolean = false
)

class CommsViewModel : ViewModel() {

    private val _state = MutableStateFlow(CommsUiState())
    val state: StateFlow<CommsUiState> = _state.asStateFlow()

    init {
        val initChannels = listOf(
            Channel("COMMAND", "HQ-6", "COMMAND NET"),
            Channel("CIPHER-DEC", "DECRYPT-NET", "DECRYPTION TERMINAL"),
            Channel("BEACON-NET", "BEACON-NET", "DISTRESS COORDINATION"),
            Channel("TELEM-FEED", "SENSORS-7", "PASSIVE SENSOR FEED")
        )

        val initTransmissions = mapOf(
            "COMMAND" to listOf(
                Transmission("HQ-6", "Command net open. All units report status on the hour.", now(), false, "COMMAND"),
                Transmission("RECON-2", "RECON-2 in position, north ridge. Quiet so far.", now(), false, "COMMAND"),
                Transmission("HQ-6", "Copy RECON-2. Hold and report any movement.", now(), false, "COMMAND")
            ),
            "CIPHER-DEC" to listOf(
                Transmission("DECRYPT-NET", "Decryption terminal online. Select an intercepted signal or paste a payload below.", now(), false, "CIPHER-DEC")
            ),
            "BEACON-NET" to listOf(
                Transmission("BEACON-NET", "Beacon coordination net active. Monitoring 3 orbital arrays overhead.", now(), false, "BEACON-NET")
            ),
            "TELEM-FEED" to listOf(
                Transmission("SENSORS-7", "Passive scan complete. Ambient temp 28.4C. Radiation: background only.", now(), false, "TELEM-FEED")
            )
        )

        val initSatellites = listOf(
            SatelliteLink("QSAT-ALPHA", "LOCKED", -62),
            SatelliteLink("QSAT-BETA", "ACQUIRING", -94),
            SatelliteLink("QSAT-GAMMA", "ORBITING", -72)
        )

        _state.update {
            it.copy(
                // Seeded traffic triggers one pulse on load (the dot's permitted "at rest" state is
                // silent; a fresh channel with unread seed traffic pulses once, then settles).
                channels = initChannels.map { c -> c.copy(pulseTrigger = 1) },
                transmissions = initTransmissions,
                satellites = initSatellites,
                systemLogs = listOf(
                    SystemLogEntry("COMMS terminal initialized.", now(), LogType.INFO),
                    SystemLogEntry("Callsign channels loaded.", now(), LogType.INFO),
                    SystemLogEntry("Link verified. Protocol secure.", now(), LogType.SECURE)
                )
            )
        }
    }

    // ---------- channel selection ----------

    fun selectChannel(channelName: String) {
        _state.update { it.copy(selectedChannel = channelName) }
        addLog("Channel switched to $channelName", LogType.INFO)
    }

    // ---------- transmissions ----------

    fun updateInput(input: String) = _state.update { it.copy(currentInput = input) }

    fun sendMessage() {
        val input = _state.value.currentInput.trim()
        if (input.isEmpty()) return
        val channelName = _state.value.selectedChannel
        appendTransmission(channelName, Transmission("OPERATOR", input, now(), true, channelName))
        _state.update { it.copy(currentInput = "") }
        addLog("Transmission uplinked on $channelName: \"$input\"", LogType.INFO)
    }

    /*
     * Manual, Operator-triggered test affordance for the live-pulse dot (Core Apps Fix-Pass identity
     * fix). Deliberately NOT a timer/loop -- one tap, one bounded event, exactly like every other
     * appended transmission. Fires seeded canned traffic from the channel's own callsign.
     */
    fun simulateIncomingTransmission(channelName: String) {
        val channel = _state.value.channels.firstOrNull { it.name == channelName } ?: return
        appendTransmission(
            channelName,
            Transmission(channel.callsign, "Signal check -- ${channel.callsign} reads you five by five.", now(), false, channelName)
        )
        addLog("Incoming transmission received on $channelName.", LogType.SECURE)
    }

    private fun appendTransmission(channelName: String, tx: Transmission) {
        val updatedList = (_state.value.transmissions[channelName] ?: emptyList()) + tx
        val updatedMap = _state.value.transmissions.toMutableMap()
        updatedMap[channelName] = updatedList
        _state.update {
            it.copy(
                transmissions = updatedMap,
                channels = it.channels.map { c ->
                    if (c.name == channelName) c.copy(pulseTrigger = c.pulseTrigger + 1) else c
                }
            )
        }
    }

    // ---------- beacon net (a bounded, single action -- not a simulator loop) ----------

    fun triggerDistressBeacon() {
        val id = "B-${(100..999).random()}"
        _state.update { it.copy(activeBeacons = it.activeBeacons + id) }
        addLog("HIGH-PRIORITY DISTRESS BEACON DEPLOYED: call ID $id", LogType.ERROR)
        // One bounded follow-up beat confirming satellite routing -- a single delay, not a loop.
        viewModelScope.launch {
            delay(1000)
            addLog("Distress beacon $id routed through QSAT-ALPHA satellite.", LogType.SECURE)
        }
    }

    fun clearBeacons() {
        _state.update { it.copy(activeBeacons = emptyList()) }
        addLog("All active beacons deactivated.", LogType.INFO)
    }

    // ---------- cipher decryption terminal -- REAL local decoding, no network/AI call ----------

    fun updateCipherInput(input: String) = _state.update { it.copy(cipherInputText = input) }

    fun selectPreloadedCipher(index: Int) {
        val ciphers = preloadedCiphers()
        if (index !in ciphers.indices) return
        val selected = ciphers[index]
        _state.update {
            it.copy(
                selectedPreloadedCipherIndex = index,
                cipherInputText = selected.payload,
                cipherOutputText = ""
            )
        }
        addLog("Intercept selected: ${selected.label}", LogType.SECURE)
    }

    fun decryptCipher() {
        val raw = _state.value.cipherInputText.trim()
        if (raw.isEmpty() || _state.value.isDecrypting) return
        viewModelScope.launch {
            _state.update { it.copy(isDecrypting = true, cipherOutputText = "") }
            addLog("Initiating decryption sweep...", LogType.SECURE)
            delay(500) // a bounded, one-shot PLEASE STANDBY beat -- never a loop
            _state.update { it.copy(isDecrypting = false, cipherOutputText = decodeSignal(raw)) }
            addLog("Decryption matrix resolved.", LogType.INFO)
        }
    }

    fun preloadedCiphers(): List<PreloadedCipher> = listOf(
        PreloadedCipher("INTERCEPT A-201", "Cipher: SE9NRSBORVQgQUNUSVZFLiBTVEFORCBCWSBGT1IgT1JERVJTLg==", CipherFormat.BASE64),
        PreloadedCipher("INTERCEPT B-902", "Morse: ... . -.-. ..- .-. . / -.-. --- -- -- ... / ... - .- -... .-.. .", CipherFormat.MORSE),
        PreloadedCipher("INTERCEPT X-112", "Rot13: Qrpelcgvba fvtany sbhaq. Sbyq gbjneq gnpgvpny pbbeqvangrf.", CipherFormat.ROT13)
    )

    private fun decodeSignal(raw: String): String = try {
        when {
            raw.startsWith("Cipher:", ignoreCase = true) ->
                "FORMAT: BASE64\n\n" + decodeBase64(raw.removePrefix("Cipher:").trim())
            raw.startsWith("Morse:", ignoreCase = true) ->
                "FORMAT: MORSE\n\n" + decodeMorse(raw.removePrefix("Morse:").trim())
            raw.startsWith("Rot13:", ignoreCase = true) ->
                "FORMAT: ROT13\n\n" + decodeRot13(raw.removePrefix("Rot13:").trim())
            else -> "UNRECOGNIZED SIGNAL FORMAT.\nExpected a payload prefixed Cipher: / Morse: / Rot13:"
        }
    } catch (e: Exception) {
        "DECRYPTION FAILED: payload does not match its declared format."
    }

    private fun decodeBase64(payload: String): String =
        String(Base64.decode(payload, Base64.DEFAULT))

    private fun decodeRot13(payload: String): String =
        payload.map { c ->
            when {
                c in 'a'..'z' -> 'a' + (c - 'a' + 13) % 26
                c in 'A'..'Z' -> 'A' + (c - 'A' + 13) % 26
                else -> c
            }
        }.joinToString("")

    private fun decodeMorse(payload: String): String =
        payload.split(" / ").joinToString(" ") { word ->
            word.trim().split(" ").filter { it.isNotBlank() }
                .joinToString("") { MORSE_TABLE[it]?.toString() ?: "?" }
        }

    // ---------- system log ----------

    private fun addLog(message: String, type: LogType) {
        val entry = SystemLogEntry(message, now(), type)
        _state.update { it.copy(systemLogs = (listOf(entry) + it.systemLogs).take(60)) }
    }

    private fun now(): String = TIME_FORMAT.format(Date())

    companion object {
        private val TIME_FORMAT = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

        private val MORSE_TABLE: Map<String, Char> = mapOf(
            ".-" to 'A', "-..." to 'B', "-.-." to 'C', "-.." to 'D', "." to 'E',
            "..-." to 'F', "--." to 'G', "...." to 'H', ".." to 'I', ".---" to 'J',
            "-.-" to 'K', ".-.." to 'L', "--" to 'M', "-." to 'N', "---" to 'O',
            ".--." to 'P', "--.-" to 'Q', ".-." to 'R', "..." to 'S', "-" to 'T',
            "..-" to 'U', "...-" to 'V', ".--" to 'W', "-..-" to 'X', "-.--" to 'Y',
            "--.." to 'Z',
            "-----" to '0', ".----" to '1', "..---" to '2', "...--" to '3', "....-" to '4',
            "....." to '5', "-...." to '6', "--..." to '7', "---.." to '8', "----." to '9'
        )
    }
}
