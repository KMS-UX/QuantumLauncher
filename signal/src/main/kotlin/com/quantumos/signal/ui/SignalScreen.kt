package com.quantumos.signal.ui

import android.Manifest
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.quantumos.appshell.Fonts
import com.quantumos.appshell.Phosphor
import com.quantumos.appshell.PleaseStandbyCard
import com.quantumos.appshell.SegmentedGauge
import com.quantumos.core.FieldDecodeResult
import com.quantumos.signal.GaugeReading
import com.quantumos.signal.SignalSensors
import com.quantumos.signal.SignalViewModel

/*
 * SignalScreen -- SIGNAL's main content (Task Brief §2). Four segmented gauges (cellular/wifi/GPS/
 * Bluetooth) in the exact Vitality-panel visual language (SegmentedGauge, shared from :app-shell, not
 * reimplemented), a rolling event-driven sparkline, an explicit RUN SCAN action, and the offline field
 * decoder ported from RADIO's removed cryptographic decoder (docs/future-signal/radio-decoder.md) --
 * interaction shape only, no AI/network backend.
 *
 * SignalSensors owns the real platform listeners and is started/stopped by this Composable's own
 * lifecycle (DisposableEffect below) -- nothing runs while this screen is closed (zero idle poll,
 * acceptance §7). Permission gating uses Accompanist, same pattern as :audio's RECORD_AUDIO flow.
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun SignalScreen(
    viewModel: SignalViewModel,
    themeColor: Color,
    themeColorDim: Color,
    warnColor: Color,
    onCycleTheme: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val permissionsState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.BLUETOOTH_CONNECT
        )
    )
    val allGranted = permissionsState.allPermissionsGranted
    val permissionKey = permissionsState.permissions.joinToString { it.status.isGranted.toString() }

    val sensors = remember { SignalSensors(context, viewModel) }
    DisposableEffect(permissionKey) {
        sensors.start()
        onDispose { sensors.stop() }
    }

    Column(
        modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "LINK DIAGNOSTICS",
                color = themeColor,
                fontFamily = Fonts.ChakraPetch,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.weight(1f))
            Text(
                "PHOSPHOR ⟳",
                color = themeColorDim,
                fontFamily = Fonts.ChakraPetch,
                fontSize = 11.sp,
                modifier = Modifier.clickable { onCycleTheme() }
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            "YOUR OWN LINK, MEASURED OUT",
            color = themeColorDim,
            fontFamily = Fonts.ChakraPetch,
            fontSize = 10.sp
        )

        Spacer(Modifier.height(14.dp))

        GaugeOrPermissionGate("CELLULAR", state.cellular, themeColor, themeColorDim, warnColor) {
            permissionsState.launchMultiplePermissionRequest()
        }
        GaugeOrPermissionGate("WI-FI", state.wifi, themeColor, themeColorDim, warnColor, needsGrant = false) {}
        GaugeOrPermissionGate("GPS", state.gps, themeColor, themeColorDim, warnColor) {
            permissionsState.launchMultiplePermissionRequest()
        }
        GaugeOrPermissionGate("BLUETOOTH", state.bluetooth, themeColor, themeColorDim, warnColor) {
            permissionsState.launchMultiplePermissionRequest()
        }

        if (!allGranted) {
            Spacer(Modifier.height(6.dp))
            Text(
                "SOME GAUGES NEED FIELD-UNIT ACCESS — TAP ANY DENIED GAUGE ABOVE, OR:",
                color = themeColorDim,
                fontFamily = Fonts.ChakraPetch,
                fontSize = 9.sp
            )
            Text(
                "[ GRANT DIAGNOSTIC ACCESS ]",
                color = themeColor,
                fontFamily = Fonts.ChakraPetch,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clickable { permissionsState.launchMultiplePermissionRequest() }
                    .padding(vertical = 6.dp)
            )
        }

        Spacer(Modifier.height(10.dp))
        Text("----------------------------------------", color = themeColorDim, fontFamily = Fonts.ChakraPetch, fontSize = 12.sp)
        Spacer(Modifier.height(10.dp))

        Text("SIGNAL TRACE", color = themeColorDim, fontFamily = Fonts.ChakraPetch, fontSize = 11.sp)
        Spacer(Modifier.height(4.dp))
        Sparkline(
            points = state.sparkline,
            color = themeColor,
            dimColor = themeColorDim,
            modifier = Modifier.fillMaxWidth().height(64.dp)
        )

        Spacer(Modifier.height(12.dp))

        if (state.isScanning) {
            Box(Modifier.fillMaxWidth().height(72.dp), contentAlignment = Alignment.Center) {
                PleaseStandbyCard(
                    subline = "RUNNING DIAGNOSTIC SWEEP…",
                    color = themeColor,
                    dimColor = themeColorDim,
                    font = Fonts.ChakraPetch
                )
            }
        } else {
            Text(
                "[ RUN SCAN ]",
                color = themeColor,
                fontFamily = Fonts.ChakraPetch,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clickable { viewModel.runScan() }
                    .border(width = 1.dp, color = themeColor, shape = RoundedCornerShape(4.dp))
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            )
        }

        Spacer(Modifier.height(18.dp))
        Text("----------------------------------------", color = themeColorDim, fontFamily = Fonts.ChakraPetch, fontSize = 12.sp)
        Spacer(Modifier.height(10.dp))

        FieldDecoderPane(
            input = state.decoderInput,
            onInputChange = viewModel::updateDecoderInput,
            isDecoding = state.isDecoding,
            result = state.decodeResult,
            onRunDecode = viewModel::runDecode,
            bright = themeColor,
            dim = themeColorDim,
            warn = warnColor
        )

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun GaugeOrPermissionGate(
    label: String,
    reading: GaugeReading,
    bright: Color,
    dim: Color,
    warn: Color,
    needsGrant: Boolean = true,
    onRequestGrant: () -> Unit
) {
    if (reading.permissionGranted) {
        SegmentedGauge(
            label = label,
            filled = reading.bars,
            total = 4,
            value = reading.label,
            color = bright,
            dimColor = dim,
            font = Fonts.ChakraPetch
        )
    } else {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .clickable(enabled = needsGrant) { onRequestGrant() },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label.padEnd(10), color = dim, fontFamily = Fonts.ChakraPetch, fontSize = 12.sp)
            Spacer(Modifier.weight(1f))
            Text("ACCESS REQUIRED — TAP TO GRANT", color = warn, fontFamily = Fonts.ChakraPetch, fontSize = 10.sp)
        }
    }
}

// A rolling phosphor trace of recent overall link strength (0..100). Values only ever arrive from
// real gauge/scan events (SignalViewModel.recordSparklinePoint) -- this Canvas itself never ticks or
// animates on its own; it simply redraws when the point list changes (zero idle redraw).
@Composable
private fun Sparkline(points: List<Int>, color: Color, dimColor: Color, modifier: Modifier = Modifier) {
    Box(
        modifier
            .background(Phosphor.Crt)
            .border(width = 1.dp, color = dimColor.copy(alpha = 0.4f), shape = RoundedCornerShape(4.dp))
            .padding(4.dp)
    ) {
        if (points.size < 2) {
            Text(
                "AWAITING TRACE DATA…",
                color = dimColor,
                fontFamily = Fonts.ChakraPetch,
                fontSize = 10.sp,
                modifier = Modifier.align(Alignment.Center)
            )
        } else {
            Canvas(Modifier.fillMaxSize()) {
                val stepX = size.width / (points.size - 1).coerceAtLeast(1)
                var previous: Offset? = null
                points.forEachIndexed { index, value ->
                    val x = index * stepX
                    val y = size.height - (value.coerceIn(0, 100) / 100f) * size.height
                    val point = Offset(x, y)
                    previous?.let { start ->
                        drawLine(color = color, start = start, end = point, strokeWidth = 2f)
                    }
                    previous = point
                }
            }
        }
    }
}

// The offline field decoder -- ported interaction shape from RADIO's removed cryptographic decoder
// (docs/future-signal/radio-decoder.md): custom input -> RUN DECODE -> stepped beat -> result. Real
// local decoding only (com.quantumos.core.FieldDecoder) -- no Gemini call, no network.
@Composable
private fun FieldDecoderPane(
    input: String,
    onInputChange: (String) -> Unit,
    isDecoding: Boolean,
    result: FieldDecodeResult?,
    onRunDecode: () -> Unit,
    bright: Color,
    dim: Color,
    warn: Color
) {
    Column(Modifier.fillMaxWidth()) {
        Text("FIELD DECODER", color = bright, fontFamily = Fonts.ChakraPetch, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text(
            "PASTE AN INTERCEPTED PAYLOAD — CIPHER:/HEX:/MORSE:/ROT13: OR UNTAGGED",
            color = dim,
            fontFamily = Fonts.ChakraPetch,
            fontSize = 9.sp
        )
        Spacer(Modifier.height(8.dp))
        BasicTextField(
            value = input,
            onValueChange = onInputChange,
            textStyle = TextStyle(color = bright, fontFamily = Fonts.ChakraPetch, fontSize = 13.sp),
            cursorBrush = SolidColor(bright),
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .background(bright.copy(alpha = 0.05f))
                .border(width = 1.dp, color = dim.copy(alpha = 0.4f), shape = RoundedCornerShape(4.dp))
                .padding(8.dp)
        )
        Spacer(Modifier.height(10.dp))
        Text(
            "[ RUN DECODE ]",
            color = bright,
            fontFamily = Fonts.ChakraPetch,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .clickable(enabled = !isDecoding) { onRunDecode() }
                .border(width = 1.dp, color = bright, shape = RoundedCornerShape(4.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp)
        )
        Spacer(Modifier.height(12.dp))

        if (isDecoding) {
            Box(Modifier.fillMaxWidth().height(72.dp), contentAlignment = Alignment.Center) {
                PleaseStandbyCard(
                    subline = "RESOLVING PAYLOAD…",
                    color = bright,
                    dimColor = dim,
                    font = Fonts.ChakraPetch
                )
            }
        } else if (result != null) {
            val resultColor = if (result.success) bright else warn
            Text("FORMAT: ${result.format.name}", color = dim, fontFamily = Fonts.ChakraPetch, fontSize = 10.sp)
            Spacer(Modifier.height(4.dp))
            Text(result.output, color = resultColor, fontFamily = Fonts.ChakraPetch, fontSize = 13.sp)
        }
    }
}
