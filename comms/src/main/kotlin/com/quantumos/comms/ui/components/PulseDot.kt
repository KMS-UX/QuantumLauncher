package com.quantumos.comms.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

/*
 * The live-pulse dot -- Core Apps Fix-Pass identity fix for COMMS. Bound to a real event
 * (pulseTrigger incrementing on a new transmission), never a rememberInfiniteTransition/
 * infiniteRepeatable loop. A bounded, one-shot decay: snaps large, eases back to rest, then goes
 * fully static until the next real event. At rest (pulseTrigger unchanged) nothing animates.
 */
@Composable
fun LivePulseDot(pulseTrigger: Int, color: Color, modifier: Modifier = Modifier) {
    val scale = remember { Animatable(1f) }
    LaunchedEffect(pulseTrigger) {
        if (pulseTrigger == 0) return@LaunchedEffect
        scale.snapTo(1.8f)
        scale.animateTo(1f, animationSpec = tween(450, easing = LinearOutSlowInEasing))
    }
    Box(
        modifier
            .size(7.dp)
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            }
            .clip(CircleShape)
            .background(color)
    )
}
