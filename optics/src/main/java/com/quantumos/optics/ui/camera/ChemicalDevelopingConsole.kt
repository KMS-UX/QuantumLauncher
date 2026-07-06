package com.quantumos.optics.ui.camera

import android.content.res.Configuration
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.quantumos.optics.SpoolLog
import com.quantumos.optics.capture.redevelopJpegFile
import com.quantumos.optics.capture.triggerHapticFeedback
import com.quantumos.appshell.Phosphor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ChemicalDevelopingConsole(
    log: SpoolLog,
    themeColor: Color,
    onDismiss: () -> Unit,
    onSaveFinished: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    var contrast by remember { mutableStateOf(1.0f) }
    var exposureOffset by remember { mutableStateOf(0.0f) }
    var grainDensity by remember { mutableStateOf(1.0f) }
    
    var isSaving by remember { mutableStateOf(false) }
    
    // Stepped/discrete "chemical bath" flutter -- static at rest between ticks,
    // not a smooth continuous ambient loop. Flips between two fixed scales on a
    // fixed cadence via snapTo (instant jumps, like a slide-projector click).
    var fluidFlutter by remember { mutableStateOf(1.0f) }
    LaunchedEffect(Unit) {
        while (true) {
            fluidFlutter = 1.0f
            delay(700)
            fluidFlutter = 1.015f
            delay(700)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Phosphor.Crt)
            .clickable(enabled = true, onClick = {}),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(
                // Derived from the existing phosphor tokens -- a theme-tinted glow over
                // the CRT ground, no new arbitrary hex colors introduced.
                brush = androidx.compose.ui.graphics.Brush.radialGradient(
                    colors = listOf(themeColor.copy(alpha = 0.35f), Phosphor.Crt),
                    center = Offset(size.width / 2, size.height / 2),
                    radius = size.width
                )
            )
        }

        val configuration = LocalConfiguration.current
        val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

        // Slightly lighter than the CRT ground, tinted by the active phosphor color --
        // reuses only existing palette tokens, no new arbitrary hex color.
        val cardContainerColor = themeColor.copy(alpha = 0.10f).compositeOver(Phosphor.Crt)
        Card(
            colors = CardDefaults.cardColors(containerColor = cardContainerColor),
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(if (isLandscape) 0.88f else 0.94f)
                .fillMaxHeight(if (isLandscape) 0.88f else 0.92f)
                .border(2.dp, themeColor, RoundedCornerShape(8.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Column {
                            Text(
                                "CHEMICAL DEVELOPING CONSOLE",
                                color = themeColor,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "DARKROOM EMULSION CHEMICAL BALANCES",
                                color = themeColor.copy(alpha = 0.6f),
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Text(
                        "SAFE-LIGHT STATUS: ACTIVE",
                        color = themeColor,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .border(1.dp, themeColor, RoundedCornerShape(2.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (isLandscape) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .border(1.dp, themeColor.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                .background(Color.Black)
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            DeveloperTrayPreview(
                                logUri = Uri.parse(log.uriString),
                                contrast = contrast,
                                exposureOffset = exposureOffset,
                                grainDensity = grainDensity,
                                fluidScale = fluidFlutter,
                                themeColor = themeColor
                            )
                        }

                        Column(
                            modifier = Modifier
                                .weight(1.2f)
                                .fillMaxHeight()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            ChemicalSlidersSection(
                                contrast = contrast,
                                onContrastChange = { contrast = it },
                                exposureOffset = exposureOffset,
                                onExposureOffsetChange = { exposureOffset = it },
                                grainDensity = grainDensity,
                                onGrainDensityChange = { grainDensity = it },
                                themeColor = themeColor
                            )

                            Spacer(modifier = Modifier.weight(1f))

                            ChemicalActionsSection(
                                isSaving = isSaving,
                                themeColor = themeColor,
                                onCancel = onDismiss,
                                onApply = {
                                    isSaving = true
                                    coroutineScope.launch(Dispatchers.IO) {
                                        redevelopJpegFile(
                                            context = context,
                                            uri = Uri.parse(log.uriString),
                                            contrast = contrast,
                                            exposureOffset = exposureOffset,
                                            grainDensity = grainDensity
                                        )
                                        withContext(Dispatchers.Main) {
                                            isSaving = false
                                            triggerHapticFeedback(context)
                                            onSaveFinished()
                                        }
                                    }
                                }
                            )
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1.1f)
                                .fillMaxWidth()
                                .border(1.dp, themeColor.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                .background(Color.Black)
                                .padding(6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            DeveloperTrayPreview(
                                logUri = Uri.parse(log.uriString),
                                contrast = contrast,
                                exposureOffset = exposureOffset,
                                grainDensity = grainDensity,
                                fluidScale = fluidFlutter,
                                themeColor = themeColor
                            )
                        }

                        Column(
                            modifier = Modifier
                                .weight(1.3f)
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            ChemicalSlidersSection(
                                contrast = contrast,
                                onContrastChange = { contrast = it },
                                exposureOffset = exposureOffset,
                                onExposureOffsetChange = { exposureOffset = it },
                                grainDensity = grainDensity,
                                onGrainDensityChange = { grainDensity = it },
                                themeColor = themeColor
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            ChemicalActionsSection(
                                isSaving = isSaving,
                                themeColor = themeColor,
                                onCancel = onDismiss,
                                onApply = {
                                    isSaving = true
                                    coroutineScope.launch(Dispatchers.IO) {
                                        redevelopJpegFile(
                                            context = context,
                                            uri = Uri.parse(log.uriString),
                                            contrast = contrast,
                                            exposureOffset = exposureOffset,
                                            grainDensity = grainDensity
                                        )
                                        withContext(Dispatchers.Main) {
                                            isSaving = false
                                            triggerHapticFeedback(context)
                                            onSaveFinished()
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DeveloperTrayPreview(
    logUri: Uri,
    contrast: Float,
    exposureOffset: Float,
    grainDensity: Float,
    fluidScale: Float,
    themeColor: Color
) {
    val previewMatrix = remember(contrast, exposureOffset) {
        val o = exposureOffset * 2.55f
        androidx.compose.ui.graphics.ColorMatrix(floatArrayOf(
            contrast, 0f, 0f, 0f, o,
            0f, contrast, 0f, 0f, o,
            0f, 0f, contrast, 0f, o,
            0f, 0f, 0f, 1f, 0f
        ))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                scaleX = fluidScale
                scaleY = fluidScale
            },
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = logUri,
            contentDescription = "Development preview",
            colorFilter = androidx.compose.ui.graphics.ColorFilter.colorMatrix(previewMatrix),
            modifier = Modifier
                .fillMaxSize()
                .border(2.dp, themeColor.copy(alpha = 0.3f)),
            contentScale = androidx.compose.ui.layout.ContentScale.Fit
        )

        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val random = java.util.Random()
            val baseDensity = 1000
            val count = (baseDensity * grainDensity).toInt().coerceAtMost(3000)
            
            for (i in 0 until count) {
                val gx = random.nextFloat() * w
                val gy = random.nextFloat() * h
                val sz = 1f + random.nextFloat() * 2f
                val isDark = random.nextFloat() > 0.15f
                val col = if (isDark) Color.Black.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.12f)
                drawRect(col, Offset(gx, gy), Size(sz, sz))
            }
        }
    }
}

@Composable
fun ChemicalActionsSection(
    isSaving: Boolean,
    themeColor: Color,
    onCancel: () -> Unit,
    onApply: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedButton(
            onClick = onCancel,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = themeColor),
            border = BorderStroke(1.dp, themeColor),
            shape = RoundedCornerShape(4.dp),
            modifier = Modifier.weight(1f)
        ) {
            Text("ABORT BATH", fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }

        Button(
            onClick = onApply,
            enabled = !isSaving,
            colors = ButtonDefaults.buttonColors(containerColor = themeColor, contentColor = Phosphor.Crt),
            shape = RoundedCornerShape(4.dp),
            modifier = Modifier.weight(1.2f)
        ) {
            if (isSaving) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Phosphor.Crt, strokeWidth = 2.dp)
            } else {
                Text("FIX EMULSION", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
