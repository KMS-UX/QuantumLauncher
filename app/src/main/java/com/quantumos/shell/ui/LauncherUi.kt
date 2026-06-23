package com.quantumos.shell.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.quantumos.core.BootPace
import com.quantumos.core.PhosphorHue
import com.quantumos.core.QuantumLauncherState
import com.quantumos.core.QuantumStateEngine
import com.quantumos.core.QuarkParser
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/*
 * QuantumOS — UI LAYER (Compose). Depends on com.quantumos.core for all logic.
 * Requires deps: androidx.activity:activity-compose, androidx.lifecycle:lifecycle-viewmodel-compose,
 * androidx.compose.material3:material3.  Compile in the cloud to confirm — verify-before-banking
 * applies to THIS file too; it was authored without a compiler.
 */

// ---------- M0 typography: Chakra Petch (Monofonto substitute, decision 9) ----------
// Provider certs are stubs in res/values/font_certs.xml; replace with real values from
// Android Studio (Add Downloadable Font → Chakra Petch) or bundle the TTF at res/font/.
private val _fontProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)
private val _chakraPetch = GoogleFont("Chakra Petch")
val ChakraPetchFamily = FontFamily(
    Font(googleFont = _chakraPetch, fontProvider = _fontProvider, weight = FontWeight.Normal),
    Font(googleFont = _chakraPetch, fontProvider = _fontProvider, weight = FontWeight.Bold)
)

// ---------- single phosphor token source (M0): one switch recolors everything ----------
object Phosphor {
    val GreenBright = Color(0xFF00FF00); val GreenDim = Color(0xFF00AA00)   // FIX: was 0xFF00FF66
    val AmberBright = Color(0xFFFFB000); val AmberDim = Color(0xFFA86F00)
    val CyanBright  = Color(0xFF00E5FF); val CyanDim  = Color(0xFF0090A8)
    val Warn = Color(0xFFFF3B1F)   // alerts / access-denied ONLY
    val Crt  = Color(0xFF020402)   // near-black screen ground (NOT pure black)

    fun bright(h: PhosphorHue) = when (h) {
        PhosphorHue.GREEN -> GreenBright; PhosphorHue.AMBER -> AmberBright; PhosphorHue.CYAN -> CyanBright
    }
}

// ---------- ViewModel: holds the engine so state SURVIVES config changes (fold/unfold/rotate) ----------
class QuantumViewModel : ViewModel() {
    val engine = QuantumStateEngine(viewModelScope, BootPace.SNAPPY) // SNAPPY = dev sim; ship = DELIBERATE
    val parser = QuarkParser(engine)
    private var simRan = false

    fun boot() = engine.executeColdBootSequence() // engine self-guards: cold-boot runs once only

    fun setHue(hue: PhosphorHue) = engine.updateEnvironmentProfile { it.copy(activeHue = hue) }

    // Dev-only harness so the spike shows live output. DELETE before ship.
    fun runDevSimulation() {
        if (simRan) return
        simRan = true
        viewModelScope.launch {
            delay(1500)
            engine.incomingTelemetryUpdate(84, false, 500_000L, 3, 31.2f)
            delay(150); parser.parseInput("who are you")
            delay(150); parser.parseInput("status")
            delay(150); parser.parseInput("hue.amber")
            delay(150); parser.parseInput("sys.lock")
        }
    }
}

class LauncherActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge() // FIX: modern edge-to-edge (replaces setDecorFitsSystemWindows)
        setContent {
            val vm: QuantumViewModel = viewModel()
            LaunchedEffect(Unit) {
                vm.boot()
                vm.runDevSimulation()
            }
            // forceFixedContainer = false -> FILL-AND-ADAPT (recommended). See note in the shell.
            QuantumOSLayoutShell(forceFixedContainer = false) { constraints ->
                QuantumTerminalSurface(vm = vm, constraints = constraints)
            }
        }
    }
}

@Immutable
data class TerminalConstraints(
    val containerWidth: Dp,
    val containerHeight: Dp,
    val isLetterboxed: Boolean,
    val rawWindowWidth: Dp,
    val rawWindowHeight: Dp,
    val systemBarsPadding: PaddingValues
)

/*
 * THE ONE DESIGN DECISION (yours, Director):
 *  - forceFixedContainer = FALSE (default): the surface FILLS the real screen and the CRT
 *    falloff does the framing. On the Fold 6 this uses the whole inner display. RECOMMENDED.
 *  - forceFixedContainer = TRUE: forces a 9:19.5 phone-shaped box with black bars. On the Fold's
 *    near-square screen that's a narrow strip floating in black — only do this if we WANT a
 *    deliberate "screen-in-a-chassis" look (and then we'd draw a real bezel in the margin).
 */
@Composable
fun QuantumOSLayoutShell(
    forceFixedContainer: Boolean = false,
    targetAspectRatio: Float = 9f / 19.5f,
    content: @Composable (TerminalConstraints) -> Unit
) {
    BackHandler(enabled = true) { /* consume back so the shell never exits. Real routing = TODO.
                                     Needs manifest: android:enableOnBackInvokedCallback="true" */ }

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize().background(Color.Black)
    ) {
        val rawWidth = maxWidth
        val rawHeight = maxHeight
        if (rawWidth <= 0.dp || rawHeight <= 0.dp) return@BoxWithConstraints

        val (cw, ch) = if (!forceFixedContainer) {
            rawWidth to rawHeight
        } else {
            val ratio = rawWidth.value / rawHeight.value
            when {
                ratio > targetAspectRatio -> (rawHeight * targetAspectRatio) to rawHeight
                ratio < targetAspectRatio -> rawWidth to (rawWidth / targetAspectRatio)
                else -> rawWidth to rawHeight
            }
        }

        val constraints = TerminalConstraints(
            containerWidth = cw,
            containerHeight = ch,
            isLetterboxed = (cw != rawWidth || ch != rawHeight),
            rawWindowWidth = rawWidth,
            rawWindowHeight = rawHeight,
            systemBarsPadding = WindowInsets.systemBars.asPaddingValues()
        )

        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Box(
                Modifier
                    .width(cw)
                    .height(ch)
                    .background(Phosphor.Crt)
                    .crtOverlay()
            ) {
                content(constraints)
            }
        }
    }
}

// Cheap, non-shader CRT treatment so layout is confirmable even on a software-rendered cloud
// emulator. The real AGSL phosphor glow replaces this on hardware (judged on the Fold/Pixel).
fun Modifier.crtOverlay(): Modifier = drawWithContent {
    drawContent()
    val gap = 3.dp.toPx()
    var y = 0f
    while (y < size.height) {
        drawLine(Color(0x14000000), Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
        y += gap
    }
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(Color.Transparent, Color(0xAA000000)),
            center = Offset(size.width / 2f, size.height / 2f),
            radius = size.maxDimension * 0.75f
        )
    )
}

@Composable
fun QuantumTerminalSurface(vm: QuantumViewModel, constraints: TerminalConstraints) {
    val state by vm.engine.masterState.collectAsState()
    val logs by vm.engine.systemLogs.collectAsState()
    val color = Phosphor.bright(state.environment.activeHue)
    val font = ChakraPetchFamily

    Column(
        Modifier.fillMaxSize().padding(constraints.systemBarsPadding).padding(16.dp)
    ) {
        Row {
            HueChip("GREEN", color) { vm.setHue(PhosphorHue.GREEN) }
            Spacer(Modifier.width(12.dp))
            HueChip("AMBER", color) { vm.setHue(PhosphorHue.AMBER) }
            Spacer(Modifier.width(12.dp))
            HueChip("CYAN", color) { vm.setHue(PhosphorHue.CYAN) }
        }
        Spacer(Modifier.height(12.dp))
        Text(text = buildReadout(state, logs, constraints), color = color, fontFamily = font, fontSize = 13.sp)
    }
}

@Composable
private fun HueChip(label: String, color: Color, onClick: () -> Unit) {
    Text(
        text = "[$label]",
        color = color,
        fontFamily = ChakraPetchFamily,
        fontSize = 13.sp,
        modifier = Modifier.clickable { onClick() }.padding(4.dp)
    )
}

private fun buildReadout(state: QuantumLauncherState, logs: List<String>, c: TerminalConstraints): String =
    buildString {
        appendLine("=== QUANTUMOS CORE PHOSPHOR DRIVER ===")
        appendLine("LIFECYCLE   : ${state.bootLifecycle}")
        appendLine("CONTAINER   : ${c.containerWidth} x ${c.containerHeight}  fill=${!c.isLetterboxed}")
        appendLine("HUE         : ${state.environment.activeHue}  LOCKED=${state.environment.isSystemLocked}")
        appendLine("VITALITY    : ${state.vitality.batteryPercentage}%  ${state.vitality.coreTempCelsius}C  ${state.vitality.readiness}")
        appendLine("----------------------------------------")
        if (state.quarkBrain.responseTextSnippet.isNotEmpty()) {
            appendLine("QUARK       : \"${state.quarkBrain.responseTextSnippet}\"")
            appendLine("----------------------------------------")
        }
        appendLine("CONSOLE REEL:")
        logs.takeLast(6).forEach { appendLine(" > ${it.substringAfter("] ")}") }
    }
