package com.quantumos.radio.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quantumos.appshell.Fonts
import com.quantumos.appshell.Glyph
import com.quantumos.appshell.QuantumIcon
import com.quantumos.radio.RadioBand
import com.quantumos.radio.RadioPreset
import com.quantumos.radio.RadioViewModel
import kotlinx.coroutines.delay
import java.util.Locale
import com.quantumos.appshell.GlyphLabel
import com.quantumos.appshell.SteppedSlider

/*
 * RADIO's field-tool controls -- ported from the standalone app's MechanicalTuningDial,
 * AnalogReceptionMeter, BandSelectionRow, PresetListColumn, SignalStaticCanvas (explicit port list,
 * Core Apps Fix-Pass). Domain logic (bands/frequency ranges/presets/reception math) is unchanged --
 * only the chrome/color/font/motion/text-rendering layer is fixed per the fix-pass:
 *  - every FontFamily.Monospace -> Fonts.ChakraPetch
 *  - every hardcoded Color(0x...) -> the caller's active `color` (never a second hardcoded hue)
 *  - the tuning pointer line was drawn in the old app with the raw `--warn` red as a purely
 *    decorative marker; that's a locked misuse of --warn (alerts/access-denied ONLY), so it now uses
 *    the active phosphor `color` instead
 *  - canvas tick labels now go through a Compose TextMeasurer + Fonts.ChakraPetch instead of raw
 *    android.graphics.Typeface.MONOSPACE / nativeCanvas.drawText
 *  - the reception needle's spring was DampingRatioHighBouncy (smooth bouncy overshoot); fixed to a
 *    non-bouncy settle per the house style's stepped/non-bouncy motion language
 *  - SignalStaticCanvas's unconditional `while (true)` idle-redraw loop is bounded to a finite settle
 *    burst triggered only by a real reception change (fix-pass item 4) -- zero idle redraw at rest
 */

@Composable
fun BandSelectionRow(
    selectedBand: RadioBand,
    color: Color,
    onBandSelected: (RadioBand) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, color.copy(alpha = 0.3f))
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        QuantumIcon(Glyph.TunerDial, tint = color, size = 18.dp)
        RadioBand.entries.forEach { band ->
            val isSelected = band == selectedBand
            // Box + border + clickable, NOT a Material Button (Fold 6: "the design language seems
            // to differ from the other main modules"). The shape was already CutCornerShape, but a
            // Material3 Button brings its own ripple, elevation, minimum height and content padding
            // -- none of which any other QuantumOS module uses -- and that is what made RADIO read
            // as a different app. Every other selection affordance in the OS is a bordered box.
            Box(
                modifier = Modifier
                    .weight(1f)
                    .border(
                        BorderStroke(1.dp, if (isSelected) color else color.copy(alpha = 0.2f)),
                        CutCornerShape(4.dp),
                    )
                    .background(
                        if (isSelected) color.copy(alpha = 0.2f) else Color.Transparent,
                        CutCornerShape(4.dp),
                    )
                    .clickable { onBandSelected(band) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(if (isSelected) color else Color.Transparent)
                            .border(1.dp, color)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = band.name,
                        color = color,
                        fontFamily = Fonts.ChakraPetch,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
fun AnalogReceptionMeter(
    reception: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    val animatedNeedleAngle by animateFloatAsState(
        targetValue = -50f + (reception / 100f) * 100f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium),
        label = "needle"
    )

    Column(
        modifier = modifier
            .border(1.dp, color.copy(alpha = 0.3f))
            .background(Color.Black.copy(alpha = 0.3f))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "RECEPTION CARRIER WAVE",
            color = color.copy(alpha = 0.6f),
            fontFamily = Fonts.ChakraPetch,
            fontSize = 10.sp
        )

        Spacer(modifier = Modifier.height(6.dp))

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(76.dp)
        ) {
            /*
             * Geometry derived from the box, not from a magic multiplier (Fold 6: the scale was
             * drawn OVER the "RECEPTION CARRIER WAVE" caption).
             *
             * The old maths put the pivot 8dp BELOW the canvas and the radius at 1.3x its height,
             * so the top of the arc landed at `height + 8 - 1.3*height` = about 8dp ABOVE the
             * canvas -- outside its own bounds, on top of the label. Compose does not clip a Canvas
             * by default, so it drew there happily.
             *
             * Now the pivot sits just inside the bottom edge and the radius is whatever leaves room
             * for the tick marks (which extend radius + 4dp) plus a margin. The gauge cannot escape
             * its box at any size, and the pivot is shared by the arc, the ticks and the needle --
             * previously the needle hung off a pivot 8dp above the arc's own centre, so it never
             * quite pointed at the scale it was reading.
             */
            val cx = size.width / 2f
            val cy = size.height - 6.dp.toPx()
            val radius = size.height - 18.dp.toPx()

            // scale path
            drawArc(
                color = color.copy(alpha = 0.2f),
                startAngle = -140f,
                sweepAngle = 100f,
                useCenter = false,
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round),
                topLeft = Offset(cx - radius, cy - radius),
                size = Size(radius * 2, radius * 2)
            )

            // Tick marks
            for (i in 0..10) {
                val angleRad = Math.toRadians((-140f + i * 10f).toDouble())
                val startX = cx + (radius - 4.dp.toPx()) * Math.cos(angleRad).toFloat()
                val startY = cy + (radius - 4.dp.toPx()) * Math.sin(angleRad).toFloat()
                val endX = cx + (radius + 4.dp.toPx()) * Math.cos(angleRad).toFloat()
                val endY = cy + (radius + 4.dp.toPx()) * Math.sin(angleRad).toFloat()

                drawLine(
                    color = if (i % 5 == 0) color else color.copy(alpha = 0.4f),
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = if (i % 5 == 0) 2.dp.toPx() else 1.dp.toPx()
                )
            }

            // Pointer needle -- same pivot as the arc and the ticks, so it reads against the
            // scale instead of beside it.
            val needleRad = Math.toRadians((animatedNeedleAngle - 90f).toDouble())
            val needleLength = radius * 0.88f
            val needleX = cx + needleLength * Math.cos(needleRad).toFloat()
            val needleY = cy + needleLength * Math.sin(needleRad).toFloat()

            drawLine(
                color = color,
                start = Offset(cx, cy),
                end = Offset(needleX, needleY),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )

            drawCircle(
                color = color,
                radius = 4.dp.toPx(),
                center = Offset(cx, cy)
            )
        }

        Text(
            text = "$reception% SIGNAL QUALITY",
            color = color,
            fontFamily = Fonts.ChakraPetch,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
    }
}

@Composable
fun MechanicalTuningDial(
    frequency: String,
    band: RadioBand,
    color: Color,
    onFrequencyChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentFreqDouble = frequency.toDoubleOrNull() ?: 100.0
    val (minFreq, maxFreq, unit) = when (band) {
        RadioBand.FM -> Triple(88.0, 108.0, "MHz")
        RadioBand.AM -> Triple(530.0, 1700.0, "kHz")
        RadioBand.WX -> Triple(162.40, 162.55, "MHz")
    }

    val textMeasurer = rememberTextMeasurer()
    val tickLabelStyle = TextStyle(color = color, fontFamily = Fonts.ChakraPetch, fontSize = 8.sp)

    Column(
        modifier = modifier
            .border(1.dp, color.copy(alpha = 0.3f))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "CARRIER FREQUENCY",
                color = color.copy(alpha = 0.6f),
                fontFamily = Fonts.ChakraPetch,
                fontSize = 10.sp
            )
            Text(
                text = "$frequency $unit",
                color = color,
                fontFamily = Fonts.ChakraPetch,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Horizontal sliding ticker scale
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(45.dp)
                .background(Color.Black.copy(alpha = 0.4f))
                .border(1.dp, color.copy(alpha = 0.2f))
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val ticksCount = 30
                val spacing = size.width / ticksCount

                val range = maxFreq - minFreq
                val progress = (currentFreqDouble - minFreq) / range
                val centerOffset = (progress * ticksCount * spacing).toFloat()

                for (i in -10..ticksCount + 10) {
                    val x = i * spacing - (centerOffset % spacing) + (size.width / 2)
                    if (x in 0f..size.width) {
                        val isMajor = i % 5 == 0
                        val tickHeight = if (isMajor) 22.dp.toPx() else 10.dp.toPx()

                        drawLine(
                            color = color.copy(alpha = if (isMajor) 0.6f else 0.3f),
                            start = Offset(x, 0f),
                            end = Offset(x, tickHeight),
                            strokeWidth = if (isMajor) 1.5.dp.toPx() else 1.dp.toPx()
                        )

                        if (isMajor) {
                            val tickFreq = minFreq + ((centerOffset / spacing + i - ticksCount / 2) * range / ticksCount)
                            if (tickFreq in minFreq..maxFreq) {
                                val label = if (band == RadioBand.AM) {
                                    tickFreq.toInt().toString()
                                } else {
                                    String.format(Locale.US, "%.1f", tickFreq)
                                }
                                val measured = textMeasurer.measure(label, tickLabelStyle)
                                drawText(
                                    textLayoutResult = measured,
                                    topLeft = Offset(
                                        x - measured.size.width / 2f,
                                        size.height - measured.size.height - 2.dp.toPx()
                                    )
                                )
                            }
                        }
                    }
                }

                // Static tuning pointer. Fix: the standalone app drew this permanently-visible,
                // purely-decorative marker in the raw --warn red -- a locked misuse (--warn is
                // alerts/access-denied ONLY). It now uses the active phosphor instead.
                drawLine(
                    color = color,
                    start = Offset(size.width / 2f, 0f),
                    end = Offset(size.width / 2f, size.height),
                    strokeWidth = 2.dp.toPx()
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Carrier position -- the house stepped bar, not a Material Slider. See SteppedSlider.
        SteppedSlider(
            value = ((currentFreqDouble - minFreq) / (maxFreq - minFreq)).toFloat(),
            onValueChange = { fraction ->
                val freq = minFreq + fraction * (maxFreq - minFreq)
                val formatted = when (band) {
                    RadioBand.FM -> String.format(Locale.US, "%.1f", freq)
                    RadioBand.AM -> freq.toInt().toString()
                    RadioBand.WX -> String.format(Locale.US, "%.2f", freq)
                }
                onFrequencyChanged(formatted)
            },
            color = color,
            dimColor = color,
            segments = 32,
            modifier = Modifier.fillMaxWidth(),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val step = when (band) {
                RadioBand.FM -> 0.1
                RadioBand.AM -> 10.0
                RadioBand.WX -> 0.01
            }

            Box(
                modifier = Modifier
                    .border(BorderStroke(1.dp, color.copy(alpha = 0.5f)), CutCornerShape(4.dp))
                    .clickable {
                        val prev = (currentFreqDouble - step).coerceAtLeast(minFreq)
                        val formatted = when (band) {
                            RadioBand.FM -> String.format(Locale.US, "%.1f", prev)
                            RadioBand.AM -> prev.toInt().toString()
                            RadioBand.WX -> String.format(Locale.US, "%.2f", prev)
                        }
                        onFrequencyChanged(formatted)
                    }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
                GlyphLabel(Glyph.TriangleLeft, "TUNE -", color, Fonts.ChakraPetch, fontSize = 11.sp, iconSize = 10.dp)
            }

            Box(
                modifier = Modifier
                    .border(BorderStroke(1.dp, color.copy(alpha = 0.5f)), CutCornerShape(4.dp))
                    .clickable {
                        val next = (currentFreqDouble + step).coerceAtMost(maxFreq)
                        val formatted = when (band) {
                            RadioBand.FM -> String.format(Locale.US, "%.1f", next)
                            RadioBand.AM -> next.toInt().toString()
                            RadioBand.WX -> String.format(Locale.US, "%.2f", next)
                        }
                        onFrequencyChanged(formatted)
                    }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
                GlyphLabel(Glyph.TriangleRight, "TUNE +", color, Fonts.ChakraPetch, fontSize = 11.sp, iconSize = 10.dp, trailing = true)
            }
        }
    }
}

@Composable
fun VolumeAndStatusCard(
    volume: Int,
    reception: Int,
    color: Color,
    warnColor: Color,
    onVolumeChanged: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .border(1.dp, color.copy(alpha = 0.3f))
            .background(Color.Black.copy(alpha = 0.3f))
            .padding(12.dp)
    ) {
        Text(
            text = "AUDIO POWER FEED",
            color = color.copy(alpha = 0.6f),
            fontFamily = Fonts.ChakraPetch,
            fontSize = 10.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        SteppedSlider(
            value = volume / 100f,
            onValueChange = { onVolumeChanged((it * 100f).toInt()) },
            color = color,
            dimColor = color,
            segments = 20,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "VOL: $volume%", color = color, fontFamily = Fonts.ChakraPetch, fontSize = 11.sp)

            val statusText = when {
                reception >= 80 -> "NOMINAL"
                reception >= 30 -> "UNSTABLE"
                else -> "STATIC NOISE"
            }
            // Reception below 30% is a real degraded-link condition, not decoration -- legitimate use
            // of --warn (mirrors the launcher's own readiness-threshold coloring).
            val statusColor = if (reception < 30) warnColor else color

            Text(
                text = statusText,
                color = statusColor,
                fontFamily = Fonts.ChakraPetch,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
fun PresetListColumn(
    band: RadioBand,
    currentFreq: String,
    viewModel: RadioViewModel,
    color: Color,
    modifier: Modifier = Modifier
) {
    val presets = when (band) {
        RadioBand.FM -> viewModel.fmPresets
        RadioBand.AM -> viewModel.amPresets
        RadioBand.WX -> viewModel.wxPresets
    }

    Column(
        modifier = modifier
            .border(1.dp, color.copy(alpha = 0.3f))
            .background(Color.Black.copy(alpha = 0.2f))
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
            QuantumIcon(Glyph.Preset, tint = color.copy(alpha = 0.6f), size = 12.dp)
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = "CARRIER PRESETS",
                color = color.copy(alpha = 0.6f),
                fontFamily = Fonts.ChakraPetch,
                fontSize = 10.sp
            )
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(presets) { preset: RadioPreset ->
                val isSelected = currentFreq == preset.frequency
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.tuneFrequency(preset.frequency) }
                        .border(
                            1.dp,
                            if (isSelected) color else color.copy(alpha = 0.15f),
                            CutCornerShape(2.dp)
                        )
                        .background(if (isSelected) color.copy(alpha = 0.12f) else Color.Transparent)
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = preset.name,
                            color = color,
                            fontFamily = Fonts.ChakraPetch,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        Text(
                            text = preset.description,
                            color = color.copy(alpha = 0.5f),
                            fontFamily = Fonts.ChakraPetch,
                            fontSize = 10.sp
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (preset.isEncrypted) {
                            // Fix: standalone app hardcoded amber (#FFB000) here regardless of the
                            // active hue. Uses the active phosphor `color` instead -- one token source.
                            QuantumIcon(Glyph.Lock, tint = color, size = 12.dp)
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text(
                            text = preset.frequency,
                            color = color,
                            fontFamily = Fonts.ChakraPetch,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SignalStaticCanvas(
    reception: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    if (reception >= 97) return

    val density = (100 - reception) / 100f
    var clockTrigger by remember { mutableStateOf(0) }

    // Fix (Core Apps Fix-Pass, item 4): the standalone app's `while (true) { delay(100); ... }` ran
    // forever once triggered, regardless of whether reception changed again -- an unconditional idle
    // redraw loop. Bounded to a finite ~800ms settle burst, triggered only by a real reception change.
    // At rest (no new reception value), nothing runs.
    LaunchedEffect(reception) {
        repeat(8) {
            delay(100)
            clockTrigger++
        }
    }

    Canvas(modifier = modifier) {
        val rand = java.util.Random(clockTrigger.toLong())
        val grainSize = 2.dp.toPx()
        val cols = (size.width / grainSize).toInt()
        val rows = (size.height / grainSize).toInt()

        val pointsToDraw = (cols * rows * density * 0.08f).toInt().coerceAtMost(550)
        for (i in 0 until pointsToDraw) {
            val c = rand.nextInt(cols.coerceAtLeast(1))
            val r = rand.nextInt(rows.coerceAtLeast(1))
            drawRect(
                color = color.copy(alpha = rand.nextFloat() * 0.14f),
                topLeft = Offset(c * grainSize, r * grainSize),
                size = Size(grainSize, grainSize)
            )
        }
    }
}
