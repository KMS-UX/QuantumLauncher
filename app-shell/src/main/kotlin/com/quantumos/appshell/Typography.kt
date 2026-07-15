package com.quantumos.appshell

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

/*
 * QuantumOS — bundled type faces, shared by every docked module (App Shell Integration, Phase 3).
 *
 *  - Chakra Petch — the squared industrial-technical SYSTEM face (Monofonto substitute). Bundled as
 *    real .ttf in res/font (Regular/Medium/Bold) — NOT Downloadable Fonts, so it renders identically
 *    offline in the field, and no runtime font-provider/certificate risk for docked modules.
 *  - Monoton — neon-tube DISPLAY accent. ONE blessed ceremonial use only: the boot-splash wordmark
 *    stamp. Never a body or system face (house style hard stop).
 */
object Fonts {
    val ChakraPetch = FontFamily(
        Font(R.font.chakra_petch_regular, FontWeight.Normal),
        Font(R.font.chakra_petch_medium, FontWeight.Medium),
        Font(R.font.chakra_petch_bold, FontWeight.Bold)
    )

    val Monoton = FontFamily(Font(R.font.monoton_regular, FontWeight.Normal))
}
