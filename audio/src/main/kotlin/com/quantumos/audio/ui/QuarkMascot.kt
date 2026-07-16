package com.quantumos.audio.ui

import androidx.compose.animation.core.EaseInOutQuad
import androidx.compose.animation.core.EaseOutQuad
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.quantumos.appshell.Phosphor
import com.quantumos.core.QuarkReflexPosture

/*
 * AUDIO's local QUARK mascot -- ported from the standalone repo's QuarkMascot.kt. Fix-pass point 4:
 * the source app had ONE `rememberInfiniteTransition` driving all 7-8 animateFloat calls (idle bob,
 * happy hop, happy tilt, warn shake x/y, scan-line sweep, expanding ring radius+alpha)
 * unconditionally, all the time, with only the *rendered* values branched on quarkState. That is
 * exactly the "zero idle redraw" violation the House Style skill calls out.
 *
 * Fixed shape: each reactive posture gets its OWN `rememberInfiniteTransition`, created only while
 * the mascot is actually in that posture. IDLE is fully static (no transition object at all, no
 * offset) -- "static at rest," matching the House Style north star exactly; QUARK's life shows only
 * through the Scan/Happy/Warn reactive states, not an always-on ambient bob.
 */
@Composable
fun QuarkMascot(
    modifier: Modifier = Modifier,
    quarkState: QuarkReflexPosture,
    themeColor: Color,
    onClick: () -> Unit = {}
) {
    var translationX = 0f
    var translationY = 0f
    var tiltRotation = 0f
    var ringRadius = 1f
    var ringAlpha = 0f
    var scanLineProgress = 0.5f
    var showRing = false
    var showScanLine = false

    when (quarkState) {
        QuarkReflexPosture.HAPPY -> {
            val transition = rememberInfiniteTransition(label = "quark_happy")
            val hop by transition.animateFloat(
                initialValue = 0f, targetValue = -25f,
                animationSpec = infiniteRepeatable(tween(400, easing = EaseInOutQuad), RepeatMode.Reverse),
                label = "hop"
            )
            val tilt by transition.animateFloat(
                initialValue = -12f, targetValue = 12f,
                animationSpec = infiniteRepeatable(tween(500, easing = EaseInOutQuad), RepeatMode.Reverse),
                label = "tilt"
            )
            val ringR by transition.animateFloat(
                initialValue = 1.0f, targetValue = 2.4f,
                animationSpec = infiniteRepeatable(tween(1000, easing = EaseOutQuad), RepeatMode.Restart),
                label = "ringR"
            )
            val ringA by transition.animateFloat(
                initialValue = 1.0f, targetValue = 0.0f,
                animationSpec = infiniteRepeatable(tween(1000, easing = EaseOutQuad), RepeatMode.Restart),
                label = "ringA"
            )
            translationY = hop; tiltRotation = tilt; ringRadius = ringR; ringAlpha = ringA; showRing = true
        }
        QuarkReflexPosture.WARN -> {
            val transition = rememberInfiniteTransition(label = "quark_warn")
            val shakeX by transition.animateFloat(
                initialValue = -6f, targetValue = 6f,
                animationSpec = infiniteRepeatable(tween(50, easing = LinearEasing), RepeatMode.Reverse),
                label = "shakeX"
            )
            val shakeY by transition.animateFloat(
                initialValue = -4f, targetValue = 4f,
                animationSpec = infiniteRepeatable(tween(60, easing = LinearEasing), RepeatMode.Reverse),
                label = "shakeY"
            )
            translationX = shakeX; translationY = shakeY
        }
        QuarkReflexPosture.SCAN -> {
            val transition = rememberInfiniteTransition(label = "quark_scan")
            val scan by transition.animateFloat(
                initialValue = 0.1f, targetValue = 0.9f,
                animationSpec = infiniteRepeatable(tween(1500, easing = LinearEasing), RepeatMode.Reverse),
                label = "scan"
            )
            val ringR by transition.animateFloat(
                initialValue = 1.0f, targetValue = 2.4f,
                animationSpec = infiniteRepeatable(tween(1000, easing = EaseOutQuad), RepeatMode.Restart),
                label = "ringR"
            )
            val ringA by transition.animateFloat(
                initialValue = 1.0f, targetValue = 0.0f,
                animationSpec = infiniteRepeatable(tween(1000, easing = EaseOutQuad), RepeatMode.Restart),
                label = "ringA"
            )
            scanLineProgress = scan; ringRadius = ringR; ringAlpha = ringA; showRing = true; showScanLine = true
        }
        QuarkReflexPosture.IDLE -> {
            // Fully static -- no rememberInfiniteTransition is created at all. Zero idle redraw.
        }
    }

    val primaryColor = if (quarkState == QuarkReflexPosture.WARN) Phosphor.Warn else themeColor
    val dimTone = if (quarkState == QuarkReflexPosture.WARN) Phosphor.Warn.copy(alpha = 0.4f) else primaryColor.copy(alpha = 0.4f)
    val brightTone = if (quarkState == QuarkReflexPosture.WARN) Phosphor.Warn.copy(alpha = 0.85f) else primaryColor.copy(alpha = 0.85f)

    Box(
        modifier = modifier.size(180.dp).clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .size(180.dp)
                .offset(x = translationX.dp, y = translationY.dp)
        ) {
            val center = Offset(size.width / 2, size.height / 2)
            val radius = size.width / 4.5f

            if (showRing) {
                drawCircle(
                    color = primaryColor.copy(alpha = ringAlpha * 0.35f),
                    radius = radius * ringRadius,
                    center = center,
                    style = Stroke(width = 2.dp.toPx())
                )
            }

            // Side thruster pods
            val leftPodCenter = Offset(center.x - radius * 1.35f, center.y + radius * 0.25f)
            drawOval(
                color = dimTone,
                topLeft = Offset(leftPodCenter.x - radius * 0.3f, leftPodCenter.y - radius * 0.5f),
                size = Size(radius * 0.6f, radius),
                style = Stroke(width = 2.dp.toPx())
            )
            drawLine(
                color = primaryColor,
                start = Offset(center.x - radius, center.y + radius * 0.1f),
                end = leftPodCenter,
                strokeWidth = 2.dp.toPx()
            )
            val rightPodCenter = Offset(center.x + radius * 1.35f, center.y + radius * 0.25f)
            drawOval(
                color = dimTone,
                topLeft = Offset(rightPodCenter.x - radius * 0.3f, rightPodCenter.y - radius * 0.5f),
                size = Size(radius * 0.6f, radius),
                style = Stroke(width = 2.dp.toPx())
            )
            drawLine(
                color = primaryColor,
                start = Offset(center.x + radius, center.y + radius * 0.1f),
                end = rightPodCenter,
                strokeWidth = 2.dp.toPx()
            )

            // Antenna
            val antennaBase = Offset(center.x, center.y - radius)
            val antennaTip = Offset(center.x + (tiltRotation * 1.2f), center.y - radius * 2.1f)
            drawLine(color = primaryColor, start = antennaBase, end = antennaTip, strokeWidth = 2.dp.toPx())
            drawCircle(color = brightTone, radius = 6.dp.toPx(), center = antennaTip)

            // Spherical chassis
            drawCircle(color = dimTone, radius = radius, center = center)
            drawCircle(color = primaryColor, radius = radius, center = center, style = Stroke(width = 3.dp.toPx()))
            drawLine(
                color = primaryColor,
                start = Offset(center.x - radius, center.y + radius * 0.2f),
                end = Offset(center.x + radius, center.y + radius * 0.2f),
                strokeWidth = 1.5f.dp.toPx()
            )

            // Camera iris-eye
            val eyeRadius = radius * 0.55f
            drawCircle(color = Phosphor.Crt, radius = eyeRadius, center = center)
            drawCircle(color = primaryColor, radius = eyeRadius, center = center, style = Stroke(width = 2.dp.toPx()))

            val pupilRadius = if (quarkState == QuarkReflexPosture.SCAN) eyeRadius * 0.4f else eyeRadius * 0.55f
            drawCircle(color = dimTone.copy(alpha = 0.6f), radius = pupilRadius, center = center)
            drawCircle(color = primaryColor, radius = pupilRadius * 0.6f, center = center, style = Stroke(width = 1.5f.dp.toPx()))

            val reflectionOffset = Offset(center.x - pupilRadius * 0.3f, center.y - pupilRadius * 0.3f)
            drawCircle(color = brightTone, radius = 4.dp.toPx(), center = reflectionOffset)

            if (showScanLine) {
                val scanY = center.y - eyeRadius + (eyeRadius * 2 * scanLineProgress)
                val widthAtY = kotlin.math.sqrt((eyeRadius * eyeRadius) - ((scanY - center.y) * (scanY - center.y)))
                drawLine(
                    color = brightTone,
                    start = Offset(center.x - widthAtY, scanY),
                    end = Offset(center.x + widthAtY, scanY),
                    strokeWidth = 2.5.dp.toPx()
                )
                drawCircle(color = primaryColor.copy(alpha = 0.3f), radius = eyeRadius, center = center)
            }

            // Speaker grille
            val grilleCenterY = center.y + radius * 0.6f
            val dotSpacing = 8.dp.toPx()
            for (offsetIdx in -2..2) {
                val dx = offsetIdx * dotSpacing
                drawCircle(color = primaryColor, radius = 1.5.dp.toPx(), center = Offset(center.x + dx, grilleCenterY))
            }
        }
    }
}
