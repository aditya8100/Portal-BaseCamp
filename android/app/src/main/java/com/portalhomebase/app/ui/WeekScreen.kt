package com.portalhomebase.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.portalhomebase.app.data.BoardState
import com.portalhomebase.app.data.WeekDay
import com.portalhomebase.app.data.WeekTimed
import com.portalhomebase.app.ui.theme.Ink
import com.portalhomebase.app.ui.theme.LocalIsLight
import com.portalhomebase.app.ui.theme.CardBg
import com.portalhomebase.app.ui.theme.Event
import com.portalhomebase.app.ui.theme.Muted
import com.portalhomebase.app.ui.theme.OnAccent
import com.portalhomebase.app.ui.theme.Outline
import com.portalhomebase.app.ui.theme.Accent
import com.portalhomebase.app.ui.theme.Content
import com.portalhomebase.app.ui.theme.Selected
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private const val DAY_START_MIN = 0
private const val DAY_END_MIN = 24 * 60
private val HOUR_H: Dp = 56.dp

@Composable
fun WeekScreen(state: BoardState, modifier: Modifier = Modifier) {
    val week by state.week.collectAsState()
    val tz by state.tz.collectAsState()
    if (week.isEmpty()) {
        Text("loading week…", style = MaterialTheme.typography.bodyLarge, color = Muted)
        return
    }
    val zone = try {
        ZoneId.of(tz)
    } catch (e: Exception) {
        ZoneId.systemDefault()
    }
    val now = ZonedDateTime.now(zone)
    val todayKey = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
    val nowMin = now.hour * 60 + now.minute
    val scroll = rememberScrollState()
    val density = LocalDensity.current
    var selected by remember { mutableStateOf<EvSel?>(null) }
    LaunchedEffect(week) {
        if (week.isEmpty()) return@LaunchedEffect
        kotlinx.coroutines.delay(250)
        val target = (nowMin - 60).coerceIn(DAY_START_MIN, DAY_END_MIN - 180)
        val px = ((target - DAY_START_MIN) / 60f * 56f * density.density).toInt()
        scroll.scrollTo(px)
    }

    Column(modifier.fillMaxSize()) {
        // Day headers.
        Row(Modifier.fillMaxWidth()) {
            Spacer(Modifier.width(52.dp))
            week.forEach { day ->
                Column(
                    Modifier.weight(1f).padding(bottom = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(day.label, style = MaterialTheme.typography.bodySmall, color = Muted)
                    if (day.key == todayKey) {
                        Surface(shape = CircleShape, color = Accent) {
                            Text(
                                "${day.dayNum}",
                                style = MaterialTheme.typography.titleMedium,
                                color = OnAccent,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            )
                        }
                    } else {
                        Text("${day.dayNum}", style = MaterialTheme.typography.titleMedium, color = Ink)
                    }
                }
            }
        }
        // All-day row (only if any).
        if (week.any { it.allDay.isNotEmpty() }) {
            Row(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                Spacer(Modifier.width(52.dp))
                week.forEach { day ->
                    Column(Modifier.weight(1f)) {
                        day.allDay.take(2).forEach { ev ->
                            Box(
                                Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                                    .clickable { selected = EvSel(ev.title, "${dayHead(day)} · All day", ev.location) }
                                    .background(Selected).padding(horizontal = 8.dp, vertical = 6.dp),
                            ) {
                                Text(
                                    ev.title, style = MaterialTheme.typography.bodySmall,
                                    color = Ink, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                        }
                        if (day.allDay.size > 2) {
                            Text("+${day.allDay.size - 2} more", style = MaterialTheme.typography.labelSmall, color = Muted)
                        }
                    }
                }
            }
        }
        // Timed grid: full 24h (1344dp) so content always exceeds the
        // viewport and the column actually scrolls; opens near now.
        val totalH = HOUR_H * (DAY_END_MIN - DAY_START_MIN) / 60
        Column(Modifier.fillMaxSize().verticalScroll(scroll)) {
            Row(Modifier.fillMaxWidth()) {
            // Time gutter.
            Column(Modifier.width(52.dp).height(totalH)) {
                for (h in DAY_START_MIN / 60 until DAY_END_MIN / 60) {
                    val label = when (h % 12) {
                        0 -> "12"
                        else -> "${h % 12}"
                    } + if (h < 12) "a" else "p"
                    Box(Modifier.height(HOUR_H), contentAlignment = Alignment.TopEnd) {
                        // Content in light mode: Muted washes out against the cream sky.
                        Text(label, style = MaterialTheme.typography.labelSmall, color = if (LocalIsLight.current) Content else Muted, modifier = Modifier.padding(end = 8.dp))
                    }
                }
            }
            week.forEach { day ->
                DayColumn(day, totalH, day.key == todayKey, nowMin) { ev ->
                    selected = EvSel(
                        ev.title,
                        "${dayHead(day)} · ${fmtT(ev.startMin)} – ${fmtT(ev.endMin)}",
                        ev.location,
                    )
                }
            }
            }
        }
    }
    selected?.let { ev ->
        Dialog(onDismissRequest = { selected = null }) {
            Surface(shape = RoundedCornerShape(18.dp), color = CardBg) {
                Column(Modifier.padding(32.dp).width(560.dp)) {
                    Text(ev.title, style = MaterialTheme.typography.titleLarge, color = Ink)
                    Spacer(Modifier.height(8.dp))
                    Text(ev.whenText, style = MaterialTheme.typography.bodyLarge, color = Event)
                    if (ev.location.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text(ev.location, style = MaterialTheme.typography.bodyMedium, color = Muted)
                    }
                    Spacer(Modifier.height(24.dp))
                    Button(
                        onClick = { selected = null },
                        modifier = Modifier.fillMaxWidth().height(72.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Selected),
                        border = BorderStroke(1.dp, Outline),
                    ) {
                        Text("Close", style = MaterialTheme.typography.titleMedium, color = Ink)
                    }
                }
            }
        }
    }
}

private data class EvSel(val title: String, val whenText: String, val location: String)

private val MONTHS = arrayOf(
    "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec",
)

private fun dayHead(day: WeekDay): String {
    val mo = day.key.substring(5, 7).toIntOrNull()?.coerceIn(1, 12) ?: 1
    return "${day.label}, ${MONTHS[mo - 1]} ${day.dayNum}"
}

private fun fmtT(min: Int): String {
    val h24 = (min / 60).coerceIn(0, 24)
    val m = min % 60
    val h12 = if (h24 % 12 == 0) 12 else h24 % 12
    val ap = if (h24 < 12) "AM" else "PM"
    return "$h12:${m.toString().padStart(2, '0')} $ap"
}

private data class Lane(val event: WeekTimed, val lane: Int, val lanes: Int)

private fun layoutDay(events: List<WeekTimed>): List<Lane> {
    if (events.isEmpty()) return emptyList()
    val sorted = events.sortedWith(compareBy({ it.startMin }, { it.endMin }))
    val laneEnd = mutableListOf<Int>()
    val assigned = sorted.map { ev ->
        val s = ev.startMin.coerceIn(DAY_START_MIN, DAY_END_MIN)
        val e = ev.endMin.coerceIn(s + 15, DAY_END_MIN + 60)
        var lane = laneEnd.indexOfFirst { it <= s }
        if (lane < 0) {
            lane = laneEnd.size
            laneEnd.add(e)
        } else {
            laneEnd[lane] = e
        }
        Triple(ev, s, lane)
    }
    val lanes = laneEnd.size.coerceAtLeast(1)
    return assigned.map { (ev, _, lane) -> Lane(ev, lane, lanes) }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.DayColumn(
    day: WeekDay,
    totalH: Dp,
    isToday: Boolean,
    nowMin: Int,
    onEvent: (WeekTimed) -> Unit,
) {
    val hours = (DAY_END_MIN - DAY_START_MIN) / 60
    // Grid lines. Muted carries in light mode, where Outline on cream
    // is too faint to read the grid by.
    val lineColor = if (LocalIsLight.current) Muted.copy(alpha = 0.55f)
    else Outline.copy(alpha = 0.6f)
    // No column padding: separators sit exactly on column edges, so the
    // event's 3dp start/end insets read as equal gaps on both sides.
    Box(Modifier.weight(1f).height(totalH)) {
        // Day separator.
        Box(Modifier.align(Alignment.TopStart).width(1.dp).fillMaxHeight().background(lineColor))
        // Hour lines.
        Column(Modifier.fillMaxSize()) {
            repeat(hours) {
                Box(Modifier.fillMaxWidth().weight(1f)) {
                    Box(
                        Modifier.fillMaxWidth().height(1.dp).background(lineColor),
                    )
                }
            }
        }
        // Events.
        layoutDay(day.timed).forEach { (ev, lane, lanes) ->
            val s = ev.startMin.coerceIn(DAY_START_MIN, DAY_END_MIN)
            val e = ev.endMin.coerceIn(s + 15, DAY_END_MIN + 60)
            val topFrac = (s - DAY_START_MIN).toFloat() / (DAY_END_MIN - DAY_START_MIN)
            val hFrac = ((e - s).coerceAtLeast(20)).toFloat() / (DAY_END_MIN - DAY_START_MIN)
            Row(Modifier.fillMaxSize()) {
                Spacer(Modifier.weight(lane.toFloat().coerceAtLeast(0.001f) / lanes))
                Box(
                    Modifier.weight((1f / lanes).coerceAtLeast(0.05f)).fillMaxHeight()
                        .padding(top = totalH * topFrac, start = 3.dp, end = 3.dp),
                ) {
                    Box(
                        Modifier.fillMaxWidth().height(totalH * hFrac)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onEvent(ev) }
                            .background(Accent)
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                    ) {
                        // Time range under the title; short blocks keep title only
                        // so text never overflows the block.
                        val blockH = totalH * hFrac
                        Column {
                            Text(
                                ev.title, style = MaterialTheme.typography.bodySmall,
                                color = OnAccent,
                                maxLines = if (blockH >= 76.dp) 2 else 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (blockH >= 52.dp) {
                                Text(
                                    "${fmtT(ev.startMin)} – ${fmtT(ev.endMin)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = OnAccent,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.weight(((lanes - lane - 1).toFloat() / lanes).coerceAtLeast(0.001f)))
            }
        }
        // Now line.
        if (isToday && nowMin in DAY_START_MIN..DAY_END_MIN) {
            val frac = (nowMin - DAY_START_MIN).toFloat() / (DAY_END_MIN - DAY_START_MIN)
            Box(
                Modifier.fillMaxWidth().offset(y = totalH * frac - 1.dp)
                    .height(2.dp).background(Event),
            )
        }
    }
}
