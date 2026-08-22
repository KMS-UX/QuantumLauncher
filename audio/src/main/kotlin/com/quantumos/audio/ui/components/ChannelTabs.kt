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
import com.quantumos.appshell.Glyph
import com.quantumos.appshell.QuantumIcon
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Alignment

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
            val recording = ch == AudioChannel.RECORDER && isRecording
            val tint = when {
                recording -> Phosphor.Warn
                active -> themeColor
                else -> dimColor
            }
            // The live-recording mark was a bare "●" appended to the label. It is a drawn Dot
            // now, which means the bracket pair -- the house selection affordance -- has to sit
            // either side of a composable rather than inside one string. The whole tab stays ONE
            // click target: the clickable moved up to this Row, so nothing shrank.
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clickable { onSelect(ch) }
                    .padding(horizontal = 4.dp, vertical = 4.dp),
            ) {
                Text(
                    text = if (active) "[${ch.displayName}" else " ${ch.displayName}",
                    color = tint,
                    fontFamily = Fonts.ChakraPetch,
                    fontSize = 10.sp,
                )
                if (recording) {
                    Spacer(Modifier.width(4.dp))
                    QuantumIcon(Glyph.Dot, Phosphor.Warn, size = 7.dp)
                }
                Text(
                    text = if (active) "]" else " ",
                    color = tint,
                    fontFamily = Fonts.ChakraPetch,
                    fontSize = 10.sp,
                )
            }
        }
    }
}
