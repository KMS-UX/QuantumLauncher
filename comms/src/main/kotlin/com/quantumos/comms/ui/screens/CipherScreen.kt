package com.quantumos.comms.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quantumos.appshell.Fonts
import com.quantumos.appshell.PleaseStandbyCard
import com.quantumos.comms.CommsUiState
import com.quantumos.comms.CommsViewModel

/*
 * The cipher-decryption terminal -- the one feature the earlier audit flagged as genuinely on-
 * identity, kept intact. Real local decoding (Base64/Morse/ROT13, see CommsViewModel) -- no network,
 * no AI call. The bounded isDecrypting beat uses the real PleaseStandbyCard, never a spinner.
 */
@Composable
fun CipherScreen(
    viewModel: CommsViewModel,
    state: CommsUiState,
    bright: Color,
    dim: Color
) {
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text("INTERCEPTED SIGNALS", color = dim, fontFamily = Fonts.ChakraPetch, fontSize = 11.sp)
        Spacer(Modifier.height(6.dp))
        LazyColumn(Modifier.fillMaxWidth().height(96.dp)) {
            items(viewModel.preloadedCiphers().withIndex().toList()) { (index, cipher) ->
                val selected = index == state.selectedPreloadedCipherIndex
                Text(
                    text = (if (selected) "> " else "  ") + cipher.label,
                    color = if (selected) bright else dim,
                    fontFamily = Fonts.ChakraPetch,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.selectPreloadedCipher(index) }
                        .padding(vertical = 4.dp)
                )
            }
        }

        Box(Modifier.fillMaxWidth().height(1.dp).background(dim.copy(alpha = 0.3f)).padding(vertical = 8.dp))
        Spacer(Modifier.height(8.dp))

        Text("PAYLOAD", color = dim, fontFamily = Fonts.ChakraPetch, fontSize = 11.sp)
        Spacer(Modifier.height(4.dp))
        BasicTextField(
            value = state.cipherInputText,
            onValueChange = viewModel::updateCipherInput,
            textStyle = TextStyle(color = bright, fontFamily = Fonts.ChakraPetch, fontSize = 13.sp),
            cursorBrush = SolidColor(bright),
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .background(bright.copy(alpha = 0.05f))
                .padding(8.dp)
        )

        Spacer(Modifier.height(10.dp))
        Text(
            text = "[ ACTIVATE DECRYPTOR ]",
            color = bright,
            fontFamily = Fonts.ChakraPetch,
            fontSize = 12.sp,
            modifier = Modifier.clickable { viewModel.decryptCipher() }
        )

        Spacer(Modifier.height(14.dp))

        if (state.isDecrypting) {
            Box(Modifier.fillMaxWidth().height(96.dp), contentAlignment = androidx.compose.ui.Alignment.Center) {
                PleaseStandbyCard(
                    subline = "RESOLVING DECRYPTION MATRIX…",
                    color = bright,
                    dimColor = dim,
                    font = Fonts.ChakraPetch
                )
            }
        } else if (state.cipherOutputText.isNotEmpty()) {
            Text("OUTPUT", color = dim, fontFamily = Fonts.ChakraPetch, fontSize = 11.sp)
            Spacer(Modifier.height(4.dp))
            Text(
                text = state.cipherOutputText,
                color = bright,
                fontFamily = Fonts.ChakraPetch,
                fontSize = 13.sp
            )
        }
    }
}
