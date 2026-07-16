package com.quantumos.comms.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quantumos.appshell.Fonts
import com.quantumos.appshell.Phosphor
import com.quantumos.comms.CommsUiState
import com.quantumos.comms.LogType

/*
 * Satellite links (a static seeded snapshot, not a ticking fake-telemetry loop -- Core Apps
 * Fix-Pass zero-idle-redraw fix) + the system event log.
 */
@Composable
fun LogScreen(state: CommsUiState, bright: Color, dim: Color) {
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text("SATELLITE LINKS", color = dim, fontFamily = Fonts.ChakraPetch, fontSize = 11.sp)
        Spacer(Modifier.height(6.dp))
        state.satellites.forEach { sat ->
            Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                Text(sat.name, color = bright, fontFamily = Fonts.ChakraPetch, fontSize = 12.sp, modifier = Modifier.weight(1f))
                Text(sat.status, color = dim, fontFamily = Fonts.ChakraPetch, fontSize = 11.sp)
                Spacer(Modifier.width(12.dp))
                Text("${sat.connectionDb}dB", color = dim, fontFamily = Fonts.ChakraPetch, fontSize = 11.sp)
            }
        }

        Box(Modifier.fillMaxWidth().height(1.dp).background(dim.copy(alpha = 0.3f)).padding(vertical = 10.dp))
        Spacer(Modifier.height(8.dp))

        Text("EVENT LOG", color = dim, fontFamily = Fonts.ChakraPetch, fontSize = 11.sp)
        Spacer(Modifier.height(6.dp))
        LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
            items(state.systemLogs) { entry ->
                val color = when (entry.type) {
                    LogType.ERROR -> Phosphor.Warn // a real alert log line -- the one legitimate --warn use
                    LogType.WARNING -> bright.copy(alpha = 0.8f)
                    LogType.SECURE -> bright
                    LogType.INFO -> dim
                }
                Text(
                    text = "[${entry.timestamp}] ${entry.message}",
                    color = color,
                    fontFamily = Fonts.ChakraPetch,
                    fontSize = 11.sp,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                )
            }
        }
    }
}
