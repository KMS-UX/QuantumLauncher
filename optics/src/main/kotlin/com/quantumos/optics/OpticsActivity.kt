package com.quantumos.optics

import com.quantumos.appshell.Phosphor
import com.quantumos.appshell.PhosphorHueRuntime

import android.os.Bundle
import android.content.Context
import android.content.ContentValues
import android.provider.MediaStore
import android.os.Vibrator
import android.os.VibratorManager
import android.os.Build
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.lifecycleScope
import com.quantumos.optics.capture.FilmProfile
import com.quantumos.optics.capture.applyFilmFilterToUri
import com.quantumos.optics.capture.blendDoubleExposureBitmaps
import com.quantumos.optics.capture.createSimulatedCaptureUri
import com.quantumos.optics.capture.playMechanicalShutterSound
import com.quantumos.optics.ui.camera.ChemicalDevelopingConsole
import com.quantumos.optics.ui.camera.ViewfinderFoundation
import com.quantumos.optics.ui.components.AppShell
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import android.widget.Toast
import com.quantumos.optics.ui.theme.OpticsTheme

/*
 * OpticsActivity -- docked into the launcher's shared App Shell (App Shell Integration, Phase 3).
 * Launched internally by the launcher's CAM instrument tile via a plain Intent (same task, no
 * NEW_TASK/CLEAR_TOP) after a stepped PLEASE STANDBY hand-off beat. No BackHandler is added here --
 * the Shell owns back once docked, so the system/predictive back gesture simply finishes this
 * Activity and returns to the still-live LauncherActivity on HOME. The "◄ HOME" line in AppShell's
 * header is the same return path, made explicit and tappable.
 */
class OpticsActivity : ComponentActivity(), SensorEventListener {
    private var sensorManager: SensorManager? = null
    private var rotationSensor: Sensor? = null

    private val headingState = mutableStateOf(248f)
    private val pitchState = mutableStateOf(0f)
    private val rollState = mutableStateOf(0f)
    private var hasRealSensor = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        rotationSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

        enableEdgeToEdge()
        setContent {
            OpticsTheme {
                val context = LocalContext.current
                PhosphorHueRuntime.init(context)
                // Reads PhosphorHueRuntime (Core Apps Polish Pass, Item 2) -- the one process-wide
                // live source of truth every docked module + CONFIG + the launcher shares, replacing
                // the old local remember-only hue state.
                val activeHue by PhosphorHueRuntime.activeHue.collectAsState()
                val currentThemeColor = Phosphor.bright(activeHue)
                val currentThemeColorDim = Phosphor.dim(activeHue)
                val activeColorName = activeHue.name

                // Frame spool / index counter like classic Leica manual winding count
                var frameCounter by remember { mutableStateOf(1) }
                
                // Shutter flash state
                var triggerShutterFlash by remember { mutableStateOf(false) }
                
                // Real captured photo URI state
                var latestCapturedUri by remember { mutableStateOf<Uri?>(null) }
                
                // Capture callback reference
                var imageCaptureInstance by remember { mutableStateOf<ImageCapture?>(null) }

                // Active film emulation profile (Hoisted!)
                var activeFilmProfile by remember { mutableStateOf(FilmProfile.BW_TRI_X) }

                // Hoisted configuration preferences
                var burnEnabled by remember { mutableStateOf(true) }
                var telemetryHudEnabled by remember { mutableStateOf(true) }
                var showSettingsPanel by remember { mutableStateOf(false) }
                var goodOldTimesEnabled by remember { mutableStateOf(false) }
                var chemicalDevDelayEnabled by remember { mutableStateOf(true) }

                // Creative Film Techniques (Phase 2)
                var doubleExposureEnabled by remember { mutableStateOf(false) }
                var firstDoubleExposureUri by remember { mutableStateOf<Uri?>(null) }
                var developingLog by remember { mutableStateOf<SpoolLog?>(null) }
                var activeFocalLength by remember { mutableStateOf(50) }
                var activeFocusDistance by remember { mutableStateOf(2.0f) }
                var simulatedSubjectDistance by remember { mutableStateOf(1.5f) }
                var useSensorForSubjectDistance by remember { mutableStateOf(false) }
                val subjectDistance = if (useSensorForSubjectDistance) {
                    ((pitchState.value + 45f) / 60f).coerceIn(0f, 1.0f) * 9.3f + 0.7f
                } else {
                    simulatedSubjectDistance
                }

                var shutterSpeedMultiplier by remember { mutableStateOf(1.0f) }
                var isoSensitivityValue by remember { mutableStateOf(200) }
                var latitude by remember { mutableStateOf(35.6895) }
                var longitude by remember { mutableStateOf(139.6917) }
                var altitude by remember { mutableStateOf(42.5) }

                AppShell(
                    title = "Optics",
                    themeColor = currentThemeColor,
                    onReturnHome = { finish() },
                    onShutterClick = {
                        playMechanicalShutterSound()
                        val imgCap = imageCaptureInstance
                        val currentIso = isoSensitivityValue
                        val currentShutterMultiplier = shutterSpeedMultiplier
                        val calculatedShutter = when (currentShutterMultiplier) {
                            0.5f -> "1/60"
                            1.0f -> "1/125"
                            2.0f -> "1/250"
                            4.0f -> "1/500"
                            else -> "1/125"
                        }
                        val currentLat = latitude
                        val currentLng = longitude
                        val currentHeading = headingState.value
                        val currentPitch = pitchState.value
                        val currentRoll = rollState.value
                        val currentProfile = activeFilmProfile
                        val currentFrameCount = frameCounter
                        val currentBurnEnabled = burnEnabled
                        val captureTime = System.currentTimeMillis()
                        val currentGoodOldTimes = goodOldTimesEnabled
                        val currentDevDelay = chemicalDevDelayEnabled
                        val currentFocusDistance = activeFocusDistance
                        val currentSubjectDistance = subjectDistance

                        if (doubleExposureEnabled) {
                            if (firstDoubleExposureUri == null) {
                                // 1. FIRST SHUTTER RELEASE
                                if (imgCap != null) {
                                    val contentValues = ContentValues().apply {
                                        put(MediaStore.MediaColumns.DISPLAY_NAME, "blackhole_de1_${System.currentTimeMillis()}")
                                        put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Blackhole")
                                        }
                                    }
                                    val outputOptions = ImageCapture.OutputFileOptions.Builder(
                                        context.contentResolver,
                                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                        contentValues
                                    ).build()
                                    
                                    imgCap.takePicture(
                                        outputOptions,
                                        ContextCompat.getMainExecutor(context),
                                        object : ImageCapture.OnImageSavedCallback {
                                            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                                val savedUri = outputFileResults.savedUri ?: Uri.fromFile(
                                                    File(context.cacheDir, "blackhole_de1_${System.currentTimeMillis()}.jpg")
                                                )
                                                firstDoubleExposureUri = savedUri
                                                triggerShutterFlash = true
                                                triggerHapticFeedback(context)
                                                Toast.makeText(context, "FIRST EXPOSURE SECURED. COCK SHUTTER AGAIN.", Toast.LENGTH_SHORT).show()
                                            }
                                            override fun onError(exception: ImageCaptureException) {
                                                exception.printStackTrace()
                                                Toast.makeText(context, "Exposure failed. Try again.", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    )
                                } else {
                                    val savedUri = createSimulatedCaptureUri(context, seedOffset = 0)
                                    firstDoubleExposureUri = savedUri
                                    triggerShutterFlash = true
                                    triggerHapticFeedback(context)
                                    Toast.makeText(context, "SIMULATED FIRST EXPOSURE SECURED.", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                // 2. SECOND SHUTTER RELEASE: MERGE AND DEVELOPE
                                val firstUri = firstDoubleExposureUri!!
                                if (imgCap != null) {
                                    val contentValues = ContentValues().apply {
                                        put(MediaStore.MediaColumns.DISPLAY_NAME, "blackhole_de2_${System.currentTimeMillis()}")
                                        put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Blackhole")
                                        }
                                    }
                                    val outputOptions = ImageCapture.OutputFileOptions.Builder(
                                        context.contentResolver,
                                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                        contentValues
                                    ).build()
                                    
                                    imgCap.takePicture(
                                        outputOptions,
                                        ContextCompat.getMainExecutor(context),
                                        object : ImageCapture.OnImageSavedCallback {
                                            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                                val savedUri = outputFileResults.savedUri ?: Uri.fromFile(
                                                    File(context.cacheDir, "blackhole_de2_${System.currentTimeMillis()}.jpg")
                                                )
                                                
                                                Thread {
                                                    val blended = blendDoubleExposureBitmaps(context, firstUri, savedUri)
                                                    if (blended != null) {
                                                        try {
                                                            context.contentResolver.openOutputStream(savedUri, "w")?.use { os ->
                                                                blended.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, os)
                                                                os.flush()
                                                            }
                                                            blended.recycle()
                                                        } catch (e: Exception) {
                                                            e.printStackTrace()
                                                        }
                                                    }
                                                    
                                                    applyFilmFilterToUri(
                                                        context, savedUri, currentProfile,
                                                        burnEnabled = currentBurnEnabled,
                                                        iso = currentIso,
                                                        shutterSpeed = calculatedShutter,
                                                        latitude = currentLat,
                                                        longitude = currentLng,
                                                        heading = currentHeading,
                                                        pitch = currentPitch,
                                                        frameIndex = currentFrameCount,
                                                        timestamp = captureTime,
                                                        goodOldTimesEnabled = currentGoodOldTimes,
                                                        activeFocusDistance = currentFocusDistance,
                                                        subjectDistance = currentSubjectDistance
                                                    )
                                                    
                                                    val spoolLog = SpoolLog(
                                                        uriString = savedUri.toString(),
                                                        timestamp = captureTime,
                                                        iso = currentIso,
                                                        shutterSpeed = calculatedShutter,
                                                        latitude = currentLat,
                                                        longitude = currentLng,
                                                        heading = currentHeading,
                                                        pitch = currentPitch,
                                                        filmProfile = "${currentProfile.label} [DBL EXP]",
                                                        isDeveloping = currentDevDelay,
                                                        devStartTime = if (currentDevDelay) System.currentTimeMillis() else 0L
                                                    )
                                                    lifecycleScope.launch(Dispatchers.IO) {
                                                        val insertedId = SpoolDatabase.getDatabase(context).spoolLogDao().insertLog(spoolLog)
                                                        if (currentDevDelay) {
                                                            delay(8000)
                                                            SpoolDatabase.getDatabase(context).spoolLogDao().completeDevelopment(insertedId)
                                                        }
                                                    }
                                                    
                                                    (context as? android.app.Activity)?.runOnUiThread {
                                                        latestCapturedUri = savedUri
                                                        triggerShutterFlash = true
                                                        frameCounter += 1
                                                        firstDoubleExposureUri = null
                                                        triggerHapticFeedback(context)
                                                        Toast.makeText(context, "EMULSION MERGED & SECURED.", Toast.LENGTH_SHORT).show()
                                                    }
                                                }.start()
                                            }
                                            override fun onError(exception: ImageCaptureException) {
                                                exception.printStackTrace()
                                                Toast.makeText(context, "Exposure failed. Try again.", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    )
                                } else {
                                    val secondUri = createSimulatedCaptureUri(context, seedOffset = 8)
                                    Thread {
                                        val blended = blendDoubleExposureBitmaps(context, firstUri, secondUri)
                                        if (blended != null) {
                                            try {
                                                context.contentResolver.openOutputStream(secondUri, "w")?.use { os ->
                                                    blended.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, os)
                                                    os.flush()
                                                }
                                                blended.recycle()
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                            }
                                        }
                                        
                                        applyFilmFilterToUri(
                                            context, secondUri, currentProfile,
                                            burnEnabled = currentBurnEnabled,
                                            iso = currentIso,
                                            shutterSpeed = calculatedShutter,
                                            latitude = currentLat,
                                            longitude = currentLng,
                                            heading = currentHeading,
                                            pitch = currentPitch,
                                            frameIndex = currentFrameCount,
                                            timestamp = captureTime,
                                            goodOldTimesEnabled = currentGoodOldTimes,
                                            activeFocusDistance = currentFocusDistance,
                                            subjectDistance = currentSubjectDistance
                                        )
                                        
                                        val spoolLog = SpoolLog(
                                            uriString = secondUri.toString(),
                                            timestamp = captureTime,
                                            iso = currentIso,
                                            shutterSpeed = calculatedShutter,
                                            latitude = currentLat,
                                            longitude = currentLng,
                                            heading = currentHeading,
                                            pitch = currentPitch,
                                            filmProfile = "${currentProfile.label} [DBL EXP]",
                                            isDeveloping = currentDevDelay,
                                            devStartTime = if (currentDevDelay) System.currentTimeMillis() else 0L
                                        )
                                        lifecycleScope.launch(Dispatchers.IO) {
                                            val insertedId = SpoolDatabase.getDatabase(context).spoolLogDao().insertLog(spoolLog)
                                            if (currentDevDelay) {
                                                delay(8000)
                                                SpoolDatabase.getDatabase(context).spoolLogDao().completeDevelopment(insertedId)
                                            }
                                        }
                                        
                                        (context as? android.app.Activity)?.runOnUiThread {
                                            latestCapturedUri = secondUri
                                            triggerShutterFlash = true
                                            frameCounter += 1
                                            firstDoubleExposureUri = null
                                            triggerHapticFeedback(context)
                                            Toast.makeText(context, "SIMULATED EMULSION MERGED & SECURED.", Toast.LENGTH_SHORT).show()
                                        }
                                    }.start()
                                }
                            }
                        } else {
                            // NORMAL SINGLE EXPOSURE
                            if (imgCap != null) {
                                val contentValues = ContentValues().apply {
                                    put(MediaStore.MediaColumns.DISPLAY_NAME, "blackhole_${System.currentTimeMillis()}")
                                    put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                        put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Blackhole")
                                    }
                                }
                                val outputOptions = ImageCapture.OutputFileOptions.Builder(
                                    context.contentResolver,
                                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                    contentValues
                                ).build()
                                
                                imgCap.takePicture(
                                    outputOptions,
                                    ContextCompat.getMainExecutor(context),
                                    object : ImageCapture.OnImageSavedCallback {
                                        override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                            val savedUri = outputFileResults.savedUri ?: Uri.fromFile(
                                                File(context.cacheDir, "blackhole_${System.currentTimeMillis()}.jpg")
                                            )
                                            Thread {
                                                applyFilmFilterToUri(
                                                    context, savedUri, currentProfile,
                                                    burnEnabled = currentBurnEnabled,
                                                    iso = currentIso,
                                                    shutterSpeed = calculatedShutter,
                                                    latitude = currentLat,
                                                    longitude = currentLng,
                                                    heading = currentHeading,
                                                    pitch = currentPitch,
                                                    frameIndex = currentFrameCount,
                                                    timestamp = captureTime,
                                                    goodOldTimesEnabled = currentGoodOldTimes,
                                                    activeFocusDistance = currentFocusDistance,
                                                    subjectDistance = currentSubjectDistance
                                                )
                                                
                                                val spoolLog = SpoolLog(
                                                    uriString = savedUri.toString(),
                                                    timestamp = captureTime,
                                                    iso = currentIso,
                                                    shutterSpeed = calculatedShutter,
                                                    latitude = currentLat,
                                                    longitude = currentLng,
                                                    heading = currentHeading,
                                                    pitch = currentPitch,
                                                    filmProfile = currentProfile.label,
                                                    isDeveloping = currentDevDelay,
                                                    devStartTime = if (currentDevDelay) System.currentTimeMillis() else 0L
                                                )
                                                lifecycleScope.launch(Dispatchers.IO) {
                                                    val insertedId = SpoolDatabase.getDatabase(context).spoolLogDao().insertLog(spoolLog)
                                                    if (currentDevDelay) {
                                                        delay(8000)
                                                        SpoolDatabase.getDatabase(context).spoolLogDao().completeDevelopment(insertedId)
                                                    }
                                                }
                                                
                                                (context as? android.app.Activity)?.runOnUiThread {
                                                    latestCapturedUri = savedUri
                                                    triggerShutterFlash = true
                                                    frameCounter += 1
                                                    triggerHapticFeedback(context)
                                                }
                                            }.start()
                                        }
                                        override fun onError(exception: ImageCaptureException) {
                                            exception.printStackTrace()
                                            triggerShutterFlash = true
                                            frameCounter += 1
                                            triggerHapticFeedback(context)
                                        }
                                    }
                                )
                            } else {
                                val savedUri = createSimulatedCaptureUri(context, seedOffset = 0)
                                Thread {
                                    applyFilmFilterToUri(
                                        context, savedUri, currentProfile,
                                        burnEnabled = currentBurnEnabled,
                                        iso = currentIso,
                                        shutterSpeed = calculatedShutter,
                                        latitude = currentLat,
                                        longitude = currentLng,
                                        heading = currentHeading,
                                        pitch = currentPitch,
                                        frameIndex = currentFrameCount,
                                        timestamp = captureTime,
                                        goodOldTimesEnabled = currentGoodOldTimes,
                                        activeFocusDistance = currentFocusDistance,
                                        subjectDistance = currentSubjectDistance
                                    )
                                    val spoolLog = SpoolLog(
                                        uriString = savedUri.toString(),
                                        timestamp = captureTime,
                                        iso = currentIso,
                                        shutterSpeed = calculatedShutter,
                                        latitude = currentLat,
                                        longitude = currentLng,
                                        heading = currentHeading,
                                        pitch = currentPitch,
                                        filmProfile = currentProfile.label,
                                        isDeveloping = currentDevDelay,
                                        devStartTime = if (currentDevDelay) System.currentTimeMillis() else 0L
                                    )
                                    lifecycleScope.launch(Dispatchers.IO) {
                                        val insertedId = SpoolDatabase.getDatabase(context).spoolLogDao().insertLog(spoolLog)
                                        if (currentDevDelay) {
                                            delay(8000)
                                            SpoolDatabase.getDatabase(context).spoolLogDao().completeDevelopment(insertedId)
                                        }
                                    }
                                    (context as? android.app.Activity)?.runOnUiThread {
                                        latestCapturedUri = savedUri
                                        triggerShutterFlash = true
                                        frameCounter += 1
                                        triggerHapticFeedback(context)
                                    }
                                }.start()
                            }
                        }
                    }
                ) {
                    ViewfinderFoundation(
                        themeColor = currentThemeColor,
                        themeColorDim = currentThemeColorDim,
                        colorName = activeColorName,
                        frameCounter = frameCounter,
                        triggerShutterFlash = triggerShutterFlash,
                        latestCapturedUri = latestCapturedUri,
                        onFlashComplete = { triggerShutterFlash = false },
                        onBindCamera = { imgCap -> imageCaptureInstance = imgCap },
                        onCycleColor = { PhosphorHueRuntime.cycleHue(context) },
                        activeFilmProfile = activeFilmProfile,
                        onFilmProfileChange = { activeFilmProfile = it },
                        burnEnabled = burnEnabled,
                        onBurnEnabledChange = { burnEnabled = it },
                        telemetryHudEnabled = telemetryHudEnabled,
                        onTelemetryHudEnabledChange = { telemetryHudEnabled = it },
                        showSettingsPanel = showSettingsPanel,
                        onShowSettingsPanelChange = { showSettingsPanel = it },
                        shutterSpeedMultiplier = shutterSpeedMultiplier,
                        onShutterSpeedMultiplierChange = { shutterSpeedMultiplier = it },
                        isoSensitivityValue = isoSensitivityValue,
                        onIsoSensitivityValueChange = { isoSensitivityValue = it },
                        latitude = latitude,
                        longitude = longitude,
                        onCoordsChange = { lat, lng -> latitude = lat; longitude = lng },
                        altitude = altitude,
                        onAltitudeChange = { altitude = it },
                        heading = headingState.value,
                        pitch = pitchState.value,
                        roll = rollState.value,
                        doubleExposureEnabled = doubleExposureEnabled,
                        onDoubleExposureEnabledChange = { doubleExposureEnabled = it },
                        firstDoubleExposureUri = firstDoubleExposureUri,
                        onDevelopLog = { log -> developingLog = log },
                        goodOldTimesEnabled = goodOldTimesEnabled,
                        onGoodOldTimesEnabledChange = { goodOldTimesEnabled = it },
                        chemicalDevDelayEnabled = chemicalDevDelayEnabled,
                        onChemicalDevDelayEnabledChange = { chemicalDevDelayEnabled = it },
                        activeFocalLength = activeFocalLength,
                        onFocalLengthChange = { activeFocalLength = it },
                        activeFocusDistance = activeFocusDistance,
                        onFocusDistanceChange = { activeFocusDistance = it },
                        subjectDistance = subjectDistance,
                        useSensorForSubjectDistance = useSensorForSubjectDistance,
                        onUseSensorChange = { useSensorForSubjectDistance = it },
                        simulatedSubjectDistance = simulatedSubjectDistance,
                        onSimulatedSubjectDistanceChange = { simulatedSubjectDistance = it }
                    )

                    developingLog?.let { log ->
                        ChemicalDevelopingConsole(
                            log = log,
                            themeColor = currentThemeColor,
                            onDismiss = { developingLog = null },
                            onSaveFinished = {
                                developingLog = null
                                // Trigger recomposition refresh for thumbnails/latest uri
                                val temp = latestCapturedUri
                                latestCapturedUri = null
                                latestCapturedUri = temp
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        rotationSensor?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager?.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ROTATION_VECTOR) {
            hasRealSensor = true
            val rotationMatrix = FloatArray(9)
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
            val orientation = FloatArray(3)
            SensorManager.getOrientation(rotationMatrix, orientation)
            
            val yawDegrees = Math.toDegrees(orientation[0].toDouble()).toFloat()
            val pitchDegrees = Math.toDegrees(orientation[1].toDouble()).toFloat()
            val rollDegrees = Math.toDegrees(orientation[2].toDouble()).toFloat()

            var normHeading = yawDegrees
            if (normHeading < 0) normHeading += 360f

            headingState.value = normHeading
            pitchState.value = pitchDegrees
            rollState.value = rollDegrees
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun triggerHapticFeedback(context: Context) {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            vibrator?.vibrate(android.os.VibrationEffect.createOneShot(45, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
