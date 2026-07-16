package com.quantumos.audio.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quantumos.appshell.Fonts
import com.quantumos.appshell.Phosphor
import com.quantumos.audio.AudioViewModel

/*
 * LOG -- AUDIO's own local system-event stream (distinct from the launcher's own M2 LOG channel,
 * same relationship the launcher's :core module documents for QUARK's ConversationEntry log vs.
 * systemLogs). Ported from the standalone repo's LogScreen.kt.
 */
@Composable
fun LogScreen(
    viewModel: AudioViewModel,
    themeColor: Color,
    dimColor: Color
) {
    val logsState by viewModel.systemLogs.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(14.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "AUDIO MODULE EVENT STREAM",
                color = themeColor,
                fontSize = 13.sp,
                fontFamily = Fonts.ChakraPetch,
                fontWeight = FontWeight.Bold
            )
            Button(
                onClick = { viewModel.clearLogs() },
                colors = ButtonDefaults.buttonColors(containerColor = themeColor.copy(alpha = 0.1f)),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier
                    .height(28.dp)
                    .border(width = 1.dp, color = themeColor, shape = RoundedCornerShape(4.dp)),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "PURGE CONSOLE",
                    color = themeColor,
                    fontSize = 9.sp,
                    fontFamily = Fonts.ChakraPetch,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .border(width = 1.dp, color = themeColor.copy(alpha = 0.4f), shape = RoundedCornerShape(4.dp))
                .background(Phosphor.Crt)
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            reverseLayout = true
        ) {
            items(logsState) { logLine ->
                Text(
                    text = logLine,
                    color = themeColor,
                    fontSize = 10.sp,
                    fontFamily = Fonts.ChakraPetch,
                    fontWeight = FontWeight.Normal,
                    lineHeight = 13.sp
                )
            }
        }
    }
}
