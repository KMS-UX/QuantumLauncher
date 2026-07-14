package com.quantumos.optics.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.quantumos.appshell.Fonts

// App Shell Integration Step 2 — Optics inherits its type face from the shared app-shell module
// (bundled Chakra Petch) instead of its own Google-Fonts-provider setup, which never actually
// resolved at runtime (the cert-fingerprint array in font_certs.xml was a placeholder — Compose
// was silently falling back to the platform default font). Text-style shapes are unchanged.
val Typography =
  Typography(
    displayLarge =
      TextStyle(
        fontFamily = Fonts.ChakraPetch,
        fontWeight = FontWeight.Normal,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = (-0.25).sp,
      ),
    displayMedium =
      TextStyle(
        fontFamily = Fonts.ChakraPetch,
        fontWeight = FontWeight.Normal,
        fontSize = 45.sp,
        lineHeight = 52.sp,
        letterSpacing = 0.sp,
      ),
    displaySmall =
      TextStyle(
        fontFamily = Fonts.ChakraPetch,
        fontWeight = FontWeight.Normal,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = 0.sp,
      ),
    headlineLarge =
      TextStyle(
        fontFamily = Fonts.ChakraPetch,
        fontWeight = FontWeight.Normal,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp,
      ),
    headlineMedium =
      TextStyle(
        fontFamily = Fonts.ChakraPetch,
        fontWeight = FontWeight.Normal,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp,
      ),
    headlineSmall =
      TextStyle(
        fontFamily = Fonts.ChakraPetch,
        fontWeight = FontWeight.Normal,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp,
      ),
    titleLarge =
      TextStyle(
        fontFamily = Fonts.ChakraPetch,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 1.sp,
      ),
    titleMedium =
      TextStyle(
        fontFamily = Fonts.ChakraPetch,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.8.sp,
      ),
    titleSmall =
      TextStyle(
        fontFamily = Fonts.ChakraPetch,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
      ),
    bodyLarge =
      TextStyle(
        fontFamily = Fonts.ChakraPetch,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp,
      ),
    bodyMedium =
      TextStyle(
        fontFamily = Fonts.ChakraPetch,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp,
      ),
    bodySmall =
      TextStyle(
        fontFamily = Fonts.ChakraPetch,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp,
      ),
    labelLarge =
      TextStyle(
        fontFamily = Fonts.ChakraPetch,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
      ),
    labelMedium =
      TextStyle(
        fontFamily = Fonts.ChakraPetch,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp,
      ),
    labelSmall =
      TextStyle(
        fontFamily = Fonts.ChakraPetch,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp,
      ),
  )
