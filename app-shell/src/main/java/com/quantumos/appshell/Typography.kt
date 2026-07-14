package com.quantumos.appshell

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

/*
 * QuantumOS — bundled type faces (M6 Step 1). Resolves the M0-deferred font placeholder.
 *
 *  - Chakra Petch — the squared industrial-technical SYSTEM face (Monofonto substitute). Replaces
 *    FontFamily.Monospace everywhere it was standing in: the terminal readout, STATUS, LOG, the
 *    Vitality panel, and the Assistant View. Bundled as real .ttf in res/font (Regular/Medium/Bold)
 *    — NOT Downloadable Fonts, so it renders identically offline in the field.
 *  - Monoton — neon-tube DISPLAY accent. ONE blessed ceremonial use only: the boot-splash wordmark
 *    stamp (Step 3). Never a body or system face (house style / M6 hard stop).
 */
object Fonts {
    val ChakraPetch = FontFamily(
        Font(R.font.chakra_petch_regular, FontWeight.Normal),
        Font(R.font.chakra_petch_medium, FontWeight.Medium),
        Font(R.font.chakra_petch_bold, FontWeight.Bold)
    )

    val Monoton = FontFamily(Font(R.font.monoton_regular, FontWeight.Normal))
}
