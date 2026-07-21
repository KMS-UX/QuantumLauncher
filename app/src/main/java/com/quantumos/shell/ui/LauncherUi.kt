package com.quantumos.shell.ui

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.quantumos.shell.overlay.QuarkTriggerService
import com.quantumos.appshell.ChannelStrip
import com.quantumos.appshell.Fonts
import com.quantumos.appshell.Glyph
import com.quantumos.appshell.NameplateHeader
import com.quantumos.appshell.Phosphor
import com.quantumos.appshell.PleaseStandbyCard
import com.quantumos.appshell.QuantumIcon
import com.quantumos.appshell.QuantumOSLayoutShell
import com.quantumos.appshell.SegmentedGauge
import com.quantumos.appshell.TerminalConstraints
import com.quantumos.appshell.crtOverlay
import com.quantumos.appshell.crtShader
import com.quantumos.core.BootLifecycleState
import com.quantumos.core.DeploymentRegions
import com.quantumos.core.DockedModule
import com.quantumos.core.EnvironmentProfile
import com.quantumos.core.InstrumentConsole
import com.quantumos.core.InstrumentId
import com.quantumos.core.InstrumentSpec
import com.quantumos.core.NavigationChannel
import com.quantumos.core.PhosphorHue
import com.quantumos.core.QuantumLauncherState
import com.quantumos.core.ReelPager
import com.quantumos.core.SoundCue
import com.quantumos.core.SystemReadiness
import com.quantumos.core.VitalityState
import com.quantumos.audio.AudioActivity
import com.quantumos.comms.CommsActivity
import com.quantumos.config.ConfigActivity
import com.quantumos.files.FilesActivity
import com.quantumos.nav.NavActivity
import com.quantumos.optics.OpticsActivity
import com.quantumos.radio.RadioActivity
import com.quantumos.signal.SignalActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

/*
 * QuantumOS — UI LAYER (Compose). Depends on com.quantumos.core for all logic and
 * com.quantumos.appshell for the shared chrome (App Shell Integration, Phase 3 —
 * Phosphor/Fonts/TerminalConstraints/QuantumOSLayoutShell/NameplateHeader/ChannelStrip/
 * PleaseStandbyCard/crtShader/crtOverlay all moved there so :optics and :nav share one source).
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
@Immutable
data class AppInfo(
    val label: String,
    val packageName: String,
    val activityName: String
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

    fun navigate(target: NavigationChannel) {
        engine.transitionNavigation(target)
        QuantumRuntime.playCue(SoundCue.UI_CLUNK)        // UI-select clunk on channel change
    }

    // Launcher Restructure Phase 1 — tapping a not-yet-built HOME instrument. Reuses the
    // access-denied cue bank entry (previously unwired — BUILD_LOG "known issues"): the Operator
    // attempted a real action and it didn't happen, which is exactly what buzz_denied is for.
    fun signalInstrumentOffline() = QuantumRuntime.playCue(SoundCue.BUZZ_DENIED)

    // App Shell Integration (Phase 3) — the same UI-select clunk as a channel switch, since docking
    // into CAM/MAPS is the same kind of committed navigation as switching a channel.
    fun signalInstrumentDock() = QuantumRuntime.playCue(SoundCue.UI_CLUNK)

    // ---------- M3 Vitality-panel wiring (all delegate to the single engine seam) ----------
    fun toggleVitalityPanel() { _vitalityPanelOpen.value = !_vitalityPanelOpen.value }
    fun stowVitalityPanel() { _vitalityPanelOpen.value = false }

    // Phosphor now cycles + persists through the runtime (SIGNAL + CONFIG Task Brief §3) so this
    // quick action and CONFIG's own control share one durable store, not two independent states.
    fun cyclePhosphor() = QuantumRuntime.cyclePhosphorHue()
    fun toggleStealth() = engine.toggleStealthMode()
    fun toggleBeacon() = engine.toggleBeacon()
    // Lock is cosmetic (Bible decision 56): lock plays the PLEASE STANDBY → DEVICE SECURED beat;
    // unlock is via the lock-overlay tap. Both call the existing engine methods — nothing rebuilt.
    fun engageLock() = engine.executeCosmeticLockSequence()
    fun releaseLock() = engine.unlockDeviceProfile()

    fun loadApps(pm: PackageManager) {
        viewModelScope.launch {
            val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            @Suppress("DEPRECATION")
            val resolved = pm.queryIntentActivities(intent, 0)
            _installedApps.value = resolved
                .map { ri ->
                    AppInfo(
                        label = ri.loadLabel(pm).toString(),
                        packageName = ri.activityInfo.packageName,
                        activityName = ri.activityInfo.name
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
            // Same beat also resyncs CONFIG's persisted phosphor/region/boot-pace into the live engine
            // (SIGNAL + CONFIG Task Brief §3) — CONFIG is a docked module and can't reach this engine
            // directly, so a change made there is picked up the moment HOME is resumed.
            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) {
                        canOverlay = Settings.canDrawOverlays(context)
                        QuantumRuntime.resyncPersistedSettings()
                    }
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
// TerminalConstraints / QuantumOSLayoutShell / crtShader() / crtOverlay() now live in
// com.quantumos.appshell (App Shell Integration, Phase 3) — imported above.
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
                    NavigationChannel.HOME -> HomeChannelBody(vm = vm, state = state, panelOpen = panelOpen, color = color, dimColor = dimColor, font = font, canOverlay = canOverlay, onRequestOverlay = onRequestOverlay)
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

// NameplateHeader / ChannelStrip now live in com.quantumos.appshell — imported above.

// ---------- HOME channel body ----------
// Launcher Restructure Phase 1 (Build Brief v1.0): HOME's centrepiece is now the static
// eight-instrument console, not a debug readout. The M3/M4 cross-cutting controls (Vitality
// atom-mark pull, Beacon field-flag, QUARK trigger affordance) are untouched — this brief only
// restructures HOME's own content, not those separately-milestoned features.
//
// Judgment calls flagged for the Director (BUILD_LOG has the full note):
//  - The old M0-era hue-chip row + system readout text are removed here: both are fully
//    superseded by real UI (Vitality panel's Phosphor action; STATUS/LOG channels) and were
//    debug scaffolding, not a locked surface.
//  - CONFIG hops to the STATUS channel (its function already lives there) rather than showing
//    STANDBY — treated as "the app already exists," just not as a separate module yet.
@Composable
private fun HomeChannelBody(
    vm: QuantumViewModel,
    state: QuantumLauncherState,
    panelOpen: Boolean,
    color: Color,
    dimColor: Color,
    font: FontFamily,
    canOverlay: Boolean,
    onRequestOverlay: () -> Unit
) {
    val conn by vm.connectivity.collectAsState()
    val context = LocalContext.current
    var offlineInstrument by remember { mutableStateOf<InstrumentSpec?>(null) }

    // App Shell Integration (Phase 3): CAM/MAPS hand-off is a stepped PLEASE STANDBY beat (never an
    // instant cut — "loading = a PLEASE STANDBY card," motion is mechanical not silky) followed by
    // starting the docked Activity in this same task (no NEW_TASK/CLEAR_TOP), so back naturally
    // returns to this still-live HOME screen.
    var dockingModule by remember { mutableStateOf<DockedModule?>(null) }
    LaunchedEffect(dockingModule) {
        val module = dockingModule ?: return@LaunchedEffect
        delay(360)   // three ~120ms beats — a felt hand-off, not an instant cut
        val target = when (module) {
            DockedModule.OPTICS -> OpticsActivity::class.java
            DockedModule.NAV -> NavActivity::class.java
            DockedModule.COMMS -> CommsActivity::class.java
            DockedModule.FILES -> FilesActivity::class.java
            DockedModule.AUDIO -> AudioActivity::class.java
            DockedModule.RADIO -> RadioActivity::class.java
            DockedModule.SIGNAL -> SignalActivity::class.java
            DockedModule.CONFIG -> ConfigActivity::class.java
        }
        context.startActivity(Intent(context, target))
        dockingModule = null
    }

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
            Spacer(Modifier.height(6.dp))
            // Deployment Region status line — terse, utilitarian status text (NOT QUARK speaking).
            Text(
                text = "DEPLOYMENT: ${DeploymentRegions.label(state.deploymentRegion)}",
                color = dimColor,
                fontFamily = font,
                fontSize = 11.sp
            )
            Spacer(Modifier.height(10.dp))

            // ---------- the static instrument console (Phase 1 centrepiece) ----------
            HomeInstrumentConsole(
                color = color,
                dimColor = dimColor,
                font = font,
                onDock = { module ->
                    vm.signalInstrumentDock()
                    dockingModule = module
                },
                onNavigate = { vm.navigate(it) },
                onOffline = { spec ->
                    vm.signalInstrumentOffline()
                    offlineInstrument = spec
                },
                modifier = Modifier.weight(1f)
            )

            Spacer(Modifier.height(10.dp))
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

        // Tapping a not-yet-built instrument surfaces this — a correct-looking "not online" state,
        // never faked functionality (Build Brief Phase 1).
        offlineInstrument?.let { spec ->
            InstrumentOfflineOverlay(
                instrument = spec,
                dimColor = dimColor,
                font = font,
                onDismiss = { offlineInstrument = null }
            )
        }

        // The docking hand-off beat (App Shell Integration, Phase 3) — the universal loading/
        // transition card, never a generic spinner, while CAM/MAPS stand up their docked Activity.
        if (dockingModule != null) {
            Box(Modifier.fillMaxSize().background(Phosphor.Crt), contentAlignment = Alignment.Center) {
                PleaseStandbyCard(subline = "OPENING MODULE…", color = color, dimColor = dimColor, font = font)
            }
        }
    }
}

// ---------- Launcher Restructure Phase 1 — the static eight-instrument console ----------

// What tapping an instrument actually does — never a second state path (SESSION-PLAYBOOK reuse
// rule). App Shell Integration (Phase 3): CAM/MAPS now dock directly into the bundled :optics/:nav
// modules (Dock) instead of handing off to a separately-installed app by label match (Launch).
private sealed class InstrumentAction {
    data class Dock(val module: DockedModule) : InstrumentAction()
    data class Navigate(val channel: NavigationChannel) : InstrumentAction()
    object Offline : InstrumentAction()
}

private fun resolveInstrumentAction(spec: InstrumentSpec): InstrumentAction {
    spec.opensChannel?.let { return InstrumentAction.Navigate(it) }
    spec.dockedModule?.let { return InstrumentAction.Dock(it) }
    return InstrumentAction.Offline
}

// Fixed 2-column, 4-row grid — all eight instruments visible at once, no scroll, no paging.
@Composable
private fun HomeInstrumentConsole(
    color: Color,
    dimColor: Color,
    font: FontFamily,
    onDock: (DockedModule) -> Unit,
    onNavigate: (NavigationChannel) -> Unit,
    onOffline: (InstrumentSpec) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        InstrumentConsole.INSTRUMENTS.chunked(2).forEach { rowSpecs ->
            Row(
                Modifier.fillMaxWidth().weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowSpecs.forEach { spec ->
                    val action = remember(spec) { resolveInstrumentAction(spec) }
                    InstrumentTile(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        spec = spec,
                        available = action !is InstrumentAction.Offline,
                        color = color,
                        dimColor = dimColor,
                        font = font,
                        onClick = {
                            when (val a = action) {
                                is InstrumentAction.Navigate -> onNavigate(a.channel)
                                is InstrumentAction.Dock -> onDock(a.module)
                                InstrumentAction.Offline -> onOffline(spec)
                            }
                        }
                    )
                }
            }
        }
    }
}

// One instrument tile: house-style bordered box, line-icon + label + function sub-label. Bright
// phosphor when its target is real and reachable; dim + STANDBY caption when not yet built —
// mirrors the ActionCell active/inactive convention already established in the Vitality panel.
@Composable
private fun InstrumentTile(
    modifier: Modifier,
    spec: InstrumentSpec,
    available: Boolean,
    color: Color,
    dimColor: Color,
    font: FontFamily,
    onClick: () -> Unit
) {
    val edge = if (available) color else dimColor
    Column(
        modifier
            .clickable { onClick() }
            .border(BorderStroke(1.dp, edge))
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.weight(1f))
        InstrumentIcon(id = spec.id, tint = edge, size = 26.dp)
        Spacer(Modifier.height(6.dp))
        Text(spec.label, color = edge, fontFamily = font, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Text(spec.function, color = dimColor, fontFamily = font, fontSize = 9.sp)
        if (!available) {
            Spacer(Modifier.height(3.dp))
            Text("STANDBY", color = dimColor, fontFamily = font, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.weight(1f))
    }
}

// Original SVG-in-spirit line-icons drawn on the GPU-cheap Canvas path (house stroke language:
// consistent weight, themed with the active phosphor, no platform emoji). Pixel-level icon masters
// are a later identity/polish pass (House Style skill) — these are the working set for Phase 1.
@Composable
private fun InstrumentIcon(id: InstrumentId, tint: Color, size: Dp) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = Stroke(width = w * 0.09f)
        when (id) {
            InstrumentId.COMMS -> {
                // transmission thread: a speech-frame with a tail.
                val path = Path().apply {
                    moveTo(w * 0.15f, h * 0.2f)
                    lineTo(w * 0.85f, h * 0.2f)
                    lineTo(w * 0.85f, h * 0.65f)
                    lineTo(w * 0.45f, h * 0.65f)
                    lineTo(w * 0.3f, h * 0.85f)
                    lineTo(w * 0.3f, h * 0.65f)
                    lineTo(w * 0.15f, h * 0.65f)
                    close()
                }
                drawPath(path, tint, style = stroke)
            }
            InstrumentId.FILES -> {
                // field file manager: a folder outline.
                val path = Path().apply {
                    moveTo(w * 0.15f, h * 0.28f)
                    lineTo(w * 0.42f, h * 0.28f)
                    lineTo(w * 0.5f, h * 0.4f)
                    lineTo(w * 0.85f, h * 0.4f)
                    lineTo(w * 0.85f, h * 0.78f)
                    lineTo(w * 0.15f, h * 0.78f)
                    close()
                }
                drawPath(path, tint, style = stroke)
            }
            InstrumentId.AUDIO -> {
                // field recorder: a small waveform.
                val xs = listOf(0.22f, 0.39f, 0.56f, 0.73f, 0.9f)
                val topFrac = listOf(0.4f, 0.2f, 0.1f, 0.3f, 0.45f)
                xs.forEachIndexed { i, x ->
                    drawLine(
                        tint,
                        Offset(w * x, h * topFrac[i]),
                        Offset(w * x, h * 0.9f),
                        strokeWidth = stroke.width
                    )
                }
            }
            InstrumentId.CAM -> {
                // Optics: phosphor viewfinder + reticle.
                drawRect(
                    tint,
                    topLeft = Offset(w * 0.12f, h * 0.22f),
                    size = Size(w * 0.76f, h * 0.62f),
                    style = stroke
                )
                drawLine(tint, Offset(w * 0.38f, h * 0.1f), Offset(w * 0.62f, h * 0.1f), strokeWidth = stroke.width)
                val r = w * 0.16f
                val c = Offset(w * 0.5f, h * 0.53f)
                drawCircle(tint, radius = r, center = c, style = stroke)
                drawLine(tint, c - Offset(r * 1.6f, 0f), c + Offset(r * 1.6f, 0f), strokeWidth = stroke.width * 0.7f)
                drawLine(tint, c - Offset(0f, r * 1.6f), c + Offset(0f, r * 1.6f), strokeWidth = stroke.width * 0.7f)
            }
            InstrumentId.MAPS -> {
                // Nav: a field waypoint pin.
                val r = w * 0.24f
                val c = Offset(w * 0.5f, h * 0.35f)
                drawCircle(tint, radius = r, center = c, style = stroke)
                drawCircle(tint, radius = r * 0.35f, center = c)
                val path = Path().apply {
                    moveTo(w * 0.5f - r * 0.55f, h * 0.5f)
                    lineTo(w * 0.5f, h * 0.9f)
                    lineTo(w * 0.5f + r * 0.55f, h * 0.5f)
                }
                drawPath(path, tint, style = stroke)
            }
            InstrumentId.RADIO -> {
                // broadcast receiver: nested incoming-signal arcs over an antenna dot.
                val origin = Offset(w * 0.22f, h * 0.78f)
                drawCircle(tint, radius = w * 0.05f, center = origin)
                for (i in 1..3) {
                    val r = w * 0.18f * i
                    drawArc(
                        tint,
                        startAngle = -60f,
                        sweepAngle = 60f,
                        useCenter = false,
                        topLeft = Offset(origin.x - r, origin.y - r),
                        size = Size(r * 2f, r * 2f),
                        style = stroke
                    )
                }
            }
            InstrumentId.SIGNAL -> {
                // link diagnostics: ascending measured-out bars.
                val xs = listOf(0.28f, 0.5f, 0.72f)
                val heights = listOf(0.3f, 0.55f, 0.8f)
                xs.forEachIndexed { i, x ->
                    drawLine(
                        tint,
                        Offset(w * x, h * (0.85f - heights[i])),
                        Offset(w * x, h * 0.85f),
                        strokeWidth = stroke.width * 1.4f
                    )
                }
            }
            InstrumentId.CONFIG -> {
                // field-unit console: a dial with four radial ticks.
                val r = w * 0.28f
                val c = Offset(w * 0.5f, h * 0.5f)
                drawCircle(tint, radius = r, center = c, style = stroke)
                listOf(0f, 90f, 180f, 270f).forEach { deg ->
                    val rad = Math.toRadians(deg.toDouble())
                    val inner = c + Offset((r * 1.15f * kotlin.math.cos(rad)).toFloat(), (r * 1.15f * kotlin.math.sin(rad)).toFloat())
                    val outer = c + Offset((r * 1.5f * kotlin.math.cos(rad)).toFloat(), (r * 1.5f * kotlin.math.sin(rad)).toFloat())
                    drawLine(tint, inner, outer, strokeWidth = stroke.width)
                }
            }
        }
    }
}

// Full-screen "correct-looking not-yet-online state" — never a fake feature. Reuses the same
// full-screen overlay shape as the cosmetic Lock beat; tap anywhere (or the explicit line) returns.
@Composable
private fun InstrumentOfflineOverlay(
    instrument: InstrumentSpec,
    dimColor: Color,
    font: FontFamily,
    onDismiss: () -> Unit
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Phosphor.Crt)
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "${instrument.label} // INSTRUMENT OFFLINE",
                color = dimColor, fontFamily = font, fontSize = 16.sp, fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(10.dp))
            Text("MODULE NOT YET DEPLOYED", color = dimColor, fontFamily = font, fontSize = 12.sp)
            Spacer(Modifier.height(24.dp))
            Text("◄ TAP TO RETURN, OPERATOR", color = dimColor, fontFamily = font, fontSize = 11.sp)
        }
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
                    glyph = Glyph.Stealth,
                    title = "STEALTH",
                    status = if (environment.isStealthMode) "ENGAGED" else "STANDBY",
                    active = environment.isStealthMode,
                    color = color, dimColor = dimColor, font = font, onClick = onStealth
                )
                ActionCell(
                    modifier = Modifier.weight(1f),
                    glyph = Glyph.Phosphor,
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
                    glyph = Glyph.Beacon,
                    title = "BEACON",
                    status = if (environment.isBeaconActive) "ACTIVE" else "DARK",
                    active = environment.isBeaconActive,
                    color = color, dimColor = dimColor, font = font, onClick = onBeacon
                )
                ActionCell(
                    modifier = Modifier.weight(1f),
                    glyph = Glyph.Lock,
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

// SegmentedGauge now lives in com.quantumos.appshell (SIGNAL + CONFIG Task Brief) — imported above,
// so SIGNAL's link-diagnostic gauges share the exact same phosphor-segment renderer, not a copy.

// One Zone-2 quick-action tile. Active = bright phosphor frame; inactive = dim. No Material chrome.
@Composable
private fun ActionCell(
    modifier: Modifier,
    glyph: Glyph,
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
        Row(verticalAlignment = Alignment.CenterVertically) {
            QuantumIcon(glyph, tint = edge, size = 16.dp)
            Spacer(Modifier.width(6.dp))
            Text(title, color = edge, fontFamily = font, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
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

// PleaseStandbyCard now lives in com.quantumos.appshell — imported above.

// ---------- APPS channel — Launcher Restructure Phase 2 (v5): paged grid + canon nav buttons ----------
//
// The flat LazyVerticalGrid was replaced by a PAGED grid in v3/v4; v5 (Director simplification,
// after Fold 6 testing of the gear dial) replaces the dial itself with the two canon navigation
// buttons from the "QuantumOS Launcher Navigation Buttons" design sheet: PREV PAGE (◀ BACKWARD) and
// NEXT PAGE (FORWARD ▶) — octagon-cut double-bordered plates with a filled phosphor triangle and a
// bracketed two-line label beneath. Motion stays stepped and reactive-only: a tap flips exactly one
// page (slide-projector) with a mechanical click; there is no drag, coast, or idle animation. The
// buttons are DRAWN (Canvas, phosphor-tokenized), not the sheet's bitmap, so the live hue switch
// still recolors them — the sheet bakes in green, which would violate the one-token-source rule.
//
// STUTTER-FIX INHERITANCE (unchanged since v3 — see BUILD_LOG): the paged grid is a plain non-lazy
// Row/Column of exactly one page of cells (no LazyGrid view-recycling to jank on a flip) and every
// icon is decoded ONCE into the process-scoped `AppIconCache` below, keyed by package name.

// Fixed page capacity so the page count is stable regardless of screen width. Columns × rows.
private const val REEL_COLUMNS = 4
private const val REEL_ROWS = 5
private const val REEL_PAGE_CAPACITY = REEL_COLUMNS * REEL_ROWS

/*
 * AppIconCache — process-scoped, decode-once cache of launcher icons as Compose ImageBitmaps, keyed
 * by package name. THE durable stutter fix the reel's paging rides on: an icon is rasterised from the
 * PackageManager drawable exactly once per session, then every page flip just hands back the cached
 * bitmap — no re-decode on the UI thread mid-scrub. Lives outside composition (a plain object) so it
 * survives recomposition, config change, and channel switches alike.
 */
object AppIconCache {
    private val cache = HashMap<String, ImageBitmap?>()

    fun get(context: Context, packageName: String): ImageBitmap? {
        cache[packageName]?.let { return it }
        if (cache.containsKey(packageName)) return null   // cached miss — don't retry the decode
        val bmp = runCatching {
            context.packageManager.getApplicationIcon(packageName).toBitmapCompat()?.asImageBitmap()
        }.getOrNull()
        cache[packageName] = bmp
        return bmp
    }
}

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

    val pageCount = (apps.size + REEL_PAGE_CAPACITY - 1) / REEL_PAGE_CAPACITY

    // v5: the committed page is the only paging state (survives fold/rotate). No drag, no queue,
    // no rotation — a nav-button tap flips exactly one page, stepped, with a mechanical click.
    var settledPage by rememberSaveable { mutableStateOf(ReelPager.clampPage(0, pageCount)) }

    // Safety re-clamp if the installed-apps count (and so pageCount) shrinks mid-session — rare,
    // but keeps settledPage from ever pointing past the end.
    LaunchedEffect(pageCount) {
        val clamped = ReelPager.clampPage(settledPage, pageCount)
        if (clamped != settledPage) settledPage = clamped
    }

    fun step(direction: Int) {
        val target = ReelPager.clampPage(settledPage + direction, pageCount)
        if (target != settledPage) {
            settledPage = target
            QuantumRuntime.playCue(SoundCue.REEL_DETENT)
        }
    }

    val pageApps = remember(apps, settledPage) {
        apps.drop(settledPage * REEL_PAGE_CAPACITY).take(REEL_PAGE_CAPACITY)
    }

    Column(Modifier.fillMaxSize()) {
        // ---- the paged grid (above) ----
        ReelPageGrid(
            pageApps = pageApps,
            color = color,
            dimColor = dimColor,
            font = font,
            onLaunch = { app ->
                context.packageManager.getLaunchIntentForPackage(app.packageName)
                    ?.let { context.startActivity(it) }
            },
            modifier = Modifier.weight(1f).fillMaxWidth()
        )

        // ---- page ordinal readout (terse status microcopy) ----
        Text(
            text = "PAGE ${settledPage + 1} / $pageCount",
            color = dimColor,
            fontFamily = font,
            fontSize = 11.sp,
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            textAlign = TextAlign.Center
        )

        // ---- the canon PREV/NEXT navigation buttons (design sheet), at the bottom edge ----
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterHorizontally)
        ) {
            PageNavButton(
                forward = false,
                enabled = settledPage > 0,
                color = color, dimColor = dimColor, font = font
            ) { step(-1) }
            PageNavButton(
                forward = true,
                enabled = settledPage < pageCount - 1,
                color = color, dimColor = dimColor, font = font
            ) { step(+1) }
        }
    }
}

/*
 * PageNavButton — one canon navigation button from the "QuantumOS Launcher Navigation Buttons"
 * design sheet: an octagon-cut plate with a bright outer rim + dim inner line, a large filled
 * phosphor triangle, and a bracketed two-line label beneath ("PREV PAGE / ◀ BACKWARD" or
 * "NEXT PAGE / FORWARD ▶"). Drawn, not the sheet's bitmap, so the live phosphor hue switch
 * recolors it (the sheet bakes in green). Static at rest; disabled at the clamp = dim + inert
 * (a hard stop needs no sound — nothing was denied, there's simply no further page).
 */
@Composable
private fun PageNavButton(
    forward: Boolean,
    enabled: Boolean,
    color: Color,
    dimColor: Color,
    font: FontFamily,
    onClick: () -> Unit
) {
    val edge = if (enabled) color else dimColor.copy(alpha = 0.45f)
    val fill = if (enabled) color else dimColor.copy(alpha = 0.45f)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Canvas(
            Modifier
                .size(32.dp)   // ~30% of the original 108dp — Director: was reading as ~4 app-icons wide on the Fold 6
                .clickable(enabled = enabled) { onClick() }
        ) {
            val w = size.width
            val h = size.height
            val cut = w * 0.16f
            fun octagon(inset: Float): Path {
                val c = cut - inset * 0.4f
                return Path().apply {
                    moveTo(inset + c, inset)
                    lineTo(w - inset - c, inset)
                    lineTo(w - inset, inset + c)
                    lineTo(w - inset, h - inset - c)
                    lineTo(w - inset - c, h - inset)
                    lineTo(inset + c, h - inset)
                    lineTo(inset, h - inset - c)
                    lineTo(inset, inset + c)
                    close()
                }
            }
            // Plate ground, then the double border: bright outer rim + dim inner line.
            drawPath(octagon(0f), Phosphor.Crt)
            drawPath(octagon(0f), edge, style = Stroke(width = w * 0.035f))
            drawPath(octagon(w * 0.075f), dimColor.copy(alpha = if (enabled) 1f else 0.45f), style = Stroke(width = w * 0.012f))
            // The filled direction triangle.
            val tri = Path().apply {
                if (forward) {
                    moveTo(w * 0.38f, h * 0.30f); lineTo(w * 0.68f, h * 0.50f); lineTo(w * 0.38f, h * 0.70f)
                } else {
                    moveTo(w * 0.62f, h * 0.30f); lineTo(w * 0.32f, h * 0.50f); lineTo(w * 0.62f, h * 0.70f)
                }
                close()
            }
            drawPath(tri, fill)
        }
        Spacer(Modifier.height(4.dp))
        // Bracketed two-line label, per the sheet — scaled down to match the smaller plate.
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("[", color = dimColor, fontFamily = font, fontSize = 12.sp)
            Spacer(Modifier.width(3.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    if (forward) "NEXT PAGE" else "PREV PAGE",
                    color = if (enabled) color else dimColor, fontFamily = font, fontSize = 8.sp, fontWeight = FontWeight.Bold
                )
                Text(
                    if (forward) "FORWARD ▶" else "◀ BACKWARD",
                    color = dimColor, fontFamily = font, fontSize = 7.sp
                )
            }
            Spacer(Modifier.width(3.dp))
            Text("]", color = dimColor, fontFamily = font, fontSize = 12.sp)
        }
    }
}

// A single page of apps, rendered as a plain (non-lazy) grid — no view recycling to stutter on a
// flip; icons come straight from AppIconCache. Fixed REEL_COLUMNS across; short pages just leave
// the trailing cells empty so the page count stays stable.
@Composable
private fun ReelPageGrid(
    pageApps: List<AppInfo>,
    color: Color,
    dimColor: Color,
    font: FontFamily,
    onLaunch: (AppInfo) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier.padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        for (row in 0 until REEL_ROWS) {
            Row(
                Modifier.fillMaxWidth().weight(1f),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                for (col in 0 until REEL_COLUMNS) {
                    val index = row * REEL_COLUMNS + col
                    val app = pageApps.getOrNull(index)
                    Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.TopCenter) {
                        if (app != null) {
                            AppCell(app = app, color = color, dimColor = dimColor, font = font) { onLaunch(app) }
                        }
                    }
                }
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
    val context = LocalContext.current
    val iconBitmap = remember(app.packageName) { AppIconCache.get(context, app.packageName) }

    Column(
        modifier = Modifier
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
            if (iconBitmap != null) {
                Image(
                    bitmap = iconBitmap,
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

        // CONFIG's Boot Pace / Deployment Region rows used to live inline here (M6). SIGNAL + CONFIG
        // Task Brief §3 makes CONFIG a real docked module and the single settings home — this inline
        // hop is removed so there is exactly one settings surface, not two (brief acceptance §5).
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
