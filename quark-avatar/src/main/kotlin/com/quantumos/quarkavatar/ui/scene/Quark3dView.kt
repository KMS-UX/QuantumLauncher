package com.quantumos.quarkavatar.ui.scene

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.google.android.filament.LightManager
import dev.romainguy.kotlin.math.Float3
import io.github.sceneview.SceneView
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
import io.github.sceneview.node.ModelNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberEnvironmentLoader
import io.github.sceneview.rememberModelInstance
import io.github.sceneview.rememberModelLoader

/*
 * Quark3dView -- the "Option B" real-time 3D render path (PRODUCTION_LOG Phase 5), sitting beside
 * the Phase 4b pre-rendered-frames + AGSL-overlay path so the two can be compared on the same
 * device, in the same app, on the same screen. This is an EVALUATION surface, not a decision:
 * whether QUARK ships as real-time 3D or as polished pre-rendered frames is still open.
 *
 * What this path buys, concretely -- and it is the whole architectural argument:
 *   - the accent stops being a green-dominance colour key over a baked PNG (a fragile trick this
 *     log has spent several passes debugging) and becomes a real emissive material parameter;
 *   - pose and camera angle become free rather than pre-baked, so the posture library stops being
 *     an N-postures x M-hues asset explosion.
 * What it costs: +28.83 MB of APK (measured, debug/all-ABI) and a fidelity ceiling below the
 * reference art.
 *
 * The model is `assets/models/quark.glb`, produced by `art/quark-avatar/blender/scripts/
 * 04_export_gltf.py` -- see that script for why the export needs a bake-down pass rather than
 * `export_apply=True`.
 */
@Composable
fun Quark3dView(
    accentColor: Color,
    stealthDim: Float,
    modifier: Modifier = Modifier,
) {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val environmentLoader = rememberEnvironmentLoader(engine)
    val modelInstance = rememberModelInstance(modelLoader, MODEL_ASSET)

    // Slow idle turntable. There is no authored idle ACTION in quark_base.blend yet -- an earlier
    // log entry claimed a `QUARK_Idle` action survived export, but the .blend contains zero
    // actions, so that animation was authored in a throwaway session and never saved. Until one
    // exists, a gentle yaw is what proves the path is live rather than a still image.
    var yaw by remember { mutableStateOf(0f) }

    SceneView(
        modifier = modifier,
        engine = engine,
        modelLoader = modelLoader,
        environmentLoader = environmentLoader,
        onFrame = { yaw = (yaw + 0.25f) % 360f },
    ) {
        LightNode(
            type = LightManager.Type.DIRECTIONAL,
            intensity = 80_000f * stealthDim,
            direction = Float3(-0.4f, -0.7f, -0.6f),
        )
        modelInstance?.let { instance ->
            ModelNode(
                modelInstance = instance,
                scaleToUnits = 1.15f,
                centerOrigin = Position(0f, 0f, 0f),
                rotation = Rotation(0f, yaw, 0f),
            )
        }
    }

    // Retint the emissive accent from the live phosphor hue. This is the replacement for the AGSL
    // colour key: the headband and spine conduit share the QUARK_Emissive material, so one
    // parameter write recolours every accent surface -- no green-dominance heuristic, no risk of
    // catching a skin or plate pixel by accident.
    LaunchedEffect(modelInstance, accentColor, stealthDim) {
        val instance = modelInstance ?: return@LaunchedEffect
        instance.materialInstances
            .filter { it.name.contains(EMISSIVE_MATERIAL) }
            .forEach { material ->
                // ACCENT_GAIN, measured not guessed: QUARK_Emissive carries
                // KHR_materials_emissive_strength = 7.0 from the Blender authoring material, and
                // Filament multiplies that by emissiveFactor. Writing the raw sRGB accent colour
                // therefore lands at ~7x and clips to white -- the first on-device 3D render showed
                // an AMBER headband as a white bar with only a faint amber halo. Pre-dividing here
                // keeps the hue readable instead of blown out.
                val gain = ACCENT_GAIN * stealthDim
                material.setParameter(
                    "emissiveFactor",
                    accentColor.red * gain,
                    accentColor.green * gain,
                    accentColor.blue * gain,
                )
            }
    }
}

private const val MODEL_ASSET = "models/quark.glb"
private const val EMISSIVE_MATERIAL = "QUARK_Emissive"
private const val ACCENT_GAIN = 0.3f
