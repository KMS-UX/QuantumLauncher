package com.quantumos.shell.ui

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.quantumos.appshell.ChannelStrip
import com.quantumos.appshell.Fonts
import com.quantumos.appshell.NameplateHeader
import com.quantumos.appshell.Phosphor
import com.quantumos.appshell.PleaseStandbyCard
import com.quantumos.appshell.QuantumOSLayoutShell
import com.quantumos.appshell.TerminalConstraints
import com.quantumos.appshell.crtShader
import com.quantumos.shell.overlay.QuarkTriggerService
import com.quantumos.core.BootLifecycleState
import com.quantumos.core.BootPace
import com.quantumos.core.DeploymentRegions
import com.quantumos.core.EnvironmentProfile
import com.quantumos.core.NavigationChannel
import com.quantumos.core.PhosphorHue
import com.quantumos.core.QuantumLauncherState
import com.quantumos.core.SoundCue
import com.quantumos.core.SystemReadiness
import com.quantumos.core.VitalityState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

/*
 * QuantumOS — UI LAYER (Compose). Depends on com.quantumos.core for logic and com.quantumos.appshell
 * for the shared chrome (App Shell Integration Step 1).
 */

// Stealth dim target for this window. Hard-dim but not fully black — the Operator must still read
// the panel. Saturation is untouched (we only lower brightness), per the M3 Stealth spec.
private const val STEALTH_BRIGHTNESS = 0.04f

// First back/any camera that advertises a flash unit — for Beacon's setTorchMode call.
private fun firstFlashCameraId(cm: CameraManager?): String? {
    cm ?: return null
    return cm.cameraIdList.firstOrNull { id ->
        cm.getCameraCharacteristics(id)
            .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
    }
}

// ---------- installed-app entry (UI layer only; no PackageManager dep in core) ----------
// `icon` is decoded once, off the main thread, in loadApps() below — never per-item at scroll
// time (see BUILD_LOG: Apps-menu scroll-stutter diagnosis/fix).
@Immutable
data class AppInfo(
    val label: String,
    val packageName: String,
    val activityName: String,
    val icon: ImageBitmap?
)

// ---------- connectivity readout (UI layer only; core's VitalityState has no transport field) ----------
// M2 keeps this deliberately coarse: connected/not + a transport label. Precise signal-strength
// bars are NOT in scope here (they'd need READ_PHONE_STATE) — see the M2 brief hard stops.
@Immutable
data class ConnectivityInfo(
    val connected: Boolean = false,
    val transport: String = "OFFLINE"
)

// ---------- ViewModel ----------
// M5: the engine/parser/telemetry now live in the process-singleton QuantumRuntime so the launcher
// and the QUARK Assistant View (a separate Activity) share ONE state. The ViewModel keeps only the
// launcher-local UI state (installed apps + the Vitality-panel open flag) and delegates the rest.
class QuantumViewModel : ViewModel() {
    val engine = QuantumRuntime.engine
    val parser = QuantumRuntime.parser

    private val _installedApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val installedApps: StateFlow<List<AppInfo>> = _installedApps.asStateFlow()

    // UI-only connectivity label for the STATUS readout — shared via the runtime.
    val connectivity: StateFlow<ConnectivityInfo> = QuantumRuntime.connectivity

    // M3: Vitality-panel open/stow. UI navigation state — held here so it survives fold/rotate
    // (per platform rule: state that must survive config change lives in the ViewModel, not composition).
    private val _vitalityPanelOpen = MutableStateFlow(false)
    val vitalityPanelOpen: StateFlow<Boolean> = _vitalityPanelOpen.asStateFlow()

    fun boot(context: Context) = QuantumRuntime.boot(context)

    fun setHue(hue: PhosphorHue) = engine.updateEnvironmentProfile { it.copy(activeHue = hue) }

    fun navigate(target: NavigationChannel) {
        engine.transitionNavigation(target)
        QuantumRuntime.playCue(SoundCue.UI_CLUNK)        // UI-select clunk on channel change
    }

    // M6 STATUS toggles — cycle + persist + (region) QUARK ack, all via the runtime.
    fun cycleDeploymentRegion() = QuantumRuntime.cycleDeploymentRegion()
    fun cycleBootPace() = QuantumRuntime.cycleBootPace()

    // ---------- M3 Vitality-panel wiring (all delegate to the single engine seam) ----------
    fun toggleVitalityPanel() { _vitalityPanelOpen.value = !_vitalityPanelOpen.value }
    fun stowVitalityPanel() { _vitalityPanelOpen.value = false }

    fun cyclePhosphor() = engine.cyclePhosphorHue()
    fun toggleStealth() = engine.toggleStealthMode()
    fun toggleBeacon() = engine.toggleBeacon()
    // Lock is cosmetic (Bible decision 56): lock plays the PLEASE STANDBY → DEVICE SECURED beat;
    // unlock is via the lock-overlay tap. Both call the existing engine methods — nothing rebuilt.
    fun engageLock() = engine.executeCosmeticLockSequence()
    fun releaseLock() = engine.unlockDeviceProfile()

    // Runs entirely off the main thread: PackageManager queries + icon decode/compositing are not
    // free, and AppCell used to pay that cost per-item, synchronously, the first time each cell
    // scrolled into view — the cause of the Apps-menu scroll stutter (see BUILD_LOG). Decoding
    // every icon once here, up front, means AppCell only ever renders an already-decoded bitmap.
    fun loadApps(pm: PackageManager) {
        viewModelScope.launch(Dispatchers.Default) {
            val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            @Suppress("DEPRECATION")
            val resolved = pm.queryIntentActivities(intent, 0)
            val iconCache = mutableMapOf<String, ImageBitmap?>()
            _installedApps.value = resolved
                .map { ri ->
                    val pkg = ri.activityInfo.packageName
                    val icon = iconCache.getOrPut(pkg) {
                        runCatching { pm.getApplicationIcon(pkg).toBitmapCompat()?.asImageBitmap() }.getOrNull()
                    }
                    AppInfo(
                        label = ri.loadLabel(pm).toString(),
                        packageName = pkg,
                        activityName = ri.activityInfo.name,
                        icon = icon
                    )
                }
                .sortedBy { it.label.lowercase() }
        }
    }

    // M2 telemetry now runs in QuantumRuntime (shared, app-scoped) — the launcher just kicks it off.
    fun startTelemetry(context: Context) = QuantumRuntime.startTelemetry(context)
}

// ---------- Activity ----------
class LauncherActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val vm: QuantumViewModel = viewModel()
            val context = LocalContext.current
            val state by vm.engine.masterState.collectAsState()
            LaunchedEffect(Unit) {
                vm.boot(context)                  // loads persisted settings, then cold boot
                vm.startTelemetry(context)        // M2: real battery/uptime/connectivity poll
                vm.loadApps(context.packageManager)
            }

            // M3 Stealth — hard-dim THIS window only via screenBrightness (no WRITE_SETTINGS, no
            // system-wide change, fully reversible). Phosphor colour saturation is untouched — only
            // the panel brightness drops. BRIGHTNESS_OVERRIDE_NONE hands control back to the system.
            LaunchedEffect(state.environment.isStealthMode) {
                window.attributes = window.attributes.apply {
                    screenBrightness = if (state.environment.isStealthMode) STEALTH_BRIGHTNESS
                    else WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                }
            }

            // M3 Beacon — real torch via CameraManager.setTorchMode (no camera permission needed for
            // this call, build spec §5). Wrapped in runCatching: foldables can momentarily lose the
            // flash camera across a fold/unfold; we fail dark rather than crash. onDispose kills the
            // torch so it never strands on when the launcher leaves the screen.
            val cameraManager = remember {
                context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
            }
            DisposableEffect(state.environment.isBeaconActive, cameraManager) {
                val flashId = runCatching { firstFlashCameraId(cameraManager) }.getOrNull()
                if (flashId != null) {
                    runCatching { cameraManager?.setTorchMode(flashId, state.environment.isBeaconActive) }
                }
                onDispose {
                    if (flashId != null) runCatching { cameraManager?.setTorchMode(flashId, false) }
                }
            }

            // ---------- M4 — floating QUARK trigger: permission walkthrough + service deploy ----------
            // The "draw over other apps" capability is the one new permission this milestone touches
            // (Launcher Build Spec §5, pre-approved as a one-time Settings toggle). It CANNOT be asked
            // through a runtime dialog — only via the system overlay-settings screen.
            val lifecycleOwner = LocalLifecycleOwner.current
            var canOverlay by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
            // Re-check on resume: the Operator grants it OUTSIDE the app and returns — no restart needed.
            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) canOverlay = Settings.canDrawOverlays(context)
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
            }
            // Deploy (or re-tint) the overlay once granted. Keyed on the active hue too, so a live
            // phosphor switch recolours the floating mark via a redelivered start command.
            LaunchedEffect(canOverlay, state.environment.activeHue) {
                if (canOverlay) {
                    QuarkTriggerService.deploy(context, Phosphor.bright(state.environment.activeHue).toArgb())
                }
            }
            val onRequestOverlay: () -> Unit = {
                context.startActivity(
                    Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + context.packageName)
                    )
                )
            }

            QuantumOSLayoutShell(forceFixedContainer = false) { constraints ->
                QuantumAppShell(
                    vm = vm,
                    constraints = constraints,
                    canOverlay = canOverlay,
                    onRequestOverlay = onRequestOverlay
                )
            }
        }
    }
}

// ---------- App Shell (house-style chrome: nameplate + channel strip + content) ----------
@Composable
fun QuantumAppShell(
    vm: QuantumViewModel,
    constraints: TerminalConstraints,
    canOverlay: Boolean,
    onRequestOverlay: () -> Unit
) {
    val state by vm.engine.masterState.collectAsState()
    val panelOpen by vm.vitalityPanelOpen.collectAsState()
    val color = Phosphor.bright(state.environment.activeHue)
    val dimColor = Phosphor.dim(state.environment.activeHue)
    val font = Fonts.ChakraPetch    // M6 Step 1: real bundled face, replacing the Monospace placeholder

    // Track whether cold boot has completed at least once, so the Lock overlay's
    // PLEASE STANDBY → DEVICE SECURED beat shows only for a real lock, never during boot
    // (which transits PLEASE_STANDBY on its way to ACTIVE).
    var hasBooted by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(state.bootLifecycle) {
        if (state.bootLifecycle == BootLifecycleState.ACTIVE) hasBooted = true
    }
    val showLock = hasBooted && (state.bootLifecycle == BootLifecycleState.PLEASE_STANDBY ||
        state.bootLifecycle == BootLifecycleState.DEVICE_SECURED)

    // M6 Step 3 — the boot-splash ceremony plays only on a TRUE cold boot (before the first ACTIVE).
    // A plain Home-press resumes the existing ViewModel (hasBooted already true) and the engine's own
    // bootLifecycle != UNINITIALIZED guard skips the sequence — so this never replays on resume.
    val booting = !hasBooted && state.bootLifecycle != BootLifecycleState.ACTIVE

    BackHandler(enabled = true) {
        when {
            panelOpen -> vm.stowVitalityPanel()                       // stow the shade first
            state.currentNavigation != NavigationChannel.HOME -> vm.navigate(NavigationChannel.HOME)
            // on HOME with nothing open: consume the back press (shell never exits)
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(constraints.systemBarsPadding)
        ) {
            NameplateHeader(
                channelName = state.currentNavigation.name,
                color = color,
                dimColor = dimColor,
                font = font
            )
            ChannelStrip(
                current = state.currentNavigation,
                color = color,
                dimColor = dimColor,
                font = font,
                onSelect = { vm.navigate(it) }
            )
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (state.currentNavigation) {
                    NavigationChannel.HOME -> HomeChannelBody(vm = vm, state = state, panelOpen = panelOpen, color = color, dimColor = dimColor, font = font, constraints = constraints, canOverlay = canOverlay, onRequestOverlay = onRequestOverlay)
                    NavigationChannel.APPS -> AppsChannelScreen(vm = vm, color = color, dimColor = dimColor, font = font)
                    NavigationChannel.STATUS -> StatusChannelScreen(vm = vm, state = state, color = color, dimColor = dimColor, font = font)
                    NavigationChannel.LOG -> LogChannelScreen(vm = vm, color = color, dimColor = dimColor, font = font)
                }
            }
        }

        // Cosmetic Lock overlay (Bible decision 56) — full-screen, above all chrome. Tap to unlock.
        if (showLock) {
            LockOverlay(
                lifecycle = state.bootLifecycle,
                color = color,
                dimColor = dimColor,
                font = font,
                onUnlock = { vm.releaseLock() }
            )
        }

        // The full-screen cold-boot ceremony — drawn above the shell while booting, resolves to Home.
        if (booting) {
            BootSplash(state = state, color = color, dimColor = dimColor, font = font)
        }
    }
}

/*
 * BootSplash — the M6 Step 3 cold-boot ceremony. Replaces the old "background log lines" boot with a
 * real full-screen beat: CRT power-on flash → stepped boot log (each step ticked) → QUARK online (her
 * §6 canon line with live data + power-up sweep, iris opening) → Monoton wordmark stamp → PLEASE
 * STANDBY → Home. Static at rest within each state; life comes only from the discrete state changes
 * the engine drives. Resolves to Home in ALL cases this milestone (the "Lock (cold)" nuance is flagged
 * in BUILD_LOG.md, not guessed at — brief Step 3).
 */
@Composable
private fun BootSplash(
    state: QuantumLauncherState,
    color: Color,
    dimColor: Color,
    font: FontFamily
) {
    val lc = state.bootLifecycle
    // The five stepped boot stages, in order, mapped to their lifecycle states.
    val stages = listOf(
        BootLifecycleState.STEP_CORE to "CORE",
        BootLifecycleState.STEP_PHOSPHOR_DRIVER to "PHOSPHOR DRIVER",
        BootLifecycleState.STEP_SENSOR_ARRAY to "SENSOR ARRAY",
        BootLifecycleState.STEP_BIOMETRICS to "BIOMETRICS",
        BootLifecycleState.STEP_QUARK to "QUARK"
    )

    // CRT power-on: a brief bright flash that falls off into the ceremony (one-shot, not an idle loop).
    var flash by remember { mutableStateOf(0f) }
    LaunchedEffect(lc) {
        if (lc == BootLifecycleState.CRT_POWER_ON) {
            for (a in listOf(1f, 0.7f, 0.4f, 0.15f, 0f)) { flash = a; delay(45) }
        } else flash = 0f
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Phosphor.Crt)
            .crtShader(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            Modifier.fillMaxWidth().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ----- stepped boot log (CORE → … → QUARK) -----
            Text("QUANTUMOS // COLD BOOT", color = dimColor, fontFamily = font, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))
            stages.forEach { (stageState, label) ->
                val done = lc.ordinal > stageState.ordinal
                val active = lc.ordinal == stageState.ordinal
                val marker = when {
                    done -> "[ OK ]"
                    active -> "[ >> ]"
                    else -> "[ .. ]"
                }
                Row(Modifier.fillMaxWidth(0.7f).padding(vertical = 2.dp)) {
                    Text(marker, color = if (done || active) color else dimColor, fontFamily = font, fontSize = 12.sp)
                    Spacer(Modifier.width(10.dp))
                    Text(label, color = if (done || active) color else dimColor, fontFamily = font, fontSize = 12.sp)
                }
            }

            Spacer(Modifier.height(24.dp))

            // ----- stage-specific centrepiece -----
            when (lc) {
                BootLifecycleState.QUARK_ONLINE -> {
                    BootIris(color = color, dimColor = dimColor)
                    Spacer(Modifier.height(16.dp))
                    val line = state.quarkBrain.responseTextSnippet
                    Text(
                        text = if (line.isNotBlank()) "QUARK: $line" else "QUARK ONLINE",
                        color = color, fontFamily = font, fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }
                BootLifecycleState.WORDMARK_STAMP -> {
                    // The ONE ceremonial Monoton use — the boot wordmark stamp (Step 1 / Step 3).
                    Text(
                        text = "QuantumOS",
                        color = color,
                        fontFamily = Fonts.Monoton,
                        fontSize = 40.sp,
                        textAlign = TextAlign.Center
                    )
                }
                BootLifecycleState.PLEASE_STANDBY -> {
                    PleaseStandbyCard(subline = "BRINGING UP HOME…", color = color, dimColor = dimColor, font = font)
                }
                else -> {
                    Text("◈", color = dimColor, fontFamily = font, fontSize = 28.sp)
                }
            }
        }

        // CRT power-on flash overlay — fades to reveal the ceremony beneath.
        if (flash > 0f) {
            Box(Modifier.fillMaxSize().background(color.copy(alpha = flash)))
        }
    }
}

// A small boot-only iris that "opens" once when QUARK comes online — a stepped aperture bloom, not an
// ambient loop. Distinct from the Assistant View's QuarkPresence (this is the boot ceremony beat).
@Composable
private fun BootIris(color: Color, dimColor: Color) {
    var aperture by remember { mutableStateOf(0f) }
    LaunchedEffect(Unit) {
        for (a in listOf(0.15f, 0.4f, 0.65f, 0.85f, 1f)) { aperture = a; delay(60) }
    }
    androidx.compose.foundation.Canvas(Modifier.size(96.dp)) {
        val r = size.minDimension / 2f * 0.9f
        val stroke = r * 0.1f
        drawCircle(color = Phosphor.Crt, radius = r)
        drawCircle(color = color, radius = r, style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke))
        drawCircle(color = dimColor, radius = r * 0.6f, style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke * 0.7f))
        drawCircle(color = color, radius = r * 0.5f * aperture)
    }
}

// ---------- HOME channel body (M0 readout + M3 Vitality atom-mark / roll-down panel) ----------
@Composable
private fun HomeChannelBody(
    vm: QuantumViewModel,
    state: QuantumLauncherState,
    panelOpen: Boolean,
    color: Color,
    dimColor: Color,
    font: FontFamily,
    constraints: TerminalConstraints,
    canOverlay: Boolean,
    onRequestOverlay: () -> Unit
) {
    val logs by vm.engine.systemLogs.collectAsState()
    val conn by vm.connectivity.collectAsState()

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Home header: the quantum-atom Vitality pull (Home-channel-only, M3 scope boundary),
            // with the blinking Beacon field-flag riding beside it while Beacon is lit.
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("FIELD OPS", color = dimColor, fontFamily = font, fontSize = 11.sp)
                Spacer(Modifier.weight(1f))
                if (state.environment.isBeaconActive) {
                    BeaconFlag(font = font)
                    Spacer(Modifier.width(12.dp))
                }
                AtomMark(open = panelOpen, color = color, dimColor = dimColor, font = font) {
                    vm.toggleVitalityPanel()
                }
            }
            Spacer(Modifier.height(12.dp))
            Row {
                HueChip("GREEN", color) { vm.setHue(PhosphorHue.GREEN) }
                Spacer(Modifier.width(12.dp))
                HueChip("AMBER", color) { vm.setHue(PhosphorHue.AMBER) }
                Spacer(Modifier.width(12.dp))
                HueChip("CYAN", color) { vm.setHue(PhosphorHue.CYAN) }
            }
            Spacer(Modifier.height(8.dp))
            // Deployment Region status line — terse, utilitarian status text (NOT QUARK speaking).
            Text(
                text = "DEPLOYMENT: ${DeploymentRegions.label(state.deploymentRegion)}",
                color = dimColor,
                fontFamily = font,
                fontSize = 11.sp
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = buildReadout(state, logs, constraints),
                color = color,
                fontFamily = font,
                fontSize = 13.sp
            )
            Spacer(Modifier.height(12.dp))
            // M4 — the floating QUARK trigger's enable/status affordance lives on HOME.
            QuarkTriggerControl(
                canOverlay = canOverlay,
                color = color,
                dimColor = dimColor,
                font = font,
                onRequestOverlay = onRequestOverlay
            )
        }

        // The Vitality panel rolls down (stepped) over the Home content from the top of this body.
        VitalityPanel(
            open = panelOpen,
            vitality = state.vitality,
            environment = state.environment,
            connectivityLabel = if (conn.connected) conn.transport else "OFFLINE",
            color = color,
            dimColor = dimColor,
            font = font,
            onStow = { vm.stowVitalityPanel() },
            onStealth = { vm.toggleStealth() },
            onPhosphor = { vm.cyclePhosphor() },
            onBeacon = { vm.toggleBeacon() },
            onLock = { vm.engageLock() }
        )
    }
}

// The quantum-atom mark: static at rest; one stepped spin when the panel opens. Tap toggles the panel.
@Composable
private fun AtomMark(open: Boolean, color: Color, dimColor: Color, font: FontFamily, onClick: () -> Unit) {
    // Stepped spin: discrete quarter-turn clicks on open only (no idle animation, house-style static-at-rest).
    var spin by remember { mutableStateOf(0f) }
    LaunchedEffect(open) {
        if (open) {
            for (a in listOf(90f, 180f, 270f, 360f)) { spin = a; delay(45) }
            spin = 0f
        }
    }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { onClick() }.padding(4.dp)) {
        Text(
            text = "⚛",                       // ⚛ atom mark
            color = if (open) color else dimColor,
            fontFamily = font,
            fontSize = 22.sp,
            modifier = Modifier.rotate(spin)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = if (open) "[VITALITY]" else "VITALITY",
            color = if (open) color else dimColor,
            fontFamily = font,
            fontSize = 11.sp
        )
    }
}

// Blinking warn-red field flag — shown on Home while Beacon is active. Stepped blink, runs ONLY
// while mounted (i.e. only while Beacon is on) so there's no idle redraw at rest.
@Composable
private fun BeaconFlag(font: FontFamily) {
    var on by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        while (true) { delay(450); on = !on }
    }
    Text(
        text = "⚑ BEACON",                    // ⚑ flag
        color = if (on) Phosphor.Warn else Color.Transparent,
        fontFamily = font,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun HueChip(label: String, color: Color, onClick: () -> Unit) {
    Text(
        text = "[$label]",
        color = color,
        fontFamily = Fonts.ChakraPetch,
        fontSize = 13.sp,
        modifier = Modifier.clickable { onClick() }.padding(4.dp)
    )
}

// M4 trigger control: when the overlay permission isn't granted, a tappable line opens the system
// settings screen (the only path — this permission has no runtime dialog). Once granted, the service
// is already deployed (see LauncherActivity) and this reads as a static, dim confirmation.
@Composable
private fun QuarkTriggerControl(
    canOverlay: Boolean,
    color: Color,
    dimColor: Color,
    font: FontFamily,
    onRequestOverlay: () -> Unit
) {
    if (canOverlay) {
        Text(
            text = "QUARK TRIGGER // DEPLOYED",
            color = dimColor,
            fontFamily = font,
            fontSize = 11.sp
        )
    } else {
        Box(
            Modifier
                .clickable { onRequestOverlay() }
                .border(BorderStroke(1.dp, color))
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Text(
                text = "QUARK TRIGGER // GRANT OVERLAY ►",
                color = color,
                fontFamily = font,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
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

// ---------- M3 Vitality panel (rolls down from the atom mark, Home-channel-only) ----------
@Composable
private fun VitalityPanel(
    open: Boolean,
    vitality: VitalityState,
    environment: EnvironmentProfile,
    connectivityLabel: String,
    color: Color,
    dimColor: Color,
    font: FontFamily,
    onStow: () -> Unit,
    onStealth: () -> Unit,
    onPhosphor: () -> Unit,
    onBeacon: () -> Unit,
    onLock: () -> Unit
) {
    // Stepped roll-down: advance a discrete step count (window-blind clicks), NOT a smooth slide.
    val steps = 9
    var step by remember { mutableIntStateOf(0) }
    LaunchedEffect(open) {
        if (open) while (step < steps) { step++; delay(26) }
        else while (step > 0) { step--; delay(20) }
    }
    if (step == 0) return
    val fraction = step.toFloat() / steps

    // Uptime is the one vital that ticks continuously (it's a clock); tick 1s while open only,
    // so there's zero idle redraw once the panel is stowed (static-at-rest).
    var nowMs by remember { mutableStateOf(SystemClock.elapsedRealtime()) }
    LaunchedEffect(open) {
        while (open) { nowMs = SystemClock.elapsedRealtime(); delay(1000) }
    }

    Box(
        Modifier
            .fillMaxWidth()
            .fillMaxHeight(fraction)
            .clipToBounds()                       // the clip is what reveals the panel as it grows
            .background(Phosphor.Crt)             // opaque — Home content must never bleed through
            .border(BorderStroke(1.dp, dimColor))
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text("VITALITY // FIELD READINESS", color = color, fontFamily = font, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))

            // --- Zone 1: vitals at a gaze (read-only) ---
            val readinessColor = if (vitality.readiness == SystemReadiness.CRITICAL) Phosphor.Warn else color
            val readinessWord = when (vitality.readiness) {
                SystemReadiness.NOMINAL -> "NOMINAL"
                SystemReadiness.DEGRADED -> "DEGRADED"
                SystemReadiness.CRITICAL -> "CRITICAL"
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("READINESS", color = dimColor, fontFamily = font, fontSize = 13.sp)
                Spacer(Modifier.weight(1f))
                Text(
                    "${vitality.readinessPercent}%  $readinessWord",
                    color = readinessColor,
                    fontFamily = font,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(8.dp))

            SegmentedGauge(
                label = "SIGNAL",
                filled = vitality.connectivityStrength.coerceIn(0, 4),
                total = 4,
                value = connectivityLabel,
                color = color, dimColor = dimColor, font = font
            )
            SegmentedGauge(
                label = "POWER",
                filled = ((vitality.batteryPercentage.coerceIn(0, 100) + 5) / 10),
                total = 10,
                value = "${vitality.batteryPercentage}%${if (vitality.isCharging) " ⚡" else ""}",
                color = color, dimColor = dimColor, font = font
            )
            SegmentedGauge(
                label = "CORE TEMP",
                // Map the locked battery-temp stand-in across a 25–50°C field range onto 10 segments.
                filled = (((vitality.coreTempCelsius - 25f) / 25f) * 10f).coerceIn(0f, 10f).toInt(),
                total = 10,
                value = String.format(Locale.US, "%.1f°C", vitality.coreTempCelsius),
                color = color, dimColor = dimColor, font = font
            )
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Text("UPTIME".padEnd(10), color = dimColor, fontFamily = font, fontSize = 12.sp)
                Spacer(Modifier.weight(1f))
                Text(formatUptime(nowMs), color = color, fontFamily = font, fontSize = 12.sp)
            }

            Spacer(Modifier.height(10.dp))
            Text("----------------------------------------", color = dimColor, fontFamily = font, fontSize = 12.sp)
            Spacer(Modifier.height(10.dp))

            // --- Zone 2: the four quick actions (Bible decision 36 order: Stealth · Phosphor · Beacon · Lock) ---
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ActionCell(
                    modifier = Modifier.weight(1f),
                    title = "STEALTH",
                    status = if (environment.isStealthMode) "ENGAGED" else "STANDBY",
                    active = environment.isStealthMode,
                    color = color, dimColor = dimColor, font = font, onClick = onStealth
                )
                ActionCell(
                    modifier = Modifier.weight(1f),
                    title = "PHOSPHOR",
                    status = environment.activeHue.name,
                    active = false,                    // momentary cycle, not a sticky toggle
                    color = color, dimColor = dimColor, font = font, onClick = onPhosphor
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ActionCell(
                    modifier = Modifier.weight(1f),
                    title = "BEACON",
                    status = if (environment.isBeaconActive) "ACTIVE" else "DARK",
                    active = environment.isBeaconActive,
                    color = color, dimColor = dimColor, font = font, onClick = onBeacon
                )
                ActionCell(
                    modifier = Modifier.weight(1f),
                    title = "LOCK",
                    status = "COSMETIC",
                    active = false,
                    color = color, dimColor = dimColor, font = font, onClick = onLock
                )
            }

            Spacer(Modifier.height(12.dp))
            // STOW handle — the second close affordance (tapping the atom mark also stows).
            Box(
                Modifier
                    .fillMaxWidth()
                    .clickable { onStow() }
                    .border(BorderStroke(1.dp, dimColor))
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("▲ STOW", color = dimColor, fontFamily = font, fontSize = 12.sp)
            }
        }
    }
}

// In-house segmented gauge — a short row of filled/unfilled phosphor segments + a numeric value.
// No Material LinearProgressIndicator; themed with the active phosphor.
@Composable
private fun SegmentedGauge(
    label: String,
    filled: Int,
    total: Int,
    value: String,
    color: Color,
    dimColor: Color,
    font: FontFamily
) {
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(Modifier.fillMaxWidth()) {
            Text(label.padEnd(10), color = dimColor, fontFamily = font, fontSize = 12.sp)
            Spacer(Modifier.weight(1f))
            Text(value, color = color, fontFamily = font, fontSize = 12.sp)
        }
        Spacer(Modifier.height(3.dp))
        Row(Modifier.fillMaxWidth()) {
            repeat(total) { i ->
                Box(
                    Modifier
                        .weight(1f)
                        .height(10.dp)
                        .background(if (i < filled) color else dimColor.copy(alpha = 0.22f))
                )
                if (i < total - 1) Spacer(Modifier.width(2.dp))
            }
        }
    }
}

// One Zone-2 quick-action tile. Active = bright phosphor frame; inactive = dim. No Material chrome.
@Composable
private fun ActionCell(
    modifier: Modifier,
    title: String,
    status: String,
    active: Boolean,
    color: Color,
    dimColor: Color,
    font: FontFamily,
    onClick: () -> Unit
) {
    val edge = if (active) color else dimColor
    Column(
        modifier
            .clickable { onClick() }
            .border(BorderStroke(1.dp, edge))
            .padding(horizontal = 12.dp, vertical = 12.dp)
    ) {
        Text(title, color = edge, fontFamily = font, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(
            text = if (active) "[●] $status" else "[ ] $status",
            color = if (active) color else dimColor,
            fontFamily = font,
            fontSize = 10.sp
        )
    }
}

// Cosmetic Lock overlay — plays the existing PLEASE STANDBY → DEVICE SECURED beat; tap to unlock.
// Does NOT use Device Admin / real lockNow() (Bible decision 56).
@Composable
private fun LockOverlay(
    lifecycle: BootLifecycleState,
    color: Color,
    dimColor: Color,
    font: FontFamily,
    onUnlock: () -> Unit
) {
    val secured = lifecycle == BootLifecycleState.DEVICE_SECURED
    Box(
        Modifier
            .fillMaxSize()
            .background(Phosphor.Crt)
            .clickable(enabled = secured) { onUnlock() },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (secured) {
                Text("◼ DEVICE SECURED", color = color, fontFamily = font, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                Text("TAP TO UNSEAL, OPERATOR", color = dimColor, fontFamily = font, fontSize = 12.sp)
            } else {
                // The universal loading beat — reused, never a generic spinner.
                PleaseStandbyCard(subline = "SEALING PERIMETER…", color = color, dimColor = dimColor, font = font)
            }
        }
    }
}

// ---------- APPS channel (Step 3) ----------
@Composable
private fun AppsChannelScreen(
    vm: QuantumViewModel,
    color: Color,
    dimColor: Color,
    font: FontFamily
) {
    val apps by vm.installedApps.collectAsState()
    val context = LocalContext.current

    if (apps.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "SCANNING PACKAGE REGISTRY...",
                color = dimColor,
                fontFamily = font,
                fontSize = 13.sp
            )
        }
        return
    }

    // M2 Step 0: adaptive columns from a target cell width — column count follows screen width
    // (more columns on the unfolded Fold 6, fewer when narrow), not a hardcoded count.
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 88.dp),
        contentPadding = PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        gridItems(apps, key = { it.packageName + it.activityName }) { app ->
            AppCell(
                app = app,
                color = color,
                dimColor = dimColor,
                font = font
            ) {
                val launchIntent = context.packageManager.getLaunchIntentForPackage(app.packageName)
                launchIntent?.let { context.startActivity(it) }
            }
        }
    }
}

@Composable
private fun AppCell(
    app: AppInfo,
    color: Color,
    dimColor: Color,
    font: FontFamily,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
            if (app.icon != null) {
                Image(
                    bitmap = app.icon,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp)
                )
            } else {
                Text("◈", color = dimColor, fontFamily = font, fontSize = 22.sp)
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = app.label,
            color = color,
            fontFamily = font,
            fontSize = 9.sp,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 11.sp,
            modifier = Modifier.width(68.dp)
        )
    }
}

// ---------- STATUS channel (M2 Step 1): real vitals readout ----------
@Composable
private fun StatusChannelScreen(
    vm: QuantumViewModel,
    state: QuantumLauncherState,
    color: Color,
    dimColor: Color,
    font: FontFamily
) {
    val v = state.vitality
    val conn by vm.connectivity.collectAsState()

    // Readiness uses --warn ONLY for the genuine alert (CRITICAL), per the design tokens.
    val readinessColor = when (v.readiness) {
        SystemReadiness.NOMINAL -> color
        SystemReadiness.DEGRADED -> dimColor
        SystemReadiness.CRITICAL -> Phosphor.Warn
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("=== STATUS // FIELD VITALS ===", color = color, fontFamily = font, fontSize = 13.sp)
        Spacer(Modifier.height(10.dp))
        ReadoutRow("POWER", "${v.batteryPercentage}%", color, dimColor, font)
        ReadoutRow("STATE", if (v.isCharging) "CHARGING" else "DISCHARGING", color, dimColor, font)
        ReadoutRow("CORE TEMP", String.format(Locale.US, "%.1f°C", v.coreTempCelsius), color, dimColor, font)
        ReadoutRow("UPTIME", formatUptime(v.systemUptimeMs), color, dimColor, font)
        ReadoutRow("LINK", if (conn.connected) conn.transport else "OFFLINE", color, dimColor, font)
        Spacer(Modifier.height(4.dp))
        Text("----------------------------------------", color = dimColor, fontFamily = font, fontSize = 13.sp)
        ReadoutRow("READINESS", v.readiness.name, readinessColor, dimColor, font)

        // ----- CONFIG: tap-to-cycle settings (same interaction pattern as Cycle-phosphor). Both
        // persist across restarts (M6 Step 0/2). -----
        Spacer(Modifier.height(14.dp))
        Text("=== CONFIG // FIELD SETTINGS ===", color = color, fontFamily = font, fontSize = 13.sp)
        Spacer(Modifier.height(6.dp))
        ConfigCycleRow(
            label = "DEPLOYMENT REGION",
            value = DeploymentRegions.label(state.deploymentRegion),
            color = color, dimColor = dimColor, font = font
        ) { vm.cycleDeploymentRegion() }
        ConfigCycleRow(
            label = "BOOT PACE",
            value = if (state.bootPace == BootPace.DELIBERATE) "DELIBERATE" else "SNAPPY",
            color = color, dimColor = dimColor, font = font
        ) { vm.cycleBootPace() }
    }
}

// A tappable STATUS settings row: dim label · bright value · ► cycle affordance. Tap cycles the
// setting (and persists it). Same tap-to-cycle pattern as the Vitality-panel Phosphor control.
@Composable
private fun ConfigCycleRow(
    label: String,
    value: String,
    color: Color,
    dimColor: Color,
    font: FontFamily,
    onClick: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label.padEnd(18), color = dimColor, fontFamily = font, fontSize = 13.sp)
        Text(": $value", color = color, fontFamily = font, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.weight(1f))
        Text("►", color = color, fontFamily = font, fontSize = 13.sp)
    }
}

// Aligned label/value line in the terminal-readout strip style (monospace, dim label, bright value).
@Composable
private fun ReadoutRow(label: String, value: String, valueColor: Color, dimColor: Color, font: FontFamily) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(
            text = label.padEnd(12),
            color = dimColor,
            fontFamily = font,
            fontSize = 13.sp
        )
        Text(
            text = ": $value",
            color = valueColor,
            fontFamily = font,
            fontSize = 13.sp
        )
    }
}

private fun formatUptime(ms: Long): String {
    val totalSeconds = ms / 1000
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    return String.format(Locale.US, "%02d:%02d:%02d", h, m, s)
}

// ---------- LOG channel (M2 Step 2): the console reel ----------
@Composable
private fun LogChannelScreen(
    vm: QuantumViewModel,
    color: Color,
    dimColor: Color,
    font: FontFamily
) {
    val logs by vm.engine.systemLogs.collectAsState()
    // Engine caps storage at 150; render the recent window. Most-recent stays visible (auto-scroll to end).
    val recent = remember(logs) { logs.takeLast(100) }
    val listState = rememberLazyListState()

    LaunchedEffect(recent.size) {
        if (recent.isNotEmpty()) listState.animateScrollToItem(recent.lastIndex)
    }

    if (recent.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("LOG REGISTER EMPTY", color = dimColor, fontFamily = font, fontSize = 13.sp)
        }
        return
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        items(recent) { entry ->
            // Strip the raw epoch-millis prefix the engine stamps ("[<millis>] message").
            Text(
                text = "> ${entry.substringAfter("] ")}",
                color = color,
                fontFamily = font,
                fontSize = 12.sp
            )
        }
    }
}

// Converts any Drawable to Bitmap for Compose. Falls back to null on failure.
private fun Drawable.toBitmapCompat(): Bitmap? = runCatching {
    if (this is BitmapDrawable && bitmap != null) return bitmap
    val w = intrinsicWidth.takeIf { it > 0 } ?: 48
    val h = intrinsicHeight.takeIf { it > 0 } ?: 48
    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    setBounds(0, 0, canvas.width, canvas.height)
    draw(canvas)
    bmp
}.getOrNull()
