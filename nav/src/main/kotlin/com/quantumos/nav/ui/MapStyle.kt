package com.quantumos.nav.ui

import android.content.Context
import com.quantumos.appshell.Phosphor
import com.quantumos.core.PhosphorHue

/*
 * Loads the phosphor map-style template from assets and tints it with the ACTIVE hue. The style is
 * a template with %%TOKEN%% slots filled from the one shared Phosphor source (:app-shell), so a
 * live hue switch recolours the map the same way it recolours the chrome — no off-palette color,
 * one token source.
 */
object MapStyle {
    private const val ASSET = "nav_phosphor_style.json"

    fun tinted(context: Context, hue: PhosphorHue): String {
        val template = context.assets.open(ASSET).bufferedReader().use { it.readText() }
        return template
            .replace("%%CRT%%", Phosphor.crtHex)
            .replace("%%BRIGHT%%", Phosphor.brightHex(hue))
            .replace("%%DIM%%", Phosphor.dimHex(hue))
            .replace("%%WATER_FILL%%", Phosphor.dimFillRgba(hue, 0.20f))
            .replace("%%LAND_FILL%%", Phosphor.dimFillRgba(hue, 0.10f))
            .replace("%%LANDCOVER_FILL%%", Phosphor.dimFillRgba(hue, 0.14f))
            .replace("%%BUILDING_FILL%%", Phosphor.dimFillRgba(hue, 0.28f))
    }
}
