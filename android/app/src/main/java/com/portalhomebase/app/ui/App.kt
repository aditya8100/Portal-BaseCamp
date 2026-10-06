package com.portalhomebase.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.unit.dp
import com.portalhomebase.app.data.BoardState
import com.portalhomebase.app.ui.theme.Backdrop
import com.portalhomebase.app.ui.theme.CardBg
import com.portalhomebase.app.ui.theme.HomebaseTheme
import com.portalhomebase.app.ui.theme.Error
import com.portalhomebase.app.ui.theme.Ink
import com.portalhomebase.app.ui.theme.LocalIsLight
import com.portalhomebase.app.ui.theme.Outline
import com.portalhomebase.app.ui.theme.resolveLightSun
import com.portalhomebase.app.ui.theme.Selected
import com.portalhomebase.app.ui.theme.SelectedBorder
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.random.Random
import kotlinx.coroutines.delay

@Composable
fun App(state: BoardState) {
    var tab by remember { mutableIntStateOf(0) }
    val status by state.status.collectAsState()
    val weather by state.weather.collectAsState()
    var tick by remember { mutableIntStateOf(0) }
    var themeMode by remember { mutableStateOf(state.themeMode()) }
    // Resolved theme as explicit state: provider recomputes alone did not
    // propagate on time-driven recompositions, but state writes always do.
    // Auto follows the sun; missing sun times fall back to fixed hours.
    fun autoLight(mode: String): Boolean =
        resolveLightSun(mode, LocalTime.now(), weather?.sunrise ?: "", weather?.sunset ?: "")
    var isLight by remember { mutableStateOf(autoLight(themeMode)) }
    LaunchedEffect(state) { state.startPolling(this) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1_000)
            tick++
            isLight = autoLight(themeMode)
        }
    }

    CompositionLocalProvider(LocalIsLight provides isLight) {
    HomebaseTheme {
    Box(Modifier.fillMaxSize().background(Backdrop)) {
        AmbientBackground(weather?.label)
        Column(
            modifier = Modifier.fillMaxSize()
                .padding(top = 72.dp, start = 32.dp, end = 32.dp, bottom = 32.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Base Camp", style = MaterialTheme.typography.headlineSmall, color = Ink)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val now = remember(tick) { LocalTime.now() }
                    val today = remember(tick) { LocalDate.now() }
                    FlipClock(
                        dayWord = today.format(DateTimeFormatter.ofPattern("EEE", Locale.US)).uppercase(),
                        monWord = today.format(DateTimeFormatter.ofPattern("MMM", Locale.US)).uppercase(),
                        dayNum = today.format(DateTimeFormatter.ofPattern("d")),
                        hms = now.format(DateTimeFormatter.ofPattern("hh:mm:ss")),
                        ampm = now.format(DateTimeFormatter.ofPattern("a")),
                        tick = tick,
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TabButton("Board", tab == 0, Modifier.weight(1f)) { tab = 0 }
                TabButton("Home", tab == 1, Modifier.weight(1f)) { tab = 1 }
                TabButton("Week", tab == 2, Modifier.weight(1f)) { tab = 2 }
                TabButton("Meals", tab == 3, Modifier.weight(1f)) { tab = 3 }
                TabButton("Recipes", tab == 4, Modifier.weight(1f)) { tab = 4 }
                TabButton("⚙", tab == 5, Modifier.width(76.dp), PaddingValues(0.dp)) { tab = 5 }
            }
            Spacer(Modifier.height(16.dp))
            // Only Board shows the weather/calendar summary (scrolls with the
            // cards); every other tab runs fullscreen.
            if (status.isNotEmpty()) {
                Text(status, style = MaterialTheme.typography.bodySmall, color = Error)
                Spacer(Modifier.height(8.dp))
            }
            when (tab) {
                0 -> BoardScreen(
                    state, Modifier.weight(1f), { tab = 3 },
                    header = { HeaderStrip(state) },
                )
                1 -> HomeScreen(state, Modifier.weight(1f))
                2 -> WeekScreen(state, Modifier.weight(1f))
                3 -> MealsScreen(state, Modifier.weight(1f))
                4 -> RecipesScreen(state, Modifier.weight(1f))
                else -> SettingsScreen(state, Modifier.weight(1f), themeMode) {
                    themeMode = it
                    isLight = autoLight(it)
                    state.saveThemeMode(it)
                }
            }
        }
    }
    }
    }
}

// Ambient backdrop: animated Austin skyline (day/night + live weather) +
// grain + vignette. Content leads; the sky whispers.
@Composable
private fun AmbientBackground(condition: String?) {
    val light = LocalIsLight.current
    SkylineBackground(condition)

    val grain = remember {
        val rnd = Random(7)
        List(1600) { Offset(rnd.nextFloat(), rnd.nextFloat()) }
    }
    val grainInk = if (light) Color(0xFF5A5347).copy(alpha = 0.07f) else Color.White.copy(alpha = 0.05f)
    Canvas(Modifier.fillMaxSize()) {
        val pts = grain.map { Offset(it.x * size.width, it.y * size.height) }
        drawPoints(pts, PointMode.Points, grainInk, strokeWidth = 2.5f)
    }

    Box(
        Modifier.fillMaxSize().background(
            Brush.radialGradient(
                colors = listOf(Color.Transparent, Color.Black.copy(alpha = if (light) 0.14f else 0.38f)),
                radius = 1600f,
            ),
        ),
    )
}

@Composable
private fun TabButton(
    label: String,
    active: Boolean,
    modifier: Modifier,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(64.dp),
        shape = RoundedCornerShape(14.dp),
        contentPadding = contentPadding,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (active) Selected else CardBg,
        ),
        border = BorderStroke(
            1.dp, if (active) SelectedBorder else Outline,
        ),
    ) {
        Text(label, style = MaterialTheme.typography.titleMedium, color = Ink)
    }
}
