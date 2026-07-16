package com.quantumos.nav.ui

import androidx.compose.ui.graphics.Color
import com.quantumos.appshell.Phosphor
import com.quantumos.core.PhosphorHue

/*
 * Hex/rgba helpers for templating the MapLibre style JSON at runtime from the shared Phosphor
 * token source (App Shell Integration, Phase 3 -- these used to live on Nav's own local, now-
 * deleted Phosphor object; the map-styling need is Nav-specific, so it stays here rather than
 * moving into :app-shell, which has no MapLibre/map concern).
 */

private fun Color.toArgbInt(): Int {
    val r = (red * 255f).toInt() and 0xFF
    val g = (green * 255f).toInt() and 0xFF
    val b = (blue * 255f).toInt() and 0xFF
    return (r shl 16) or (g shl 8) or b
}

/** Hex strings (no leading `0xFF` alpha) for templating the MapLibre style JSON at runtime. */
fun Phosphor.brightHex(h: PhosphorHue) = "#%06X".format(0xFFFFFF and bright(h).toArgbInt())
fun Phosphor.dimHex(h: PhosphorHue) = "#%06X".format(0xFFFFFF and dim(h).toArgbInt())
val Phosphor.crtHex get() = "#%06X".format(0xFFFFFF and Crt.toArgbInt())

/** A faint fill of the dim hue — used for water/landuse layers so they read as tone, not line. */
fun Phosphor.dimFillRgba(h: PhosphorHue, alpha: Float): String {
    val d = dim(h)
    val r = (d.red * 255f).toInt(); val g = (d.green * 255f).toInt(); val b = (d.blue * 255f).toInt()
    return "rgba($r,$g,$b,$alpha)"
}
