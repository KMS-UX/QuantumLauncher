package com.quantumos.files.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quantumos.appshell.Fonts
import com.quantumos.appshell.Phosphor
import com.quantumos.appshell.PleaseStandbyCard
import com.quantumos.files.viewmodel.FileExplorerViewModel

/*
 * DecryptorView -- ported from the standalone app's DecryptorView, keeping the surrounding shell
 * (input/output pane, the "DECRYPT AI" terminal-log framing) intact per fix-pass §6, but the old
 * Gemini "high thinking" call is entirely gone: this now reads FileExplorerViewModel's
 * AiAssistBridge-backed state instead.
 *  - aiLoading      -> the house-style PLEASE STANDBY card (never a CircularProgressIndicator).
 *  - aiUnavailableReason -> a clearly-styled offline/standby message (AiAssistResult.Unavailable).
 *  - aiResultText   -> a genuine AiAssistResult.Answer, if a real backend is ever wired in.
 * Wiring a real implementation later is a one-line change at the ViewModel's bridge call site.
 */
@Composable
fun DecryptorView(
    viewModel: FileExplorerViewModel,
    primaryColor: Color,
    dimColor: Color
) {
    val logs = viewModel.thinkingLogState

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "QUANTUM DECRYPT MODULE",
            fontFamily = Fonts.ChakraPetch,
            fontSize = 14.sp,
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
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                when {
                    viewModel.aiLoading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            PleaseStandbyCard(
                                subline = logs ?: "QUERYING AI ASSIST BRIDGE...",
                                color = primaryColor,
                                dimColor = dimColor,
                                font = Fonts.ChakraPetch
                            )
                        }
                    }
                    viewModel.aiUnavailableReason != null -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "AI BRIDGE OFFLINE",
                                    fontFamily = Fonts.ChakraPetch,
                                    fontSize = 14.sp,
                                    color = Phosphor.Warn,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = viewModel.aiUnavailableReason.orEmpty(),
                                    fontFamily = Fonts.ChakraPetch,
                                    fontSize = 12.sp,
                                    color = dimColor,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                    viewModel.aiResultText != null -> {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            item {
                                Text(
                                    text = viewModel.aiResultText.orEmpty(),
                                    fontFamily = Fonts.ChakraPetch,
                                    fontSize = 13.sp,
                                    color = primaryColor,
                                    lineHeight = 22.sp
                                )
                            }
                        }
                    }
                    else -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = "NO ENVELOPE SECTOR SELECTED\n\nNavigate to Explorer, select a txt/log file, and tap 'DECRYPT AI' to invoke the AI assist bridge.",
                                fontFamily = Fonts.ChakraPetch,
                                fontSize = 12.sp,
                                color = dimColor,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}
