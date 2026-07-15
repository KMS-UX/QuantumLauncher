package com.quantumos.optics.ui.camera

import com.quantumos.appshell.Phosphor

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ReticleOverlay(
    style: ReticleStyle,
    themeColor: Color,
    bracketAlpha: Float,
    dialMode: DialMode,
    astroRotation: Float,
    isoSensitivityValue: Int,
    grainSeed: Float,
    heading: Float,
    pitch: Float,
    roll: Float,
    telemetryHudEnabled: Boolean,
    activeFocalLength: Int,
    activeFocusDistance: Float,
    subjectDistance: Float
) {
    val animatedColor by animateColorAsState(targetValue = themeColor.copy(alpha = bracketAlpha))

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val pad = 48.dp.toPx()

        val nativeCanvas = drawContext.canvas.nativeCanvas

        // 0. LIVE LENS VIGNETTE EMULATION (Noctilux Radial Shadow)
        val centerX = w / 2f
        val centerY = h / 2f
        val diagRadius = Math.hypot(w.toDouble(), h.toDouble()).toFloat() / 2f

        // 0.2. Vintage Leica Bright-line Viewfinder Framelines (35mm / 50mm / 90mm)
        // Authentic rangefinder bright-line projections with parallax corner guides & labels.
        val frameAlpha = animatedColor.alpha * 0.75f
        val secondaryAlpha = animatedColor.alpha * 0.22f
        val framelineStroke = 1.2f.dp.toPx()

        fun drawLeicaFrameline(fl: Int, isMain: Boolean) {
            val alpha = if (isMain) frameAlpha else secondaryAlpha
            val pColor = animatedColor.copy(alpha = alpha)
            
            // Scaled dimensions relative to viewport size:
            // 35mm (wide-angle: 82% crop area)
            // 50mm (standard-range: 58% crop area)
            // 90mm (telephoto-portrait: 32% crop area)
            val scale = when (fl) {
                35 -> 0.82f
                50 -> 0.58f
                90 -> 0.32f
                else -> 0.58f
            }
            
            val frameW = w * scale
            val frameH = h * scale
            val left = centerX - frameW / 2f
            val top = centerY - frameH / 2f
            val right = left + frameW
            val bottom = top + frameH
            
            // Draw classic rangefinder corners / frame boundaries (Leica M design with central splits/gaps)
            val cornerLen = 22.dp.toPx() * (if (fl == 90) 0.65f else 1.0f)
            
            // Top-Left corner bracket
            drawLine(pColor, Offset(left, top), Offset(left + cornerLen, top), framelineStroke)
            drawLine(pColor, Offset(left, top), Offset(left, top + cornerLen), framelineStroke)
            
            // Top-Right corner bracket
            drawLine(pColor, Offset(right, top), Offset(right - cornerLen, top), framelineStroke)
            drawLine(pColor, Offset(right, top), Offset(right, top + cornerLen), framelineStroke)
            
            // Bottom-Left corner bracket
            drawLine(pColor, Offset(left, bottom), Offset(left + cornerLen, bottom), framelineStroke)
            drawLine(pColor, Offset(left, bottom), Offset(left, bottom - cornerLen), framelineStroke)
            
            // Bottom-Right corner bracket
            drawLine(pColor, Offset(right, bottom), Offset(right - cornerLen, bottom), framelineStroke)
            drawLine(pColor, Offset(right, bottom), Offset(right, bottom - cornerLen), framelineStroke)

            // Split markers along the middle edges for parallax / center composition
            val gap = 12.dp.toPx()
            if (fl == 35 || fl == 50) {
                // Left & right center markers
                drawLine(pColor, Offset(left, centerY - gap), Offset(left, centerY - gap / 3), framelineStroke)
                drawLine(pColor, Offset(left, centerY + gap / 3), Offset(left, centerY + gap), framelineStroke)
                drawLine(pColor, Offset(right, centerY - gap), Offset(right, centerY - gap / 3), framelineStroke)
                drawLine(pColor, Offset(right, centerY + gap / 3), Offset(right, centerY + gap), framelineStroke)
            }
            
            // Global central Rangefinder Focusing Patch (double-image ghost alignment spot)
            if (isMain) {
                val patchW = 28.dp.toPx()
                val patchH = 18.dp.toPx()
                // Yellow-gold phosphor classic warm color
                val patchColor = Color(0xFFE5A93B)
                
                // Draw patch background (semi-transparent)
                drawRect(
                    color = patchColor.copy(alpha = alpha * 0.18f),
                    topLeft = Offset(centerX - patchW / 2, centerY - patchH / 2),
                    size = androidx.compose.ui.geometry.Size(patchW, patchH)
                )
                // Draw patch outline
                drawRect(
                    color = patchColor.copy(alpha = alpha * 0.50f),
                    topLeft = Offset(centerX - patchW / 2, centerY - patchH / 2),
                    size = androidx.compose.ui.geometry.Size(patchW, patchH),
                    style = Stroke(width = 1.dp.toPx())
                )

                // Draw the double-image (coincidence rangefinder) ghost alignment marks
                val focusError = activeFocusDistance - subjectDistance
                val horizontalShift = (focusError * 15f).coerceIn(-12f, 12f).dp.toPx()
                
                // Left ghost marker (shifting based on focus)
                drawCircle(
                    color = patchColor.copy(alpha = alpha * 0.70f),
                    radius = 3.5f.dp.toPx(),
                    center = Offset(centerX + horizontalShift, centerY)
                )
                
                // Right aligned target indicator (remains stationary at absolute optical alignment center)
                drawCircle(
                    color = animatedColor.copy(alpha = alpha * 0.90f),
                    radius = 3.5f.dp.toPx(),
                    center = Offset(centerX, centerY),
                    style = Stroke(width = 1.2f.dp.toPx())
                )
            }
            
            // Render focal length label
            val textPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.argb(
                    (alpha * 255).toInt(), 
                    (animatedColor.red * 255).toInt(), 
                    (animatedColor.green * 255).toInt(), 
                    (animatedColor.blue * 255).toInt()
                )
                textSize = (if (fl == 90) 7.sp else 8.5.sp).toPx()
                isAntiAlias = true
                typeface = android.graphics.Typeface.create(android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.BOLD)
            }
            nativeCanvas.drawText(
                "${fl}mm", 
                left + 6.dp.toPx(), 
                top + 16.dp.toPx(), 
                textPaint
            )
        }

        // 1. Draw main active bright-line frame
        drawLeicaFrameline(activeFocalLength, isMain = true)
        
        // 2. Draw authentic paired secondary frameline (as in real Leica M dual-projector)
        val secondaryFocal = when (activeFocalLength) {
            35 -> 90
            50 -> 90
            90 -> 35
            else -> 90
        }
        drawLeicaFrameline(secondaryFocal, isMain = false)
        
        val vignettePaint = android.graphics.Paint().apply {
            shader = android.graphics.RadialGradient(
                centerX, centerY, diagRadius,
                intArrayOf(
                    android.graphics.Color.TRANSPARENT,
                    android.graphics.Color.argb(10, 0, 0, 0),
                    android.graphics.Color.argb(100, 0, 0, 0)
                ),
                floatArrayOf(0.4f, 0.75f, 1.0f),
                android.graphics.Shader.TileMode.CLAMP
            )
            isAntiAlias = true
        }
        nativeCanvas.drawRect(0f, 0f, w, h, vignettePaint)

        // 0.5. LIVE VIEWFINDER FILM GRAIN (12.5 FPS Organic Silver-Halide Flutter)
        val seedInt = (grainSeed * 100000).toInt()
        val random = java.util.Random(seedInt.toLong())
        
        val liveGrainPaint = android.graphics.Paint().apply {
            setStyle(android.graphics.Paint.Style.FILL)
            isAntiAlias = false
        }
        
        val liveBaseGrains = 250
        val liveIsoMultiplier = (isoSensitivityValue.toFloat() / 400f).coerceIn(0.5f, 3.5f)
        val liveGrainCount = (liveBaseGrains * liveIsoMultiplier).toInt()
        
        for (i in 0 until liveGrainCount) {
            val gx = random.nextFloat() * w
            val gy = random.nextFloat() * h
            val sizeVal = (0.5f + random.nextFloat() * 1.5f) * (if (isoSensitivityValue >= 800) 1.5f else 1.0f)
            
            val isLight = random.nextFloat() < 0.15f
            val alpha = random.nextInt(15) + 5
            val rgb = if (isLight) 255 else 0
            
            liveGrainPaint.color = android.graphics.Color.argb(alpha, rgb, rgb, rgb)
            nativeCanvas.drawRect(gx, gy, gx + sizeVal, gy + sizeVal, liveGrainPaint)
        }

        // Real-time Astronomical Constellation Vector Overlay
        if (dialMode == DialMode.AST) {
            val center = Offset(w / 2, h / 2)
            val scale = 110.dp.toPx()
            
            rotate(degrees = astroRotation, pivot = center) {
                // Constellation relative offsets (Orion representation)
                val betelgeuse = Offset(center.x - 0.4f * scale, center.y - 0.5f * scale)
                val bellatrix = Offset(center.x + 0.35f * scale, center.y - 0.45f * scale)
                val rigel = Offset(center.x - 0.35f * scale, center.y + 0.5f * scale)
                val saiph = Offset(center.x + 0.3f * scale, center.y + 0.45f * scale)
                
                val belt1 = Offset(center.x - 0.12f * scale, center.y + 0.02f * scale)
                val belt2 = Offset(center.x, center.y)
                val belt3 = Offset(center.x + 0.12f * scale, center.y - 0.02f * scale)
                
                // Draw connecting vectors
                val lineAlpha = animatedColor.alpha * 0.3f
                val strokeW = 1.dp.toPx()
                
                drawLine(animatedColor.copy(alpha = lineAlpha), betelgeuse, bellatrix, strokeW)
                drawLine(animatedColor.copy(alpha = lineAlpha), bellatrix, belt3, strokeW)
                drawLine(animatedColor.copy(alpha = lineAlpha), betelgeuse, belt1, strokeW)
                drawLine(animatedColor.copy(alpha = lineAlpha), belt1, rigel, strokeW)
                drawLine(animatedColor.copy(alpha = lineAlpha), belt3, saiph, strokeW)
                drawLine(animatedColor.copy(alpha = lineAlpha), rigel, saiph, strokeW)
                
                // Belt segment
                drawLine(animatedColor.copy(alpha = lineAlpha * 1.5f), belt1, belt3, 1.5.dp.toPx())

                // Bright stellar points
                drawCircle(animatedColor, 4.dp.toPx(), betelgeuse)
                drawCircle(animatedColor, 3.5f.dp.toPx(), bellatrix)
                drawCircle(animatedColor, 5.dp.toPx(), rigel)
                drawCircle(animatedColor, 3.5f.dp.toPx(), saiph)
                
                drawCircle(animatedColor, 3.dp.toPx(), belt1)
                drawCircle(animatedColor, 3.dp.toPx(), belt2)
                drawCircle(animatedColor, 3.dp.toPx(), belt3)

                // Calibration ring
                drawCircle(
                    color = animatedColor.copy(alpha = animatedColor.alpha * 0.12f),
                    radius = scale * 0.78f,
                    center = center,
                    style = Stroke(width = 1.dp.toPx())
                )
            }
        }

        // 4. Real-time Telemetry Compass Ribbon & Pitch/Roll Ladder (Aviation/Astronomical HUD style!)
        if (telemetryHudEnabled) {
            // A. COMPASS RIBBON (Horizontal scroll tape at top center of viewport)
            val compassY = pad + 15.dp.toPx()
            val compassWidth = 180.dp.toPx()
            val compassLeft = (w - compassWidth) / 2
            val compassRight = (w + compassWidth) / 2
            val clipPath = androidx.compose.ui.graphics.Path().apply {
                addRect(Rect(compassLeft, compassY - 15.dp.toPx(), compassRight, compassY + 25.dp.toPx()))
            }
            
            // Draw compass backing lines and central tick
            drawLine(
                color = animatedColor.copy(alpha = animatedColor.alpha * 0.4f),
                start = Offset(compassLeft, compassY),
                end = Offset(compassRight, compassY),
                strokeWidth = 1.dp.toPx()
            )
            // Center triangle pointer pointing down
            val pointerPath = androidx.compose.ui.graphics.Path().apply {
                moveTo(w / 2, compassY)
                lineTo(w / 2 - 4.dp.toPx(), compassY - 6.dp.toPx())
                lineTo(w / 2 + 4.dp.toPx(), compassY - 6.dp.toPx())
                close()
            }
            drawPath(pointerPath, color = animatedColor)

            // Clip drawing to compass viewport width
            clipPath(clipPath) {
                // Every 5 degrees draw a tick, every 30 degrees draw a line and text label
                val headingRad = heading
                val pxPerDegree = compassWidth / 60f // 60 degrees of field of view shown
                
                val startH = (headingRad - 30).toInt()
                val endH = (headingRad + 30).toInt()
                
                for (hDeg in startH..endH) {
                    val normDeg = (hDeg + 360) % 360
                    val offsetFromCenter = (hDeg - headingRad) * pxPerDegree
                    val x = (w / 2) + offsetFromCenter
                    
                    if (normDeg % 5 == 0) {
                        val isMajor = normDeg % 30 == 0
                        val tickLen = if (isMajor) 10.dp.toPx() else 5.dp.toPx()
                        
                        drawLine(
                            color = animatedColor.copy(alpha = animatedColor.alpha * (if (isMajor) 0.8f else 0.4f)),
                            start = Offset(x, compassY),
                            end = Offset(x, compassY + tickLen),
                            strokeWidth = (if (isMajor) 1.5f else 1f).dp.toPx()
                        )
                        
                        if (isMajor) {
                            val text = when (normDeg) {
                                0 -> "N"
                                90 -> "E"
                                180 -> "S"
                                270 -> "W"
                                else -> normDeg.toString()
                            }
                            val nativeCanvas = drawContext.canvas.nativeCanvas
                            val textPaint = android.graphics.Paint().apply {
                                color = android.graphics.Color.argb((animatedColor.alpha * 255).toInt(), (animatedColor.red * 255).toInt(), (animatedColor.green * 255).toInt(), (animatedColor.blue * 255).toInt())
                                textSize = 8.sp.toPx()
                                isAntiAlias = true
                                typeface = android.graphics.Typeface.create(android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.BOLD)
                                textAlign = android.graphics.Paint.Align.CENTER
                            }
                            nativeCanvas.drawText(text, x, compassY + tickLen + 10.dp.toPx(), textPaint)
                        }
                    }
                }
            }
            
            // B. PITCH & ROLL LADDER (Centered, aviation style tilt/pitch gauge)
            val centerX = w / 2
            val centerY = h / 2
            val scale = 60.dp.toPx() // pixels per 10 degrees pitch
            
            // Rotate the entire pitch ladder by roll degrees
            rotate(degrees = -roll, pivot = Offset(centerX, centerY)) {
                val lineAlpha = animatedColor.alpha * 0.45f
                val strokeW = 1.2.dp.toPx()
                
                // Draw horizon line at pitch offset
                val horizonY = centerY + (pitch * (scale / 10f))
                
                // Left horizon bracket
                drawLine(
                    color = animatedColor.copy(alpha = lineAlpha),
                    start = Offset(centerX - 80.dp.toPx(), horizonY),
                    end = Offset(centerX - 24.dp.toPx(), horizonY),
                    strokeWidth = strokeW
                )
                drawLine(
                    color = animatedColor.copy(alpha = lineAlpha),
                    start = Offset(centerX - 80.dp.toPx(), horizonY),
                    end = Offset(centerX - 80.dp.toPx(), horizonY + 8.dp.toPx() * (if (pitch >= 0) 1 else -1)),
                    strokeWidth = strokeW
                )
                
                // Right horizon bracket
                drawLine(
                    color = animatedColor.copy(alpha = lineAlpha),
                    start = Offset(centerX + 24.dp.toPx(), horizonY),
                    end = Offset(centerX + 80.dp.toPx(), horizonY),
                    strokeWidth = strokeW
                )
                drawLine(
                    color = animatedColor.copy(alpha = lineAlpha),
                    start = Offset(centerX + 80.dp.toPx(), horizonY),
                    end = Offset(centerX + 80.dp.toPx(), horizonY + 8.dp.toPx() * (if (pitch >= 0) 1 else -1)),
                    strokeWidth = strokeW
                )

                // Draw pitch tick increments (e.g. +10, +20, -10, -20)
                listOf(-20, -10, 10, 20).forEach { pStep ->
                    val stepY = centerY + ((pitch - pStep) * (scale / 10f))
                    val stepAlpha = lineAlpha * (1f - Math.abs(pitch - pStep) / 35f).coerceIn(0f, 1f)
                    
                    if (stepAlpha > 0.05f) {
                        // Left step line
                        drawLine(
                            color = animatedColor.copy(alpha = stepAlpha),
                            start = Offset(centerX - 50.dp.toPx(), stepY),
                            end = Offset(centerX - 24.dp.toPx(), stepY),
                            strokeWidth = strokeW
                        )
                        drawLine(
                            color = animatedColor.copy(alpha = stepAlpha),
                            start = Offset(centerX - 50.dp.toPx(), stepY),
                            end = Offset(centerX - 50.dp.toPx(), stepY + 5.dp.toPx() * (if (pStep > 0) 1 else -1)),
                            strokeWidth = strokeW
                        )

                        // Right step line
                        drawLine(
                            color = animatedColor.copy(alpha = stepAlpha),
                            start = Offset(centerX + 24.dp.toPx(), stepY),
                            end = Offset(centerX + 50.dp.toPx(), stepY),
                            strokeWidth = strokeW
                        )
                        drawLine(
                            color = animatedColor.copy(alpha = stepAlpha),
                            start = Offset(centerX + 50.dp.toPx(), stepY),
                            end = Offset(centerX + 50.dp.toPx(), stepY + 5.dp.toPx() * (if (pStep > 0) 1 else -1)),
                            strokeWidth = strokeW
                        )

                        // Text label for pitch degree step
                        val nativeCanvas = drawContext.canvas.nativeCanvas
                        val textPaint = android.graphics.Paint().apply {
                            color = android.graphics.Color.argb((stepAlpha * 255).toInt(), (animatedColor.red * 255).toInt(), (animatedColor.green * 255).toInt(), (animatedColor.blue * 255).toInt())
                            textSize = 7.sp.toPx()
                            isAntiAlias = true
                            typeface = android.graphics.Typeface.create(android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.NORMAL)
                        }
                        nativeCanvas.drawText(pStep.toString(), centerX - 62.dp.toPx(), stepY + 3.dp.toPx(), textPaint)
                        nativeCanvas.drawText(pStep.toString(), centerX + 54.dp.toPx(), stepY + 3.dp.toPx(), textPaint)
                    }
                }
            }
            
            // Stationary aircraft/scope reticle at center
            drawCircle(animatedColor, 2.dp.toPx(), Offset(centerX, centerY))
            drawLine(animatedColor, Offset(centerX - 15.dp.toPx(), centerY), Offset(centerX - 6.dp.toPx(), centerY), 1.dp.toPx())
            drawLine(animatedColor, Offset(centerX + 6.dp.toPx(), centerY), Offset(centerX + 15.dp.toPx(), centerY), 1.dp.toPx())
            drawLine(animatedColor, Offset(centerX, centerY - 15.dp.toPx()), Offset(centerX, centerY - 6.dp.toPx()), 1.dp.toPx())
            drawLine(animatedColor, Offset(centerX, centerY + 6.dp.toPx()), Offset(centerX, centerY + 15.dp.toPx()), 1.dp.toPx())
        }

        // 3. Viewfinder Reticle styles
        when (style) {
            ReticleStyle.CORNERS -> {
                val len = 24.dp.toPx()
                drawLine(animatedColor, Offset(pad, pad), Offset(pad + len, pad), 2.dp.toPx())
                drawLine(animatedColor, Offset(pad, pad), Offset(pad, pad + len), 2.dp.toPx())
                drawLine(animatedColor, Offset(w - pad, pad), Offset(w - pad - len, pad), 2.dp.toPx())
                drawLine(animatedColor, Offset(w - pad, pad), Offset(w - pad, pad + len), 2.dp.toPx())
                drawLine(animatedColor, Offset(pad, h - pad), Offset(pad + len, h - pad), 2.dp.toPx())
                drawLine(animatedColor, Offset(pad, h - pad), Offset(pad, h - pad - len), 2.dp.toPx())
                drawLine(animatedColor, Offset(w - pad, h - pad), Offset(w - pad - len, h - pad), 2.dp.toPx())
                drawLine(animatedColor, Offset(w - pad, h - pad), Offset(w - pad, h - pad - len), 2.dp.toPx())
            }
            ReticleStyle.CROSSHAIR -> {
                val center = Offset(w / 2, h / 2)
                val arm = 32.dp.toPx()
                val gap = 6.dp.toPx()
                drawLine(animatedColor, Offset(center.x - arm, center.y), Offset(center.x - gap, center.y), 1.5.dp.toPx())
                drawLine(animatedColor, Offset(center.x + gap, center.y), Offset(center.x + arm, center.y), 1.5.dp.toPx())
                drawLine(animatedColor, Offset(center.x, center.y - arm), Offset(center.x, center.y - gap), 1.5.dp.toPx())
                drawLine(animatedColor, Offset(center.x, center.y + gap), Offset(center.x, center.y + arm), 1.5.dp.toPx())
                drawCircle(animatedColor, 3.dp.toPx(), center, style = Stroke(1.dp.toPx()))
            }
            ReticleStyle.GRID -> {
                val col1 = w / 3
                val col2 = (w / 3) * 2
                val row1 = h / 3
                val row2 = (h / 3) * 2

                drawLine(animatedColor.copy(alpha = animatedColor.alpha * 0.4f), Offset(col1, pad), Offset(col1, h - pad), 1.dp.toPx())
                drawLine(animatedColor.copy(alpha = animatedColor.alpha * 0.4f), Offset(col2, pad), Offset(col2, h - pad), 1.dp.toPx())
                drawLine(animatedColor.copy(alpha = animatedColor.alpha * 0.4f), Offset(pad, row1), Offset(w - pad, row1), 1.dp.toPx())
                drawLine(animatedColor.copy(alpha = animatedColor.alpha * 0.4f), Offset(pad, row2), Offset(w - pad, row2), 1.dp.toPx())
            }
            ReticleStyle.MINIMAL -> {
                val notchSize = 8.dp.toPx()
                val midX = w / 2
                val midY = h / 2
                drawLine(animatedColor, Offset(midX, pad), Offset(midX, pad + notchSize), 2.dp.toPx())
                drawLine(animatedColor, Offset(midX, h - pad), Offset(midX, h - pad - notchSize), 2.dp.toPx())
                drawLine(animatedColor, Offset(pad, midY), Offset(pad + notchSize, midY), 2.dp.toPx())
                drawLine(animatedColor, Offset(w - pad, midY), Offset(w - pad - notchSize, midY), 2.dp.toPx())
            }
        }
    }
}

@Composable
fun TelemetryItem(label: String, value: String, color: Color) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Phosphor.Crt.copy(alpha = 0.85f)),
        shape = RoundedCornerShape(2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(2.dp))
    ) {
        Column(modifier = Modifier.padding(6.dp)) {
            Text(label, color = color.copy(alpha = 0.6f), fontSize = 9.sp)
            Text(value, color = color, fontSize = 11.sp, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun UtilityButton(onClick: () -> Unit, text: String, color: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Phosphor.Crt.copy(alpha = 0.85f))
            .border(1.dp, color, RoundedCornerShape(4.dp))
            .clickable { onClick() }
            .padding(vertical = 5.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text.uppercase(),
            color = color,
            fontSize = 8.5.sp,
            style = MaterialTheme.typography.labelSmall
        )
    }
}
