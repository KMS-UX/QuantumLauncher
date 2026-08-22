package com.quantumos.shell.overlay

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import com.quantumos.appshell.engageFieldUnitDisplay
import com.quantumos.appshell.hideSystemBars
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.width
import androidx.compose.ui.graphics.Brush
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
import com.quantumos.shell.ai.VoiceModelProvisioner
import com.quantumos.quarkavatar.ui.QuarkProjection
import com.quantumos.core.ScriptedResponse
import com.quantumos.core.SoundCue
import com.quantumos.quarkbrain.BrainReadyState
import com.quantumos.quarkbrain.QuarkModelConfig
import com.quantumos.appshell.Fonts
import com.quantumos.appshell.Glyph
import com.quantumos.appshell.Phosphor
import com.quantumos.appshell.PleaseStandbyCard
import com.quantumos.appshell.QuantumIcon
import com.quantumos.appshell.crtShader
import com.quantumos.shell.ui.QuantumRuntime
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.quantumos.appshell.GlyphLabel
import com.quantumos.appshell.SegmentedGauge

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

    override fun onWindowFocusChanged(hasFocus: Boolean) {

        super.onWindowFocusChanged(hasFocus)

        // A transient reveal, a fold/unfold or coming back from another app all leave

        // the system bars showing. Re-hide whenever this window is the one in front.

        if (hasFocus) hideSystemBars()

    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        engageFieldUnitDisplay()

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
            // W3 (Phase 22): QUARK's own settings panel. This used to be `diagnosticsOpen`, reached
            // by TRIPLE-TAPPING the title and described in this file as "visible only if you know to
            // look". That was right for engineering scaffolding and wrong for the only route the
            // Operator has to her voice and her weights, so it is a visible control now.
            var quarkConfigOpen by rememberSaveable { mutableStateOf(false) }
            // Lets the acquisition panel be reopened AFTER the brain has loaded. Before this it was
            // shown only while `!brainLoaded`, so once she was online there was no way back to it --
            // no re-import, no way to see what was on the device.
            var forceAcquisition by rememberSaveable { mutableStateOf(false) }
            // HOLSTER (Director, Fold 6 pass): stow the conversation, the rail and the panels so
            // QUARK is unobstructed. She fills the surface now, and there was no way to actually
            // LOOK at her -- the content sat over her from the chest down at all times.
            var holstered by rememberSaveable { mutableStateOf(false) }
            val killSwitchActive by QuantumRuntime.killSwitchActive.collectAsState()

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
            // The live backend readout (engine identity + why it is down). See
            // QuantumRuntime.voiceDiagnostic for why MODEL below is not sufficient on its own.
            val voiceDiagnostic by QuantumRuntime.voiceDiagnostic.collectAsState()
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

            // Fold 6 pass: "voice model for QUARK did not work (placeholder worked)". The sherpa
            // native libraries ARE in the APK for arm64, so the overwhelmingly likely cause is that
            // the Kokoro model was never imported -- it is deliberately not bundled, and until the
            // QUARK panel existed its import control was behind a triple-tap nobody would find.
            //
            // Selecting QUARK-H2 with no model silently falls back to the placeholder, which is
            // exactly what "it didn't work" looks like from outside. So the panel now REPORTS it:
            // recomputed whenever an import changes `voiceModelStatus`.
            val voiceModel = remember(voiceModelStatus) { VoiceModelProvisioner.status(ctx) }

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
                    // W2 (Phase 22) — QUARK herself, filling the surface, with the conversation and
                    // the command rail drawn OVER her lower frame.
                    //
                    // This replaces a 132dp ring-and-iris mark that had stood in for her since M5.
                    // The avatar track spent Phases 15-21 building this presentation and it was
                    // reachable only through a dev row in CONFIG; the floating trigger opens THIS
                    // screen, so putting her here is what makes the trigger summon QUARK rather than
                    // summon a menu with a logo on it.
                    //
                    // It takes no state: posture, speaking and stealth come from the same engine
                    // this screen already reads (B4), so the two cannot disagree.
                    QuarkProjection(modifier = Modifier.fillMaxSize())

                    // A scrim, because QUARK is bright and the conversation has to stay readable
                    // over her. Measured on the first build: the acquisition panel's body text sat
                    // directly on her lit chest plate and was barely legible.
                    //
                    // A vertical CRT-ground gradient rather than a panel: it is the same falloff
                    // language the housing and the App Shell already use, so it reads as the screen
                    // fading rather than as a drawn box over her -- which the house style forbids.
                    // Transparent across her face, opaque by the time it reaches the text.
                    // The scrim exists ONLY to keep the conversation legible over her. Holstered,
                    // there is nothing to keep legible and it would just be dimming her for no
                    // reason, so it goes too.
                    if (!holstered) Box(
                        Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    0.00f to Color.Transparent,
                                    SCRIM_START to Color.Transparent,
                                    SCRIM_FULL to Phosphor.Crt.copy(alpha = SCRIM_ALPHA),
                                    1.00f to Phosphor.Crt.copy(alpha = SCRIM_ALPHA),
                                )
                            )
                    )
                    Column(
                        Modifier
                            .fillMaxSize()
                            .padding(WindowInsetsPadding())
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        // ----- header: stow · title (triple-tap = diagnostics) · caption -----
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            GlyphLabel(
                                Glyph.Back, "STOW", dimColor, font,
                                fontSize = 12.sp, iconSize = 12.dp,
                                modifier = Modifier.clickable { close() }.padding(4.dp),
                            )
                            Spacer(Modifier.width(10.dp))
                            // Terse and mechanical, per the house voice. DEPLOY is the way back, so
                            // the control never strands the Operator with a QUARK they cannot talk
                            // to -- it is the same reasoning as the transient system bars.
                            Text(
                                if (holstered) "[ DEPLOY ]" else "[ HOLSTER ]",
                                color = dimColor, fontFamily = font, fontSize = 12.sp,
                                modifier = Modifier
                                    .clickable {
                                        holstered = !holstered
                                        QuantumRuntime.playCue(SoundCue.UI_CLUNK)
                                    }
                                    .padding(4.dp)
                            )
                            Spacer(Modifier.weight(1f))
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                GlyphLabel(
                                    glyph = if (quarkConfigOpen) Glyph.CaretUp else Glyph.CaretDown,
                                    text = "QUARK",
                                    tint = color,
                                    font = font,
                                    fontSize = 16.sp,
                                    iconSize = 12.dp,
                                    trailing = true,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .clickable { quarkConfigOpen = !quarkConfigOpen }
                                        .padding(4.dp),
                                )
                            }
                            Spacer(Modifier.weight(1f))
                            Text(
                                brain.caption,
                                color = if (brain.activePosture == QuarkReflexPosture.WARN) Phosphor.Warn else dimColor,
                                fontFamily = font, fontSize = 11.sp
                            )
                        }

                        // The panel sits BELOW the header row, not inside its centre column. Nested
                        // in the title's column it expanded inside the Row and sat across the STOW
                        // control -- caught on device.
                        if (quarkConfigOpen) {
                            QuarkConfigPanel(
                                brainLoaded = brainLoaded,
                                killSwitchActive = killSwitchActive,
                                voiceOn = voiceOn,
                                isH2 = voiceId == QuantumRuntime.VoiceIdentity.QUARK_H2,
                                voiceModelStatus = voiceModelStatus,
                                voiceModelReady = voiceModel.modelReady,
                                voiceModelMissing = voiceModel.missing,
                                voiceDiagnostic = voiceDiagnostic,
                                color = color, dimColor = dimColor, font = font,
                                onToggleKillSwitch = { QuantumRuntime.toggleKillSwitch() },
                                onToggleVoice = { QuantumRuntime.toggleVoice() },
                                onToggleVoiceIdentity = {
                                    val h2 = voiceId == QuantumRuntime.VoiceIdentity.QUARK_H2
                                    QuantumRuntime.setVoiceIdentity(
                                        if (h2) QuantumRuntime.VoiceIdentity.PLACEHOLDER
                                        else QuantumRuntime.VoiceIdentity.QUARK_H2
                                    )
                                },
                                onImportVoiceModel = {
                                    voiceModelLauncher.launch(
                                        arrayOf("application/x-bzip2", "application/octet-stream", "*/*")
                                    )
                                },
                                onManageWeights = {
                                    forceAcquisition = true
                                    quarkConfigOpen = false
                                },
                            )
                        }

                        // Headroom, not a placeholder: QUARK is behind this Column now, and this is
                        // what keeps her face clear of the conversation. Tuned on device.
                        if (!holstered) Spacer(Modifier.height(PRESENCE_HEADROOM))

                        // ----- body: first-run acquisition panel, or normal content -----
                        // Shows automatically (brief §3) whenever the brain isn't loaded yet AND the
                        // Operator hasn't engaged the kill switch — a production first-run consent/
                        // progress step, not something gated behind finding the diagnostics panel.
                        //
                        // All of it holsters together. Stowing the log but leaving the rail would
                        // just be a smaller obstruction; the point is to see her.
                        if (holstered) {
                            // Nothing. Deliberately not a placeholder or a hint: a holstered QUARK
                            // is the whole screen, and the DEPLOY control in the header is the way
                            // back.
                        } else if (!killSwitchActive && (!brainLoaded || forceAcquisition)) {
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
            RailButton("STATUS", Glyph.StatusReport, Modifier.weight(1f), color, dimColor, font) { parser.railStatusReport() }
            RailButton("STEALTH", Glyph.Stealth, Modifier.weight(1f), color, dimColor, font) { parser.railEngageStealth() }
            RailButton("PHOSPHOR", Glyph.Phosphor, Modifier.weight(1f), color, dimColor, font) { parser.railCyclePhosphor() }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RailButton("BEACON", Glyph.Beacon, Modifier.weight(1f), color, dimColor, font) { parser.railLightBeacon() }
            RailButton("SAY", Glyph.Say, Modifier.weight(1f), color, dimColor, font) { parser.railSaySomething() }
            RailButton("WARN", Glyph.Warn, Modifier.weight(1f), color, dimColor, font) { parser.railTriggerWarn() }
        }
    }
}

@Composable
private fun RailButton(
    label: String,
    glyph: Glyph,
    modifier: Modifier,
    color: Color,
    dimColor: Color,
    font: FontFamily,
    onClick: () -> Unit
) {
    Column(
        modifier
            .clickable { onClick() }
            .border(BorderStroke(1.dp, color))
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        QuantumIcon(glyph, tint = color, size = 16.dp)
        Spacer(Modifier.height(4.dp))
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
                            // "//" not an arrow: this is prose, not a control, and the house
                            // microcopy already uses // as its separator everywhere else. An icon
                            // here would be wrong; a font-fallback arrow was merely fragile.
                            "Source: HuggingFace // google/gemma-4-E2B-it",
                            color = dimColor, fontFamily = font, fontSize = 11.sp
                        )
                    }
                    is BrainReadyState.Downloading -> {
                        val pct = (readyState.fraction * 100).toInt()
                        val mbRead = readyState.bytesRead / (1024 * 1024)
                        val mbTotal = readyState.total / (1024 * 1024)
                        Text("ACQUIRING QUARK — $pct%", color = color, fontFamily = font, fontSize = 12.sp)
                        // Discrete phosphor progress bar — 20 segments, stepped not smooth.
                        // Was built from "█"/"░" block characters, i.e. a bar whose segment shape,
                        // width and gap were all decided by whichever font the device fell back to.
                        // SegmentedGauge is the house renderer for exactly this and is already
                        // shared by the Vitality panel and SIGNAL's link gauges.
                        val filled = (readyState.fraction * 20).toInt().coerceIn(0, 20)
                        SegmentedGauge(
                            label = "TRANSFER",
                            filled = filled,
                            total = 20,
                            value = "$pct%",
                            color = color, dimColor = dimColor, font = font,
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

// How much clear space the conversation leaves above itself so QUARK's face is not covered. She is
// bottom-anchored and runs off the bottom of the frame, so the content sits over her chest and
// shoulders rather than her head.
private val PRESENCE_HEADROOM = 300.dp

// The scrim behind the conversation. Starts below QUARK's face and reaches full strength before the
// first line of text, so she is unobscured where it matters and the copy is legible where it sits.
private const val SCRIM_START = 0.34f
private const val SCRIM_FULL = 0.52f
private const val SCRIM_ALPHA = 0.86f

/*
 * QUARK's settings panel (W3, Phase 22) — everything about HER, in one place the Operator can find.
 *
 * What this replaces: a list of dim `// TOKEN: VALUE` lines that only appeared after triple-tapping
 * the title, described in this file as "visible only if you know to look". As engineering
 * scaffolding that was fine. As the ONLY route to her voice model and the only place her brain's
 * fallback can be switched, it was not — the Operator had no way to reach any of it.
 *
 * Grouped by what the Operator is actually thinking about — her mind, then her voice — rather than
 * by which subsystem owns the flag. Microcopy stays terse and status-reporting per the house style.
 */
@Composable
private fun QuarkConfigPanel(
    brainLoaded: Boolean,
    killSwitchActive: Boolean,
    voiceOn: Boolean,
    isH2: Boolean,
    voiceModelStatus: String,
    voiceModelReady: Boolean,
    voiceModelMissing: List<String>,
    voiceDiagnostic: String,
    color: Color,
    dimColor: Color,
    font: FontFamily,
    onToggleKillSwitch: () -> Unit,
    onToggleVoice: () -> Unit,
    onToggleVoiceIdentity: () -> Unit,
    onImportVoiceModel: () -> Unit,
    onManageWeights: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = 6.dp)
            .border(1.dp, dimColor)
            .background(Phosphor.Crt.copy(alpha = 0.92f))
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        ConfigGroup("// MIND", dimColor, font)
        ConfigRow("WEIGHTS", if (brainLoaded) "ON-DEVICE" else "NOT LOADED", color, dimColor, font)
        ConfigRow("[ MANAGE WEIGHTS ]", "", color, dimColor, font, onClick = onManageWeights)
        // The kill switch forces free text back onto the Scripted-Line Library even with a healthy
        // brain. `--warn` red when engaged, because a degraded QUARK is a state the Operator must be
        // able to see at a glance rather than discover by her answers getting worse.
        ConfigRow(
            "FALLBACK",
            if (killSwitchActive) "SCRIPTED (KILL-SWITCH)" else "OFF",
            if (killSwitchActive) Phosphor.Warn else color, dimColor, font,
            onClick = onToggleKillSwitch,
        )

        Spacer(Modifier.height(8.dp))
        ConfigGroup("// VOICE", dimColor, font)
        ConfigRow("SPEECH", if (voiceOn) "ON" else "OFF", color, dimColor, font, onClick = onToggleVoice)
        // The LIVE backend, as opposed to MODEL below (which reports files on disk and so reads
        // READY even when the engine never loaded). `--warn` when down, because a QUARK who cannot
        // speak is a degraded state the Operator must see rather than discover by her silence.
        ConfigRow(
            "ENGINE", voiceDiagnostic.substringBefore(" // "),
            if (voiceDiagnostic.contains("DOWN")) Phosphor.Warn else color, dimColor, font,
        )
        if (voiceDiagnostic.contains(" // ")) {
            Text(
                voiceDiagnostic.substringAfter(" // "),
                color = dimColor, fontFamily = font, fontSize = 10.sp,
                modifier = Modifier.padding(start = 4.dp, bottom = 2.dp),
            )
        }
        ConfigRow(
            "IDENTITY", if (isH2) "QUARK-H2" else "PLACEHOLDER",
            color, dimColor, font, onClick = onToggleVoiceIdentity,
        )
        // Only offered for H2 — PLACEHOLDER is Android's own TTS and needs nothing imported.
        if (isH2) {
            // The row that would have answered the Fold 6 question on the spot. H2 with no model
            // does not error -- it quietly speaks in the placeholder voice, so the only way to tell
            // them apart from outside is to say so here.
            ConfigRow(
                "MODEL",
                if (voiceModelReady) "READY" else "NOT IMPORTED",
                if (voiceModelReady) color else Phosphor.Warn, dimColor, font,
            )
            if (!voiceModelReady && voiceModelMissing.isNotEmpty()) {
                Text(
                    "MISSING: " + voiceModelMissing.joinToString(" · "),
                    color = dimColor, fontFamily = font, fontSize = 10.sp,
                    modifier = Modifier.padding(start = 4.dp, bottom = 2.dp),
                )
            }
            ConfigRow(
                "[ IMPORT VOICE MODEL ]",
                voiceModelStatus.ifBlank { "" },
                color, dimColor, font, onClick = onImportVoiceModel,
            )
        }
    }
}

@Composable
private fun ConfigGroup(label: String, dimColor: Color, font: FontFamily) {
    Text(label, color = dimColor, fontFamily = font, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(2.dp))
}

@Composable
private fun ConfigRow(
    label: String,
    value: String,
    color: Color,
    dimColor: Color,
    font: FontFamily,
    onClick: (() -> Unit)? = null,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(vertical = 3.dp)
    ) {
        Text(label, color = dimColor, fontFamily = font, fontSize = 11.sp)
        if (value.isNotBlank()) {
            Spacer(Modifier.weight(1f))
            Text(value, color = color, fontFamily = font, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}
