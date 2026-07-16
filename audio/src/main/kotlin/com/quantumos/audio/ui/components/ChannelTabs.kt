package com.quantumos.audio.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quantumos.appshell.Fonts
import com.quantumos.appshell.Phosphor
import com.quantumos.audio.AudioChannel

/*
 * AUDIO's own internal channel strip -- the "strip" of the App Shell's strip -> content -> action-
 * rail body pattern, scoped to this docked module's five in-app screens (RECORDER/PLAYER/QUARK/
 * CONFIG/LOG). Distinct from the launcher's HOME/APPS/STATUS/LOG chrome (:app-shell's ChannelStrip),
 * which is launcher-only.
 */
@Composable
fun ChannelTabs(
    current: AudioChannel,
    themeColor: Color,
    dimColor: Color,
    isRecording: Boolean,
    onSelect: (AudioChannel) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Phosphor.Crt)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        AudioChannel.entries.forEach { ch ->
            val active = ch == current
            val label = if (ch == AudioChannel.RECORDER && isRecording) "${ch.displayName} ●" else ch.displayName
            Text(
                text = if (active) "[$label]" else " $label ",
                color = when {
                    ch == AudioChannel.RECORDER && isRecording -> Phosphor.Warn
                    active -> themeColor
                    else -> dimColor
                },
                fontFamily = Fonts.ChakraPetch,
                fontSize = 10.sp,
                modifier = Modifier
                    .clickable { onSelect(ch) }
                    .padding(horizontal = 4.dp, vertical = 4.dp)
            )
        }
    }
}
