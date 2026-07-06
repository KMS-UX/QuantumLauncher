package com.quantumos.optics.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import android.content.res.Configuration
import com.quantumos.appshell.Fonts
import com.quantumos.appshell.NameplateHeader
import com.quantumos.appshell.Phosphor
import com.quantumos.appshell.QuantumOSLayoutShell

/*
 * App Shell Integration Step 2 — Optics's own nameplate/header row and the standalone-app-only
 * QUARK trigger placeholder are gone. The shared app-shell module now supplies the CRT container
 * (`QuantumOSLayoutShell`, the same AGSL treatment as the launcher) and the nameplate
 * (`NameplateHeader`) — Optics renders inside that shell's nameplate + body pattern instead of
 * its own, exactly as the launcher does. The real floating QUARK trigger is a system-wide overlay
 * (`QuarkTriggerService`, owned by the launcher) that already floats above every app on screen,
 * so Optics no longer needs to fake one locally.
 *
 * The mechanical shutter button is Optics's primary control (per the App Shell Lab audit) and is
 * unchanged in look/behavior — it now sits at the bottom-center of the body area below the
 * nameplate rather than floating over full-bleed content, matching how the shared shell places
 * chrome above content everywhere else. Nothing about the shutter's tap target, art, or press
 * feedback changed.
 */
@Composable
fun AppShell(
    title: String,
    themeColor: Color = Phosphor.GreenBright,
    onShutterClick: () -> Unit = {},
    content: @Composable () -> Unit
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val shutterPadding = if (isLandscape) 12.dp else 36.dp

    QuantumOSLayoutShell(forceFixedContainer = false) {
        Column(Modifier.fillMaxSize()) {
            NameplateHeader(
                channelName = title.uppercase(),
                color = themeColor,
                dimColor = themeColor.copy(alpha = 0.6f),
                font = Fonts.ChakraPetch
            )

            Box(Modifier.fillMaxWidth().weight(1f)) {
                content()

                // Mechanical Shutter Button (Center-Bottom) — the primary camera control; the
                // shared shell's chrome (nameplate above, the real system-wide QUARK trigger
                // overlay) is routed around it, never over it.
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
}
