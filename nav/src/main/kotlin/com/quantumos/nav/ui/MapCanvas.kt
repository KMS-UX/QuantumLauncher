package com.quantumos.nav.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.quantumos.core.PhosphorHue
import com.quantumos.nav.WarpRequest
import com.quantumos.nav.core.NavCoordinates
import com.quantumos.nav.core.stepWaypoints
import kotlinx.coroutines.delay
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapLibreMapOptions
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Point

/*
 * MapLibre MapView hosted inside Compose (AndroidView interop) — the pragmatic port path from the
 * audit: keep the working map engine, wrap it rather than rewrite it. Owns:
 *  - full MapView lifecycle binding (onStart/onResume/onPause/onStop/onDestroy/onLowMemory);
 *  - a hue-driven restyle (the active phosphor re-tints the map, matching the chrome);
 *  - the STEPPED warp (discrete camera hops, not a smooth 3s tween) + a transit callback so the
 *    shell can show the PLEASE STANDBY beat while the warp is in flight.
 */
@Composable
fun MapCanvas(
    activeHue: PhosphorHue,
    warp: WarpRequest?,
    positionFix: NavCoordinates?,
    onWarpTransit: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // MapLibre must be initialised before a MapView is constructed. onCreate is called here (once);
    // adding the lifecycle observer below replays START/RESUME up to the current state.
    //
    // textureMode(true): render the map into a TextureView, NOT the default GLSurfaceView. A
    // SurfaceView is composited on its own separate surface and is invisible under a Compose
    // RenderEffect/graphicsLayer and awkward to z-order under our chrome overlays — which is why the
    // map came up blank. A TextureView draws inline in the normal view pass, so it composites
    // correctly beneath the CRT scanline overlay and the floating readouts.
    val mapView = remember {
        MapLibre.getInstance(context)
        val options = MapLibreMapOptions.createFromAttributes(context, null).textureMode(true)
        MapView(context, options).apply { onCreate(null) }
    }
    var map by remember { mutableStateOf<MapLibreMap?>(null) }

    // Bind MapView to the composition's lifecycle (prevents the leaks the old Activity handled by hand).
    // onCreate runs once in the factory above; the observer drives start/resume/pause/stop; onDestroy
    // is done ONCE in onDispose (not also in the observer) to avoid a double-destroy on teardown.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        mapView.getMapAsync { map = it }
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onStop()
            mapView.onDestroy()
        }
    }

    // Strip MapLibre's default logo + attribution widgets — the sharp white marks clash with the
    // used-future look. We render our own dim-phosphor credit line in the shell instead (OSM's
    // attribution requirement is still met, just on-brand).
    LaunchedEffect(map) {
        map?.uiSettings?.apply {
            isLogoEnabled = false
            isAttributionEnabled = false
        }
    }

    // Re-tint the map from the active phosphor on first load and on every live hue switch — one token
    // source drives both chrome and map, so there is never an off-palette green baked into the style.
    // The style reload recreates the (empty) "position" source, so re-push the current fix here too.
    LaunchedEffect(map, activeHue) {
        val m = map ?: return@LaunchedEffect
        m.setStyle(Style.Builder().fromJson(MapStyle.tinted(context, activeHue))) { style ->
            positionFix?.let {
                style.getSourceAs<GeoJsonSource>("position")
                    ?.setGeoJson(Point.fromLngLat(it.longitude, it.latitude))
            }
        }
    }

    // Position blip — push the latest GPS fix into the baked-in "position" geojson source. The style
    // draws it as a phosphor contact blip (soft halo + bright ring + core dot). getStyle waits for the
    // style to be ready, so this is safe both on first fix and after a hue-driven restyle.
    LaunchedEffect(map, positionFix) {
        val m = map ?: return@LaunchedEffect
        val fix = positionFix ?: return@LaunchedEffect
        m.getStyle { style ->
            style.getSourceAs<GeoJsonSource>("position")
                ?.setGeoJson(Point.fromLngLat(fix.longitude, fix.latitude))
        }
    }

    // STEPPED warp: jump the camera through discrete waypoints with a short beat between each, rather
    // than MapLibre's smooth animateCamera. Signals transit start/end so the shell shows PLEASE STANDBY.
    LaunchedEffect(warp?.id, map) {
        val m = map ?: return@LaunchedEffect
        val request = warp ?: return@LaunchedEffect
        onWarpTransit(true)
        val current = m.cameraPosition.target
        val from = if (current != null) {
            NavCoordinates(current.latitude, current.longitude)
        } else {
            request.destination
        }
        val hops = stepWaypoints(from, request.destination, steps = 4)
        hops.forEachIndexed { index, hop ->
            val zoom = if (index == hops.lastIndex) request.zoom else m.cameraPosition.zoom
            m.moveCamera(
                CameraUpdateFactory.newCameraPosition(
                    CameraPosition.Builder()
                        .target(LatLng(hop.latitude, hop.longitude))
                        .zoom(zoom)
                        .build()
                )
            )
            delay(90)  // window-blind click between hops — mechanical, not silky
        }
        onWarpTransit(false)
    }

    AndroidView(factory = { mapView }, modifier = modifier)
}
