package com.quantumos.config.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quantumos.appshell.Fonts
import com.quantumos.appshell.Glyph
import com.quantumos.appshell.QuantumIcon
import com.quantumos.core.BootPace
import com.quantumos.core.DeploymentRegion
import com.quantumos.core.DeploymentRegions
import com.quantumos.core.PhosphorHue

/*
 * ConfigScreen -- CONFIG's main content (Task Brief §3): the single settings home. Three durable
 * settings, each a tap-to-cycle row (same interaction language the old STATUS-channel rows used,
 * ported here since this module now replaces that inline hop -- brief §3/acceptance §5). All three
 * persist via the shared SettingsStore (see ConfigViewModel) -- no local/fake copies of anything.
 */
@Composable
fun ConfigScreen(
    phosphorHue: PhosphorHue,
    bootPace: BootPace,
    region: DeploymentRegion,
    themeColor: Color,
    themeColorDim: Color,
    onCyclePhosphor: () -> Unit,
    onCycleBootPace: () -> Unit,
    onCycleRegion: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    Column(
        modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(horizontal = 16.dp)
    ) {
        Text(
            "FIELD UNIT CONFIGURATION",
            color = themeColor,
            fontFamily = Fonts.ChakraPetch,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "THE SINGLE SETTINGS HOME -- CHANGES HERE PERSIST ACROSS RESTARTS",
            color = themeColorDim,
            fontFamily = Fonts.ChakraPetch,
            fontSize = 9.sp
        )
        Spacer(Modifier.height(18.dp))

        SettingCycleRow(
            glyph = Glyph.Phosphor,
            label = "PHOSPHOR",
            value = phosphorHue.name,
            color = themeColor,
            dimColor = themeColorDim,
            onClick = onCyclePhosphor
        )
        SettingCycleRow(
            glyph = Glyph.BootPace,
            label = "BOOT PACE",
            value = if (bootPace == BootPace.DELIBERATE) "DELIBERATE" else "SNAPPY",
            color = themeColor,
            dimColor = themeColorDim,
            onClick = onCycleBootPace
        )
        SettingCycleRow(
            glyph = Glyph.Region,
            label = "DEPLOYMENT REGION",
            value = DeploymentRegions.label(region),
            color = themeColor,
            dimColor = themeColorDim,
            onClick = onCycleRegion
        )

        Spacer(Modifier.height(18.dp))
        Text(
            "----------------------------------------",
            color = themeColorDim,
            fontFamily = Fonts.ChakraPetch,
            fontSize = 12.sp
        )
        Spacer(Modifier.height(10.dp))
        Text(
            "NO NETWORK DEPENDENCY. THIS IS THE ONLY SETTINGS SURFACE IN THE FIELD UNIT.",
            color = themeColorDim,
            fontFamily = Fonts.ChakraPetch,
            fontSize = 9.sp
        )
    }
}

// A tappable settings row: dim label · bright value · ► cycle affordance. Tap cycles the setting
// (and persists it, see ConfigViewModel). Same tap-to-cycle pattern the old STATUS rows used.
@Composable
private fun SettingCycleRow(
    glyph: Glyph,
    label: String,
    value: String,
    color: Color,
    dimColor: Color,
    onClick: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        QuantumIcon(glyph, tint = color, size = 16.dp)
        Spacer(Modifier.width(8.dp))
        Text(label.padEnd(18), color = dimColor, fontFamily = Fonts.ChakraPetch, fontSize = 13.sp)
        Text(": $value", color = color, fontFamily = Fonts.ChakraPetch, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.weight(1f))
        Text("►", color = color, fontFamily = Fonts.ChakraPetch, fontSize = 13.sp)
    }
}
