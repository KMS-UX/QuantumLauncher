package com.quantumos.files.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import com.quantumos.appshell.Phosphor
import com.quantumos.files.viewmodel.FileExplorerViewModel
import com.quantumos.files.viewmodel.QuarkChatState

/*
 * QuarkChatView -- ported from the standalone app's QuarkChatView (FILES's own in-app co-pilot
 * dialogue, distinct from the real launcher QUARK assistant). Its Gemini-backed talkToQuark() call
 * is now routed through FileExplorerViewModel's AiAssistBridge (see fix-pass §6 notes on the
 * ViewModel) -- the input/output pane and conversation-log framing stay intact.
 */
@Composable
fun QuarkChatView(
    viewModel: FileExplorerViewModel,
    primaryColor: Color,
    dimColor: Color
) {
    val messages by viewModel.quarkConversation.collectAsState()
    var inputChat by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(
                        color = if (viewModel.quarkState == QuarkChatState.SCAN) Phosphor.AmberBright else primaryColor,
                        shape = RoundedCornerShape(6.dp)
                    )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "QUARK CO-PILOT DIALOGUE",
                fontFamily = Fonts.ChakraPetch,
                fontSize = 13.sp,
                color = primaryColor
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Surface(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            color = Phosphor.Crt,
            border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.5f))
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (messages.isEmpty()) {
                    item {
                        Text(
                            text = "QUARK: 'Operator online. Secure files mapped. Ask me to scan sector directories or describe current sensor captures.'",
                            fontFamily = Fonts.ChakraPetch,
                            fontSize = 13.sp,
                            color = primaryColor
                        )
                    }
                } else {
                    items(messages) { msg ->
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = if (msg.isUser) Alignment.End else Alignment.Start
                        ) {
                            Text(
                                text = "${msg.sender} [${msg.timestamp}]:",
                                fontFamily = Fonts.ChakraPetch,
                                fontSize = 10.sp,
                                color = dimColor
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Surface(
                                color = if (msg.isUser) primaryColor.copy(alpha = 0.1f) else Color.Transparent,
                                border = if (msg.isUser) BorderStroke(1.dp, primaryColor.copy(alpha = 0.3f)) else null,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = msg.text,
                                    fontFamily = Fonts.ChakraPetch,
                                    fontSize = 13.sp,
                                    color = primaryColor,
                                    modifier = Modifier.padding(6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputChat,
                onValueChange = { inputChat = it },
                textStyle = TextStyle(
                    fontFamily = Fonts.ChakraPetch,
                    fontSize = 13.sp,
                    color = primaryColor
                ),
                placeholder = { Text("Consult with QUARK...", color = dimColor) },
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = primaryColor,
                    unfocusedBorderColor = dimColor,
                    cursorColor = primaryColor,
                    focusedTextColor = primaryColor,
                    unfocusedTextColor = primaryColor
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = {
                    if (inputChat.isNotBlank()) {
                        viewModel.talkToQuark(inputChat)
                        inputChat = ""
                    }
                }
            ) {
                Icon(
                    imageVector = Icons.Filled.Send,
                    contentDescription = "Send message",
                    tint = primaryColor
                )
            }
        }
    }
}
