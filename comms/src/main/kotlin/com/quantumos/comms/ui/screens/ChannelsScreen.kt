package com.quantumos.comms.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quantumos.appshell.Fonts
import com.quantumos.appshell.Glyph
import com.quantumos.appshell.QuantumIcon
import com.quantumos.comms.CommsUiState
import com.quantumos.comms.CommsViewModel
import com.quantumos.comms.ui.components.LivePulseDot

/*
 * The transmission-log redesign (Core Apps Fix-Pass, Decision 86): a single-column callsign log,
 * NOT a Discord-style channel sidebar or a WhatsApp-style bubble thread. No avatars, no rounded chat
 * bubbles, no left/right alignment by sender -- every entry (incoming or the Operator's own outgoing
 * transmission) sits in the same flat log with a thin left-edge phosphor rule, read top-to-bottom
 * like a radio operator's log.
 */
@Composable
fun ChannelsScreen(
    viewModel: CommsViewModel,
    state: CommsUiState,
    bright: Color,
    dim: Color
) {
    var openChannel by remember { mutableStateOf<String?>(null) }

    if (openChannel == null) {
        ChannelListLog(state = state, bright = bright, dim = dim, onOpen = { openChannel = it })
    } else {
        val name = openChannel!!
        TransmissionThread(
            channelName = name,
            state = state,
            viewModel = viewModel,
            bright = bright,
            dim = dim,
            onBack = { openChannel = null }
        )
    }
}

@Composable
private fun ChannelListLog(
    state: CommsUiState,
    bright: Color,
    dim: Color,
    onOpen: (String) -> Unit
) {
    LazyColumn(Modifier.fillMaxSize()) {
        items(state.channels, key = { it.name }) { channel ->
            Column(
                Modifier
                    .fillMaxWidth()
                    .clickable { onOpen(channel.name) }
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    QuantumIcon(Glyph.CommsChannel, tint = bright, size = 14.dp)
                    Spacer(Modifier.width(6.dp))
                    LivePulseDot(pulseTrigger = channel.pulseTrigger, color = bright)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = channel.callsign,
                        color = bright,
                        fontFamily = Fonts.ChakraPetch,
                        fontSize = 15.sp
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = channel.name,
                        color = dim,
                        fontFamily = Fonts.ChakraPetch,
                        fontSize = 11.sp
                    )
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = channel.functionLine,
                    color = dim,
                    fontFamily = Fonts.ChakraPetch,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(start = 15.dp)
                )
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(dim.copy(alpha = 0.3f)))
        }
    }
}

@Composable
private fun TransmissionThread(
    channelName: String,
    state: CommsUiState,
    viewModel: CommsViewModel,
    bright: Color,
    dim: Color,
    onBack: () -> Unit
) {
    val channel = state.channels.firstOrNull { it.name == channelName }
    val log = state.transmissions[channelName].orEmpty()

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "◄ CHANNELS",
                color = dim,
                fontFamily = Fonts.ChakraPetch,
                fontSize = 11.sp,
                modifier = Modifier.clickable { onBack() }
            )
            Spacer(Modifier.width(14.dp))
            Text(
                text = channel?.callsign ?: channelName,
                color = bright,
                fontFamily = Fonts.ChakraPetch,
                fontSize = 13.sp
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = "[ SIGNAL CHECK ]",
                color = dim,
                fontFamily = Fonts.ChakraPetch,
                fontSize = 10.sp,
                modifier = Modifier.clickable { viewModel.simulateIncomingTransmission(channelName) }
            )
        }

        LazyColumn(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp)) {
            items(log) { tx ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min)
                        .padding(vertical = 6.dp)
                ) {
                    Box(
                        Modifier
                            .width(2.dp)
                            .fillMaxHeight()
                            .background(dim.copy(alpha = 0.5f))
                    )
                    Column(
                        Modifier
                            .padding(start = 8.dp)
                    ) {
                        Text(
                            text = "[${tx.timestamp}] ${if (tx.isOutgoing) "> " else ""}${tx.sender}",
                            color = dim,
                            fontFamily = Fonts.ChakraPetch,
                            fontSize = 10.sp
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = tx.text,
                            color = if (tx.isOutgoing) bright.copy(alpha = 0.85f) else bright,
                            fontFamily = Fonts.ChakraPetch,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("> ", color = bright, fontFamily = Fonts.ChakraPetch, fontSize = 13.sp)
            BasicTextField(
                value = state.currentInput,
                onValueChange = viewModel::updateInput,
                textStyle = TextStyle(color = bright, fontFamily = Fonts.ChakraPetch, fontSize = 13.sp),
                modifier = Modifier.weight(1f),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(bright)
            )
            Text(
                text = "[SEND]",
                color = bright,
                fontFamily = Fonts.ChakraPetch,
                fontSize = 11.sp,
                modifier = Modifier.clickable { viewModel.sendMessage() }.padding(start = 8.dp)
            )
        }
    }
}
