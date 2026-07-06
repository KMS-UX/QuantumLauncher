package com.quantumos.optics.ui.camera

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quantumos.appshell.Phosphor

@Composable
fun SettingsPanel(
    showSettingsPanel: Boolean,
    onShowSettingsPanelChange: (Boolean) -> Unit,
    themeColor: Color,
    colorName: String,
    onCycleColor: () -> Unit,
    burnEnabled: Boolean,
    onBurnEnabledChange: (Boolean) -> Unit,
    telemetryHudEnabled: Boolean,
    onTelemetryHudEnabledChange: (Boolean) -> Unit,
    doubleExposureEnabled: Boolean,
    onDoubleExposureEnabledChange: (Boolean) -> Unit,
    goodOldTimesEnabled: Boolean,
    onGoodOldTimesEnabledChange: (Boolean) -> Unit,
    chemicalDevDelayEnabled: Boolean,
    onChemicalDevDelayEnabledChange: (Boolean) -> Unit,
    reticleStyle: ReticleStyle,
    onReticleStyleChange: (ReticleStyle) -> Unit,
    bracketAlpha: Float,
    onBracketAlphaChange: (Float) -> Unit,
    latitude: Double,
    longitude: Double,
    onCoordsChange: (Double, Double) -> Unit
) {
        if (showSettingsPanel) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.85f))
                    .clickable(enabled = true, onClick = { onShowSettingsPanelChange(false) }),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Phosphor.Crt),
                    modifier = Modifier
                        .width(300.dp)
                        .border(2.dp, themeColor, RoundedCornerShape(8.dp))
                        .clickable(enabled = false, onClick = {}) // Prevent click propagation
                        .padding(16.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            "[ CORE SYSTEM CALIBRATION ]",
                            color = themeColor,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Divider(color = themeColor.copy(alpha = 0.5f), thickness = 1.dp)

                        // 1. Metadata Burn Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("METADATA BURN imprints", color = themeColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text("Burns telemetry directly on saved frame", color = themeColor.copy(alpha = 0.6f), fontSize = 8.sp)
                            }
                            Switch(
                                checked = burnEnabled,
                                onCheckedChange = onBurnEnabledChange,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Phosphor.Crt,
                                    checkedTrackColor = themeColor,
                                    uncheckedThumbColor = themeColor.copy(alpha = 0.5f),
                                    uncheckedTrackColor = Color.Transparent,
                                    uncheckedBorderColor = themeColor
                                )
                            )
                        }

                        // 2. Telemetry HUD Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("TELEMETRY GRAPH HUD", color = themeColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text("Compass ribbon & dynamic pitch ladder", color = themeColor.copy(alpha = 0.6f), fontSize = 8.sp)
                            }
                            Switch(
                                checked = telemetryHudEnabled,
                                onCheckedChange = onTelemetryHudEnabledChange,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Phosphor.Crt,
                                    checkedTrackColor = themeColor,
                                    uncheckedThumbColor = themeColor.copy(alpha = 0.5f),
                                    uncheckedTrackColor = Color.Transparent,
                                    uncheckedBorderColor = themeColor
                                )
                            )
                        }

                        // 2.5. Double Exposure Mode Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("DOUBLE EXPOSURE MODE", color = themeColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text("Composite two consecutive captures on one frame", color = themeColor.copy(alpha = 0.6f), fontSize = 8.sp)
                            }
                            Switch(
                                checked = doubleExposureEnabled,
                                onCheckedChange = onDoubleExposureEnabledChange,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Phosphor.Crt,
                                    checkedTrackColor = themeColor,
                                    uncheckedThumbColor = themeColor.copy(alpha = 0.5f),
                                    uncheckedTrackColor = Color.Transparent,
                                    uncheckedBorderColor = themeColor
                                )
                            )
                        }

                        // 2.6. Good Old Times Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                Text("GOOD OLD TIMES (1950s)", color = themeColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text("Pre-war atomic era rendering style (disables isotopic decay, core leaks, and thermal scratches)", color = themeColor.copy(alpha = 0.6f), fontSize = 8.sp)
                            }
                            Switch(
                                checked = goodOldTimesEnabled,
                                onCheckedChange = onGoodOldTimesEnabledChange,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Phosphor.Crt,
                                    checkedTrackColor = themeColor,
                                    uncheckedThumbColor = themeColor.copy(alpha = 0.5f),
                                    uncheckedTrackColor = Color.Transparent,
                                    uncheckedBorderColor = themeColor
                                )
                            )
                        }

                        // 2.7. Darkroom Chemical Develop Delay Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                Text("DARKROOM DEVELOP DELAY", color = themeColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text("Immersive 8s delayed chemical processing before frame stabilizes in database", color = themeColor.copy(alpha = 0.6f), fontSize = 8.sp)
                            }
                            Switch(
                                checked = chemicalDevDelayEnabled,
                                onCheckedChange = onChemicalDevDelayEnabledChange,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Phosphor.Crt,
                                    checkedTrackColor = themeColor,
                                    uncheckedThumbColor = themeColor.copy(alpha = 0.5f),
                                    uncheckedTrackColor = Color.Transparent,
                                    uncheckedBorderColor = themeColor
                                )
                            )
                        }

                        // 3. Phosphor Emission Color (Symmetric Tactile Switch!)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("PHOSPHOR EMISSION", color = themeColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text("Select CRT monochrome hue profile", color = themeColor.copy(alpha = 0.6f), fontSize = 8.sp)
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier
                                    .border(1.dp, themeColor, RoundedCornerShape(2.dp))
                                    .clickable { onCycleColor() }
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                                    .background(Phosphor.Crt)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(themeColor, CircleShape)
                                )
                                Text(
                                    text = colorName,
                                    color = themeColor,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // 4. Viewfinder Reticle Style
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("VIEWFINDER RETICLE", color = themeColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text("Select center layout reticle style", color = themeColor.copy(alpha = 0.6f), fontSize = 8.sp)
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier
                                    .border(1.dp, themeColor, RoundedCornerShape(2.dp))
                                    .clickable {
                                        onReticleStyleChange(
                                            when (reticleStyle) {
                                                ReticleStyle.CORNERS -> ReticleStyle.CROSSHAIR
                                                ReticleStyle.CROSSHAIR -> ReticleStyle.GRID
                                                ReticleStyle.GRID -> ReticleStyle.MINIMAL
                                                ReticleStyle.MINIMAL -> ReticleStyle.CORNERS
                                            }
                                        )
                                    }
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                                    .background(Phosphor.Crt)
                            ) {
                                Text(
                                    text = reticleStyle.name,
                                    color = themeColor,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // 5. Viewfinder Notch Opacity
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("VIEWFINDER NOTCH", color = themeColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text("Select center frame overlay opacity", color = themeColor.copy(alpha = 0.6f), fontSize = 8.sp)
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier
                                    .border(1.dp, themeColor, RoundedCornerShape(2.dp))
                                    .clickable {
                                        onBracketAlphaChange(if (bracketAlpha == 1f) 0.3f else if (bracketAlpha == 0.3f) 0.05f else 1f)
                                    }
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                                    .background(Phosphor.Crt)
                            ) {
                                val blendLabel = when (bracketAlpha) {
                                    1f -> "HI-VIS"
                                    0.3f -> "MID"
                                    else -> "AMBIENT"
                                }
                                Text(
                                    text = blendLabel,
                                    color = themeColor,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Divider(color = themeColor.copy(alpha = 0.3f), thickness = 1.dp)

                        // 4. Manual Coords Adjusters
                        Text("MANUAL COORDINATE OVERRIDE", color = themeColor, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Latitude
                            Column(modifier = Modifier.weight(1f)) {
                                Text("LATITUDE", color = themeColor.copy(alpha = 0.6f), fontSize = 7.sp)
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .border(1.dp, themeColor, RoundedCornerShape(2.dp))
                                            .clickable { onCoordsChange(latitude - 0.01, longitude) },
                                        contentAlignment = Alignment.Center
                                    ) { Text("-", color = themeColor, fontSize = 12.sp) }
                                    Text(String.format("%.2f°", latitude), color = themeColor, fontSize = 10.sp, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .border(1.dp, themeColor, RoundedCornerShape(2.dp))
                                            .clickable { onCoordsChange(latitude + 0.01, longitude) },
                                        contentAlignment = Alignment.Center
                                    ) { Text("+", color = themeColor, fontSize = 12.sp) }
                                }
                            }

                            // Longitude
                            Column(modifier = Modifier.weight(1f)) {
                                Text("LONGITUDE", color = themeColor.copy(alpha = 0.6f), fontSize = 7.sp)
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .border(1.dp, themeColor, RoundedCornerShape(2.dp))
                                            .clickable { onCoordsChange(latitude, longitude - 0.01) },
                                        contentAlignment = Alignment.Center
                                    ) { Text("-", color = themeColor, fontSize = 12.sp) }
                                    Text(String.format("%.2f°", longitude), color = themeColor, fontSize = 10.sp, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .border(1.dp, themeColor, RoundedCornerShape(2.dp))
                                            .clickable { onCoordsChange(latitude, longitude + 0.01) },
                                        contentAlignment = Alignment.Center
                                    ) { Text("+", color = themeColor, fontSize = 12.sp) }
                                }
                            }
                        }

                        Divider(color = themeColor.copy(alpha = 0.5f), thickness = 1.dp)

                        // Close Button
                        Button(
                            onClick = { onShowSettingsPanelChange(false) },
                            colors = ButtonDefaults.buttonColors(containerColor = themeColor, contentColor = Phosphor.Crt),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("SECURE & DISMISS", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
}

@Composable
fun ChemicalSlidersSection(
    contrast: Float,
    onContrastChange: (Float) -> Unit,
    exposureOffset: Float,
    onExposureOffsetChange: (Float) -> Unit,
    grainDensity: Float,
    onGrainDensityChange: (Float) -> Unit,
    themeColor: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Column {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("DEVELOPER CONTRAST", color = themeColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text(String.format("%.2fx", contrast), color = themeColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            Slider(
                value = contrast,
                onValueChange = onContrastChange,
                valueRange = 0.5f..2.0f,
                colors = SliderDefaults.colors(
                    thumbColor = themeColor,
                    activeTrackColor = themeColor,
                    inactiveTrackColor = themeColor.copy(alpha = 0.25f)
                )
            )
        }

        Column {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("ENLARGER EXPOSURE", color = themeColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text(String.format("%+.0f EV", exposureOffset), color = themeColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            Slider(
                value = exposureOffset,
                onValueChange = onExposureOffsetChange,
                valueRange = -100f..100f,
                colors = SliderDefaults.colors(
                    thumbColor = themeColor,
                    activeTrackColor = themeColor,
                    inactiveTrackColor = themeColor.copy(alpha = 0.25f)
                )
            )
        }

        Column {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("BATH TEMP (GRAIN)", color = themeColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text(String.format("%.2fx DENSITY", grainDensity), color = themeColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            Slider(
                value = grainDensity,
                onValueChange = onGrainDensityChange,
                valueRange = 0.0f..3.0f,
                colors = SliderDefaults.colors(
                    thumbColor = themeColor,
                    activeTrackColor = themeColor,
                    inactiveTrackColor = themeColor.copy(alpha = 0.25f)
                )
            )
        }
    }
}
