package com.quantumos.optics.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import com.quantumos.appshell.Fonts
import com.quantumos.appshell.Phosphor

/*
 * House-style replacement for the standalone Optics app's ui/theme/{Theme,Type}.kt (App Shell
 * Integration, Phase 3). The original wrapped content in a Material3 `dynamicColor` scheme (device
 * wallpaper-derived Material You colors) and resolved Chakra Petch via a Google Fonts
 * `GoogleFont.Provider` with a placeholder/empty certificate array — both are house-style
 * violations (one phosphor token source; bundled font, no runtime provider risk). This keeps the
 * many existing `style = MaterialTheme.typography.*` call sites in the ported camera UI working,
 * but backs every role with the shared :app-shell bundled Chakra Petch and a fixed phosphor color
 * scheme — never dynamic, never off-palette.
 */
private val OpticsTypography: Typography = Typography().let { base ->
    val face = Fonts.ChakraPetch
    Typography(
        displayLarge = base.displayLarge.copy(fontFamily = face),
        displayMedium = base.displayMedium.copy(fontFamily = face),
        displaySmall = base.displaySmall.copy(fontFamily = face),
        headlineLarge = base.headlineLarge.copy(fontFamily = face),
        headlineMedium = base.headlineMedium.copy(fontFamily = face),
        headlineSmall = base.headlineSmall.copy(fontFamily = face),
        titleLarge = base.titleLarge.copy(fontFamily = face),
        titleMedium = base.titleMedium.copy(fontFamily = face),
        titleSmall = base.titleSmall.copy(fontFamily = face),
        bodyLarge = base.bodyLarge.copy(fontFamily = face),
        bodyMedium = base.bodyMedium.copy(fontFamily = face),
        bodySmall = base.bodySmall.copy(fontFamily = face),
        labelLarge = base.labelLarge.copy(fontFamily = face),
        labelMedium = base.labelMedium.copy(fontFamily = face),
        labelSmall = base.labelSmall.copy(fontFamily = face)
    )
}

private val OpticsColorScheme = darkColorScheme(
    primary = Phosphor.GreenBright,
    secondary = Phosphor.GreenDim,
    tertiary = Phosphor.CyanBright,
    background = Phosphor.Crt,
    surface = Phosphor.Crt
)

// No dynamic color, no light-theme branch — the phosphor CRT ground is the ONE ground, always
// (house style: one token source, no off-palette colors).
@Composable
fun OpticsTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = OpticsColorScheme, typography = OpticsTypography, content = content)
}
