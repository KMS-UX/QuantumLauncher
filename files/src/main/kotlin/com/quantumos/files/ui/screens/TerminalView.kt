package com.quantumos.files.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quantumos.appshell.Fonts
import com.quantumos.appshell.Phosphor
import com.quantumos.files.viewmodel.FileExplorerViewModel

/*
 * TerminalView -- ported from the standalone app's TerminalView (the ls/cd/cat/create/edit/rm/
 * decrypt/quark/phosphor/stealth/beacon/clear command set, unchanged), restyled onto :app-shell's
 * Phosphor tokens/Chakra Petch font.
 */
@Composable
fun TerminalView(
    viewModel: FileExplorerViewModel,
    primaryColor: Color,
    dimColor: Color
) {
    val history by viewModel.terminalHistory.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "QUANTUM TERMINAL INTEL-LINK",
            fontFamily = Fonts.ChakraPetch,
            fontSize = 13.sp,
            color = primaryColor
        )

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
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(history) { line ->
                    Text(
                        text = line,
                        fontFamily = Fonts.ChakraPetch,
                        fontSize = 12.sp,
                        color = primaryColor
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$ ",
                fontFamily = Fonts.ChakraPetch,
                fontSize = 14.sp,
                color = primaryColor
            )

            OutlinedTextField(
                value = viewModel.terminalInput,
                onValueChange = { viewModel.terminalInput = it },
                textStyle = TextStyle(
                    fontFamily = Fonts.ChakraPetch,
                    fontSize = 14.sp,
                    color = primaryColor
                ),
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    cursorColor = primaryColor,
                    focusedTextColor = primaryColor,
                    unfocusedTextColor = primaryColor
                ),
                singleLine = true
            )

            IconButton(
                onClick = {
                    if (viewModel.terminalInput.isNotBlank()) {
                        viewModel.executeTerminalCommand(viewModel.terminalInput)
                    }
                }
            ) {
                Icon(
                    imageVector = Icons.Filled.Send,
                    contentDescription = "Execute Command",
                    tint = primaryColor
                )
            }
        }
    }
}
