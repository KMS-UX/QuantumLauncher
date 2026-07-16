package com.quantumos.files.viewmodel

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.quantumos.core.AiAssistBridge
import com.quantumos.core.AiAssistResult
import com.quantumos.core.PhosphorHue
import com.quantumos.quarkbrain.QuarkBrainProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

/*
 * FileExplorerViewModel -- ported from the standalone QuantumFiles repo's viewmodel of the same
 * name (Core Apps Fix-Pass, Decision 86). Real java.io.File domain logic (the four seeded
 * FIELD-LOGS/CAPTURES/COMMS-CACHE/MAPS folders under context.filesDir/QUANTUM, generic mkdir/
 * writeText/deleteRecursively/listFiles calls, the terminal-emulator ls/cd/cat/rm commands) is
 * kept intact -- taxonomy enforcement is explicitly NOT added (Director ruling, fix-pass §5).
 *
 * Both Gemini-backed features from the standalone app -- the "DECRYPT AI" file-analysis call and
 * the QUARK co-pilot chat call -- read a live BuildConfig.GEMINI_API_KEY over INTERNET. Neither is
 * reachable from a docked library module without new secrets plumbing this launcher never had, and
 * this module's build.gradle.kts carries no INTERNET permission / network dependency at all (fix-
 * pass §6). Both call sites are rewired onto the shared com.quantumos.core.AiAssistBridge contract
 * instead. QUARK Brain Promotion (Decision 88): the default arg below now resolves
 * QuarkBrainProvider.bridge(application) -- the real, shared on-device brain -- instead of the
 * NotYetWiredAiAssistBridge placeholder, exactly the one-line swap this class doc used to describe
 * as a to-do. This module still carries no network dependency of its own; the brain's own weights
 * acquisition (if any) happens through the launcher's Assistant View, not from here.
 *
 * AndroidViewModel because file-explorer state needs an Application Context for filesDir access,
 * same as the standalone app (platform rule: state lives in a ViewModel, not composition).
 *
 * Phosphor hue/stealth/beacon are this module's OWN local vitals simulation, not synced with the
 * launcher's shared VitalityState -- a known, pre-existing limitation shared with Optics/Nav (not
 * something to fix in this pass). Default hue is GREEN.
 */

enum class QuarkChatState { IDLE, SCAN, HAPPY, WARN }

data class FileItem(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val size: Long,
    val lastModified: Long,
    val permissions: String = "RW-X",
    val integrity: Int = 95
)

data class ChatMessage(
    val sender: String,
    val text: String,
    val isUser: Boolean,
    val timestamp: String = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
)

class FileExplorerViewModel @JvmOverloads constructor(
    application: Application,
    private val aiAssistBridge: AiAssistBridge = QuarkBrainProvider.bridge(application)
) : AndroidViewModel(application) {

    // --- System UI / theme state (local to this module -- see class doc) ---
    var activeHue by mutableStateOf(PhosphorHue.GREEN)
        private set
    var stealthMode by mutableStateOf(false)
        private set
    var beaconActive by mutableStateOf(false)
        private set

    // --- Device vitals (local simulation, same as the standalone app) ---
    var batteryLevel by mutableStateOf(100)
        private set
    var signalStrength by mutableStateOf(85)
        private set
    var coreTemperature by mutableStateOf(37)
        private set
    var uptimeString by mutableStateOf("00:00:00")
        private set
    var readinessScore by mutableStateOf(100)
        private set

    // --- File explorer state ---
    private val context = getApplication<Application>().applicationContext
    private val rootDirectory = File(context.filesDir, "QUANTUM")

    private val _currentPath = MutableStateFlow("")
    val currentPath: StateFlow<String> = _currentPath.asStateFlow()

    private val _files = MutableStateFlow<List<FileItem>>(emptyList())
    val files: StateFlow<List<FileItem>> = _files.asStateFlow()

    var selectedFile by mutableStateOf<FileItem?>(null)
    var fileContentText by mutableStateOf("")
    var isEditingFile by mutableStateOf(false)

    // --- Command terminal state ---
    private val _terminalHistory = MutableStateFlow<List<String>>(emptyList())
    val terminalHistory: StateFlow<List<String>> = _terminalHistory.asStateFlow()
    var terminalInput by mutableStateOf("")

    // --- AI assist state (bridge-backed; see class doc) ---
    var aiLoading by mutableStateOf(false)
        private set
    // Set only for a genuine AiAssistResult.Answer -- null while the bridge is not yet wired.
    var aiResultText by mutableStateOf<String?>(null)
        private set
    // Set for AiAssistResult.Unavailable -- rendered as a clearly-styled offline/standby state.
    var aiUnavailableReason by mutableStateOf<String?>(null)
        private set
    var thinkingLogState by mutableStateOf<String?>(null)
        private set

    // --- QUARK co-pilot chat state (Files' own in-app dialogue, distinct from the real launcher
    // QUARK assistant -- also bridge-backed, see class doc) ---
    var quarkState by mutableStateOf(QuarkChatState.IDLE)
        private set
    private val _quarkConversation = MutableStateFlow<List<ChatMessage>>(emptyList())
    val quarkConversation: StateFlow<List<ChatMessage>> = _quarkConversation.asStateFlow()

    init {
        setupDirectories()
        navigateToDir("")
        startUptimeClock()
        monitorBattery()
        logTerminal("QuantumOS FILES access system loaded.")
        logTerminal("Terminal initialized. Type 'help' for field protocols.")
    }

    // --- Navigation & core file system ---
    private fun setupDirectories() {
        if (!rootDirectory.exists()) {
            rootDirectory.mkdirs()
        }

        // Taxonomy: the four seeded folders, exactly as-is (fix-pass §5 -- Director ruling, not an
        // oversight). No enforcement that content stays inside these buckets.
        val folders = listOf("FIELD-LOGS", "CAPTURES", "COMMS-CACHE", "MAPS")
        folders.forEach { folderName ->
            val folder = File(rootDirectory, folderName)
            if (!folder.exists()) {
                folder.mkdirs()
            }
        }

        createPrepopulatedFile(
            "FIELD-LOGS/operator_journal_01.txt",
            "LOG DATE: 2026-07-08 // OPERATOR KEY: 0x24F\n" +
                "Day 114. Sector C-4 is completely dark. Sector power-grid failed.\n" +
                "Found weird organic residue near anomaly 44. QUARK scans confirm molecular decay.\n" +
                "Thermal emissions are highly unstable. Advise complete avoidance of sector C-4."
        )

        createPrepopulatedFile(
            "FIELD-LOGS/anomaly_44_threat_report.txt",
            "CLASSIFIED // FIELD RECON INTELLIGENCE\n" +
                "ANOMALY-44 threat vector: HIGH.\n" +
                "Core temperature is fluctuating by 40 degrees Celsius per hour.\n" +
                "Radio emissions match the Kokoro frequency modulation. DO NOT ENGAGE.\n" +
                "Operatives must switch to STEALTH MODE immediately when entering the visual cone."
        )

        createPrepopulatedFile(
            "CAPTURES/signal_dump_b3.log",
            "TIME_STAMP: 23:25:10 // FREQUENCY: 443.20MHz\n" +
                "0xFF34AA99: SIGNAL STRENGTH DEGRADED.\n" +
                "Retrying relay bridge... 12% decrypted...\n" +
                "Decrypted phrase fragment: 'we kept the watch... the light is on.'\n" +
                "Source matches encrypted field beacon. Origin coordinates unresolved."
        )

        createPrepopulatedFile(
            "CAPTURES/telemetry_c1.dat",
            "SECTOR_GRID // RAD_RECON_LEVELS\n" +
                "01001001 01010100 00100000 01001001 01010011 00100000 01000001 01001100 01001001 01010110 01000101\n" +
                "INTEGRITY: 88%\n" +
                "RADIATION: 120mSv/h\n" +
                "WARNING: Extreme magnetic fields. Compass lock disabled."
        )

        createPrepopulatedFile(
            "COMMS-CACHE/crew_intercept_02.txt",
            "DECRYPTED INTERCEPT // SENSITIVE\n" +
                "Transmission intercepted at 23:25 UTC.\n" +
                "'If you can hear us, keep your phosphor brightness down. The Watcher sees the green glow.\n" +
                "Switch your terminal to AMBER or CYAN on your Vitality Panel immediately to minimize signature.'"
        )

        createPrepopulatedFile(
            "MAPS/sector_b3_tactical.json",
            "{\n" +
                "  \"sector\": \"B3\",\n" +
                "  \"waypoints\": [\n" +
                "    {\"lat\": 35.6895, \"lng\": 139.6917, \"label\": \"ANOMALY-44-RECON\"},\n" +
                "    {\"lat\": 35.6123, \"lng\": 139.7543, \"label\": \"FIELD-BASE-ALPHA\"}\n" +
                "  ],\n" +
                "  \"radiation_level\": \"120mSv/h\",\n" +
                "  \"integrity\": \"NOMINAL\"\n" +
                "}"
        )
    }

    private fun createPrepopulatedFile(relativePath: String, content: String) {
        val file = File(rootDirectory, relativePath)
        if (!file.exists()) {
            file.writeText(content)
        }
    }

    fun navigateToDir(subPath: String) {
        val targetDir = if (subPath.isEmpty()) rootDirectory else File(rootDirectory, subPath)
        if (targetDir.exists() && targetDir.isDirectory) {
            val relative = targetDir.canonicalPath.removePrefix(rootDirectory.canonicalPath)
            _currentPath.value = relative.replace(File.separator, "/").ifEmpty { "" }
            loadFiles()
        }
    }

    fun navigateUp() {
        if (_currentPath.value.isEmpty()) return
        val currentFile = File(rootDirectory, _currentPath.value)
        val parent = currentFile.parentFile
        if (parent != null && parent.canonicalPath.startsWith(rootDirectory.canonicalPath)) {
            val relative = parent.canonicalPath.removePrefix(rootDirectory.canonicalPath)
            _currentPath.value = relative.replace(File.separator, "/").ifEmpty { "" }
            loadFiles()
        } else {
            _currentPath.value = ""
            loadFiles()
        }
    }

    private fun loadFiles() {
        val currentDir = if (_currentPath.value.isEmpty()) rootDirectory else File(rootDirectory, _currentPath.value)
        val list = currentDir.listFiles() ?: emptyArray()
        val mapped = list.map { file ->
            val permissions = if (file.isDirectory) "R-X" else "RW-"
            val integrity = if (file.name.endsWith(".txt")) 98 else if (file.name.endsWith(".log")) 85 else 92
            FileItem(
                name = file.name,
                path = file.canonicalPath.removePrefix(rootDirectory.canonicalPath),
                isDirectory = file.isDirectory,
                size = file.length(),
                lastModified = file.lastModified(),
                permissions = permissions,
                integrity = integrity
            )
        }.sortedWith(compareBy({ !it.isDirectory }, { it.name }))
        _files.value = mapped
    }

    // --- File manipulation operations ---
    fun createDirectory(name: String): Boolean {
        val currentDir = if (_currentPath.value.isEmpty()) rootDirectory else File(rootDirectory, _currentPath.value)
        val newDir = File(currentDir, name)
        return if (!newDir.exists()) {
            val success = newDir.mkdir()
            if (success) {
                loadFiles()
                logTerminal("CREATED DIR: $name")
                triggerQuarkFeedback(QuarkChatState.HAPPY, "Created directory '$name' successfully.")
            }
            success
        } else {
            triggerQuarkFeedback(QuarkChatState.WARN, "Directory '$name' already exists.")
            false
        }
    }

    fun createNewFile(name: String, content: String): Boolean {
        val currentDir = if (_currentPath.value.isEmpty()) rootDirectory else File(rootDirectory, _currentPath.value)
        val newFile = File(currentDir, name)
        return if (!newFile.exists()) {
            newFile.writeText(content)
            loadFiles()
            logTerminal("CREATED FILE: $name (${content.length} Bytes)")
            triggerQuarkFeedback(QuarkChatState.HAPPY, "Saved file '$name' into memory.")
            true
        } else {
            triggerQuarkFeedback(QuarkChatState.WARN, "File '$name' already exists.")
            false
        }
    }

    fun saveFileEdits(fileItem: FileItem, content: String) {
        val file = File(rootDirectory, fileItem.path)
        if (file.exists() && !file.isDirectory) {
            file.writeText(content)
            loadFiles()
            logTerminal("EDITED FILE: ${fileItem.name} (${content.length} Bytes)")
            triggerQuarkFeedback(QuarkChatState.HAPPY, "Updated sectors for '${fileItem.name}' file.")
        }
    }

    fun deleteFileItem(fileItem: FileItem): Boolean {
        val file = File(rootDirectory, fileItem.path)
        return if (file.exists()) {
            val deleted = file.deleteRecursively()
            if (deleted) {
                loadFiles()
                if (selectedFile?.path == fileItem.path) {
                    selectedFile = null
                }
                logTerminal("DELETED: ${fileItem.name}")
                triggerQuarkFeedback(QuarkChatState.WARN, "Deleted object and purged sectors for '${fileItem.name}'.")
            }
            deleted
        } else false
    }

    fun openFile(fileItem: FileItem) {
        val file = File(rootDirectory, fileItem.path)
        if (file.exists() && !file.isDirectory) {
            fileContentText = file.readText()
            selectedFile = fileItem
            isEditingFile = false
            triggerQuarkFeedback(QuarkChatState.IDLE, "Viewing file: ${fileItem.name}")
        }
    }

    // --- Global theme & controls ---
    fun selectHue(hue: PhosphorHue) {
        activeHue = hue
        logTerminal("SYS_CONF: Phosphor hue changed to $hue.")
        triggerQuarkFeedback(QuarkChatState.HAPPY, "Phosphor frequency retuned to $hue.")
    }

    fun toggleStealth() {
        stealthMode = !stealthMode
        if (stealthMode) {
            beaconActive = false // Beacon override -- active signalling can't coexist with stealth.
            logTerminal("STEALTH_MODE: ACTIVE. Minimizing emissions.")
            triggerQuarkFeedback(QuarkChatState.IDLE, "Stealth protocol fully engaged. Phosphor desaturated.")
        } else {
            logTerminal("STEALTH_MODE: INACTIVE. Core glow restored.")
            triggerQuarkFeedback(QuarkChatState.HAPPY, "Standard thermal profile active.")
        }
        recalculateReadiness()
    }

    fun toggleBeacon() {
        beaconActive = !beaconActive
        if (beaconActive) {
            stealthMode = false // Stealth override -- see toggleStealth.
            logTerminal("WARNING: Beacon broadcast active on emergency coordinates.")
            triggerQuarkFeedback(QuarkChatState.WARN, "Broadcasting distress beacon. Signature high!")
        } else {
            logTerminal("Beacon broadcast deactivated.")
            triggerQuarkFeedback(QuarkChatState.IDLE, "Beacon offline.")
        }
        recalculateReadiness()
    }

    // --- Device vitals logic ---
    private fun startUptimeClock() {
        viewModelScope.launch {
            while (true) {
                val ms = SystemClock.elapsedRealtime()
                val sec = (ms / 1000) % 60
                val min = (ms / (1000 * 60)) % 60
                val hr = (ms / (1000 * 60 * 60)) % 24
                uptimeString = String.format(Locale.US, "%02d:%02d:%02d", hr, min, sec)
                delay(1000)
            }
        }
    }

    private fun monitorBattery() {
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                intent?.let {
                    val level = it.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                    val scale = it.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                    if (level != -1 && scale != -1) {
                        batteryLevel = (level * 100 / scale.toFloat()).toInt()
                        recalculateReadiness()
                    }
                }
            }
        }
        context.registerReceiver(receiver, filter)

        // Simulate core temperature slight fluctuations to make it dynamic and rustic.
        viewModelScope.launch {
            while (true) {
                coreTemperature = 35 + Random.nextInt(5) // Ranges 35C to 39C
                recalculateReadiness()
                delay(8000)
            }
        }
    }

    private fun recalculateReadiness() {
        // Power (battery) weighted heaviest, then signal, then temp, and affected by beacon/stealth.
        val baseScore = (batteryLevel * 0.5f + signalStrength * 0.3f + (100 - (coreTemperature - 30) * 5) * 0.2f).toInt()
        var finalScore = baseScore.coerceIn(10, 100)
        if (beaconActive) {
            finalScore -= 15
        }
        if (stealthMode) {
            finalScore += 5
        }
        readinessScore = finalScore.coerceIn(5, 100)
    }

    // --- Command terminal interface ---
    fun executeTerminalCommand(input: String) {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return
        logTerminal("> $trimmed")
        terminalInput = ""

        val parts = trimmed.split("\\s+".toRegex())
        val command = parts[0].lowercase(Locale.US)
        val args = parts.drop(1)

        viewModelScope.launch {
            when (command) {
                "help" -> {
                    logTerminal("QUANTUM SHELL RECON COMMANDS:")
                    logTerminal("  ls                  - List files & directories")
                    logTerminal("  cd <dir>            - Change path (use 'cd ..' to go up)")
                    logTerminal("  cat <file>          - Print file contents")
                    logTerminal("  create <file> <txt> - Create text document")
                    logTerminal("  edit <file> <txt>   - Overwrite document data")
                    logTerminal("  rm <file>           - Erase document or folder")
                    logTerminal("  decrypt <file>      - Request QUARK AI analysis")
                    logTerminal("  quark <question>    - Prompt QUARK co-pilot dialogue")
                    logTerminal("  phosphor <color>    - Switch theme (green, amber, cyan)")
                    logTerminal("  stealth             - Toggle low emissions stealth")
                    logTerminal("  beacon              - Toggle emergency beacon signal")
                    logTerminal("  clear               - Clear terminal output buffer")
                }
                "ls" -> {
                    val list = files.value
                    if (list.isEmpty()) {
                        logTerminal("Directory is empty.")
                    } else {
                        list.forEach {
                            val typeMark = if (it.isDirectory) "<DIR>" else "     "
                            val sizeStr = if (it.isDirectory) "      " else "${it.size}B"
                            logTerminal("  [${it.permissions}]  $typeMark  $sizeStr  ${it.name}")
                        }
                    }
                }
                "cd" -> {
                    if (args.isEmpty()) {
                        navigateToDir("")
                    } else if (args[0] == "..") {
                        navigateUp()
                    } else {
                        navigateToDir(args[0])
                    }
                }
                "cat" -> {
                    if (args.isEmpty()) {
                        logTerminal("Error: Specify file path.")
                    } else {
                        val name = args[0]
                        val fileItem = files.value.find { it.name.equals(name, true) }
                        if (fileItem != null && !fileItem.isDirectory) {
                            val f = File(rootDirectory, fileItem.path)
                            logTerminal("--- PRINTING: ${fileItem.name} ---")
                            f.readLines().forEach { logTerminal(it) }
                            logTerminal("---------------------------------")
                        } else {
                            logTerminal("Error: File not found.")
                        }
                    }
                }
                "create" -> {
                    if (args.size < 2) {
                        logTerminal("Syntax: create <filename.txt> <content text...>")
                    } else {
                        val name = args[0]
                        val txt = args.drop(1).joinToString(" ")
                        val success = createNewFile(name, txt)
                        if (success) {
                            logTerminal("File '$name' allocated.")
                        } else {
                            logTerminal("Error: File allocation failed.")
                        }
                    }
                }
                "edit" -> {
                    if (args.size < 2) {
                        logTerminal("Syntax: edit <filename.txt> <new content text...>")
                    } else {
                        val name = args[0]
                        val txt = args.drop(1).joinToString(" ")
                        val fileItem = files.value.find { it.name.equals(name, true) }
                        if (fileItem != null && !fileItem.isDirectory) {
                            saveFileEdits(fileItem, txt)
                            logTerminal("File '$name' overwritten successfully.")
                        } else {
                            logTerminal("Error: File not found.")
                        }
                    }
                }
                "rm" -> {
                    if (args.isEmpty()) {
                        logTerminal("Error: Specify target file.")
                    } else {
                        val name = args[0]
                        val fileItem = files.value.find { it.name.equals(name, true) }
                        if (fileItem != null) {
                            val success = deleteFileItem(fileItem)
                            if (success) logTerminal("Purged '${fileItem.name}'.") else logTerminal("Error purging.")
                        } else {
                            logTerminal("Error: Entity not found.")
                        }
                    }
                }
                "decrypt" -> {
                    if (args.isEmpty()) {
                        logTerminal("Error: Specify document to decrypt.")
                    } else {
                        val name = args[0]
                        val fileItem = files.value.find { it.name.equals(name, true) }
                        if (fileItem != null && !fileItem.isDirectory) {
                            val f = File(rootDirectory, fileItem.path)
                            val text = f.readText()
                            logTerminal("DECRYPT COMMAND DETECTED. QUERYING AI ASSIST BRIDGE...")
                            triggerDeepDecrypt(fileItem, text)
                        } else {
                            logTerminal("Error: Document not found.")
                        }
                    }
                }
                "quark" -> {
                    if (args.isEmpty()) {
                        logTerminal("QUARK: 'Operator, please enter a valid query.'")
                    } else {
                        val query = args.joinToString(" ")
                        logTerminal("QUARK: Querying AI assist bridge...")
                        talkToQuark(query)
                    }
                }
                "phosphor" -> {
                    if (args.isEmpty()) {
                        logTerminal("Current Phosphor Hue: $activeHue")
                    } else {
                        when (args[0].lowercase(Locale.US)) {
                            "green" -> selectHue(PhosphorHue.GREEN)
                            "amber" -> selectHue(PhosphorHue.AMBER)
                            "cyan" -> selectHue(PhosphorHue.CYAN)
                            else -> logTerminal("Unknown hue. Use green, amber, or cyan.")
                        }
                    }
                }
                "stealth" -> toggleStealth()
                "beacon" -> toggleBeacon()
                "clear" -> _terminalHistory.value = emptyList()
                else -> {
                    logTerminal("Protocol command un-resolved. Type 'help' for support.")
                    triggerQuarkFeedback(QuarkChatState.WARN, "Terminal reported command failure.")
                }
            }
        }
    }

    private fun logTerminal(line: String) {
        _terminalHistory.value = _terminalHistory.value + "[${SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())}] $line"
    }

    // --- AI assist bridge calls (Core Apps Fix-Pass, Decision 86 -- see class doc) ---
    private fun triggerQuarkFeedback(state: QuarkChatState, message: String) {
        quarkState = state
        _quarkConversation.value = _quarkConversation.value + ChatMessage("QUARK", message, false)
    }

    fun triggerDeepDecrypt(fileItem: FileItem, fileContent: String) {
        viewModelScope.launch {
            aiLoading = true
            quarkState = QuarkChatState.SCAN
            aiResultText = null
            aiUnavailableReason = null
            thinkingLogState = ""

            // Stepped, mechanical "please standby" beat -- not a continuous spinner (house style).
            val logs = listOf(
                "CONNECTING COGNITIVE NODES...",
                "ACQUIRING NEURAL SYNC GRID...",
                "LOADING ENVELOPE STRUCT...",
                "QUERYING AI ASSIST BRIDGE..."
            )
            for (log in logs) {
                thinkingLogState = log
                logTerminal("NEURAL REASONER: $log")
                delay(450)
            }

            val prompt = "Analyze the contents of file: ${fileItem.name} (integrity: ${fileItem.integrity}%)\n" +
                "FILE CONTENT:\n\"\"\"\n$fileContent\n\"\"\"\nExecute tactical analysis."
            val result = aiAssistBridge.ask(prompt)

            thinkingLogState = "DECRYPT REQUEST RESOLVED."
            when (result) {
                is AiAssistResult.Answer -> aiResultText = result.text
                is AiAssistResult.Unavailable -> aiUnavailableReason = result.reason
            }
            aiLoading = false
            quarkState = QuarkChatState.HAPPY
            logTerminal("DECRYPT REQUEST FOR '${fileItem.name}' RESOLVED.")
        }
    }

    fun talkToQuark(message: String) {
        val userMsg = ChatMessage("Operator", message, true)
        _quarkConversation.value = _quarkConversation.value + userMsg

        viewModelScope.launch {
            aiLoading = true
            quarkState = QuarkChatState.SCAN
            thinkingLogState = "QUARK IS THINKING..."

            val fileContext = files.value.joinToString("\n") { "File: ${it.name}, directory: ${it.isDirectory}, size: ${it.size}B" }
            val prompt = "Operator asks: $message\n\nCURRENT WORKING DIR SYSTEM PROFILE:\n$fileContext"
            val result = aiAssistBridge.ask(prompt)
            val responseText = when (result) {
                is AiAssistResult.Answer -> result.text
                is AiAssistResult.Unavailable -> result.reason
            }

            thinkingLogState = null
            aiLoading = false
            quarkState = QuarkChatState.HAPPY
            _quarkConversation.value = _quarkConversation.value + ChatMessage("QUARK", responseText, false)
        }
    }
}
