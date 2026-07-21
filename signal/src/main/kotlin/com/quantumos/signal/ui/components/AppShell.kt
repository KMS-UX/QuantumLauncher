package com.quantumos.signal.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quantumos.appshell.BackHomeAffordance
import com.quantumos.appshell.Fonts
import com.quantumos.appshell.NameplateHeader
import com.quantumos.appshell.Phosphor
import com.quantumos.appshell.QuantumOSLayoutShell

/*
 * SIGNAL's docked App Shell wrapper (SIGNAL + CONFIG Task Brief §4). Delegates the CRT container and
 * nameplate to :app-shell -- never reimplemented locally, same shape as RADIO's wrapper. No
 * BackHandler: the Shell owns back once docked (system/predictive back finishes this Activity,
 * returning to the still-live launcher HOME); the explicit "◄ HOME" line is the visible equivalent.
 */
@Composable
fun AppShell(
    title: String,
    themeColor: Color = Phosphor.GreenBright,
    onReturnHome: () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    QuantumOSLayoutShell(forceFixedContainer = false) {
        Box(Modifier.fillMaxSize()) {
            content(PaddingValues(top = 56.dp))
            Box(Modifier.align(Alignment.TopStart).fillMaxSize()) {
                NameplateHeader(
                    channelName = title.uppercase(),
                    color = themeColor,
                    dimColor = themeColor.copy(alpha = 0.6f),
                    font = Fonts.ChakraPetch
                )
                BackHomeAffordance(
                    color = themeColor.copy(alpha = 0.6f),
                    font = Fonts.ChakraPetch,
                    onReturnHome = onReturnHome,
                    modifier = Modifier.align(Alignment.TopEnd).padding(top = 34.dp, end = 16.dp)
                )
            }
        }
    }
}
