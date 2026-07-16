package com.quantumos.audio.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quantumos.appshell.Fonts
import com.quantumos.appshell.Phosphor
import com.quantumos.audio.AudioViewModel

/*
 * CONFIG -- AUDIO's own thinned settings tab. Ported from the standalone repo's ConfigScreen.kt but
 * deliberately smaller: the source screen's "hardware simulation telemetry" (fake local battery/temp
 * sliders driving a fake local readiness readout) and "boot ceremony pace" control were the standalone
 * app's own stand-in for vitals it didn't otherwise have access to. Now that this module is docked,
 * the launcher owns REAL Vitality telemetry at the OS level (:core's VitalityState / M3 panel) and
 * boot pacing is a launcher-only concept -- reimplementing fake local copies of either here would be
 * exactly the kind of local reimplementation the Core Apps Fix-Pass exists to remove, and per :core's
 * own InstrumentSpec comment, CONFIG's real function already lives at the OS level. Flagging this
 * thinning as a scope call for the Director to review, not locking it silently.
 *
 * What's left is genuinely local to AUDIO: the operator call sign register (used in QUARK's local
 * flavor lines) and the phosphor line cycle (the same locally-scoped hue toggle Optics/Nav/QUARK's
 * own tab already use).
 */
@Composable
fun ConfigScreen(
    viewModel: AudioViewModel,
    themeColor: Color,
    dimColor: Color,
    activeHueName: String,
    onCycleHue: () -> Unit
) {
    val operatorNameState by viewModel.operatorName.collectAsState()
    var nameInput by remember { mutableStateOf(operatorNameState) }

    Column(modifier = Modifier.fillMaxSize().padding(14.dp)) {
        Text(
            text = "FIELD UNIT CONFIGURATION -- AUDIO MODULE",
            color = themeColor,
            fontSize = 13.sp,
            fontFamily = Fonts.ChakraPetch,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 14.dp)
        )

        Text(
            text = "OPERATOR CALL SIGN REGISTER ID",
            color = themeColor,
            fontSize = 10.sp,
            fontFamily = Fonts.ChakraPetch,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        TextField(
            value = nameInput,
            onValueChange = {
                nameInput = it
                viewModel.setOperatorName(it)
            },
            placeholder = {
                Text("OPERATOR", color = themeColor.copy(alpha = 0.4f), fontFamily = Fonts.ChakraPetch, fontSize = 13.sp)
            },
            textStyle = TextStyle(color = themeColor, fontSize = 13.sp, fontFamily = Fonts.ChakraPetch, fontWeight = FontWeight.Bold),
            colors = TextFieldDefaults.colors(
                focusedTextColor = themeColor,
                unfocusedTextColor = themeColor,
                focusedContainerColor = Phosphor.Crt,
                unfocusedContainerColor = Phosphor.Crt,
                disabledContainerColor = Phosphor.Crt,
                focusedIndicatorColor = themeColor,
                unfocusedIndicatorColor = themeColor.copy(alpha = 0.4f)
            ),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .border(width = 1.dp, color = themeColor.copy(alpha = 0.4f), shape = RoundedCornerShape(4.dp))
        )

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(color = themeColor.copy(alpha = 0.15f))
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "PHOSPHOR LINE",
                    color = themeColor,
                    fontSize = 12.sp,
                    fontFamily = Fonts.ChakraPetch,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Active hue: $activeHueName (local to this module)",
                    color = themeColor.copy(alpha = 0.5f),
                    fontSize = 9.sp,
                    fontFamily = Fonts.ChakraPetch
                )
            }
            Row(
                modifier = Modifier
                    .border(width = 1.dp, color = themeColor, shape = RoundedCornerShape(4.dp))
                    .clickable { onCycleHue() }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "CYCLE",
                    color = themeColor,
                    fontSize = 10.sp,
                    fontFamily = Fonts.ChakraPetch,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
