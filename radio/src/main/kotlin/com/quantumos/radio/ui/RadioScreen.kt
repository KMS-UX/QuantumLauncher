package com.quantumos.radio.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quantumos.appshell.Fonts
import com.quantumos.radio.RadioViewModel
import com.quantumos.radio.ui.components.AnalogReceptionMeter
import com.quantumos.radio.ui.components.AtomMark
import com.quantumos.radio.ui.components.BandSelectionRow
import com.quantumos.radio.ui.components.MechanicalTuningDial
import com.quantumos.radio.ui.components.PresetListColumn
import com.quantumos.radio.ui.components.SignalStaticCanvas
import com.quantumos.radio.ui.components.VitalityDropdownConsole
import com.quantumos.radio.ui.components.VolumeAndStatusCard

/*
 * RadioScreen -- the docked module's main content, replacing the standalone app's RadioAppContent.
 * Everything that used to be private chrome is gone: no local header/registration-marks row (the old
 * "◄ APPS" back stub and the channel-selector strip were both dead no-ops -- not ported), no local CRT
 * flicker/scanline/vignette (MainActivity.kt:88-97,1329-1342,1344-1351 -- QuantumOSLayoutShell already
 * applies the real GPU CRT shader upstream of this content), no CryptographicDecoderConsole /
 * InterceptControlPanel (deleted feature, see docs/future-signal/radio-decoder.md).
 *
 * The old app's "VITALS" atom-pull lived in that deleted header, so it wasn't simply left dangling --
 * it's re-anchored here as a real, wired control at the top of the content column (still opens the
 * same VitalityDropdownConsole roll-down). Stealth Mode's screen-dim is preserved as a real reactive
 * transition (animateFloatAsState keyed on the stealthMode flag itself -- event-driven, not an idle
 * loop) rather than the old app's continuous flicker.
 */
@Composable
fun RadioScreen(
    viewModel: RadioViewModel,
    themeColor: Color,
    themeColorDim: Color,
    warnColor: Color,
    onCycleTheme: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    val currentBand by viewModel.currentBand.collectAsStateWithLifecycle()
    val frequency by viewModel.frequency.collectAsStateWithLifecycle()
    val volume by viewModel.volume.collectAsStateWithLifecycle()
    val reception by viewModel.reception.collectAsStateWithLifecycle()
    val stealthMode by viewModel.stealthMode.collectAsStateWithLifecycle()

    val configuration = LocalConfiguration.current
    val isWideScreen = configuration.screenWidthDp > 580

    var isVitalityOpen by remember { mutableStateOf(false) }

    // Stealth Mode dims the emission heavily -- a real reactive state transition (fires only when
    // stealthMode itself changes), not the standalone app's unconditional flicker animation.
    val stealthAlpha by animateFloatAsState(
        targetValue = if (stealthMode) 0.35f else 1f,
        animationSpec = tween(durationMillis = 200),
        label = "stealthAlpha"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(contentPadding)
            .graphicsLayer(alpha = stealthAlpha)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Real, wired vitals pull (replaces the deleted header's atom-pull row).
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Row(
                    modifier = Modifier
                        .clickable { isVitalityOpen = !isVitalityOpen }
                        .border(1.dp, themeColor.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                        .background(if (isVitalityOpen) themeColor.copy(alpha = 0.15f) else Color.Transparent)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AtomMark(color = themeColor, isExpanded = isVitalityOpen)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "VITALS",
                        color = themeColor,
                        fontFamily = Fonts.ChakraPetch,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (isWideScreen) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Left Column: Dial and Reception
                    Column(
                        modifier = Modifier
                            .weight(1.1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        BandSelectionRow(
                            selectedBand = currentBand,
                            color = themeColor,
                            onBandSelected = { viewModel.selectBand(it) }
                        )

                        MechanicalTuningDial(
                            frequency = frequency,
                            band = currentBand,
                            color = themeColor,
                            onFrequencyChanged = { viewModel.tuneFrequency(it) }
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            AnalogReceptionMeter(
                                reception = reception,
                                color = themeColor,
                                modifier = Modifier.weight(1f)
                            )

                            VolumeAndStatusCard(
                                volume = volume,
                                reception = reception,
                                color = themeColor,
                                warnColor = warnColor,
                                onVolumeChanged = { viewModel.setVolume(it) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Right Column: Presets
                    Column(modifier = Modifier.weight(0.9f)) {
                        PresetListColumn(
                            band = currentBand,
                            currentFreq = frequency,
                            viewModel = viewModel,
                            color = themeColor,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            } else {
                // Vertical scrolling layout for compact portrait devices
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    BandSelectionRow(
                        selectedBand = currentBand,
                        color = themeColor,
                        onBandSelected = { viewModel.selectBand(it) }
                    )

                    MechanicalTuningDial(
                        frequency = frequency,
                        band = currentBand,
                        color = themeColor,
                        onFrequencyChanged = { viewModel.tuneFrequency(it) }
                    )

                    AnalogReceptionMeter(
                        reception = reception,
                        color = themeColor,
                        modifier = Modifier.fillMaxWidth()
                    )

                    VolumeAndStatusCard(
                        volume = volume,
                        reception = reception,
                        color = themeColor,
                        warnColor = warnColor,
                        onVolumeChanged = { viewModel.setVolume(it) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    PresetListColumn(
                        band = currentBand,
                        currentFreq = frequency,
                        viewModel = viewModel,
                        color = themeColor,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 240.dp)
                    )
                }
            }
        }

        // Animated Signal Static overlay (grainy static noise when off-carrier)
        SignalStaticCanvas(
            reception = reception,
            color = themeColor,
            modifier = Modifier.fillMaxSize()
        )

        // Vitality Dropdown Panel (rolls down mechanically -- stepped enter/exit, not a smooth fade)
        AnimatedVisibility(
            visible = isVitalityOpen,
            modifier = Modifier.align(Alignment.TopCenter),
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut()
        ) {
            VitalityDropdownConsole(
                reception = reception,
                color = themeColor,
                colorDim = themeColorDim,
                stealthMode = stealthMode,
                onClose = { isVitalityOpen = false },
                onStealthToggle = { viewModel.toggleStealthMode() },
                onCycleTheme = onCycleTheme
            )
        }
    }
}
