package com.quantumos.radio.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quantumos.appshell.Fonts
import com.quantumos.appshell.Glyph
import com.quantumos.appshell.Phosphor
import com.quantumos.appshell.QuantumIcon

/*
 * RADIO's own local "vitals" quick-actions console -- the same shape as the standalone app's
 * VitalityDropdownConsole/AtomMark (ported per the explicit port list), reduced per the Core Apps
 * Fix-Pass: the "BEACON FLAG" button was a decorative no-op (`onClick = { /* Simulated Beacon Alert */ }`)
 * with no real launcher-level Beacon hook reachable from a docked library module -- removed rather
 * than faked, per fix-pass rule "make dead-looking taps real or remove them." Stealth toggle and
 * phosphor cycle are both real, wired to RadioViewModel / the local hue state in RadioActivity.
 *
 * All ad hoc hex literals from the standalone app (amber `#FFB000` for the "STEALTH RECON" readiness
 * word, amber for the lock icon) are gone -- every color here derives from the caller's active
 * `color`/`colorDim`, never a second hardcoded hue (house style: "never hardcode hues per-screen").
 */

@Composable
fun AtomMark(
    color: Color,
    isExpanded: Boolean,
    modifier: Modifier = Modifier
) {
    // Fix (Core Apps Fix-Pass): the standalone app used a bouncy spring here, which violates the
    // house style's stepped/non-bouncy motion language. Cleaner, non-bouncy settle instead.
    val rotationAngle by animateFloatAsState(
        targetValue = if (isExpanded) 360f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium),
        label = "atomRotation"
    )

    Canvas(
        modifier = modifier
            .size(24.dp)
            .graphicsLayer(rotationZ = rotationAngle)
    ) {
        val strokeWidth = 1.5.dp.toPx()

        // Nucleus core
        drawCircle(color = color, radius = 4.dp.toPx())

        // Orbit Path A
        withTransform({
            rotate(45f)
        }) {
            drawOval(
                color = color.copy(alpha = 0.5f),
                style = Stroke(width = strokeWidth),
                topLeft = Offset(x = size.width / 2 - 11.dp.toPx(), y = size.height / 2 - 4.dp.toPx()),
                size = Size(22.dp.toPx(), 8.dp.toPx())
            )
        }

        // Orbit Path B
        withTransform({
            rotate(-45f)
        }) {
            drawOval(
                color = color.copy(alpha = 0.5f),
                style = Stroke(width = strokeWidth),
                topLeft = Offset(x = size.width / 2 - 11.dp.toPx(), y = size.height / 2 - 4.dp.toPx()),
                size = Size(22.dp.toPx(), 8.dp.toPx())
            )
        }
    }
}

@Composable
fun VitalityDropdownConsole(
    reception: Int,
    color: Color,
    colorDim: Color,
    stealthMode: Boolean,
    onClose: () -> Unit,
    onStealthToggle: () -> Unit,
    onCycleTheme: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .background(Phosphor.Crt.copy(alpha = 0.95f))
            .border(BorderStroke(1.5.dp, color), CutCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp))
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "VITALITY MONITOR [OPERATOR LINK]",
                    color = color,
                    fontFamily = Fonts.ChakraPetch,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
                IconButton(onClick = onClose) {
                    QuantumIcon(Glyph.Close, tint = color, size = 16.dp)
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(color.copy(alpha = 0.3f))
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Readiness headline -- both states colored from the active hue, no second hardcoded hue.
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "UNIT READINESS:",
                    color = colorDim,
                    fontFamily = Fonts.ChakraPetch,
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (stealthMode) "STEALTH RECON" else "NOMINAL SECURE",
                    color = color,
                    fontFamily = Fonts.ChakraPetch,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Bar gauges. RECEIVER CAPTURE is real (driven by the tuner). POWER RESERVES / CORE
            // TEMPERATURE are illustrative placeholders (RADIO has no wired path to the launcher's
            // real Vitality telemetry from a docked library module) -- unchanged from the standalone
            // app's own behavior, not something this pass introduced or was asked to fix.
            VitalityGaugeRow(label = "RECEIVER CAPTURE", value = reception, color = color)
            Spacer(modifier = Modifier.height(8.dp))
            VitalityGaugeRow(label = "POWER RESERVES", value = 92, color = color)
            Spacer(modifier = Modifier.height(8.dp))
            VitalityGaugeRow(label = "CORE TEMPERATURE (37°C)", value = 68, color = color)

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onStealthToggle,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (stealthMode) color.copy(alpha = 0.25f) else Color.Transparent
                    ),
                    border = BorderStroke(1.dp, color),
                    shape = CutCornerShape(4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        QuantumIcon(Glyph.Stealth, tint = color, size = 16.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (stealthMode) "ACTIVE STEALTH" else "STEALTH MODE",
                            color = color,
                            fontSize = 10.sp,
                            fontFamily = Fonts.ChakraPetch
                        )
                    }
                }

                Button(
                    onClick = onCycleTheme,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    border = BorderStroke(1.dp, color),
                    shape = CutCornerShape(4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        QuantumIcon(Glyph.Phosphor, tint = color, size = 16.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("CYCLE PHOSPHOR", color = color, fontSize = 10.sp, fontFamily = Fonts.ChakraPetch)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onClose,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = color.copy(alpha = 0.15f)),
                border = BorderStroke(1.dp, color),
                shape = CutCornerShape(4.dp)
            ) {
                Text(
                    "STOW CONSOLE",
                    color = color,
                    fontSize = 11.sp,
                    fontFamily = Fonts.ChakraPetch,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun VitalityGaugeRow(
    label: String,
    value: Int,
    color: Color
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, color = color.copy(alpha = 0.5f), fontFamily = Fonts.ChakraPetch, fontSize = 9.sp)
            Text(text = "$value%", color = color, fontFamily = Fonts.ChakraPetch, fontSize = 9.sp)
        }
        Spacer(modifier = Modifier.height(2.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .background(Color.Black.copy(alpha = 0.5f))
                .border(1.dp, color.copy(alpha = 0.2f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(value / 100f)
                    .background(color)
            )
        }
    }
}
