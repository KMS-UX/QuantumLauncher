package com.quantumos.nav

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.quantumos.appshell.PhosphorHueRuntime
import com.quantumos.nav.core.SectorPresets
import com.quantumos.nav.ui.NavScreen

/*
 * QuantumOS Nav — Compose entry point, docked into the launcher's shared App Shell (App Shell
 * Integration, Phase 3). Launched internally by the launcher's MAPS instrument tile via a plain
 * Intent (same task, no NEW_TASK/CLEAR_TOP) after a stepped PLEASE STANDBY hand-off beat. Thin:
 * edge-to-edge, hosts the ViewModel, wires location, and renders the App-Shell-compatible
 * NavScreen. All map logic lives in the MapCanvas interop; all pure logic lives in
 * com.quantumos.nav.core.
 *
 * No BackHandler is added here -- the Shell owns back once docked, so the system/predictive back
 * gesture simply finishes this Activity and returns to the still-live LauncherActivity on HOME.
 */
class NavActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()   // we own inset handling; the CRT surface reaches the screen edges
        setContent {
            val vm: NavViewModel = viewModel()
            val context = LocalContext.current
            PhosphorHueRuntime.init(context)
            val activeHue by PhosphorHueRuntime.activeHue.collectAsState()
            val gps by vm.gps.collectAsState()
            val statusLine by vm.statusLine.collectAsState()
            val warp by vm.warp.collectAsState()

            val fused = remember { LocationServices.getFusedLocationProviderClient(context) }

            var hasLocationPermission by remember {
                mutableStateOf(
                    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                        PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                        PackageManager.PERMISSION_GRANTED
                )
            }

            val permLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions()
            ) { grants ->
                val granted = grants.values.any { it }
                hasLocationPermission = granted
                if (!granted) vm.onGpsPermissionDenied()
            }

            // Cold boot: kick the initial broad-sector warp once, and request location if needed.
            LaunchedEffect(Unit) {
                vm.warpToDefaultSector()
                if (!hasLocationPermission) {
                    permLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                }
            }

            // Live GPS fixes -> VM, only while permission is held and the composition is present.
            // Updates are removed onDispose to conserve battery (field-tool vitality discipline).
            DisposableEffect(hasLocationPermission) {
                var callback: LocationCallback? = null
                if (hasLocationPermission &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED
                ) {
                    val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
                        .setMinUpdateIntervalMillis(5000)
                        .build()
                    callback = object : LocationCallback() {
                        override fun onLocationResult(result: LocationResult) {
                            val loc = result.lastLocation ?: return
                            vm.onGpsFix(loc.latitude, loc.longitude, loc.altitude)
                        }
                    }
                    runCatching {
                        fused.requestLocationUpdates(request, callback, android.os.Looper.getMainLooper())
                    }
                }
                onDispose { callback?.let { fused.removeLocationUpdates(it) } }
            }

            NavScreen(
                activeHue = activeHue,
                gps = gps,
                statusLine = statusLine,
                warp = warp,
                onCyclePhosphor = { PhosphorHueRuntime.cycleHue(context) },
                onWarpEntry = { lat, lng -> vm.warpToEntry(lat, lng) },
                onWarpPreset = { index -> vm.warpToPreset(SectorPresets.ALL[index]) },
                onLocate = { vm.warpToCurrentLocation() },
                onReturnHome = { finish() },
            )
        }
    }
}
