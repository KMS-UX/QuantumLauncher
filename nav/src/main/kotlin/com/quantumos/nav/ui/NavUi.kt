package com.quantumos.nav.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import com.quantumos.appshell.Fonts
import com.quantumos.appshell.NameplateHeader
import com.quantumos.appshell.Phosphor
import com.quantumos.appshell.PleaseStandbyCard
import com.quantumos.appshell.crtOverlay
import com.quantumos.core.PhosphorHue
import com.quantumos.nav.GpsReadout
import com.quantumos.nav.core.NavCoordinates
import com.quantumos.nav.core.SectorPresets

/*
 * QuantumOS Nav — docked into the launcher's shared App Shell (App Shell Integration, Phase 3).
 * Uses the real shared NameplateHeader/PleaseStandbyCard from :app-shell instead of the local
 * duplicates the standalone companion app carried while un-docked.
 *
 * QUARK-trigger parking note (audit finding, QuantumMAP BUILD_LOG "Known issues / flags for the
 * Director & Brief 2", §d): the primary controls are the bottom action rail (WARP + presets) and
 * the top-left GPS readout. The clear default park for the floating QUARK trigger is the RIGHT
 * EDGE, mid-viewport — routed around here, and already satisfied by the launcher's
 * OverlayGeometry.defaultPark (right edge, mid-height) with no change needed.
 *
 * No BackHandler here: the Shell owns back once docked -- system/predictive back simply finishes
 * NavActivity and returns to the still-live launcher on HOME. The "◄ HOME" line is the same return
 * path, made explicit and tappable.
 */
@Composable
fun NavScreen(
    activeHue: PhosphorHue,
    gps: GpsReadout,
    statusLine: String?,
    warp: com.quantumos.nav.WarpRequest?,
    onCyclePhosphor: () -> Unit,
    onWarpEntry: (String, String) -> Unit,
    onWarpPreset: (Int) -> Unit,
    onLocate: () -> Unit,
    onReturnHome: () -> Unit = {},
) {
    val color = Phosphor.bright(activeHue)
    val dim = Phosphor.dim(activeHue)
    val font = Fonts.ChakraPetch
    val insets = WindowInsets.systemBars.asPaddingValues()
    var warpInTransit by remember { mutableStateOf(false) }

    Box(
        Modifier
            .fillMaxSize()
            .background(Phosphor.Crt)
            // CRT falloff over the whole surface. We use the OVERLAY treatment (translucent scanlines
            // + vignette drawn on top), NOT the AGSL RenderEffect shader: a RenderEffect renders its
            // subtree into an offscreen buffer, and the MapLibre map view cannot be captured that way,
            // so the shader blanked the map. The overlay is plain overdraw — the map shows through.
            .crtOverlay()
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(insets)
        ) {
            Box(Modifier.fillMaxWidth()) {
                NameplateHeader(channelName = "NAV", color = color, dimColor = dim, font = font)
                // Explicit return-to-HOME affordance, alongside the back gesture.
                Text(
                    text = "◄ HOME",
                    color = dim,
                    fontFamily = font,
                    fontSize = 11.sp,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 40.dp)
                        .clickable { onReturnHome() }
                )
            }

            // Current position for the map blip — only when we actually hold a GPS fix.
            val positionFix = (gps as? GpsReadout.Fix)?.let { NavCoordinates(it.latitude, it.longitude) }

            // ---- Content: the map viewport fills the middle; GPS readout floats top-left ----
            Box(Modifier.fillMaxWidth().weight(1f)) {
                MapCanvas(
                    activeHue = activeHue,
                    warp = warp,
                    positionFix = positionFix,
                    onWarpTransit = { warpInTransit = it },
                    modifier = Modifier.fillMaxSize(),
                )
                GpsReadoutPanel(
                    gps = gps,
                    color = color,
                    dim = dim,
                    font = font,
                    modifier = Modifier.align(Alignment.TopStart).padding(16.dp),
                )
                // On-brand map credit — replaces MapLibre's sharp white watermark (OSM attribution,
                // kept visible but dim). Bottom-start, low emphasis.
                MapAttribution(
                    dim = dim,
                    font = font,
                    modifier = Modifier.align(Alignment.BottomStart).padding(horizontal = 10.dp, vertical = 6.dp),
                )
                // Return-to-current-location — top-END corner (opposite the GPS readout, and clear of
                // the right-edge-mid QUARK park). Warps the camera to the latest GPS fix.
                LocateControl(
                    color = color,
                    dim = dim,
                    font = font,
                    modifier = Modifier.align(Alignment.TopEnd).padding(16.dp),
                    onClick = onLocate,
                )
                if (warpInTransit) {
                    // The universal loading/transition beat — never a generic spinner.
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        PleaseStandbyCard(subline = "PLOTTING TRANSIT…", color = color, dimColor = dim, font = font)
                    }
                }
            }

            ControlRail(
                color = color,
                dim = dim,
                font = font,
                statusLine = statusLine,
                onCyclePhosphor = onCyclePhosphor,
                onWarpEntry = onWarpEntry,
                onWarpPreset = onWarpPreset,
            )
        }
    }
}

// Floating GPS locator readout — terse, three discrete states. Bordered dim panel over the map.
@Composable
private fun GpsReadoutPanel(
    gps: GpsReadout,
    color: Color,
    dim: Color,
    font: FontFamily,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .background(Phosphor.Crt.copy(alpha = 0.82f))
            .border(BorderStroke(1.dp, dim))
            .padding(12.dp)
    ) {
        Text("GPS LOCATOR", color = dim, fontFamily = font, fontSize = 9.sp, letterSpacing = 1.sp)
        Spacer(Modifier.height(4.dp))
        when (gps) {
            GpsReadout.Scanning -> Text(
                "ACQUIRING FIX…", color = color, fontFamily = font, fontSize = 11.sp
            )
            GpsReadout.PermissionDenied -> Text(
                "SIGNAL DENIED", color = Phosphor.Warn, fontFamily = font, fontSize = 11.sp, fontWeight = FontWeight.Bold
            )
            is GpsReadout.Fix -> Text(
                text = "LAT %.5f\nLNG %.5f\nALT %.1f M".format(gps.latitude, gps.longitude, gps.altitudeMeters),
                color = color, fontFamily = font, fontSize = 11.sp, lineHeight = 15.sp
            )
        }
    }
}

// The action rail — coordinate entry + WARP + sector presets + the phosphor cycle. The App Shell's
// strip->content->action-rail body pattern; replaces the old Material Button/EditText footer.
// FlowRow (for the wrapping sector-preset chips) is still @ExperimentalLayoutApi in the pinned
// Compose BOM 2024.10.01 — opt in here rather than avoid it; the API is stable enough on a pinned BOM.
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ControlRail(
    color: Color,
    dim: Color,
    font: FontFamily,
    statusLine: String?,
    onCyclePhosphor: () -> Unit,
    onWarpEntry: (String, String) -> Unit,
    onWarpPreset: (Int) -> Unit,
) {
    var lat by rememberSaveable { mutableStateOf("") }
    var lng by rememberSaveable { mutableStateOf("") }

    Column(
        Modifier
            .fillMaxWidth()
            .background(Phosphor.Crt)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("COORDINATE ENTRY", color = dim, fontFamily = font, fontSize = 10.sp, letterSpacing = 1.sp)
            Spacer(Modifier.weight(1f))
            // Live phosphor switch — the same field capability as the launcher's Vitality panel.
            Text(
                "[PHOSPHOR ►]",
                color = color, fontFamily = font, fontSize = 10.sp,
                modifier = Modifier.clickable { onCyclePhosphor() }.padding(4.dp)
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CoordField("LAT", lat, color, dim, font, Modifier.weight(1f)) { lat = it }
            CoordField("LNG", lng, color, dim, font, Modifier.weight(1f)) { lng = it }
        }
        Spacer(Modifier.height(10.dp))
        RailButton("W A R P", color, dim, font, filled = true) { onWarpEntry(lat, lng) }

        // Terse in-surface status — replaces the old Toast. --warn only for the genuine reject.
        if (statusLine != null) {
            Spacer(Modifier.height(8.dp))
            val statusColor = if (statusLine == "INVALID COORDINATES") Phosphor.Warn else dim
            Text("» $statusLine", color = statusColor, fontFamily = font, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(12.dp))
        Text("SECTOR PRESETS", color = dim, fontFamily = font, fontSize = 10.sp, letterSpacing = 1.sp)
        Spacer(Modifier.height(6.dp))
        // FlowRow so the expanded sector set (JAPAN overview + Japan areas + regional sectors) wraps
        // onto as many lines as it needs, instead of being crushed into one Row.
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            SectorPresets.ALL.forEachIndexed { index, preset ->
                PresetChip(preset.label, color, dim, font) { onWarpPreset(index) }
            }
        }
    }
}

// A compact bordered sector chip (wraps to its content — sized for FlowRow, unlike the full-width
// RailButton). Outline phosphor, no Material chrome.
@Composable
private fun PresetChip(
    label: String,
    color: Color,
    dim: Color,
    font: FontFamily,
    onClick: () -> Unit,
) {
    Box(
        Modifier
            .border(BorderStroke(1.dp, dim))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = color, fontFamily = font, fontSize = 10.sp, letterSpacing = 0.5.sp)
    }
}

// Return-to-current-location control on the map — a bordered crosshair chip. Tapping recenters the
// camera on the latest GPS fix (or reports a terse status if there's no fix / signal denied).
@Composable
private fun LocateControl(
    color: Color,
    dim: Color,
    font: FontFamily,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Row(
        modifier
            .background(Phosphor.Crt.copy(alpha = 0.82f))
            .border(BorderStroke(1.dp, dim))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("◎", color = color, fontFamily = font, fontSize = 14.sp)
        Spacer(Modifier.width(6.dp))
        Text("MY POSITION", color = color, fontFamily = font, fontSize = 10.sp, letterSpacing = 0.5.sp)
    }
}

// On-brand map credit — the required OpenStreetMap / OpenFreeMap attribution rendered as a dim,
// low-emphasis phosphor line (Chakra Petch), so it reads as chassis stencilling rather than a sharp
// white watermark. Replaces MapLibre's default logo + attribution widgets (disabled in MapCanvas).
@Composable
private fun MapAttribution(dim: Color, font: FontFamily, modifier: Modifier = Modifier) {
    Text(
        text = "MAP · OPENSTREETMAP / OPENFREEMAP",
        color = dim.copy(alpha = 0.55f),
        fontFamily = font,
        fontSize = 8.sp,
        letterSpacing = 0.5.sp,
        modifier = modifier
    )
}

@Composable
private fun CoordField(
    label: String,
    value: String,
    color: Color,
    dim: Color,
    font: FontFamily,
    modifier: Modifier = Modifier,
    onValueChange: (String) -> Unit,
) {
    Column(modifier) {
        Text("TARGET $label", color = dim, fontFamily = font, fontSize = 9.sp)
        Spacer(Modifier.height(4.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(44.dp)
                .border(BorderStroke(1.dp, dim))
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = TextStyle(color = color, fontFamily = font, fontSize = 14.sp),
                cursorBrush = SolidColor(color),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                decorationBox = { inner ->
                    if (value.isEmpty()) {
                        Text(
                            if (label == "LAT") "35.6895" else "139.6917",
                            color = dim.copy(alpha = 0.6f), fontFamily = font, fontSize = 14.sp
                        )
                    }
                    inner()
                }
            )
        }
    }
}

// House-style rail button — a bordered phosphor cell, filled (primary) or outline (secondary).
// No Material Button chrome.
@Composable
private fun RailButton(
    label: String,
    color: Color,
    dim: Color,
    font: FontFamily,
    filled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier
            .fillMaxWidth()
            .height(if (filled) 48.dp else 36.dp)
            .background(if (filled) color.copy(alpha = 0.14f) else Color.Transparent)
            .border(BorderStroke(if (filled) 2.dp else 1.dp, color))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = color,
            fontFamily = font,
            fontSize = if (filled) 15.sp else 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = if (filled) 3.sp else 1.sp,
            textAlign = TextAlign.Center
        )
    }
}
