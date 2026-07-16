package com.quantumos.comms.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quantumos.appshell.Fonts
import com.quantumos.appshell.Phosphor
import com.quantumos.comms.CommsViewModel
import com.quantumos.core.PhosphorHue

private enum class CommsTab { CHANNELS, CIPHER, LOG }

/*
 * COMMS's own internal navigation -- not the launcher's HOME/APPS/STATUS/LOG channel strip (that's
 * launcher-only chrome per :app-shell). Three tabs: CHANNELS (the transmission-log redesign, Core
 * Apps Fix-Pass), CIPHER (the real local decryption terminal, kept on-identity), LOG (satellites +
 * system event log). No live AI-chat tab -- the Gemini-backed persona was stripped entirely (see
 * CommsViewModel doc comment).
 */
@Composable
fun CommsScreen(
    viewModel: CommsViewModel,
    hue: PhosphorHue,
    onCycleHue: () -> Unit,
    contentPadding: PaddingValues
) {
    var tab by remember { mutableStateOf(CommsTab.CHANNELS) }
    val bright = Phosphor.bright(hue)
    val dim = Phosphor.dim(hue)
    val state by viewModel.state.collectAsState()

    Column(Modifier.fillMaxSize().padding(contentPadding)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CommsTab.entries.forEach { t ->
                val active = t == tab
                Text(
                    text = if (active) "[${t.name}]" else " ${t.name} ",
                    color = if (active) bright else dim,
                    fontFamily = Fonts.ChakraPetch,
                    fontSize = 11.sp,
                    modifier = Modifier
                        .clickable { tab = t }
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                )
            }
            Box(Modifier.weight(1f))
            Text(
                text = "[HUE: ${hue.name}]",
                color = dim,
                fontFamily = Fonts.ChakraPetch,
                fontSize = 10.sp,
                modifier = Modifier.clickable { onCycleHue() }.padding(6.dp)
            )
        }

        Box(
            Modifier
                .fillMaxWidth()
                .background(bright.copy(alpha = 0.25f))
                .padding(vertical = 0.5.dp)
        )

        when (tab) {
            CommsTab.CHANNELS -> ChannelsScreen(viewModel = viewModel, state = state, bright = bright, dim = dim)
            CommsTab.CIPHER -> CipherScreen(viewModel = viewModel, state = state, bright = bright, dim = dim)
            CommsTab.LOG -> LogScreen(state = state, bright = bright, dim = dim)
        }
    }
}
