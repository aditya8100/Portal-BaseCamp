package com.portalhomebase.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.portalhomebase.app.data.BoardApi
import com.portalhomebase.app.data.BoardState
import com.portalhomebase.app.ui.theme.CardBg
import com.portalhomebase.app.ui.theme.Ink
import com.portalhomebase.app.ui.theme.Muted
import com.portalhomebase.app.ui.theme.Outline
import com.portalhomebase.app.ui.theme.Selected
import com.portalhomebase.app.ui.theme.SelectedBorder
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    state: BoardState,
    modifier: Modifier = Modifier,
    themeMode: String,
    onThemeMode: (String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val (savedUrl, savedToken) = remember { state.currentConnection() }
    var url by remember { mutableStateOf(savedUrl) }
    var token by remember { mutableStateOf(savedToken) }
    var result by remember { mutableStateOf("") }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Solid cards: bare text and fields drown in the skyline behind them.
        Surface(shape = RoundedCornerShape(18.dp), color = CardBg) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Appearance", style = MaterialTheme.typography.titleLarge, color = Ink)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    listOf("auto", "light", "dark").forEach { mode ->
                        val active = themeMode == mode
                        Button(
                            onClick = { onThemeMode(mode) },
                            modifier = Modifier.widthIn(min = 160.dp).height(64.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (active) Selected else CardBg,
                            ),
                            border = BorderStroke(1.dp, if (active) SelectedBorder else Outline),
                        ) {
                            Text(
                                mode.replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.titleMedium, color = Ink,
                            )
                        }
                    }
                }
                Text(
                    "Auto follows the sun: light between sunrise and sunset.",
                    style = MaterialTheme.typography.bodySmall, color = Muted,
                )
            }
        }
        Surface(shape = RoundedCornerShape(18.dp), color = CardBg) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Server connection", style = MaterialTheme.typography.titleLarge, color = Ink)
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("Server URL") },
                    placeholder = { Text("http://192.168.1.73:8091") },
                    modifier = Modifier.widthIn(max = 640.dp).fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                )
                OutlinedTextField(
                    value = token,
                    onValueChange = { token = it },
                    label = { Text("Board token") },
                    modifier = Modifier.widthIn(max = 640.dp).fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                )
                Button(
                    onClick = {
                        scope.launch {
                            result = "testing…"
                            val ok = try {
                                BoardApi(url, token).health()
                            } catch (e: Exception) {
                                false
                            }
                            result = if (ok) "connected ✓ — saving" else "failed — check URL and token"
                            if (ok) {
                                state.saveConnection(url, token)
                                state.refreshAll()
                            }
                        }
                    },
                    modifier = Modifier.height(64.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Selected),
                ) {
                    Text("Test & Save", style = MaterialTheme.typography.titleMedium, color = Ink)
                }
                if (result.isNotEmpty()) {
                    Text(result, style = MaterialTheme.typography.bodyMedium, color = Muted)
                }
                Text(
                    "Tip: values persist on this Portal. The token lives only here and on the mini.",
                    style = MaterialTheme.typography.bodySmall, color = Muted,
                )
            }
        }
    }
}
