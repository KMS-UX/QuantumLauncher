package com.quantumos.optics.ui.camera

import android.content.res.Configuration
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CaptureRequest
import android.net.Uri
import androidx.camera.camera2.interop.Camera2CameraControl
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.CaptureRequestOptions
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import com.quantumos.optics.SpoolDatabase
import com.quantumos.optics.SpoolLog
import com.quantumos.optics.capture.FilmProfile
import com.quantumos.optics.ui.effects.crtPhosphorEffect
import com.quantumos.optics.ui.theme.*
import com.quantumos.appshell.Phosphor
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class DialMode(val label: String, val desc: String) {
    EXP("EXP", "EXPOSURE PARAMETERS"),
    AST("AST", "ASTRONOMICAL ALIGN"),
    NAV("NAV", "GPS NAVIGATION"),
    SYS("SYS", "SPOOL STORAGE")
}

enum class ReticleStyle {
    CORNERS, CROSSHAIR, GRID, MINIMAL
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ViewfinderFoundation(
    themeColor: Color,
    themeColorDim: Color,
    colorName: String,
    frameCounter: Int,
    triggerShutterFlash: Boolean,
    latestCapturedUri: Uri?,
    onFlashComplete: () -> Unit,
    onBindCamera: (ImageCapture) -> Unit,
    onCycleColor: () -> Unit,
    activeFilmProfile: FilmProfile,
    onFilmProfileChange: (FilmProfile) -> Unit,
    burnEnabled: Boolean,
    onBurnEnabledChange: (Boolean) -> Unit,
    telemetryHudEnabled: Boolean,
    onTelemetryHudEnabledChange: (Boolean) -> Unit,
    showSettingsPanel: Boolean,
    onShowSettingsPanelChange: (Boolean) -> Unit,
    shutterSpeedMultiplier: Float,
    onShutterSpeedMultiplierChange: (Float) -> Unit,
    isoSensitivityValue: Int,
    onIsoSensitivityValueChange: (Int) -> Unit,
    latitude: Double,
    longitude: Double,
    onCoordsChange: (Double, Double) -> Unit,
    altitude: Double,
    onAltitudeChange: (Double) -> Unit,
    heading: Float,
    pitch: Float,
    roll: Float,
    doubleExposureEnabled: Boolean,
    onDoubleExposureEnabledChange: (Boolean) -> Unit,
    firstDoubleExposureUri: Uri?,
    onDevelopLog: (SpoolLog) -> Unit,
    goodOldTimesEnabled: Boolean,
    onGoodOldTimesEnabledChange: (Boolean) -> Unit,
    chemicalDevDelayEnabled: Boolean,
    onChemicalDevDelayEnabledChange: (Boolean) -> Unit,
    activeFocalLength: Int,
    onFocalLengthChange: (Int) -> Unit,
    activeFocusDistance: Float,
    onFocusDistanceChange: (Float) -> Unit,
    subjectDistance: Float,
    useSensorForSubjectDistance: Boolean,
    onUseSensorChange: (Boolean) -> Unit,
    simulatedSubjectDistance: Float,
    onSimulatedSubjectDistanceChange: (Float) -> Unit
) {
    val cameraPermissionState = rememberPermissionState(android.Manifest.permission.CAMERA)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Phosphor.Crt),
        contentAlignment = Alignment.Center
    ) {
        if (cameraPermissionState.status.isGranted) {
            CameraFeedAndDashboard(
                themeColor = themeColor,
                themeColorDim = themeColorDim,
                colorName = colorName,
                frameCounter = frameCounter,
                triggerShutterFlash = triggerShutterFlash,
                latestCapturedUri = latestCapturedUri,
                onFlashComplete = onFlashComplete,
                onBindCamera = onBindCamera,
                onCycleColor = onCycleColor,
                activeFilmProfile = activeFilmProfile,
                onFilmProfileChange = onFilmProfileChange,
                burnEnabled = burnEnabled,
                onBurnEnabledChange = onBurnEnabledChange,
                telemetryHudEnabled = telemetryHudEnabled,
                onTelemetryHudEnabledChange = onTelemetryHudEnabledChange,
                showSettingsPanel = showSettingsPanel,
                onShowSettingsPanelChange = onShowSettingsPanelChange,
                shutterSpeedMultiplier = shutterSpeedMultiplier,
                onShutterSpeedMultiplierChange = onShutterSpeedMultiplierChange,
                isoSensitivityValue = isoSensitivityValue,
                onIsoSensitivityValueChange = onIsoSensitivityValueChange,
                latitude = latitude,
                longitude = longitude,
                onCoordsChange = onCoordsChange,
                altitude = altitude,
                onAltitudeChange = onAltitudeChange,
                heading = heading,
                pitch = pitch,
                roll = roll,
                doubleExposureEnabled = doubleExposureEnabled,
                onDoubleExposureEnabledChange = onDoubleExposureEnabledChange,
                firstDoubleExposureUri = firstDoubleExposureUri,
                onDevelopLog = onDevelopLog,
                goodOldTimesEnabled = goodOldTimesEnabled,
                onGoodOldTimesEnabledChange = onGoodOldTimesEnabledChange,
                chemicalDevDelayEnabled = chemicalDevDelayEnabled,
                onChemicalDevDelayEnabledChange = onChemicalDevDelayEnabledChange,
                activeFocalLength = activeFocalLength,
                onFocalLengthChange = onFocalLengthChange,
                activeFocusDistance = activeFocusDistance,
                onFocusDistanceChange = onFocusDistanceChange,
                subjectDistance = subjectDistance,
                useSensorForSubjectDistance = useSensorForSubjectDistance,
                onUseSensorChange = onUseSensorChange,
                simulatedSubjectDistance = simulatedSubjectDistance,
                onSimulatedSubjectDistanceChange = onSimulatedSubjectDistanceChange
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    "SYSTEM WARNING",
                    color = Phosphor.Warn,
                    fontSize = 24.sp,
                    style = MaterialTheme.typography.headlineMedium
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "OPTICAL VIEWPORT ACCESS EXPIRED OR UNSET.",
                    color = themeColor,
                    fontSize = 14.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Text(
                    "CAMERA HARDWARE INTENT REQUIRED.",
                    color = themeColor,
                    fontSize = 14.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(32.dp))
                Button(
                    onClick = { cameraPermissionState.launchPermissionRequest() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = themeColor
                    ),
                    modifier = Modifier.border(1.dp, themeColor, RoundedCornerShape(4.dp))
                ) {
                    Text("GRANT ACCESS INTENT", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun CameraFeedAndDashboard(
    themeColor: Color,
    themeColorDim: Color,
    colorName: String,
    frameCounter: Int,
    triggerShutterFlash: Boolean,
    latestCapturedUri: Uri?,
    onFlashComplete: () -> Unit,
    onBindCamera: (ImageCapture) -> Unit,
    onCycleColor: () -> Unit,
    activeFilmProfile: FilmProfile,
    onFilmProfileChange: (FilmProfile) -> Unit,
    burnEnabled: Boolean,
    onBurnEnabledChange: (Boolean) -> Unit,
    telemetryHudEnabled: Boolean,
    onTelemetryHudEnabledChange: (Boolean) -> Unit,
    showSettingsPanel: Boolean,
    onShowSettingsPanelChange: (Boolean) -> Unit,
    shutterSpeedMultiplier: Float,
    onShutterSpeedMultiplierChange: (Float) -> Unit,
    isoSensitivityValue: Int,
    onIsoSensitivityValueChange: (Int) -> Unit,
    latitude: Double,
    longitude: Double,
    onCoordsChange: (Double, Double) -> Unit,
    altitude: Double,
    onAltitudeChange: (Double) -> Unit,
    heading: Float,
    pitch: Float,
    roll: Float,
    doubleExposureEnabled: Boolean,
    onDoubleExposureEnabledChange: (Boolean) -> Unit,
    firstDoubleExposureUri: Uri?,
    onDevelopLog: (SpoolLog) -> Unit,
    goodOldTimesEnabled: Boolean,
    onGoodOldTimesEnabledChange: (Boolean) -> Unit,
    chemicalDevDelayEnabled: Boolean,
    onChemicalDevDelayEnabledChange: (Boolean) -> Unit,
    activeFocalLength: Int,
    onFocalLengthChange: (Int) -> Unit,
    activeFocusDistance: Float,
    onFocusDistanceChange: (Float) -> Unit,
    subjectDistance: Float,
    useSensorForSubjectDistance: Boolean,
    onUseSensorChange: (Boolean) -> Unit,
    simulatedSubjectDistance: Float,
    onSimulatedSubjectDistanceChange: (Float) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    var lastTapOffset by remember { mutableStateOf<Offset?>(null) }
    var tapFeedbackVisible by remember { mutableStateOf(false) }
    LaunchedEffect(lastTapOffset) {
        if (lastTapOffset != null) {
            tapFeedbackVisible = true
            delay(2500)
            tapFeedbackVisible = false
        }
    }

    val monoValues = remember {
        floatArrayOf(
            0.299f * 1.35f, 0.587f * 1.35f, 0.114f * 1.35f, 0f, -0.05f,
            0.299f * 1.35f, 0.587f * 1.35f, 0.114f * 1.35f, 0f, -0.05f,
            0.299f * 1.35f, 0.587f * 1.35f, 0.114f * 1.35f, 0f, -0.05f,
            0f, 0f, 0f, 1f, 0f
        )
    }
    val colorValues = remember {
        floatArrayOf(
            1.15f, 0.05f, 0.0f,  0f, 0.03f,
            0.05f, 1.10f, 0.0f,  0f, 0.02f,
            0.0f,  0.05f, 0.90f, 0f, -0.01f,
            0f,    0f,    0f,    1f, 0f
        )
    }

    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    var reticleStyle by remember { mutableStateOf(ReticleStyle.CORNERS) }
    var bracketAlpha by remember { mutableStateOf(1f) }

    // Retrieve Spool Memory logs reactively from database
    val spoolLogsState = remember {
        SpoolDatabase.getDatabase(context).spoolLogDao().getAllLogs()
    }.collectAsState(initial = emptyList())
    val spoolLogs = spoolLogsState.value

    // Astronomical alignment parameters
    var siderealTimeH by remember { mutableStateOf(14) }
    var siderealTimeM by remember { mutableStateOf(29) }
    var siderealTimeS by remember { mutableStateOf(45) }
    var declinationDeg by remember { mutableStateOf(-62) }
    var declinationMin by remember { mutableStateOf(40) }
    var activeConstellation by remember { mutableStateOf("ORION ALPHA RISING") }

    // Shutter curtain sweep animation state
    val shutterProgress = remember { androidx.compose.animation.core.Animatable(0f) }

    // Rotary Dial Mode
    var activeDialMode by remember { mutableStateOf(DialMode.EXP) }

    // Active Camera instance for hardware adjustments
    var activeCameraInstance by remember { mutableStateOf<androidx.camera.core.Camera?>(null) }

    // Astro continuous rotation drive
    var astroRotation by remember { mutableStateOf(0f) }
    LaunchedEffect(activeDialMode) {
        if (activeDialMode == DialMode.AST) {
            while (true) {
                delay(16) // ~60fps smooth sweep
                astroRotation = (astroRotation + 0.15f) % 360f
            }
        }
    }

    // High ISO film grain noise seeds
    var grainSeed by remember { mutableStateOf(0f) }
    LaunchedEffect(isoSensitivityValue) {
        if (isoSensitivityValue > 100) {
            while (true) {
                delay(80) // 12.5 fps vintage grain flutter
                grainSeed = Random.nextFloat()
            }
        }
    }

    // CRT phosphor GPU shader (scanlines + vignette + flicker) time uniform.
    // Stepped/discrete counter -- not a smooth per-frame animation -- so the
    // "motion" feeding the shader stays in the spirit of "static at rest".
    var crtShaderTime by remember { mutableStateOf(0f) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(120)
            crtShaderTime += 0.15f
        }
    }

    // Dynamic Viewfinder Optical Zoom ratio control based on selected focal length
    LaunchedEffect(activeFocalLength, activeCameraInstance) {
        val cam = activeCameraInstance
        if (cam != null) {
            val zoomRatio = when (activeFocalLength) {
                35 -> 1.0f
                50 -> 1.4f
                90 -> 2.6f
                else -> 1.4f
            }
            try {
                cam.cameraControl.setZoomRatio(zoomRatio)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Dynamic Hardware Exposure Compensation driver
    LaunchedEffect(shutterSpeedMultiplier, activeCameraInstance) {
        val cam = activeCameraInstance
        if (cam != null) {
            val index = when (shutterSpeedMultiplier) {
                0.5f -> 2
                1.0f -> 0
                2.0f -> -2
                4.0f -> -4
                else -> 0
            }
            try {
                cam.cameraControl.setExposureCompensationIndex(index)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Dynamic Hardware Manual Focus control
    @OptIn(ExperimentalCamera2Interop::class)
    LaunchedEffect(activeFocusDistance, activeCameraInstance) {
        val cam = activeCameraInstance
        if (cam != null) {
            try {
                val camera2CameraInfo = Camera2CameraInfo.from(cam.cameraInfo)
                val minFocusDistance = camera2CameraInfo.getCameraCharacteristic(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE) ?: 0.0f
                
                if (minFocusDistance > 0.0f) {
                    val camera2CameraControl = Camera2CameraControl.from(cam.cameraControl)
                    
                    // Diopter is reciprocal of distance in meters. 
                    // If near infinity (>= 9.9f), set diopters to 0f for exact infinity.
                    val targetDiopters = if (activeFocusDistance >= 9.9f) {
                        0.0f
                    } else {
                        (1.0f / activeFocusDistance).coerceIn(0.0f, minFocusDistance)
                    }
                    
                    val builder = CaptureRequestOptions.Builder()
                    // Set focus mode to manual (OFF)
                    builder.setCaptureRequestOption(
                        CaptureRequest.CONTROL_AF_MODE,
                        CaptureRequest.CONTROL_AF_MODE_OFF
                    )
                    // Set the lens focus distance in diopters
                    builder.setCaptureRequestOption(
                        CaptureRequest.LENS_FOCUS_DISTANCE,
                        targetDiopters
                    )
                    camera2CameraControl.captureRequestOptions = builder.build()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    LaunchedEffect(triggerShutterFlash) {
        if (triggerShutterFlash) {
            shutterProgress.snapTo(0f)
            shutterProgress.animateTo(
                targetValue = 2f,
                animationSpec = androidx.compose.animation.core.tween(
                    durationMillis = 320,
                    easing = androidx.compose.animation.core.LinearEasing
                )
            )
            shutterProgress.snapTo(0f)
            onFlashComplete()
        }
    }

    // Astro / Sidereal clocks loop
    LaunchedEffect(Unit) {
        val constellations = listOf(
            "ORION ALPHA RISING", 
            "CYGNUS SECTOR NE", 
            "TAURUS EYE TRANSIT", 
            "SCORPIUS APEX LOCK", 
            "PLEIADES INTEGRITY", 
            "URSA MINOR ZENITH", 
            "CENTAURI BEACON CLAMP"
        )
        while (true) {
            delay(1000)
            siderealTimeS += 1
            if (siderealTimeS >= 60) {
                siderealTimeS = 0
                siderealTimeM += 1
                if (siderealTimeM >= 60) {
                    siderealTimeM = 0
                    siderealTimeH = (siderealTimeH + 1) % 24
                }
            }
            if (Random.nextFloat() < 0.15f) {
                activeConstellation = constellations.random()
            }
            declinationMin = (declinationMin + (Random.nextInt(-1, 2))) % 60
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    onUseSensorChange(false)
                    val relativeY = offset.y / size.height
                    val tappedDist = ((1.0f - relativeY) * 9.3f + 0.7f).coerceIn(0.7f, 10.0f)
                    onSimulatedSubjectDistanceChange(tappedDist)
                    lastTapOffset = offset
                }
            }
    ) {
        val liveFocusError = Math.abs(activeFocusDistance - subjectDistance)
        val liveBlurRadius = (liveFocusError * 6.0f).coerceIn(0f, 25f)

        // Camera Viewfinder Stream with active Film Simulation Filter
        Box(
            modifier = Modifier
                .fillMaxSize()
                .blur(if (liveBlurRadius > 0.1f) liveBlurRadius.dp else 0.dp)
                .drawWithContent {
                    val matrix = if (activeFilmProfile == FilmProfile.BW_TRI_X) {
                        androidx.compose.ui.graphics.ColorMatrix(monoValues)
                    } else {
                        androidx.compose.ui.graphics.ColorMatrix(colorValues)
                    }
                    val paint = Paint().apply {
                        colorFilter = androidx.compose.ui.graphics.ColorFilter.colorMatrix(matrix)
                    }
                    drawIntoCanvas { canvas ->
                        canvas.saveLayer(Rect(0f, 0f, size.width, size.height), paint)
                        drawContent()
                        canvas.restore()
                    }
                }
                .crtPhosphorEffect(themeColor = themeColor, time = crtShaderTime)
        ) {
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx).apply {
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    }
                    val executor = ContextCompat.getMainExecutor(ctx)
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }
                        val imageCapture = ImageCapture.Builder()
                            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                            .build()
                        
                        onBindCamera(imageCapture)
                        
                        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                        try {
                            cameraProvider.unbindAll()
                            val camera = cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview,
                                imageCapture
                            )
                            activeCameraInstance = camera
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }, executor)
                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Custom reticle
        ReticleOverlay(
            style = reticleStyle,
            themeColor = themeColor,
            bracketAlpha = bracketAlpha,
            dialMode = activeDialMode,
            astroRotation = astroRotation,
            isoSensitivityValue = isoSensitivityValue,
            grainSeed = grainSeed,
            heading = heading,
            pitch = pitch,
            roll = roll,
            telemetryHudEnabled = telemetryHudEnabled,
            activeFocalLength = activeFocalLength,
            activeFocusDistance = activeFocusDistance,
            subjectDistance = subjectDistance
        )

        // Pulser target overlay for viewfinder tap
        if (tapFeedbackVisible && lastTapOffset != null) {
            val offset = lastTapOffset!!
            Box(
                modifier = Modifier
                    .offset(
                        x = (offset.x / androidx.compose.ui.platform.LocalDensity.current.density).dp - 24.dp,
                        y = (offset.y / androidx.compose.ui.platform.LocalDensity.current.density).dp - 24.dp
                    )
                    .size(48.dp)
                    .border(1.dp, themeColor, androidx.compose.foundation.shape.CircleShape),
                contentAlignment = Alignment.Center
            ) {
                // Center dot
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .background(themeColor, androidx.compose.foundation.shape.CircleShape)
                )
                // Text label below
                Text(
                    text = "TARGET: ${String.format("%.1f", simulatedSubjectDistance)}m",
                    color = themeColor,
                    fontSize = 8.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .offset(y = 12.dp)
                        .background(Phosphor.Crt.copy(alpha = 0.85f), androidx.compose.foundation.shape.RoundedCornerShape(2.dp))
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                )
            }
        }

        // ----------------- TACTILE DASHBOARD LAYOUT (Adaptive for Portrait & Landscape) -----------------
        val firstConsoleModifier = if (isLandscape) {
            Modifier
                .align(Alignment.CenterStart)
                .padding(start = 24.dp)
                .width(240.dp)
        } else {
            Modifier
                .align(Alignment.TopCenter)
                .padding(top = 96.dp)
                .width(320.dp)
        }

        val secondConsoleModifier = if (isLandscape) {
            Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 120.dp)
                .width(240.dp)
        } else {
            Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 124.dp)
                .width(320.dp)
        }

        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // 1. TOP TELEMETRY CONSOLE (Clean, focused primary readings)
            Box(
                modifier = firstConsoleModifier
                    .background(Phosphor.Crt.copy(alpha = 0.85f))
                    .border(1.dp, themeColor.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                    .padding(12.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "QUANTUM HUD REGISTER - MODEL 1950A",
                            color = themeColor.copy(alpha = 0.6f),
                            fontSize = 9.sp
                        )
                        Text("REACTOR STABLE", color = themeColor, fontSize = 9.sp)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    when (activeDialMode) {
                        DialMode.EXP -> {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text("SHUTTER SPEED", color = themeColor.copy(alpha = 0.6f), fontSize = 8.sp)
                                    val calculatedShutter = when (shutterSpeedMultiplier) {
                                        0.5f -> "1/60"
                                        1.0f -> "1/125"
                                        2.0f -> "1/250"
                                        4.0f -> "1/500"
                                        else -> "1/125"
                                    }
                                    Text(calculatedShutter, color = themeColor, fontSize = 16.sp, style = MaterialTheme.typography.bodyLarge)
                                }
                                Column {
                                    Text("APERTURE", color = themeColor.copy(alpha = 0.6f), fontSize = 8.sp)
                                    Text("f/5.6 FIXED", color = themeColor, fontSize = 16.sp, style = MaterialTheme.typography.bodyLarge)
                                }
                                Column {
                                    Text("LIGHT METER", color = themeColor.copy(alpha = 0.6f), fontSize = 8.sp)
                                    Text("+0.3 EV", color = themeColor, fontSize = 16.sp, style = MaterialTheme.typography.bodyLarge)
                                }
                            }
                        }
                        DialMode.AST -> {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text("CONSTELLATION TARGET", color = themeColor.copy(alpha = 0.6f), fontSize = 8.sp)
                                    Text(activeConstellation, color = themeColor, fontSize = 13.sp, style = MaterialTheme.typography.bodyLarge)
                                }
                                Column {
                                    Text("DECLINATION", color = themeColor.copy(alpha = 0.6f), fontSize = 8.sp)
                                    Text(String.format("DEC %02d° %02d'", declinationDeg, Math.abs(declinationMin)), color = themeColor, fontSize = 13.sp, style = MaterialTheme.typography.bodyLarge)
                                }
                            }
                        }
                        DialMode.NAV -> {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text("COORDINATES", color = themeColor.copy(alpha = 0.6f), fontSize = 8.sp)
                                    Text(String.format("%.4f°N, %.4f°E", latitude, longitude), color = themeColor, fontSize = 14.sp, style = MaterialTheme.typography.bodyLarge)
                                }
                                Column {
                                    Text("ALTITUDE", color = themeColor.copy(alpha = 0.6f), fontSize = 8.sp)
                                    Text(String.format("%.1f M", altitude), color = themeColor, fontSize = 14.sp, style = MaterialTheme.typography.bodyLarge)
                                }
                            }
                        }
                        DialMode.SYS -> {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text("SPOOL RADIATION CORE", color = themeColor.copy(alpha = 0.6f), fontSize = 8.sp)
                                    Text(String.format("EXP %03d / 036", frameCounter), color = themeColor, fontSize = 15.sp, style = MaterialTheme.typography.bodyLarge)
                                }
                                Column {
                                    Text("SPOOL DECAY CORE", color = themeColor.copy(alpha = 0.6f), fontSize = 8.sp)
                                    Text("98.4% AVAILABLE", color = themeColor, fontSize = 15.sp, style = MaterialTheme.typography.bodyLarge)
                                }
                            }
                        }
                    }
                }
            }

            // 2. BOTTOM TELEMETRY CONSOLE (Symmetric, perfectly aligned parameter overrides in Leica style)
            Box(
                modifier = secondConsoleModifier
                    .background(Phosphor.Crt.copy(alpha = 0.85f))
                    .border(1.dp, themeColor.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                    .padding(12.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "ATOMIC HARDWARE BRACKETS",
                            color = themeColor.copy(alpha = 0.6f),
                            fontSize = 9.sp
                        )
                        Text("CORE SYMMETRY", color = themeColor, fontSize = 9.sp)
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    when (activeDialMode) {
                        DialMode.EXP -> {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                // Row 1: SENSOR ISO
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "SENSOR ISO",
                                        color = themeColor.copy(alpha = 0.6f),
                                        fontSize = 8.sp,
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                        modifier = Modifier.width(80.dp)
                                    )
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        listOf(100, 200, 400, 800).forEach { iso ->
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .background(if (isoSensitivityValue == iso) themeColor else Color.Transparent)
                                                    .border(1.dp, themeColor.copy(alpha = 0.8f), RoundedCornerShape(1.dp))
                                                    .clickable { onIsoSensitivityValueChange(iso) }
                                                    .padding(vertical = 4.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    iso.toString(),
                                                    color = if (isoSensitivityValue == iso) Phosphor.Crt else themeColor,
                                                    fontSize = 7.5.sp,
                                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }

                                // Row 2: SHUTTER PRESET SPEED
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "SHUTTER",
                                        color = themeColor.copy(alpha = 0.6f),
                                        fontSize = 8.sp,
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                        modifier = Modifier.width(80.dp)
                                    )
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        listOf(0.5f, 1.0f, 2.0f, 4.0f).forEach { speed ->
                                            val display = when (speed) {
                                                0.5f -> "1/60"
                                                1.0f -> "1/125"
                                                2.0f -> "1/250"
                                                else -> "1/500"
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .background(if (shutterSpeedMultiplier == speed) themeColor else Color.Transparent)
                                                    .border(1.dp, themeColor.copy(alpha = 0.8f), RoundedCornerShape(1.dp))
                                                    .clickable { onShutterSpeedMultiplierChange(speed) }
                                                    .padding(vertical = 4.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    display,
                                                    color = if (shutterSpeedMultiplier == speed) Phosphor.Crt else themeColor,
                                                    fontSize = 7.5.sp,
                                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }

                                // Row 3: FILM PROFILE EMULATION
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "EMULATION",
                                        color = themeColor.copy(alpha = 0.6f),
                                        fontSize = 8.sp,
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                        modifier = Modifier.width(80.dp)
                                    )
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        FilmProfile.values().forEach { profile ->
                                            val shortLabel = when (profile) {
                                                FilmProfile.BW_TRI_X -> "LEICA B&W (TRI-X)"
                                                FilmProfile.COL_PORTRA -> "LEICA COLOR (PORTRA)"
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .background(if (activeFilmProfile == profile) themeColor else Color.Transparent)
                                                    .border(1.dp, themeColor.copy(alpha = 0.8f), RoundedCornerShape(1.dp))
                                                    .clickable { onFilmProfileChange(profile) }
                                                    .padding(vertical = 4.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    shortLabel,
                                                    color = if (activeFilmProfile == profile) Phosphor.Crt else themeColor,
                                                    fontSize = 7.5.sp,
                                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Row 4: VIEWFINDER FRAMELINES
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "FRAMELINES",
                                        color = themeColor.copy(alpha = 0.6f),
                                        fontSize = 8.sp,
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                        modifier = Modifier.width(80.dp)
                                    )
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        listOf(35, 50, 90).forEach { fl ->
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .background(if (activeFocalLength == fl) themeColor else Color.Transparent)
                                                    .border(1.dp, themeColor.copy(alpha = 0.8f), RoundedCornerShape(1.dp))
                                                    .clickable { onFocalLengthChange(fl) }
                                                    .padding(vertical = 4.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    "${fl}mm",
                                                    color = if (activeFocalLength == fl) Phosphor.Crt else themeColor,
                                                    fontSize = 7.5.sp,
                                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Row 5: MANUAL RANGEFINDER FOCUS DIAL
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "RANGEFINDER",
                                        color = themeColor.copy(alpha = 0.6f),
                                        fontSize = 8.sp,
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                        modifier = Modifier.width(80.dp)
                                    )
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        listOf(0.7f, 1.2f, 2.0f, 5.0f, 10.0f).forEach { fd ->
                                            val label = when (fd) {
                                                0.7f -> "0.7m"
                                                1.2f -> "1.2m"
                                                2.0f -> "2.0m"
                                                5.0f -> "5.0m"
                                                else -> "∞"
                                            }
                                            val isSelected = Math.abs(activeFocusDistance - fd) < 0.05f
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .background(if (isSelected) themeColor else Color.Transparent)
                                                    .border(1.dp, themeColor.copy(alpha = 0.8f), RoundedCornerShape(1.dp))
                                                    .clickable { onFocusDistanceChange(fd) }
                                                    .padding(vertical = 4.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    label,
                                                    color = if (isSelected) Phosphor.Crt else themeColor,
                                                    fontSize = 7.5.sp,
                                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                // Row 5.5: FINE HELICOID FOCUS RING
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "FOCUS RING",
                                        color = themeColor.copy(alpha = 0.6f),
                                        fontSize = 8.sp,
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                        modifier = Modifier.width(80.dp)
                                    )
                                    Slider(
                                        value = activeFocusDistance,
                                        onValueChange = { onFocusDistanceChange(it) },
                                        valueRange = 0.7f..10.0f,
                                        colors = SliderDefaults.colors(
                                            thumbColor = themeColor,
                                            activeTrackColor = themeColor,
                                            inactiveTrackColor = themeColor.copy(alpha = 0.25f)
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(24.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Row 6: SUBJECT DISTANCE CONTROL
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "SUBJECT DIST",
                                        color = themeColor.copy(alpha = 0.6f),
                                        fontSize = 8.sp,
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                        modifier = Modifier.width(80.dp)
                                    )
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        listOf(0.7f, 1.5f, 3.5f, 10.0f).forEach { sd ->
                                            val label = when (sd) {
                                                0.7f -> "🌸 0.7m"
                                                1.5f -> "👤 1.5m"
                                                3.5f -> "🚴 3.5m"
                                                else -> "🏔️ ∞"
                                            }
                                            val isSelected = !useSensorForSubjectDistance && Math.abs(simulatedSubjectDistance - sd) < 0.1f
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .background(if (isSelected) themeColor else Color.Transparent)
                                                    .border(1.dp, themeColor.copy(alpha = 0.8f), RoundedCornerShape(1.dp))
                                                    .clickable { 
                                                        onUseSensorChange(false)
                                                        onSimulatedSubjectDistanceChange(sd)
                                                    }
                                                    .padding(vertical = 4.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    label,
                                                    color = if (isSelected) Phosphor.Crt else themeColor,
                                                    fontSize = 7.5.sp,
                                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                                )
                                            }
                                        }

                                        val isSensorSelected = useSensorForSubjectDistance
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .background(if (isSensorSelected) themeColor else Color.Transparent)
                                                .border(1.dp, themeColor.copy(alpha = 0.8f), RoundedCornerShape(1.dp))
                                                .clickable { 
                                                    onUseSensorChange(true)
                                                }
                                                .padding(vertical = 4.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                "📱 TILT",
                                                color = if (isSensorSelected) Phosphor.Crt else themeColor,
                                                fontSize = 7.5.sp,
                                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        DialMode.AST -> {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text("SIDEREAL RATE CLOCK", color = themeColor.copy(alpha = 0.6f), fontSize = 8.sp)
                                    Text(String.format("%02dh %02dm %02ds", siderealTimeH, siderealTimeM, siderealTimeS), color = themeColor, fontSize = 14.sp)
                                }
                                Column {
                                    Text("ASTROALIGN MOTOR", color = themeColor.copy(alpha = 0.6f), fontSize = 8.sp)
                                    Text("SYNC NOMINAL", color = themeColor, fontSize = 14.sp)
                                }
                            }
                        }
                        DialMode.NAV -> {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text("BEARING HEADING", color = themeColor.copy(alpha = 0.6f), fontSize = 8.sp)
                                    Text(String.format("%03.1f° SW", heading), color = themeColor, fontSize = 14.sp)
                                }
                                Column {
                                    Text("GPS SAT INTEGRITY", color = themeColor.copy(alpha = 0.6f), fontSize = 8.sp)
                                    Text("HIGH SIGNAL (6)", color = themeColor, fontSize = 14.sp)
                                }
                            }
                        }
                        DialMode.SYS -> {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "RECOVERED EMULSION CORES (RAD-SECURED) (${spoolLogs.size} EXPOSURES)",
                                        color = themeColor,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    
                                    if (spoolLogs.isNotEmpty()) {
                                        Text(
                                            "CLEAR SPOOL",
                                            color = Phosphor.Warn,
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier
                                                .border(1.dp, Phosphor.Warn, RoundedCornerShape(2.dp))
                                                .clickable {
                                                    coroutineScope.launch(Dispatchers.IO) {
                                                        SpoolDatabase.getDatabase(context).spoolLogDao().deleteAllLogs()
                                                    }
                                                }
                                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                if (spoolLogs.isEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(86.dp)
                                            .border(1.dp, themeColor.copy(alpha = 0.3f), RoundedCornerShape(2.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "SPOOL IS EMPTY. ENGAGE SHUTTER.",
                                            color = themeColor.copy(alpha = 0.5f),
                                            fontSize = 9.sp,
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                } else {
                                    androidx.compose.foundation.lazy.LazyRow(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(86.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        items(spoolLogs.size) { index ->
                                            val log = spoolLogs[spoolLogs.size - 1 - index] // show latest first
                                            val logUri = Uri.parse(log.uriString)
                                            
                                            // Real film frame container with retro sprocket holes
                                            Column(
                                                modifier = Modifier
                                                    .width(135.dp)
                                                    .background(Color.Black)
                                                    .border(1.dp, themeColor.copy(alpha = 0.6f))
                                                    .clickable { onDevelopLog(log) }
                                                    .padding(vertical = 4.dp, horizontal = 6.dp)
                                            ) {
                                                // Top Sprocket Holes
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    repeat(4) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(width = 6.dp, height = 4.dp)
                                                                .background(Phosphor.Crt)
                                                                .border(0.5.dp, themeColor.copy(alpha = 0.3f))
                                                        )
                                                    }
                                                }
                                                
                                                Spacer(modifier = Modifier.height(3.dp))
                                                
                                                // Captured thumbnail & info row
                                                Row(
                                                    modifier = Modifier.weight(1f),
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    val isCurrentlyDeveloping = log.isDeveloping && (System.currentTimeMillis() - log.devStartTime < 8000L)

                                                    // Thumbnail or Developing Placeholder
                                                    if (isCurrentlyDeveloping) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(42.dp)
                                                                .background(Color.Black)
                                                                .border(0.5.dp, themeColor.copy(alpha = 0.8f)),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                                Text("⚗️", color = themeColor, fontSize = 12.sp)
                                                                Text("DEV", color = themeColor, fontSize = 6.sp, fontWeight = FontWeight.Bold)
                                                            }
                                                        }
                                                    } else {
                                                        AsyncImage(
                                                            model = logUri,
                                                            contentDescription = "Film thumbnail",
                                                            colorFilter = androidx.compose.ui.graphics.ColorFilter.colorMatrix(
                                                                androidx.compose.ui.graphics.ColorMatrix(
                                                                    if (activeFilmProfile == FilmProfile.BW_TRI_X) monoValues else colorValues
                                                                )
                                                            ),
                                                            modifier = Modifier
                                                                .size(42.dp)
                                                                .border(0.5.dp, themeColor.copy(alpha = 0.4f)),
                                                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                                        )
                                                    }
                                                    
                                                    // Info terminal
                                                    Column(
                                                        verticalArrangement = Arrangement.spacedBy(1.dp)
                                                    ) {
                                                        Text("#${String.format("%02d", spoolLogs.size - index)}  ${log.filmProfile}", color = themeColor, fontSize = 7.sp, fontWeight = FontWeight.Bold)
                                                        Text("ISO ${log.iso} ${log.shutterSpeed}", color = themeColor.copy(alpha = 0.8f), fontSize = 6.sp)
                                                        Text(String.format("%.2fN", log.latitude), color = themeColor.copy(alpha = 0.6f), fontSize = 6.sp)
                                                        Text(String.format("%.2fE", log.longitude), color = themeColor.copy(alpha = 0.6f), fontSize = 6.sp)
                                                        if (isCurrentlyDeveloping) {
                                                            Text("⚗️ PROCESSING", color = themeColor, fontSize = 5.5.sp, fontWeight = FontWeight.Bold)
                                                        } else {
                                                            Text("⚗️ DEVELOP", color = themeColor, fontSize = 5.5.sp, fontWeight = FontWeight.SemiBold)
                                                        }
                                                    }
                                                }
                                                
                                                Spacer(modifier = Modifier.height(3.dp))

                                                // Bottom Sprocket Holes
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    repeat(4) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(width = 6.dp, height = 4.dp)
                                                                .background(Phosphor.Crt)
                                                                .border(0.5.dp, themeColor.copy(alpha = 0.3f))
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 3. METICULOUS VINTAGE ROTARY DIAL (Mode Swapper - Shrunken slightly to 76.dp and moved slightly inward)
            Box(
                modifier = (if (isLandscape) {
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 48.dp, end = 24.dp)
                        .size(76.dp)
                } else {
                    Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 4.dp)
                        .size(76.dp)
                })
                .background(Phosphor.Crt.copy(alpha = 0.9f), CircleShape)
                .border(2.dp, themeColor, CircleShape)
                .clickable {
                    // Tap on dial to cycle mode with a retro feel
                    val modes = DialMode.values()
                    val nextIndex = (activeDialMode.ordinal + 1) % modes.size
                    activeDialMode = modes[nextIndex]
                },
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = Offset(size.width / 2, size.height / 2)
                    val radius = size.width / 2
                    
                    // Rotational angle depending on mode
                    val targetAngle = when (activeDialMode) {
                        DialMode.EXP -> 0f
                        DialMode.AST -> 90f
                        DialMode.NAV -> 180f
                        DialMode.SYS -> 270f
                    }
                    val angleRad = Math.toRadians(targetAngle.toDouble())

                    // Draw central Leica screw marker
                    drawCircle(
                        color = themeColor.copy(alpha = 0.4f),
                        radius = radius * 0.4f,
                        center = center
                    )
                    
                    // Screwdriver alignment slot in the center of dial
                    val slotStart = Offset(
                        (center.x - cos(angleRad) * radius * 0.3f).toFloat(),
                        (center.y - sin(angleRad) * radius * 0.3f).toFloat()
                    )
                    val slotEnd = Offset(
                        (center.x + cos(angleRad) * radius * 0.3f).toFloat(),
                        (center.y + sin(angleRad) * radius * 0.3f).toFloat()
                    )
                    drawLine(
                        color = themeColor,
                        start = slotStart,
                        end = slotEnd,
                        strokeWidth = 2.dp.toPx()
                    )

                    // Render tick marks of the dial
                    val steps = 12
                    for (i in 0 until steps) {
                        val tickAngle = (i * (360f / steps))
                        val tickRad = Math.toRadians(tickAngle.toDouble())
                        val tickStart = radius * 0.82f
                        val tickEnd = radius * 0.95f
                        
                        drawLine(
                            color = themeColor.copy(alpha = 0.6f),
                            start = Offset(
                                (center.x + cos(tickRad) * tickStart).toFloat(),
                                (center.y + sin(tickRad) * tickStart).toFloat()
                            ),
                            end = Offset(
                                (center.x + cos(tickRad) * tickEnd).toFloat(),
                                (center.y + sin(tickRad) * tickEnd).toFloat()
                            ),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
                }
                
                // Text Indicator centered beautifully inside
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(activeDialMode.label, color = themeColor, fontSize = 10.sp, style = MaterialTheme.typography.labelSmall)
                    Text("TAP DIAL", color = themeColor.copy(alpha = 0.5f), fontSize = 6.sp)
                }
            }


        }

        // ----------------- REAL RECOVERED SPOOL THUMBNAIL (Latest Capture) -----------------
        if (latestCapturedUri != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 24.dp, bottom = if (isLandscape) 16.dp else 44.dp)
                    .size(56.dp)
                    .background(Phosphor.Crt)
                    .border(1.dp, themeColor, RoundedCornerShape(2.dp))
                    .padding(2.dp)
            ) {
                AsyncImage(
                    model = latestCapturedUri,
                    contentDescription = "Recovered Spool Thumbnail",
                    colorFilter = androidx.compose.ui.graphics.ColorFilter.colorMatrix(
                        androidx.compose.ui.graphics.ColorMatrix(
                            if (activeFilmProfile == FilmProfile.BW_TRI_X) monoValues else colorValues
                        )
                    ),
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(1.dp))
                )
                // Small corner label
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .background(Phosphor.Crt.copy(alpha = 0.8f))
                        .padding(horizontal = 2.dp, vertical = 1.dp)
                ) {
                    Text("SPOOL", color = themeColor, fontSize = 7.sp)
                }
            }
        }

        // ----------------- RETRO SETTINGS KNOB (Bottom-Right, parallel to Shutter & Spool) -----------------
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 24.dp, bottom = if (isLandscape) 16.dp else 44.dp)
                .size(56.dp)
                .background(Phosphor.Crt, CircleShape)
                .border(2.dp, themeColor, CircleShape)
                .clickable { onShowSettingsPanelChange(true) },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2, size.height / 2)
                val radius = size.width / 2
                
                // Draw knurled outer ring
                drawCircle(
                    color = themeColor.copy(alpha = 0.2f),
                    radius = radius * 0.8f,
                    center = center
                )
                
                val teeth = 12
                for (i in 0 until teeth) {
                    val angle = (i * (360f / teeth)) * (Math.PI / 180f)
                    val startL = radius * 0.75f
                    val endL = radius * 0.95f
                    drawLine(
                        color = themeColor,
                        start = Offset(
                            (center.x + Math.cos(angle) * startL).toFloat(),
                            (center.y + Math.sin(angle) * startL).toFloat()
                        ),
                        end = Offset(
                            (center.x + Math.cos(angle) * endL).toFloat(),
                            (center.y + Math.sin(angle) * endL).toFloat()
                        ),
                        strokeWidth = 2.dp.toPx()
                    )
                }
                
                // Screwdriver slot crosshair
                drawLine(
                    color = themeColor,
                    start = Offset(center.x - 4.dp.toPx(), center.y),
                    end = Offset(center.x + 4.dp.toPx(), center.y),
                    strokeWidth = 1.5.dp.toPx()
                )
                drawLine(
                    color = themeColor,
                    start = Offset(center.x, center.y - 4.dp.toPx()),
                    end = Offset(center.x, center.y + 4.dp.toPx()),
                    strokeWidth = 1.5.dp.toPx()
                )
            }
            Text(
                "SETT",
                color = themeColor,
                fontSize = 7.5.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 16.dp)
            )
        }

        // ----------------- SPECTACULAR RETRO CALIBRATION PANEL (SETTINGS) -----------------
        SettingsPanel(
            showSettingsPanel = showSettingsPanel,
            onShowSettingsPanelChange = onShowSettingsPanelChange,
            themeColor = themeColor,
            colorName = colorName,
            onCycleColor = onCycleColor,
            burnEnabled = burnEnabled,
            onBurnEnabledChange = onBurnEnabledChange,
            telemetryHudEnabled = telemetryHudEnabled,
            onTelemetryHudEnabledChange = onTelemetryHudEnabledChange,
            doubleExposureEnabled = doubleExposureEnabled,
            onDoubleExposureEnabledChange = onDoubleExposureEnabledChange,
            goodOldTimesEnabled = goodOldTimesEnabled,
            onGoodOldTimesEnabledChange = onGoodOldTimesEnabledChange,
            chemicalDevDelayEnabled = chemicalDevDelayEnabled,
            onChemicalDevDelayEnabledChange = onChemicalDevDelayEnabledChange,
            reticleStyle = reticleStyle,
            onReticleStyleChange = { reticleStyle = it },
            bracketAlpha = bracketAlpha,
            onBracketAlphaChange = { bracketAlpha = it },
            latitude = latitude,
            longitude = longitude,
            onCoordsChange = onCoordsChange
        )

        // Holographic vertical laser scan and telemetry interference sweep overlay (Fallout style!)
        if (shutterProgress.value > 0f && shutterProgress.value < 2f) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val progress = shutterProgress.value / 2f // normalize 0f..2f down to 0f..1f
                val w = size.width
                val h = size.height
                val scanY = progress * h

                // 1. Draw the beautiful phosphor glow decay trail trailing above the scanner line
                if (scanY > 0f) {
                    val trailHeight = 180.dp.toPx()
                    val startY = (scanY - trailHeight).coerceAtLeast(0f)
                    drawRect(
                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(Color.Transparent, themeColor.copy(alpha = 0.2f), themeColor.copy(alpha = 0.45f)),
                            startY = startY,
                            endY = scanY
                        ),
                        topLeft = Offset(0f, startY),
                        size = Size(w, scanY - startY)
                    )
                }

                // 2. Draw holographic horizontal micro-grid lines that light up near the scan wavefront
                val gridDensity = 14.dp.toPx()
                var currentGridY = 0f
                while (currentGridY < h) {
                    val distanceToScan = Math.abs(currentGridY - scanY)
                    if (distanceToScan < 100.dp.toPx()) {
                        val intensity = (1f - (distanceToScan / (100.dp.toPx()))).coerceIn(0f, 1f)
                        drawLine(
                            color = themeColor.copy(alpha = intensity * 0.35f),
                            start = Offset(0f, currentGridY),
                            end = Offset(w, currentGridY),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
                    currentGridY += gridDensity
                }

                // 3. Draw the main high-intensity neon holographic laser scanline
                // Layer 1: Wide diffuse glow
                drawLine(
                    color = themeColor.copy(alpha = 0.5f),
                    start = Offset(0f, scanY),
                    end = Offset(w, scanY),
                    strokeWidth = 9.dp.toPx()
                )
                // Layer 2: Core beam color
                drawLine(
                    color = themeColor,
                    start = Offset(0f, scanY),
                    end = Offset(w, scanY),
                    strokeWidth = 3.5.dp.toPx()
                )
                // Layer 3: White-hot super-focused center filament
                drawLine(
                    color = Color.White,
                    start = Offset(0f, scanY),
                    end = Offset(w, scanY),
                    strokeWidth = 1.2.dp.toPx()
                )

                // 4. Generate random horizontal digital glitch/telemetry offset blocks near the wavefront
                val seed = (progress * 120).toInt()
                val random = java.util.Random(seed.toLong())
                if (random.nextFloat() < 0.75f) {
                    val glitchCount = random.nextInt(4) + 2
                    for (i in 0 until glitchCount) {
                        val gWidth = random.nextFloat() * 120.dp.toPx() + 30.dp.toPx()
                        val gX = random.nextFloat() * (w - gWidth)
                        val gHeight = random.nextFloat() * 3.dp.toPx() + 1.dp.toPx()
                        val gOffsetDir = if (random.nextBoolean()) 1 else -1
                        val gOffsetY = scanY + (random.nextFloat() * 30.dp.toPx() - 15.dp.toPx())
                        
                        // Draw horizontal neon shift block
                        drawRect(
                            color = themeColor.copy(alpha = 0.55f),
                            topLeft = Offset(gX + gOffsetDir * 6.dp.toPx(), gOffsetY.coerceIn(0f, h)),
                            size = Size(gWidth, gHeight)
                        )
                    }
                }
            }
        }
    }
}
