package com.portalhomebase.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.portalhomebase.app.data.BoardState
import com.portalhomebase.app.data.DayForecast
import com.portalhomebase.app.data.HourPoint
import com.portalhomebase.app.ui.theme.Event
import com.portalhomebase.app.ui.theme.Ink
import com.portalhomebase.app.ui.theme.LocalIsLight
import com.portalhomebase.app.ui.theme.Muted
import com.portalhomebase.app.ui.theme.Outline
import com.portalhomebase.app.ui.theme.Panel
import com.portalhomebase.app.ui.theme.Accent
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

fun fmtDay(ms: Long, tz: String): String =
    Instant.ofEpochMilli(ms).atZone(ZoneId.of(tz))
        .format(DateTimeFormatter.ofPattern("EEE", Locale.US))

fun fmtTime(ms: Long, tz: String): String {
    val t = Instant.ofEpochMilli(ms).atZone(ZoneId.of(tz))
    val h = t.hour % 12
    val hh = if (h == 0) 12 else h
    val ampm = if (t.hour < 12) "AM" else "PM"
    return if (t.minute == 0) "$hh $ampm" else "$hh:${t.minute.toString().padStart(2, '0')} $ampm"
}

fun fmtHour(ms: Long, tz: String, first: Boolean): String {
    if (first) return "Now"
    val t = Instant.ofEpochMilli(ms).atZone(ZoneId.of(tz))
    val h = t.hour % 12
    return "${if (h == 0) 12 else h}${if (t.hour < 12) "AM" else "PM"}"
}

fun ago(ts: Long): String {
    if (ts <= 0) return "never"
    val m = ((System.currentTimeMillis() - ts) / 60000).coerceAtLeast(0)
    return if (m < 1) "just now" else "$m min ago"
}

@Composable
fun HeaderStrip(state: BoardState, modifier: Modifier = Modifier) {
    val weather by state.weather.collectAsState()
    val events by state.events.collectAsState()
    val tz by state.tz.collectAsState()

    // Custom two-up: weather card sets the row height, events card matches it.
    // (IntrinsicSize would crash: the LazyRow inside can't do intrinsic measurement.)
    Layout(
        modifier = modifier.fillMaxWidth(),
        content = {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Panel),
            ) {
            val w = weather
            Column(Modifier.padding(20.dp)) {
                if (w == null) {
                    Text("loading weather…", style = MaterialTheme.typography.bodyLarge, color = Muted)
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("${w.temp}°C", style = MaterialTheme.typography.displayLarge, color = Ink)
                        Spacer(Modifier.width(18.dp))
                        ConditionGlyph(w.label, Modifier.size(84.dp))
                        Column(Modifier.padding(start = 14.dp)) {
                            Text(w.label, style = MaterialTheme.typography.bodyLarge, color = Ink)
                            val today = w.days.firstOrNull()
                            if (today != null) {
                                Text(
                                    "H ${today.hi}°  L ${today.lo}° · ${today.precip ?: 0}% rain",
                                    style = MaterialTheme.typography.bodyMedium, color = Muted,
                                )
                            }
                        }
                    }
                    if (w.hours.isNotEmpty()) {
                        Spacer(Modifier.height(14.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            items(w.hours) { h ->
                                HourCell(h, tz, h == w.hours.first())
                            }
                        }
                    }
                    val week = w.days.drop(1).take(7)
                    if (week.isNotEmpty()) {
                        Spacer(Modifier.height(14.dp))
                        val span = ((week.maxOf { it.hi } - week.minOf { it.lo }).coerceAtLeast(1)).toFloat()
                        val floor = week.minOf { it.lo }.toFloat()
                        week.forEach { d ->
                            DayRow(d, floor, span)
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "updated ${ago(w.fetchedAt)}${if (w.stale) " (stale)" else ""}",
                        style = MaterialTheme.typography.labelSmall, color = Muted,
                    )
                }
            }
        }
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Panel),
            ) {
            Column(
                Modifier.padding(20.dp).fillMaxSize(),
                verticalArrangement = Arrangement.SpaceEvenly,
            ) {
                Text("Coming up", style = MaterialTheme.typography.bodySmall, color = Muted)
                if (events.isEmpty()) {
                    Text("Nothing coming up", style = MaterialTheme.typography.bodyLarge, color = Muted)
                } else {
                    events.take(8).forEach { ev ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val whenText = if (ev.allDay) "${fmtDay(ev.start, tz)} · all day"
                            else "${fmtDay(ev.start, tz)} ${fmtTime(ev.start, tz)}"
                            Text(
                                whenText, style = MaterialTheme.typography.bodyLarge, color = Event,
                                modifier = Modifier.padding(end = 16.dp),
                            )
                            Text(
                                ev.title, style = MaterialTheme.typography.bodyLarge, color = Ink,
                                maxLines = 1, overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                            // Venues carry addresses after a newline or comma; name only.
                            val venue = ev.location.lineSequence().firstOrNull()?.trim().orEmpty()
                                .substringBefore(",").trim()
                            if (venue.isNotEmpty()) {
                                Text(
                                    venue, style = MaterialTheme.typography.bodyMedium, color = Muted,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.End,
                                    modifier = Modifier.weight(1f).padding(start = 12.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
        },
    ) { measurables, constraints ->
        val gap = 16.dp.roundToPx()
        val w0 = ((constraints.maxWidth - gap) * (1f / 2.2f)).toInt()
        val w1 = constraints.maxWidth - gap - w0
        val p0 = measurables[0].measure(constraints.copy(minWidth = w0, maxWidth = w0, minHeight = 0))
        val p1 = measurables[1].measure(
            constraints.copy(minWidth = w1, maxWidth = w1, minHeight = p0.height, maxHeight = p0.height),
        )
        layout(constraints.maxWidth, p0.height) {
            p0.placeRelative(0, 0)
            p1.placeRelative(w0 + gap, 0)
        }
    }
}

@Composable
private fun HourCell(h: HourPoint, tz: String, first: Boolean) {
    Column(
        modifier = Modifier.width(64.dp).padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(fmtHour(h.t, tz, first), style = MaterialTheme.typography.bodySmall, color = Muted)
        Spacer(Modifier.height(4.dp))
        Text("${h.temp}°", style = MaterialTheme.typography.bodyLarge, color = Ink)
        Spacer(Modifier.height(2.dp))
        Text(
            if (h.precip > 0) "${h.precip}%" else "·",
            style = MaterialTheme.typography.bodySmall, color = Event,
        )
    }
}

@Composable
private fun DayRow(d: DayForecast, floor: Float, span: Float) {
    val start = ((d.lo - floor) / span).coerceIn(0f, 1f)
    val width = ((d.hi - d.lo) / span).coerceIn(0.02f, 1f)
    val dayName = try {
        LocalDate.parse(d.date).format(DateTimeFormatter.ofPattern("EEE", Locale.US))
    } catch (e: Exception) {
        ""
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            if (d.label.isEmpty()) "" else conditionEmoji(d.label),
            fontSize = 22.sp, modifier = Modifier.width(40.dp),
        )
        Text(dayName, style = MaterialTheme.typography.bodyLarge, color = Ink, modifier = Modifier.width(48.dp))
        Text("${d.lo}°", style = MaterialTheme.typography.bodyLarge, color = Muted, modifier = Modifier.width(44.dp))
        Box(
            modifier = Modifier.weight(1f).height(10.dp)
                .clip(RoundedCornerShape(5.dp)).background(Outline.copy(alpha = 0.55f)),
        ) {
            Row(Modifier.matchParentSize()) {
                Spacer(Modifier.weight(start.coerceAtLeast(0.001f)))
                Box(
                    Modifier.weight(width).fillMaxHeight()
                        .background(
                            Brush.horizontalGradient(listOf(Color(0xFF6FA8DC), Color(0xFFE8B04B))),
                            RoundedCornerShape(5.dp),
                        ),
                )
                Spacer(Modifier.weight((1f - start - width).coerceAtLeast(0.001f)))
            }
        }
        Text(
            "${d.hi}°", style = MaterialTheme.typography.bodyLarge, color = Ink,
            modifier = Modifier.width(44.dp).padding(start = 8.dp),
        )
        Text(
            if (bringsRain(d.label) && (d.precip ?: 0) > 0) "💧 ${d.precip}%" else "",
            style = MaterialTheme.typography.bodySmall, color = Event,
            modifier = Modifier.width(64.dp),
        )
    }
}

private fun conditionKind(label: String): String {
    val c = label.lowercase()
    return when {
        c.contains("storm") || c.contains("thunder") -> "storm"
        c.contains("snow") -> "snow"
        c.contains("rain") || c.contains("drizzle") || c.contains("shower") -> "rain"
        c.contains("fog") || c.contains("icy") -> "fog"
        c.contains("overcast") -> "cloud"
        c.contains("cloudy") -> "partly"
        else -> "sun"
    }
}

private fun conditionEmoji(label: String): String = when (conditionKind(label)) {
    "sun" -> "☀️"
    "partly" -> "⛅"
    "cloud" -> "☁️"
    "fog" -> "🌫️"
    "rain" -> "🌧️"
    "storm" -> "⛈️"
    else -> "❄️"
}

private fun bringsRain(label: String): Boolean {
    val kind = conditionKind(label)
    return kind == "rain" || kind == "storm"
}

// Hand-drawn line-art condition glyph. No emoji, no assets.
@Composable
private fun ConditionGlyph(label: String, modifier: Modifier = Modifier) {
    val kind = conditionKind(label)
    val light = LocalIsLight.current
    val color = when (kind) {
        "sun" -> if (light) Color(0xFFC6930A) else Color(0xFFE8C876)
        "partly" -> if (light) Color(0xFF8A7B3C) else Color(0xFFD8CFA8)
        "cloud" -> if (light) Color(0xFF5B6470) else Color(0xFF8B95A3)
        "fog" -> if (light) Color(0xFF6B7684) else Color(0xFF9AA4B2)
        "rain" -> if (light) Color(0xFF2F6DA3) else Color(0xFF7FA8C9)
        "storm" -> if (light) Color(0xFF3A5A8C) else Color(0xFF8FA8C8)
        else -> if (light) Color(0xFF4A7BA6) else Color(0xFFC9D6E3)
    }
    Canvas(modifier) {
        val sw = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round)
        val w = size.width
        val h = size.height
        fun sun(cx: Float, cy: Float, r: Float, rays: Boolean) {
            drawCircle(color, r, Offset(cx, cy), style = sw)
            if (rays) {
                for (i in 0 until 8) {
                    val a = i * Math.PI / 4
                    val x1 = cx + kotlin.math.cos(a).toFloat() * r * 1.45f
                    val y1 = cy + kotlin.math.sin(a).toFloat() * r * 1.45f
                    val x2 = cx + kotlin.math.cos(a).toFloat() * r * 1.9f
                    val y2 = cy + kotlin.math.sin(a).toFloat() * r * 1.9f
                    drawLine(color, Offset(x1, y1), Offset(x2, y2), strokeWidth = sw.width, cap = sw.cap)
                }
            }
        }
        fun cloud(left: Float, top: Float, right: Float, bottom: Float) {
            drawLine(color, Offset(left, bottom), Offset(right, bottom), sw.width, sw.cap)
            drawArc(
                color, 90f, 180f, false,
                Offset(left, top), androidx.compose.ui.geometry.Size((right - left) * 0.55f, bottom - top), style = sw,
            )
            drawArc(
                color, 90f, 180f, false,
                Offset(left + (right - left) * 0.35f, top - (bottom - top) * 0.25f),
                androidx.compose.ui.geometry.Size((right - left) * 0.55f, bottom - top), style = sw,
            )
        }
        when (kind) {
            "sun" -> sun(w * 0.5f, h * 0.5f, w * 0.16f, true)
            "partly" -> {
                sun(w * 0.32f, h * 0.34f, w * 0.13f, false)
                cloud(w * 0.25f, h * 0.45f, w * 0.9f, h * 0.78f)
            }
            "cloud" -> cloud(w * 0.12f, h * 0.32f, w * 0.88f, h * 0.72f)
            "fog" -> {
                listOf(0.30f to 0.85f, 0.15f to 0.70f, 0.35f to 0.90f).forEachIndexed { i, (x0, x1) ->
                    val y = h * (0.35f + i * 0.16f)
                    drawLine(color, Offset(w * x0, y), Offset(w * x1, y), sw.width, sw.cap)
                }
            }
            "rain" -> {
                cloud(w * 0.15f, h * 0.18f, w * 0.85f, h * 0.58f)
                listOf(0.32f, 0.5f, 0.68f).forEach { x ->
                    drawLine(
                        color, Offset(w * x, h * 0.68f), Offset(w * x - w * 0.05f, h * 0.88f),
                        sw.width, sw.cap,
                    )
                }
            }
            "storm" -> {
                cloud(w * 0.15f, h * 0.15f, w * 0.85f, h * 0.55f)
                val p = androidx.compose.ui.graphics.Path().apply {
                    moveTo(w * 0.56f, h * 0.62f)
                    lineTo(w * 0.44f, h * 0.78f)
                    lineTo(w * 0.54f, h * 0.78f)
                    lineTo(w * 0.40f, h * 0.94f)
                }
                drawPath(p, color, style = sw)
            }
            else -> {
                cloud(w * 0.15f, h * 0.15f, w * 0.85f, h * 0.55f)
                listOf(0.32f to 0.72f, 0.52f to 0.80f, 0.68f to 0.70f).forEach { (x, y) ->
                    drawLine(color, Offset(w * x - 7f, h * y), Offset(w * x + 7f, h * y), sw.width, sw.cap)
                    drawLine(color, Offset(w * x, h * y - 7f), Offset(w * x, h * y + 7f), sw.width, sw.cap)
                }
            }
        }
    }
}
