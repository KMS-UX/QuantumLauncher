package com.quantumos.optics.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import android.content.res.Configuration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quantumos.appshell.BackHomeAffordance
import com.quantumos.appshell.Fonts
import com.quantumos.appshell.NameplateHeader
import com.quantumos.appshell.Phosphor
import com.quantumos.appshell.QuantumOSLayoutShell

/*
 * Optics's docked App Shell wrapper (App Shell Integration, Phase 3). Replaces the standalone
 * app's local nameplate + local floating-QUARK placeholder with the real shared chrome from
 * :app-shell: QuantumOSLayoutShell (CRT container) + NameplateHeader (opaque nameplate). The
 * mechanical shutter button stays local -- it's Optics's own primary control, not shell chrome.
 *
 * The local QUARK-trigger placeholder that used to live here is deleted outright: the real
 * system-wide QuarkTriggerService (owned by the launcher, docked-M4) already floats above this
 * screen like any other foreground app, and its resting position (OverlayGeometry.defaultPark /
 * nearestEdgeX) only ever settles at the left or right screen edge -- never horizontal-center --
 * so it structurally cannot cover this bottom-center shutter regardless of vertical position.
 *
 * No BackHandler here: per the docking convention (mirrors Nav's own documented intent), the Shell
 * owns back once docked -- system/predictive back simply finishes this Activity and returns to the
 * launcher's HOME channel, still live underneath. The explicit "◄ HOME" line is the visible,
 * tappable equivalent of that same return path.
 */
@Composable
fun AppShell(
    title: String,
    themeColor: Color = Phosphor.GreenBright,
    onShutterClick: () -> Unit = {},
    onReturnHome: () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val shutterPadding = if (isLandscape) 12.dp else 36.dp

    QuantumOSLayoutShell(forceFixedContainer = false) {
        Box(Modifier.fillMaxSize()) {
            content(PaddingValues(0.dp))

            Box(Modifier.align(Alignment.TopStart).fillMaxSize()) {
                NameplateHeader(
                    channelName = title.uppercase(),
                    color = themeColor,
                    dimColor = themeColor.copy(alpha = 0.6f),
                    font = Fonts.ChakraPetch
                )
                // Explicit return-to-HOME affordance, alongside the back gesture (no BackHandler
                // consumes it here -- system back already returns to the still-live launcher).
                BackHomeAffordance(
                    color = themeColor.copy(alpha = 0.6f),
                    font = Fonts.ChakraPetch,
                    onReturnHome = onReturnHome,
                    modifier = Modifier.align(Alignment.TopEnd).padding(top = 34.dp, end = 16.dp)
                )
            }

            // Mechanical Shutter Button (Center-Bottom) -- Optics's own primary control.
            val interactionSource = remember { MutableInteractionSource() }
            val isPressed by interactionSource.collectIsPressedAsState()
            val buttonScale by animateFloatAsState(targetValue = if (isPressed) 0.88f else 1.0f)

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = shutterPadding)
                    .size(if (isLandscape) 60.dp else 72.dp)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null
                    ) {
                        onShutterClick()
                    },
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = Offset(size.width / 2, size.height / 2)
                    val baseRadius = (size.width / 2) * buttonScale

                    // 1. Outer Knurled Metal Collar Rim
                    drawCircle(
                        color = themeColor.copy(alpha = 0.8f),
                        radius = baseRadius,
                        center = center,
                        style = Stroke(width = 2.dp.toPx())
                    )

                    // Knurling ticks (radial lines)
                    val ticks = 24
                    for (i in 0 until ticks) {
                        val angle = (i * (360f / ticks)) * (Math.PI / 180f)
                        val startLen = baseRadius - 6.dp.toPx()
                        val endLen = baseRadius - 2.dp.toPx()
                        val startOffset = Offset(
                            (center.x + Math.cos(angle) * startLen).toFloat(),
                            (center.y + Math.sin(angle) * startLen).toFloat()
                        )
                        val endOffset = Offset(
                            (center.x + Math.cos(angle) * endLen).toFloat(),
                            (center.y + Math.sin(angle) * endLen).toFloat()
                        )
                        drawLine(
                            color = themeColor.copy(alpha = 0.5f),
                            start = startOffset,
                            end = endOffset,
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    // 2. Inner Brass Release Button Collar
                    drawCircle(
                        color = themeColor.copy(alpha = 0.9f),
                        radius = baseRadius * 0.65f,
                        center = center,
                        style = Stroke(width = 1.5.dp.toPx())
                    )

                    // 3. Central Threaded Socket Hole (for Leica/M-style Cable Release)
                    drawCircle(
                        color = themeColor,
                        radius = baseRadius * 0.28f,
                        center = center,
                        style = Stroke(width = 1.5.dp.toPx())
                    )

                    // Threading details inside the socket
                    drawCircle(
                        color = themeColor.copy(alpha = 0.6f),
                        radius = baseRadius * 0.16f,
                        center = center,
                        style = Stroke(width = 1.dp.toPx())
                    )

                    // Absolute center empty dark pinhole
                    drawCircle(
                        color = Phosphor.Crt,
                        radius = baseRadius * 0.08f,
                        center = center
                    )
                }
            }
        }
    }
}
