package com.quantumos.quarkavatar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.quantumos.appshell.engageFieldUnitDisplay
import com.quantumos.appshell.hideSystemBars
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.quantumos.appshell.Phosphor
import com.quantumos.appshell.PhosphorHueRuntime
import com.quantumos.appshell.QuantumStateRuntime
import com.quantumos.appshell.SoundEngine
import com.quantumos.core.SoundCue
import com.quantumos.quarkavatar.ui.QuarkAvatarScreen
import com.quantumos.quarkavatar.ui.components.AppShell

/*
 * QuarkAvatarActivity -- the Phase 4b AGSL overlay shader dev-preview surface (see
 * art/quark-avatar/PRODUCTION_LOG.md). Reached only via CONFIG's temporary "QUARK AVATAR -- DEV
 * PREVIEW" row (a provisional entry point, NOT the real navigation decision for where "QUARK Core
 * App" lives -- that's a Director call, out of scope here). No BackHandler -- the Shell owns back
 * once docked, same as every other docked module.
 *
 * Posture/Speaking/Stealth are LOCAL, unpersisted demo state -- NOT read from
 * QuantumRuntime.masterState. Wiring the real QuarkReflexPosture/Stealth state in requires the same
 * kind of cross-module extraction :quark-brain needed (:app depends on every docked module, never
 * the reverse, so a docked module can't reach :app's QuantumRuntime directly) -- flagged as follow-up
 * work, not attempted this pass. HUE is the one exception: it drives the real, already-safe-to-reach
 * PhosphorHueRuntime, same as every other docked module.
 */
class QuarkAvatarActivity : ComponentActivity() {

    override fun onWindowFocusChanged(hasFocus: Boolean) {

        super.onWindowFocusChanged(hasFocus)

        // A transient reveal, a fold/unfold or coming back from another app all leave

        // the system bars showing. Re-hide whenever this window is the one in front.

        if (hasFocus) hideSystemBars()

    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        engageFieldUnitDisplay()
        setContent {
            val context = LocalContext.current
            PhosphorHueRuntime.init(context)
            val themeHue by PhosphorHueRuntime.activeHue.collectAsState()
            val themeColor = Phosphor.bright(themeHue)
            val themeColorDim = Phosphor.dim(themeHue)
            // B4 -- REAL state. STATE / SPEAKING / STEALTH were local demo toggles: a simulation of
            // the unit, running inside the unit. They now read the one live QuantumStateEngine
            // through the shared seam, so what QUARK shows is what the unit is actually doing.
            val master by QuantumStateRuntime.masterState.collectAsState()
            val state = QuarkState.of(master.quarkBrain.activePosture)
            val speakingPreview = master.quarkBrain.isSpeaking
            val stealthPreview = master.environment.isStealthMode
            // BODY is the default presentation from Phase 18: a bust with no projection base reads
            // as a head floating in a box, which is what the Director called out.
            var renderMode by remember { mutableStateOf(RenderMode.PLATE_BODY) }
            // How far QUARK is pulled toward the active phosphor: 0% keeps the reference art's own
            // colour, 100% is a monochrome CRT projection in the house palette. A Director call,
            // so it is a control rather than a constant.
            var phosphorBlendIndex by remember { mutableIntStateOf(0) }
            var ambient by remember { mutableStateOf(true) }
            // How big QUARK is drawn inside the housing, as a multiple of the viewport width.
            //
            // This is a control and not a constant because the framing has a hard geometric
            // trade-off in it that only the Director can settle. The bust plate is 779x956, so at
            // 100% width it renders ~1325 px tall on a 2424 px screen -- and once her base is pushed
            // off the bottom edge (which is the whole point of the new framing) her head necessarily
            // sits low, with the upper half of the frame empty. Lifting her means scaling past 100%,
            // which crops the sides -- and the hair runs to both edges of the plate, so every pixel
            // gained costs hair. There is no setting that avoids both.
            // One framing choice PER MODE, not one shared index. Each plate opens at its own
            // default, and a deliberate change to one is not lost by looking at the other -- which a
            // single shared index would do every time the Director switched to compare them.
            var framingIndices by remember {
                mutableStateOf(RenderMode.entries.map { it.defaultFramingIndex })
            }
            val framingIndex = framingIndices[renderMode.ordinal]

            // The materialise, the boot cue and the state reads all live in QuarkProjection now,
            // so this screen keeps only the one thing it actually owns: a way to ask for a replay.
            var replay by remember { mutableIntStateOf(0) }

            // NO local chirp on state change. The reflex dispatch carries the cue token now, so
            // the ENGINE emits it -- which means a posture raised anywhere in the OS sounds, not
            // only one raised while this screen happens to be open. Playing it here as well was a
            // double-fire the moment the controls became real actuators.

            AppShell(
                title = "Quark",
                themeColor = themeColor,
                onReturnHome = { finish() }
            ) { padding ->
                QuarkAvatarScreen(
                    themeHue = themeHue,
                    themeColor = themeColor,
                    themeColorDim = themeColorDim,
                    speakingPreview = speakingPreview,
                    stealthPreview = stealthPreview,
                    renderMode = renderMode,
                    phosphorBlend = PHOSPHOR_BLEND_STEPS[phosphorBlendIndex],
                    state = state,
                    ambient = ambient,
                    framingScale = FRAMING_STEPS[framingIndex],
                    replayKey = replay,
                    onCycleHue = { PhosphorHueRuntime.cycleHue(context) },
                    // SPEAKING has no actuator: QUARK speaks because she has something to say, and
                    // the flag is raised by the voice engine. The row is a readout, not a control --
                    // faking it here would put a second source of truth back on the screen.
                    onToggleSpeaking = {},
                    // Real actuators now. Stealth engages across the WHOLE unit and the engine
                    // emits its own power-down cue and LOG entry -- which is why this no longer plays
                    // a sound itself: doing both would double it.
                    onToggleStealth = { QuantumStateRuntime.toggleStealthMode() },
                    onToggleAmbient = { ambient = !ambient },
                    onCycleState = {
                        val all = QuarkState.entries
                        val next = all[(all.indexOf(state) + 1) % all.size]
                        // Dispatches a real reflex: the posture changes for the whole OS and the LOG
                        // channel records it. The chirp comes from the engine's own audio cue rather
                        // than from this screen, for the same no-double-fire reason as Stealth.
                        QuantumStateRuntime.dispatchReflex(
                            intent = "AVATAR_PREVIEW",
                            posture = next.posture,
                            audioToken = next.cue,
                        )
                    },
                    onCyclePhosphorBlend = {
                        phosphorBlendIndex = (phosphorBlendIndex + 1) % PHOSPHOR_BLEND_STEPS.size
                    },
                    onCycleFraming = {
                        val next = (framingIndex + 1) % FRAMING_STEPS.size
                        framingIndices = framingIndices.toMutableList()
                            .also { it[renderMode.ordinal] = next }
                    },
                    onReplayMaterialise = { replay++ },
                    onCycleRenderMode = {
                        val modes = RenderMode.entries
                        renderMode = modes[(modes.indexOf(renderMode) + 1) % modes.size]
                    },
                    contentPadding = padding
                )
            }
        }
    }
}

// How long the scan-in takes end to end. Long enough to read as an apparatus doing work, short
// enough that it never becomes something the Operator waits through -- this is an entrance, not a
// loading screen. The house style reserves PLEASE STANDBY for actual waiting.
private const val MATERIALISE_MS = 1400

private val PHOSPHOR_BLEND_STEPS = floatArrayOf(0f, 0.35f, 0.7f, 1f)

// 100% is the whole plate, uncropped. Each step up lifts her further up the frame and takes more of
// the hair off the sides: at 125% roughly 12% of the width is lost per side, at 175% roughly 21%.
private val FRAMING_STEPS = floatArrayOf(1.0f, 1.25f)
// Two settings, on the Director's call (Phase 19): 100% and 125%. 90% and 150% were only ever there
// to bracket the judgement and the judgement has been made.
//
// Which one each mode OPENS at is a property of the plate, not a global -- see
// RenderMode.defaultFramingIndex. BODY opens at 100%, BUST at 125%.

