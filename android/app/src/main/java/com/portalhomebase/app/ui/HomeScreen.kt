package com.portalhomebase.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.portalhomebase.app.data.BoardState
import com.portalhomebase.app.data.Accessory
import com.portalhomebase.app.ui.theme.Ink
import com.portalhomebase.app.ui.theme.CardBg
import com.portalhomebase.app.ui.theme.Muted
import com.portalhomebase.app.ui.theme.OnAccent
import com.portalhomebase.app.ui.theme.Outline
import com.portalhomebase.app.ui.theme.Toggle
import com.portalhomebase.app.ui.theme.ToggleBorder
import com.portalhomebase.app.ui.theme.ToggleOff
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun HomeScreen(state: BoardState, modifier: Modifier = Modifier) {
    val accessories by state.accessories.collectAsState()
    val scope = rememberCoroutineScope()
    val shown = accessories.filter { it.on != null || it.tempC != null }

    if (shown.isEmpty()) {
        Text("No devices — check Homebridge.", style = MaterialTheme.typography.bodyLarge, color = Muted)
    } else {
        Text(
            "${shown.size} devices", style = MaterialTheme.typography.bodySmall, color = Muted,
            modifier = Modifier.padding(bottom = 16.dp),
        )
        LazyVerticalGrid(
            columns = GridCells.Adaptive(360.dp),
            modifier = modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            items(shown, key = { it.uniqueId }) { acc ->
                AccessoryCard(
                    acc = acc,
                    onToggle = { scope.launch { state.runAction { setCharacteristic(acc.uniqueId, "On", it) } } },
                    onBrightness = { scope.launch { state.runAction { setCharacteristic(acc.uniqueId, "Brightness", it) } } },
                )
            }
        }
    }
}

@Composable
private fun AccessoryCard(acc: Accessory, onToggle: (Boolean) -> Unit, onBrightness: (Int) -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
    ) {
        Column(Modifier.padding(22.dp)) {
            Text(acc.name, style = MaterialTheme.typography.titleLarge, color = Ink)
            Text(acc.type, style = MaterialTheme.typography.labelSmall, color = Muted)
            Spacer(Modifier.height(12.dp))
            acc.tempC?.let { c ->
                val f = (c * 9 / 5 + 32).roundToInt()
                Text("$f°F · ${(c * 10).roundToInt() / 10.0}°C", style = MaterialTheme.typography.headlineSmall, color = Ink)
                Spacer(Modifier.height(12.dp))
            }
            if (acc.on != null && acc.onWritable) {
                Button(
                    onClick = { onToggle(!acc.on) },
                    modifier = Modifier.fillMaxWidth().height(64.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (acc.on) Toggle else ToggleOff,
                    ),
                    border = BorderStroke(1.dp, if (acc.on) ToggleBorder else Outline),
                ) {
                    Text(if (acc.on) "ON" else "OFF", style = MaterialTheme.typography.titleMedium, color = if (acc.on) OnAccent else Ink)
                }
            }
            if (acc.brightness != null && acc.brightnessWritable) {
                var slider by remember(acc.uniqueId, acc.brightness) { mutableFloatStateOf(acc.brightness.toFloat()) }
                Spacer(Modifier.height(12.dp))
                Slider(
                    value = slider,
                    onValueChange = { slider = it },
                    onValueChangeFinished = { onBrightness(slider.roundToInt()) },
                    valueRange = 0f..100f,
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Brightness", style = MaterialTheme.typography.bodySmall, color = Muted)
                    Text("${slider.roundToInt()}%", style = MaterialTheme.typography.bodySmall, color = Muted)
                }
            }
        }
    }
}
