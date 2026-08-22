package com.quantumos.quarkavatar.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.collectAsState
import com.quantumos.appshell.Phosphor
import com.quantumos.appshell.PhosphorHueRuntime
import com.quantumos.appshell.QuantumStateRuntime
import com.quantumos.appshell.SoundEngine
import com.quantumos.core.SoundCue
import com.quantumos.quarkavatar.QuarkState
import com.quantumos.quarkavatar.RenderMode
import com.quantumos.quarkavatar.materialiseFrameAt
import com.quantumos.quarkavatar.ui.scene.QuarkHologramOverlay
import com.quantumos.quarkavatar.ui.scene.QuarkPlateView

/*
 * QUARK herself, as a drop-in composable — the whole presentation the avatar track built across
 * Phases 15–21, with nothing of the dev screen attached to it.
 *
 * This is the piece `:app` needs. It was extracted so the QUARK Assistant View could stop drawing a
 * 132dp ring-and-iris mark and show the actual character (W2): the floating trigger opens the
 * Assistant, and the Assistant IS her.
 *
 * **It takes no state parameters, on purpose.** Posture, speaking and stealth all come from the one
 * live QuantumStateEngine through `QuantumStateRuntime` (B4), so there is no way for a caller to
 * hand it a state that disagrees with the unit. What a caller *can* choose is presentation — which
 * plate set, how large, whether the ambient carrier runs — because that is a property of the surface
 * she is being drawn on, not of what she is doing.
 *
 * Everything in here settles: with `ambient = false` and no transition running, the composable does
 * not recompose and draws an identical frame every time. That is the house rule QUARK is held to.
 */
@Composable
fun QuarkProjection(
    modifier: Modifier = Modifier,
    renderMode: RenderMode = RenderMode.PLATE_BODY,
    framingScale: Float = 1f,
    ambient: Boolean = true,
    phosphorBlend: Float = 0f,
    /**
     * Change this to re-run the materialise. It runs once when the composable first appears — a
     * projection coming up is what opening the surface IS — and again whenever the key changes.
     */
    replayKey: Any? = Unit,
) {
    val context = LocalContext.current
    PhosphorHueRuntime.init(context)
    val hue by PhosphorHueRuntime.activeHue.collectAsState()
    val themeColor = Phosphor.bright(hue)

    val master by QuantumStateRuntime.masterState.collectAsState()
    val state = QuarkState.of(master.quarkBrain.activePosture)
    val speakingNow = master.quarkBrain.isSpeaking
    val stealthNow = master.environment.isStealthMode

    // B1 — the scan-in. Owned here rather than by the caller so every surface that shows her gets
    // the entrance for free and none of them can forget the boot cue.
    val sound = remember { SoundEngine() }
    val progress = remember { Animatable(0f) }
    LaunchedEffect(replayKey) {
        sound.play(SoundCue.BOOT_SWEEP, stealthNow)
        progress.snapTo(0f)
        progress.animateTo(1f, tween(MATERIALISE_MS, easing = LinearEasing))
    }
    val materialise = materialiseFrameAt(progress.value)

    Box(modifier, contentAlignment = Alignment.Center) {
        Box(Modifier.fillMaxWidth().clipToBounds(), contentAlignment = Alignment.Center) {
            val housing by animateColorAsState(
                targetValue = state.housingAccent ?: themeColor,
                animationSpec = tween(STATE_TRANSITION_MS),
                label = "housing_accent",
            )
            val alert by animateFloatAsState(
                targetValue = if (state.housingAccent != null) 1f else 0f,
                animationSpec = tween(STATE_TRANSITION_MS),
                label = "alert_emitter",
            )
            val stealth by animateFloatAsState(
                targetValue = if (stealthNow) 1f else 0f,
                animationSpec = tween(STEALTH_TRANSITION_MS),
                label = "stealth_power",
            )
            val speaking by animateFloatAsState(
                targetValue = if (speakingNow) 1f else 0f,
                animationSpec = tween(STATE_TRANSITION_MS),
                label = "speaking_level",
            )
            Crossfade(
                targetState = state,
                animationSpec = tween(STATE_TRANSITION_MS),
                label = "quark_state",
            ) { shown ->
                QuarkPlateView(
                    plateRes = shown.plateFor(renderMode),
                    contentDescription = "QUARK -- ${shown.label}",
                    phosphor = themeColor,
                    phosphorBlend = phosphorBlend,
                    stealthDim = 1f - stealth * (1f - STEALTH_FIGURE_DIM),
                    framingScale = framingScale,
                    baseOverhang = renderMode.baseOverhang,
                    materialise = materialise,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            QuarkHologramOverlay(
                phosphor = housing,
                ambient = ambient,
                alert = alert,
                speaking = speaking,
                stealth = stealth,
                materialise = materialise,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

// Shared with the dev screen so the two cannot drift apart.
internal const val STATE_TRANSITION_MS = 320
internal const val STEALTH_TRANSITION_MS = 420
internal const val STEALTH_FIGURE_DIM = 0.35f
internal const val MATERIALISE_MS = 1400
