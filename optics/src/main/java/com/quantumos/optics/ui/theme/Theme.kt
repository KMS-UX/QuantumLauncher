package com.quantumos.optics.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import com.quantumos.appshell.Phosphor

// App Shell Integration Step 2 — phosphor-only, sourced from the shared app-shell token object
// (no dynamic/Material-You color, no light scheme: the CRT aesthetic has exactly one look).
private val PhosphorColorScheme =
  darkColorScheme(
    primary = Phosphor.GreenBright,
    secondary = Phosphor.GreenDim,
    tertiary = Phosphor.CyanBright,
    background = Phosphor.Crt,
    surface = Phosphor.Crt,
  )

@Composable
fun MyApplicationTheme(content: @Composable () -> Unit) {
  MaterialTheme(colorScheme = PhosphorColorScheme, typography = Typography, content = content)
}
