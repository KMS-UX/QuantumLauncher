package com.quantumos.shell.overlay

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quantumos.shell.ui.Phosphor
import com.quantumos.shell.ui.PleaseStandbyCard
import kotlinx.coroutines.delay

/*
 * QuantumOS — M4/M5 boundary. The floating QUARK trigger taps through to HERE.
 *
 * M4's job: play the PLEASE STANDBY beat (reused, not rebuilt) and resolve into a PLACEHOLDER
 * full-screen surface that acknowledges the real QUARK Assistant View arrives at M5. The reactive
 * states, conversation log, command rail, text entry, and scripted-brain wiring are explicitly M5's
 * scope — they are deliberately absent here. Back-press or a tap returns to whatever app was
 * underneath the overlay.
 */
class QuarkStubActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val hueColor = intent.getIntExtra(QuarkTriggerService.EXTRA_HUE_COLOR, 0xFF00FF00.toInt())
        val color = Color(hueColor)
        val dimColor = Color(
            red = color.red * 2f / 3f,
            green = color.green * 2f / 3f,
            blue = color.blue * 2f / 3f,
            alpha = 1f
        )
        // TODO M0: replace FontFamily.Monospace with bundled Chakra Petch (shared with the launcher).
        val font = FontFamily.Monospace

        setContent {
            var standby by remember { mutableStateOf(true) }
            // The beat: a brief PLEASE STANDBY before the surface resolves — a machine doing real
            // work, not a spinner. Then the placeholder stub.
            LaunchedEffect(Unit) {
                delay(700)
                standby = false
            }

            Box(
                Modifier
                    .fillMaxSize()
                    .background(Phosphor.Crt)
                    .clickable(enabled = !standby) { finish() },
                contentAlignment = Alignment.Center
            ) {
                if (standby) {
                    PleaseStandbyCard(subline = "ROUTING TO QUARK…", color = color, dimColor = dimColor, font = font)
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("QUARK", color = color, fontFamily = font, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Text("ASSISTANT VIEW", color = color, fontFamily = font, fontSize = 14.sp)
                        Spacer(Modifier.height(16.dp))
                        Text("────────────────────", color = dimColor, fontFamily = font, fontSize = 12.sp)
                        Spacer(Modifier.height(16.dp))
                        // The one acknowledgment line — system microcopy, NOT QUARK voice (M4 hard stop).
                        Text("MODULE PENDING // M5", color = dimColor, fontFamily = font, fontSize = 13.sp, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(24.dp))
                        Text("◄ TAP TO RETURN, OPERATOR", color = dimColor, fontFamily = font, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}
