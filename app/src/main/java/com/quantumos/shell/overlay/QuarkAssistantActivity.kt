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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.view.WindowManager
import com.quantumos.core.QuarkReflexPosture
import com.quantumos.core.SoundCue
import com.quantumos.shell.ui.Fonts
import com.quantumos.shell.ui.Phosphor
import com.quantumos.shell.ui.PleaseStandbyCard
import com.quantumos.shell.ui.QuantumRuntime
import com.quantumos.shell.ui.crtShader
import kotlinx.coroutines.delay

/*
 * QuantumOS — M5 QUARK Assistant View. Replaces the M4 placeholder stub the floating trigger taps
 * through to. Full-screen phosphor surface: a large central reactive presence (the four locked
 * states), a one-line state caption, a scrolling conversation log, the six-action command rail, and
 * free-text entry. Every line QUARK speaks comes from the banked ScriptedLineLibrary via the shared
 * QuarkParser — never invented here.
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

            // Step 7 — re-apply Stealth's window-level dim in THIS window (state carries over from the
            // shared engine; the window attribute does not). Phosphor hue recolours automatically
            // because every colour above reads the same shared environment state.
            LaunchedEffect(state.environment.isStealthMode) {
                window.attributes = window.attributes.apply {
                    screenBrightness = if (state.environment.isStealthMode) stealthBrightness
                    else WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                }
            }

            // The open beat: brief PLEASE STANDBY → "assistant opened" line (Scan → Idle). Fires once.
            var standby by remember { mutableStateOf(true) }
            LaunchedEffect(Unit) {
                delay(650)
                standby = false
                parser.speakOpened()
            }

            val close: () -> Unit = {
                parser.speakStowed() // "assistant stowed" (Idle) — logged before we leave
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
                        // ----- header: stow · title · caption -----
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Text(
                                "◄ STOW",
                                color = dimColor,
                                fontFamily = font,
                                fontSize = 12.sp,
                                modifier = Modifier.clickable { close() }.padding(4.dp)
                            )
                            Spacer(Modifier.weight(1f))
                            Text("QUARK", color = color, fontFamily = font, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.weight(1f))
                            Text(brain.caption, color = if (brain.activePosture == QuarkReflexPosture.WARN) Phosphor.Warn else dimColor, fontFamily = font, fontSize = 11.sp)
                        }

                        Spacer(Modifier.height(8.dp))
                        // ----- central reactive presence -----
                        Box(Modifier.fillMaxWidth().height(140.dp), contentAlignment = Alignment.Center) {
                            QuarkPresence(posture = brain.activePosture, color = color, dimColor = dimColor)
                        }
                        Spacer(Modifier.height(4.dp))

                        // ----- conversation log (its own list; console aesthetic) -----
                        ConversationLog(
                            entries = convo,
                            crisisResource = engine.effectiveCrisisResource(),
                            color = color,
                            dimColor = dimColor,
                            font = font,
                            modifier = Modifier.weight(1f).fillMaxWidth()
                        )

                        Spacer(Modifier.height(8.dp))
                        // ----- command rail: six actions, fixed order -----
                        CommandRail(font = font, color = color, dimColor = dimColor)

                        Spacer(Modifier.height(8.dp))
                        // ----- free-text entry -----
                        FreeTextEntry(color = color, dimColor = dimColor, font = font) { parser.parseInput(it) }
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
