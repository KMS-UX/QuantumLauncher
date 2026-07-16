package com.quantumos.shell.overlay

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.net.Uri
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.quantumos.core.QuarkReflexPosture
import com.quantumos.core.ScriptedResponse
import com.quantumos.core.SoundCue
import com.quantumos.quarkbrain.BrainReadyState
import com.quantumos.quarkbrain.QuarkModelConfig
import com.quantumos.appshell.Fonts
import com.quantumos.appshell.Phosphor
import com.quantumos.appshell.PleaseStandbyCard
import com.quantumos.appshell.crtShader
import com.quantumos.shell.ui.QuantumRuntime
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/*
 * QuantumOS — QUARK Assistant View. Full-screen phosphor surface: a large central reactive presence
 * (the four locked states), a one-line state caption, a scrolling conversation log, the six-action
 * command rail, and free-text entry.
 *
 * QUARK Brain Promotion (decision 88): free-text conversation now routes to the real on-device brain
 * (QuarkOnDeviceBrain, :quark-brain) by default — production path, no debug flag required. The
 * six-action command rail stays on the Scripted-Line Library (deterministic device actions, out of
 * this brief's scope). The old Phase 1 debug toggle survives as a hidden kill switch
 * (QuantumRuntime.killSwitchActive, still triple-tap the title to reach) that forces free-text back
 * onto the scripted brain — the zero-risk rollback path (brief §4). Every scripted line still comes
 * from the banked ScriptedLineLibrary via the shared QuarkParser — never invented here.
 *
 * Shared state (M5): this Activity reads and mutates the SAME QuantumStateEngine as the launcher
 * (QuantumRuntime), so phosphor hue + Stealth carry over and the four reused rail actions behave
 * exactly as their M3 originals. Stealth's window brightness is per-window, so it is re-applied here
 * in this Activity's window (brief Step 7).
 */
class QuarkAssistantActivity : ComponentActivity() {

    // Stealth dim target for THIS window — same value the launcher uses (keep them in step).
    private val stealthBrightness = 0.04f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val engine = QuantumRuntime.engine
        val parser = QuantumRuntime.parser
        // Make sure the shared engine is alive even if the assistant is the first surface reached.
        QuantumRuntime.boot(this)
        QuantumRuntime.startTelemetry(this)

        val font = Fonts.ChakraPetch    // M6 Step 1: real bundled face (shared with the launcher)

        setContent {
            val state by engine.masterState.collectAsState()
            val convo by engine.conversationLog.collectAsState()
            val color = Phosphor.bright(state.environment.activeHue)
            val dimColor = Phosphor.dim(state.environment.activeHue)
            val brain = state.quarkBrain
            val scope = rememberCoroutineScope()

            // Step 7 — re-apply Stealth's window-level dim in THIS window.
            LaunchedEffect(state.environment.isStealthMode) {
                window.attributes = window.attributes.apply {
                    screenBrightness = if (state.environment.isStealthMode) stealthBrightness
                    else WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                }
            }

            // Resync CONFIG's persisted phosphor/region/boot-pace on resume (SIGNAL + CONFIG Task
            // Brief §3) — the floating QUARK trigger can open this Activity directly from CONFIG
            // without ever passing back through LauncherActivity's own resume, so this Activity needs
            // the same resync hook rather than relying solely on the launcher's.
            val lifecycleOwner = LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) QuantumRuntime.resyncPersistedSettings()
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
            }

            // The open beat: brief PLEASE STANDBY → "assistant opened" line (Scan → Idle). Fires once.
            var standby by remember { mutableStateOf(true) }
            LaunchedEffect(Unit) {
                delay(650)
                standby = false
                parser.speakOpened()
            }

            // ── Diagnostics reveal + kill switch (brief §4) ───────────────────────────
            // Triple-tap the "QUARK" title to reveal the hidden diagnostics panel (brain/voice status,
            // the kill switch, voice-identity + model-import controls) — same gesture as the old Phase
            // 1 debug toggle, but it no longer GATES reaching the real brain: the brain is the default
            // production path now. What the panel exposes is `killSwitchActive`, the rollback flag
            // that forces free-text back onto the Scripted-Line Library. rememberSaveable so the panel
            // survives orientation changes; does NOT survive process death (intentional, matches every
            // prior debug-scaffolding toggle in this repo).
            var diagnosticsOpen by rememberSaveable { mutableStateOf(false) }
            var titleTaps by remember { mutableIntStateOf(0) }
            val killSwitchActive by QuantumRuntime.killSwitchActive.collectAsState()

            // Auto-reset tap counter if the sequence stalls (2 s window).
            LaunchedEffect(titleTaps) {
                if (titleTaps in 1..2) {
                    delay(2_000)
                    titleTaps = 0
                }
            }

            // Brain state — always subscribed (collectAsState can't be called conditionally).
            // onDeviceBrain() resolves the ONE shared QuarkBrainProvider instance; the model itself
            // isn't loaded until it's present on disk and the auto-load below fires, or the Operator
            // taps ACQUIRE/PICK FILE/IMPORT FILE in the acquisition panel.
            val onDeviceBrain = QuantumRuntime.onDeviceBrain()
            val brainReadyState by onDeviceBrain.state.collectAsState()
            val brainLoaded = brainReadyState is BrainReadyState.Loaded

            // File picker — lets the Operator select a model from any location on the device
            // (Downloads, Files app, etc.) without needing adb or a computer.
            val pickFileLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.OpenDocument()
            ) { uri: Uri? -> uri?.let { onDeviceBrain.importFromUri(it) } }

            // Voice identity + QUARK-H2 model import — engineering controls, reachable via the same
            // hidden diagnostics panel regardless of kill-switch state (voice and the brain are
            // independent rollback concerns; see BUILD_LOG). The custom voice runs on sherpa-onnx;
            // picking the sherpa Kokoro model tarball extracts it and flips H2 live.
            val voiceId by QuantumRuntime.voiceIdentity.collectAsState()
            val voiceModelStatus by QuantumRuntime.voiceModelStatus.collectAsState()
            val ctx = LocalContext.current
            val voiceModelLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.OpenDocument()
            ) { uri: Uri? -> uri?.let { QuantumRuntime.importVoiceModel(ctx, it) } }

            // Auto-load once, on first reaching this view, whenever the weights are already on disk —
            // this is the production default now, not gated behind opening the diagnostics panel.
            LaunchedEffect(Unit) {
                if (onDeviceBrain.isPresent && !onDeviceBrain.isLoaded) {
                    onDeviceBrain.loadModel()
                }
            }
            // Auto-load after a successful download or import.
            LaunchedEffect(brainReadyState) {
                if (brainReadyState is BrainReadyState.Downloaded) {
                    onDeviceBrain.loadModel()
                }
            }
            // ─────────────────────────────────────────────────────────────────────────

            // Voice defaults ON in production (QuantumRuntime); this sub-toggle stays reachable in
            // the diagnostics panel as an engineering control.
            val voiceOn by QuantumRuntime.voiceEnabled.collectAsState()

            val close: () -> Unit = {
                QuantumRuntime.stopCurrentSpeech()   // stop any in-flight TTS before leaving
                parser.speakStowed()
                finish()
            }
            BackHandler(enabled = true) { close() }

            Box(
                Modifier
                    .fillMaxSize()
                    .background(Phosphor.Crt)
                    .crtShader()
            ) {
                if (standby) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        PleaseStandbyCard(subline = "ROUTING TO QUARK…", color = color, dimColor = dimColor, font = font)
                    }
                } else {
                    Column(
                        Modifier
                            .fillMaxSize()
                            .padding(WindowInsetsPadding())
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        // ----- header: stow · title (triple-tap = diagnostics) · caption -----
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Text(
                                "◄ STOW",
                                color = dimColor, fontFamily = font, fontSize = 12.sp,
                                modifier = Modifier.clickable { close() }.padding(4.dp)
                            )
                            Spacer(Modifier.weight(1f))
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    "QUARK",
                                    color = color, fontFamily = font, fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .clickable {
                                            titleTaps++
                                            if (titleTaps >= 3) { diagnosticsOpen = !diagnosticsOpen; titleTaps = 0 }
                                        }
                                        .padding(4.dp)
                                )
                                // Dim diagnostics — visible only if you know to look.
                                if (diagnosticsOpen) {
                                    Text(
                                        "// BRAIN: ${if (brainLoaded) "ON-DEVICE" else "OFFLINE"}",
                                        color = dimColor, fontFamily = font, fontSize = 9.sp
                                    )
                                    // The kill switch (brief §4) — the zero-risk rollback path. Forces
                                    // free-text back onto the Scripted-Line Library even with a
                                    // healthy, loaded brain.
                                    Text(
                                        "// FALLBACK: ${if (killSwitchActive) "SCRIPTED (KILL-SWITCH)" else "OFF"}",
                                        color = if (killSwitchActive) Phosphor.Warn else dimColor,
                                        fontFamily = font, fontSize = 9.sp,
                                        modifier = Modifier
                                            .clickable { QuantumRuntime.toggleKillSwitch() }
                                            .padding(top = 2.dp)
                                    )
                                    // Voice sub-toggle: tap to enable/disable TTS.
                                    Text(
                                        "// VOICE: ${if (voiceOn) "ON" else "OFF"}",
                                        color = dimColor, fontFamily = font, fontSize = 9.sp,
                                        modifier = Modifier
                                            .clickable { QuantumRuntime.toggleVoice() }
                                            .padding(top = 2.dp)
                                    )
                                    // Voice identity. PLACEHOLDER = Android TTS; QUARK-H2 = her
                                    // locked sherpa-onnx voice (needs its model imported once).
                                    val isH2 = voiceId == QuantumRuntime.VoiceIdentity.QUARK_H2
                                    Text(
                                        "// VOICE-ID: ${if (isH2) "QUARK-H2" else "PLACEHOLDER"}",
                                        color = dimColor, fontFamily = font, fontSize = 9.sp,
                                        modifier = Modifier
                                            .clickable {
                                                QuantumRuntime.setVoiceIdentity(
                                                    if (isH2) QuantumRuntime.VoiceIdentity.PLACEHOLDER
                                                    else QuantumRuntime.VoiceIdentity.QUARK_H2
                                                )
                                            }
                                            .padding(top = 2.dp)
                                    )
                                    // When H2 is selected, offer to import the sherpa Kokoro model
                                    // tarball (once). Shows extract status.
                                    if (isH2) {
                                        Text(
                                            "// [IMPORT VOICE MODEL] ${voiceModelStatus}",
                                            color = dimColor, fontFamily = font, fontSize = 9.sp,
                                            modifier = Modifier
                                                .clickable {
                                                    voiceModelLauncher.launch(
                                                        arrayOf("application/x-bzip2", "application/octet-stream", "*/*")
                                                    )
                                                }
                                                .padding(top = 2.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.weight(1f))
                            Text(
                                brain.caption,
                                color = if (brain.activePosture == QuarkReflexPosture.WARN) Phosphor.Warn else dimColor,
                                fontFamily = font, fontSize = 11.sp
                            )
                        }

                        Spacer(Modifier.height(8.dp))

                        // ----- central reactive presence -----
                        Box(Modifier.fillMaxWidth().height(140.dp), contentAlignment = Alignment.Center) {
                            QuarkPresence(posture = brain.activePosture, color = color, dimColor = dimColor)
                        }
                        Spacer(Modifier.height(4.dp))

                        // ----- body: first-run acquisition panel, or normal content -----
                        // Shows automatically (brief §3) whenever the brain isn't loaded yet AND the
                        // Operator hasn't engaged the kill switch — a production first-run consent/
                        // progress step, not something gated behind finding the diagnostics panel.
                        if (!killSwitchActive && !brainLoaded) {
                            ModelAcquisitionPanel(
                                readyState = brainReadyState,
                                color = color, dimColor = dimColor, font = font,
                                modifier = Modifier.weight(1f).fillMaxWidth(),
                                onAcquire = { onDeviceBrain.downloadModel() },
                                onImport = { onDeviceBrain.importFromExternal() },
                                onPickFile = { pickFileLauncher.launch(arrayOf("*/*")) },
                                onContinueOffline = {
                                    // Honest, in-character acknowledgement of the limited state — never
                                    // a dead UI (brief §3). Reuses the kill switch's own scripted-only
                                    // routing rather than a second fallback path.
                                    if (!killSwitchActive) QuantumRuntime.toggleKillSwitch()
                                    parser.speakOfflineFallback()
                                }
                            )
                        } else {
                            // ----- conversation log -----
                            ConversationLog(
                                entries = convo,
                                crisisResource = engine.effectiveCrisisResource(),
                                color = color, dimColor = dimColor, font = font,
                                modifier = Modifier.weight(1f).fillMaxWidth()
                            )

                            Spacer(Modifier.height(8.dp))

                            // ----- command rail: six actions (scripted brain only; rail is unchanged) -----
                            CommandRail(font = font, color = color, dimColor = dimColor)

                            Spacer(Modifier.height(8.dp))

                            // ----- free-text entry -----
                            // Production default: routes to the real on-device brain, holding SCAN for
                            // the full real inference latency — thinking takes a real beat because
                            // there's a real model thinking. The kill switch (brief §4) is the only way
                            // back to the deterministic scripted loop.
                            FreeTextEntry(color = color, dimColor = dimColor, font = font) { input ->
                                if (!killSwitchActive && brainLoaded) {
                                    scope.launch {
                                        QuantumRuntime.playCue(SoundCue.KEY_TICK)
                                        // Set SCAN immediately — the model holds it for the full inference.
                                        engine.dispatchQuarkReflex(
                                            "ON_DEVICE_SCAN", QuarkReflexPosture.SCAN, "", SoundCue.CHIRP_SCAN
                                        )
                                        val response = onDeviceBrain.reply(input)
                                        // Route through quarkSay to log the exchange and settle posture.
                                        engine.quarkSay(
                                            trigger = input, isUserInput = true,
                                            response = ScriptedResponse(
                                                intent = "ON_DEVICE",
                                                posture = QuarkReflexPosture.IDLE,
                                                text = response,
                                                audio = null,
                                                scanFirst = false,
                                                isCrisis = false
                                            )
                                        )
                                    }
                                } else {
                                    parser.parseInput(input)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Pull the system-bar insets as padding (edge-to-edge; we own inset handling, platform rule).
@Composable
private fun WindowInsetsPadding() = androidx.compose.foundation.layout.WindowInsets.systemBars.asPaddingValues()

/*
 * QuarkPresence — the scaled-up central mark with the four locked reactive states. Static at rest
 * (Idle does zero idle redraw, house-style); the other three are short, discrete, STEPPED bursts
 * fired by the engine posture, consistent with the existing motion language — not a new ambient loop.
 *   Idle  — static, neutral, antenna still.
 *   Scan  — iris contracts, a scan-line sweeps, the sound-ring pulses.
 *   Happy — a hop/tilt + iris pulse.
 *   Warn  — turns --warn red and shakes, alert iris.
 */
@Composable
private fun QuarkPresence(posture: QuarkReflexPosture, color: Color, dimColor: Color) {
    var offX by remember { mutableFloatStateOf(0f) }   // shake (dp)
    var offY by remember { mutableFloatStateOf(0f) }   // hop (dp)
    var scanY by remember { mutableFloatStateOf(-1f) } // 0..1 sweep position; <0 = no scan line
    var iris by remember { mutableFloatStateOf(1f) }   // iris scale

    LaunchedEffect(posture) {
        offX = 0f; offY = 0f; scanY = -1f; iris = 1f
        when (posture) {
            QuarkReflexPosture.SCAN -> {
                iris = 0.65f
                while (true) {                         // sweep loops only WHILE scanning
                    for (s in 0..6) { scanY = s / 6f; delay(55) }
                    scanY = -1f
                    delay(120)
                }
            }
            QuarkReflexPosture.HAPPY -> {              // hop + iris pulse burst, then settle static
                for (frame in listOf(-16f to 1.3f, -9f to 1.15f, 0f to 1f, -6f to 1.1f, 0f to 1f)) {
                    offY = frame.first; iris = frame.second; delay(70)
                }
            }
            QuarkReflexPosture.WARN -> {               // shake burst, then settle (stays red)
                for (x in listOf(-12f, 12f, -9f, 9f, -5f, 5f, 0f)) { offX = x; delay(50) }
            }
            QuarkReflexPosture.IDLE -> { /* static at rest — no redraw */ }
        }
    }

    val markColor = if (posture == QuarkReflexPosture.WARN) Phosphor.Warn else color
    val ringColor = if (posture == QuarkReflexPosture.WARN) Phosphor.Warn else dimColor

    Canvas(
        Modifier
            .size(132.dp)
            .graphicsLayer { translationX = offX.dp.toPx(); translationY = offY.dp.toPx() }
    ) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val r = size.minDimension / 2f * 0.92f
        val stroke = r * 0.10f

        // CRT-ground iris disc → bright outer ring → dim inner ring → centre aperture.
        drawCircle(color = Phosphor.Crt, radius = r)
        drawCircle(color = markColor, radius = r, style = Stroke(width = stroke))
        drawCircle(color = ringColor, radius = r * 0.62f, style = Stroke(width = stroke * 0.7f))
        drawCircle(color = markColor, radius = r * 0.18f * iris)

        // antenna stub (still at Idle) — a short top mast that reads as "field unit", not decoration.
        drawLine(
            color = ringColor,
            start = Offset(cx, cy - r),
            end = Offset(cx, cy - r - r * 0.22f),
            strokeWidth = stroke * 0.8f
        )

        // Scan sweep line — only present mid-scan.
        if (scanY in 0f..1f) {
            val y = (cy - r) + 2f * r * scanY
            val halfW = kotlin.math.sqrt((r * r) - ((y - cy) * (y - cy))).coerceAtLeast(0f)
            drawLine(
                color = markColor,
                start = Offset(cx - halfW, y),
                end = Offset(cx + halfW, y),
                strokeWidth = stroke * 0.6f
            )
        }
    }
}

// The scrolling conversation log — most-recent visible, console aesthetic (monospace, phosphor, no
// Material bubbles). A `>` prefix marks the Operator's typed input; `·` marks a rail action. The
// crisis-resource line renders as plain UI text BENEATH the entry that flagged it (never spoken).
@Composable
private fun ConversationLog(
    entries: List<com.quantumos.core.ConversationEntry>,
    crisisResource: String,
    color: Color,
    dimColor: Color,
    font: FontFamily,
    modifier: Modifier
) {
    if (entries.isEmpty()) {
        Box(modifier, contentAlignment = Alignment.Center) {
            Text("QUARK STANDING BY // ASK OR TAP THE RAIL", color = dimColor, fontFamily = font, fontSize = 12.sp)
        }
        return
    }
    val listState = rememberLazyListState()
    LaunchedEffect(entries.size) {
        if (entries.isNotEmpty()) listState.animateScrollToItem(entries.lastIndex)
    }
    LazyColumn(
        state = listState,
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(entries) { e ->
            Column(Modifier.fillMaxWidth()) {
                Text(
                    text = if (e.isUserInput) "> ${e.trigger}" else "· ${e.trigger}",
                    color = dimColor,
                    fontFamily = font,
                    fontSize = 11.sp
                )
                Text(
                    text = "QUARK: ${e.line}",
                    color = if (e.posture == QuarkReflexPosture.WARN) Phosphor.Warn else color,
                    fontFamily = font,
                    fontSize = 13.sp
                )
                if (e.showCrisisResource) {
                    Spacer(Modifier.height(4.dp))
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .border(BorderStroke(1.dp, dimColor))
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Text(
                            // Plain UI text, NOT QUARK's voice — a real resource line beneath her words.
                            text = "REACH SUPPORT // $crisisResource",
                            color = color,
                            fontFamily = font,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

// The six-action command rail, fixed order. The four reused actions call the SAME engine functions
// as M3 (via the parser, which applies the action then reports); the two new ones are presence-only.
@Composable
private fun CommandRail(font: FontFamily, color: Color, dimColor: Color) {
    val parser = QuantumRuntime.parser
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RailButton("STATUS", Modifier.weight(1f), color, dimColor, font) { parser.railStatusReport() }
            RailButton("STEALTH", Modifier.weight(1f), color, dimColor, font) { parser.railEngageStealth() }
            RailButton("PHOSPHOR", Modifier.weight(1f), color, dimColor, font) { parser.railCyclePhosphor() }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RailButton("BEACON", Modifier.weight(1f), color, dimColor, font) { parser.railLightBeacon() }
            RailButton("SAY", Modifier.weight(1f), color, dimColor, font) { parser.railSaySomething() }
            RailButton("WARN", Modifier.weight(1f), color, dimColor, font) { parser.railTriggerWarn() }
        }
    }
}

@Composable
private fun RailButton(
    label: String,
    modifier: Modifier,
    color: Color,
    dimColor: Color,
    font: FontFamily,
    onClick: () -> Unit
) {
    Box(
        modifier
            .clickable { onClick() }
            .border(BorderStroke(1.dp, color))
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = color, fontFamily = font, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

/*
 * ModelAcquisitionPanel — CRT-styled, stepped/boot-log-style first-run consent + progress surface
 * (QUARK Brain Promotion §3, Ignition Lab precedent). Shown in the Assistant View body whenever the
 * on-device brain isn't loaded yet and the kill switch isn't engaged — a production first-run step,
 * not debug scaffolding. Replaces the conversation log + rail + entry until the model is ready, or
 * until the Operator explicitly chooses CONTINUE OFFLINE. House style: terse status microcopy,
 * phosphor-only palette, discrete progress bar, no Material chrome (no stock buttons, no stock
 * progress indicators, no generic spinner).
 */
@Composable
private fun ModelAcquisitionPanel(
    readyState: BrainReadyState,
    color: Color,
    dimColor: Color,
    font: FontFamily,
    modifier: Modifier = Modifier,
    onAcquire: () -> Unit,
    onImport: () -> Unit,
    onPickFile: () -> Unit,
    onContinueOffline: () -> Unit
) {
    Column(
        modifier = modifier.padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // --- consent-forward header ---
        Text(
            "ACQUIRING QUARK",
            color = color, fontFamily = font, fontSize = 14.sp, fontWeight = FontWeight.Bold
        )
        Text(
            "HER NEURAL WEIGHTS ARE NOT YET ON THIS DEVICE (GEMMA 4 · E2B-IT · ~2.6 GB · LITERTLM). " +
            "NOTHING DOWNLOADS WITHOUT YOUR TAP BELOW.",
            color = dimColor, fontFamily = font, fontSize = 11.sp
        )

        // --- status block ---
        Box(
            Modifier
                .fillMaxWidth()
                .border(BorderStroke(1.dp, dimColor))
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                when (readyState) {
                    is BrainReadyState.Idle -> {
                        Text("WEIGHTS NOT PRESENT", color = color, fontFamily = font, fontSize = 12.sp)
                        Text(
                            "Tap ACQUIRE WEIGHTS to download, or PICK FILE to import what you already " +
                            "have:\ngemma-4-E2B-it.litertlm\n" +
                            "Source: HuggingFace → google/gemma-4-E2B-it",
                            color = dimColor, fontFamily = font, fontSize = 11.sp
                        )
                    }
                    is BrainReadyState.Downloading -> {
                        val pct = (readyState.fraction * 100).toInt()
                        val mbRead = readyState.bytesRead / (1024 * 1024)
                        val mbTotal = readyState.total / (1024 * 1024)
                        Text("ACQUIRING QUARK — $pct%", color = color, fontFamily = font, fontSize = 12.sp)
                        // Discrete phosphor progress bar — 20 segments, stepped not smooth.
                        val filled = (readyState.fraction * 20).toInt().coerceIn(0, 20)
                        Text(
                            "█".repeat(filled) + "░".repeat(20 - filled) + "  $pct%",
                            color = color, fontFamily = font, fontSize = 13.sp
                        )
                        Text("$mbRead MB / $mbTotal MB TRANSFERRED", color = dimColor, fontFamily = font, fontSize = 11.sp)
                        Text("PLEASE STANDBY", color = dimColor, fontFamily = font, fontSize = 11.sp)
                    }
                    is BrainReadyState.Downloaded -> {
                        Text("WEIGHTS ACQUIRED // LOADING MODEL", color = color, fontFamily = font, fontSize = 12.sp)
                        Text("PLEASE STANDBY — INITIALISING INFERENCE ENGINE", color = dimColor, fontFamily = font, fontSize = 11.sp)
                    }
                    is BrainReadyState.Loading -> {
                        Text("LOADING WEIGHTS INTO MEMORY", color = color, fontFamily = font, fontSize = 12.sp)
                        Text("PLEASE STANDBY — THIS TAKES ~10 S ON FIRST LOAD", color = dimColor, fontFamily = font, fontSize = 11.sp)
                    }
                    is BrainReadyState.Loaded -> {
                        // Should not normally be visible (parent hides this panel when loaded).
                        Text("WEIGHTS LOADED // ONLINE", color = color, fontFamily = font, fontSize = 12.sp)
                    }
                    is BrainReadyState.NoNetwork -> {
                        Text("NO NETWORK // CANNOT DOWNLOAD", color = Phosphor.Warn, fontFamily = font, fontSize = 12.sp)
                        Text(
                            "Side-load alternative:\n" +
                            "adb push ${QuarkModelConfig.MODEL_FILENAME}\n" +
                            "  /sdcard/Android/data/com.quantumos.shell/files/\n" +
                            "then tap IMPORT FILE below, or CONTINUE OFFLINE for now.",
                            color = dimColor, fontFamily = font, fontSize = 11.sp
                        )
                    }
                    is BrainReadyState.Err -> {
                        Text("ACQUISITION ERROR", color = Phosphor.Warn, fontFamily = font, fontSize = 12.sp)
                        Text(readyState.message, color = dimColor, fontFamily = font, fontSize = 11.sp)
                    }
                }
            }
        }

        // --- action rail ---
        val acquiring = readyState is BrainReadyState.Downloading || readyState is BrainReadyState.Loading
        // ACQUIRE WEIGHTS — full width (URL download, requires DOWNLOAD_URL to be set)
        Box(
            Modifier
                .fillMaxWidth()
                .border(BorderStroke(1.dp, if (acquiring) dimColor else color))
                .clickable(enabled = !acquiring) { onAcquire() }
                .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "ACQUIRE WEIGHTS",
                color = if (acquiring) dimColor else color,
                fontFamily = font, fontSize = 11.sp, fontWeight = FontWeight.Bold
            )
        }
        // PICK FILE | IMPORT FILE — local transfer options side by side
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                Modifier
                    .weight(1f)
                    .border(BorderStroke(1.dp, if (acquiring) dimColor else color))
                    .clickable(enabled = !acquiring) { onPickFile() }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "PICK FILE",
                    color = if (acquiring) dimColor else color,
                    fontFamily = font, fontSize = 11.sp, fontWeight = FontWeight.Bold
                )
            }
            Box(
                Modifier
                    .weight(1f)
                    .border(BorderStroke(1.dp, if (acquiring) dimColor else color))
                    .clickable(enabled = !acquiring) { onImport() }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "IMPORT FILE",
                    color = if (acquiring) dimColor else color,
                    fontFamily = font, fontSize = 11.sp, fontWeight = FontWeight.Bold
                )
            }
        }
        // CONTINUE OFFLINE (LIMITED) — the honest fallback (brief §3): never a dead UI. Disabled
        // mid-transfer same as the other actions; always available otherwise, including on error/
        // no-network, so a declined or failed acquisition never strands the Operator.
        Box(
            Modifier
                .fillMaxWidth()
                .border(BorderStroke(1.dp, if (acquiring) dimColor else dimColor))
                .clickable(enabled = !acquiring) { onContinueOffline() }
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "CONTINUE OFFLINE (LIMITED)",
                color = dimColor,
                fontFamily = font, fontSize = 11.sp, fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.height(4.dp))
        Text(
            "PICK FILE: browse device · IMPORT FILE: reads adb push path",
            color = dimColor, fontFamily = font, fontSize = 10.sp
        )
    }
}

// Free-text entry — a phosphor console prompt (BasicTextField, no Material chrome). Submissions route
// through the existing QuarkParser.parseInput; the field clears after each send.
@Composable
private fun FreeTextEntry(color: Color, dimColor: Color, font: FontFamily, onSubmit: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    val send = {
        if (text.isNotBlank()) {
            QuantumRuntime.playCue(SoundCue.KEY_TICK)   // keypad relay tick on send
            onSubmit(text); text = ""
        }
    }
    Row(
        Modifier
            .fillMaxWidth()
            .border(BorderStroke(1.dp, dimColor))
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("> ", color = color, fontFamily = font, fontSize = 14.sp)
        BasicTextField(
            value = text,
            onValueChange = { text = it },
            singleLine = true,
            textStyle = TextStyle(color = color, fontFamily = font, fontSize = 14.sp),
            cursorBrush = SolidColor(color),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { send() }),
            modifier = Modifier.weight(1f)
        )
        Text(
            "SEND",
            color = color,
            fontFamily = font,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.clickable { send() }.padding(start = 8.dp)
        )
    }
}
