package com.quantumos.shell.ui

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.quantumos.core.BootPace
import com.quantumos.core.NavigationChannel
import com.quantumos.core.PhosphorHue
import com.quantumos.core.QuantumLauncherState
import com.quantumos.core.QuantumStateEngine
import com.quantumos.core.QuarkParser
import com.quantumos.core.SystemReadiness
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale

/*
 * QuantumOS — UI LAYER (Compose). Depends on com.quantumos.core for all logic.
 */

// ---------- phosphor token source: one hue switch recolors everything ----------
object Phosphor {
    val GreenBright = Color(0xFF00FF00); val GreenDim = Color(0xFF00AA00)
    val AmberBright = Color(0xFFFFB000); val AmberDim = Color(0xFFA86F00)
    val CyanBright  = Color(0xFF00E5FF); val CyanDim  = Color(0xFF0090A8)
    val Warn = Color(0xFFFF3B1F)
    val Crt  = Color(0xFF020402)

    fun bright(h: PhosphorHue) = when (h) {
        PhosphorHue.GREEN -> GreenBright; PhosphorHue.AMBER -> AmberBright; PhosphorHue.CYAN -> CyanBright
    }
    fun dim(h: PhosphorHue) = when (h) {
        PhosphorHue.GREEN -> GreenDim; PhosphorHue.AMBER -> AmberDim; PhosphorHue.CYAN -> CyanDim
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
class QuantumViewModel : ViewModel() {
    val engine = QuantumStateEngine(viewModelScope, BootPace.SNAPPY) // SNAPPY = snappy dev boot; ship = DELIBERATE
    val parser = QuarkParser(engine) // Scripted-Line seam (used from M5); held here so the brain has one owner.
    private var telemetryStarted = false

    private val _installedApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val installedApps: StateFlow<List<AppInfo>> = _installedApps.asStateFlow()

    // UI-only connectivity label for the STATUS readout (transport type isn't part of core state).
    private val _connectivity = MutableStateFlow(ConnectivityInfo())
    val connectivity: StateFlow<ConnectivityInfo> = _connectivity.asStateFlow()

    fun boot() = engine.executeColdBootSequence()

    fun setHue(hue: PhosphorHue) = engine.updateEnvironmentProfile { it.copy(activeHue = hue) }

    fun navigate(target: NavigationChannel) = engine.transitionNavigation(target)

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

    /*
     * M2 Step 1 — real telemetry poll. One loop on the ViewModel scope (survives fold/rotate),
     * NOT a per-recomposition read. Feeds the existing engine.incomingTelemetryUpdate(...) seam so
     * readiness stays a single source of truth; the UI-only transport label rides alongside.
     * Uses applicationContext so the long-lived coroutine never holds an Activity.
     */
    fun startTelemetry(context: Context) {
        if (telemetryStarted) return
        telemetryStarted = true
        val appContext = context.applicationContext
        viewModelScope.launch {
            while (isActive) {
                val battery = readBattery(appContext)
                val conn = readConnectivity(appContext)
                val uptimeMs = SystemClock.elapsedRealtime()
                // Coarse signal proxy for the engine's readiness composite: connected -> usable, else 0.
                val signal = if (conn.connected) 3 else 0
                engine.incomingTelemetryUpdate(
                    battery.percent, battery.charging, uptimeMs, signal, battery.tempCelsius
                )
                _connectivity.value = conn
                delay(3000L) // sane interval; status is functional reactive state, not an animation loop
            }
        }
    }

    private data class BatteryReading(val percent: Int, val charging: Boolean, val tempCelsius: Float)

    // ACTION_BATTERY_CHANGED sticky broadcast — no permission required.
    private fun readBattery(context: Context): BatteryReading {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val percent = if (level >= 0 && scale > 0) (level * 100) / scale else 0
        val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL
        // EXTRA_TEMPERATURE is tenths of a degree C; benign default if the device omits it.
        val rawTemp = intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) ?: -1
        val tempCelsius = if (rawTemp > 0) rawTemp / 10f else 25.0f
        return BatteryReading(percent, charging, tempCelsius)
    }

    // Basic connected/not + transport via ConnectivityManager. No precise-bars permission (M2 hard stop).
    private fun readConnectivity(context: Context): ConnectivityInfo {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return ConnectivityInfo(false, "OFFLINE")
        val caps = cm.activeNetwork?.let { cm.getNetworkCapabilities(it) }
            ?: return ConnectivityInfo(false, "OFFLINE")
        val online = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        val transport = when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WI-FI"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "CELLULAR"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "ETHERNET"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> "VPN"
            else -> "UNKNOWN"
        }
        return if (online) ConnectivityInfo(true, transport) else ConnectivityInfo(false, "OFFLINE")
    }
}

// ---------- Activity ----------
class LauncherActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val vm: QuantumViewModel = viewModel()
            val context = LocalContext.current
            LaunchedEffect(Unit) {
                vm.boot()
                vm.startTelemetry(context)        // M2: real battery/uptime/connectivity poll
                vm.loadApps(context.packageManager)
            }
            QuantumOSLayoutShell(forceFixedContainer = false) { constraints ->
                QuantumAppShell(vm = vm, constraints = constraints)
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
 *  - forceFixedContainer = FALSE (default): surface fills the real screen; CRT falloff frames it.
 *  - forceFixedContainer = TRUE: 9:19.5 letterbox. On Fold's near-square inner display that's a
 *    narrow strip in black — only if you want a deliberate "screen-in-chassis" look.
 */
@Composable
fun QuantumOSLayoutShell(
    forceFixedContainer: Boolean = false,
    targetAspectRatio: Float = 9f / 19.5f,
    content: @Composable (TerminalConstraints) -> Unit
) {
    // BackHandler is owned by QuantumAppShell so it can route APPS → HOME before consuming.

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

// Cheap non-shader CRT treatment. Real AGSL phosphor glow replaces this on hardware (M6).
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

// ---------- App Shell (house-style chrome: nameplate + channel strip + content) ----------
@Composable
fun QuantumAppShell(vm: QuantumViewModel, constraints: TerminalConstraints) {
    val state by vm.engine.masterState.collectAsState()
    val color = Phosphor.bright(state.environment.activeHue)
    val dimColor = Phosphor.dim(state.environment.activeHue)
    val font = FontFamily.Monospace

    BackHandler(enabled = true) {
        if (state.currentNavigation != NavigationChannel.HOME) {
            vm.navigate(NavigationChannel.HOME)
        }
        // on HOME: consume the back press (shell never exits)
    }

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
                NavigationChannel.HOME -> HomeChannelBody(vm = vm, state = state, color = color, font = font, constraints = constraints)
                NavigationChannel.APPS -> AppsChannelScreen(vm = vm, color = color, dimColor = dimColor, font = font)
                NavigationChannel.STATUS -> StatusChannelScreen(vm = vm, state = state, color = color, dimColor = dimColor, font = font)
                NavigationChannel.LOG -> LogChannelScreen(vm = vm, color = color, dimColor = dimColor, font = font)
            }
        }
    }
}

// Opaque nameplate header with registration marks — top App Shell chrome.
@Composable
private fun NameplateHeader(channelName: String, color: Color, dimColor: Color, font: FontFamily) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Phosphor.Crt)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "QUANTUM OS",
            color = color,
            fontFamily = font,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = "// $channelName",
            color = dimColor,
            fontFamily = font,
            fontSize = 12.sp
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = "[⊕]",
            color = dimColor,
            fontFamily = font,
            fontSize = 11.sp
        )
    }
}

// Channel navigation strip below the nameplate.
@Composable
private fun ChannelStrip(
    current: NavigationChannel,
    color: Color,
    dimColor: Color,
    font: FontFamily,
    onSelect: (NavigationChannel) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        NavigationChannel.entries.forEach { channel ->
            val active = channel == current
            Text(
                text = if (active) "[${channel.name}]" else " ${channel.name} ",
                color = if (active) color else dimColor,
                fontFamily = font,
                fontSize = 11.sp,
                modifier = Modifier
                    .clickable { onSelect(channel) }
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            )
        }
    }
}

// ---------- HOME channel body (M0 phosphor terminal readout) ----------
@Composable
private fun HomeChannelBody(
    vm: QuantumViewModel,
    state: QuantumLauncherState,
    color: Color,
    font: FontFamily,
    constraints: TerminalConstraints
) {
    val logs by vm.engine.systemLogs.collectAsState()

    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row {
            HueChip("GREEN", color) { vm.setHue(PhosphorHue.GREEN) }
            Spacer(Modifier.width(12.dp))
            HueChip("AMBER", color) { vm.setHue(PhosphorHue.AMBER) }
            Spacer(Modifier.width(12.dp))
            HueChip("CYAN", color) { vm.setHue(PhosphorHue.CYAN) }
        }
        Spacer(Modifier.height(12.dp))
        // TODO M0: replace FontFamily.Monospace with bundled Chakra Petch
        Text(
            text = buildReadout(state, logs, constraints),
            color = color,
            fontFamily = font,
            fontSize = 13.sp
        )
    }
}

@Composable
private fun HueChip(label: String, color: Color, onClick: () -> Unit) {
    Text(
        text = "[$label]",
        color = color,
        fontFamily = FontFamily.Monospace,
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
    val context = LocalContext.current
    val iconBitmap = remember(app.packageName) {
        runCatching {
            context.packageManager
                .getApplicationIcon(app.packageName)
                .toBitmapCompat()
                ?.asImageBitmap()
        }.getOrNull()
    }

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
