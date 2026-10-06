package com.portalhomebase.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.portalhomebase.app.data.Card
import com.portalhomebase.app.data.CardItem
import com.portalhomebase.app.ui.theme.Ink
import com.portalhomebase.app.ui.theme.Event
import com.portalhomebase.app.ui.theme.Muted
import com.portalhomebase.app.ui.theme.Scrim
import com.portalhomebase.app.ui.theme.Selected
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.roundToInt

@Composable
fun StepFocusDialog(steps: List<String>, startIdx: Int, onDismiss: () -> Unit) {
    var idx by remember(startIdx) { mutableIntStateOf(startIdx.coerceIn(steps.indices)) }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier.fillMaxSize().padding(48.dp),
            shape = RoundedCornerShape(20.dp),
            color = Scrim,
        ) {
            Column(Modifier.padding(48.dp)) {
                Text(
                    "Step ${idx + 1} of ${steps.size}",
                    style = MaterialTheme.typography.bodyLarge, color = Muted,
                )
                Spacer(Modifier.height(24.dp))
                Box(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                    Text(steps[idx], fontSize = 44.sp, lineHeight = 58.sp, color = Ink)
                }
                Spacer(Modifier.height(32.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    DialogButton("← Prev", Modifier.weight(1f)) {
                        idx = (idx - 1 + steps.size) % steps.size
                    }
                    DialogButton("Next →", Modifier.weight(1f), primary = true) {
                        idx = (idx + 1) % steps.size
                    }
                    DialogButton("Close", Modifier.weight(1f)) { onDismiss() }
                }
            }
        }
    }
}

@Composable
fun RecipeDetailDialog(
    card: Card,
    onToggleItem: (Int, Boolean) -> Unit,
    onCooked: () -> Unit,
    onFav: (Boolean) -> Unit,
    onPin: (Boolean) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    var stepIdx by remember { mutableStateOf<Int?>(null) }
    var imgOk by remember(card.id, card.image) { mutableStateOf(true) }
    val anchor = card.items.firstOrNull { it.anchor && (it.qty ?: 0.0) > 0.0 }
    val anchorQty = anchor?.qty ?: 0.0
    var anchorAmt by remember(card.id) { mutableStateOf(anchorQty) }
    var amtText by remember(card.id) { mutableStateOf(fmtNum(anchorQty)) }
    val factor = if (anchor != null && anchorQty > 0.0) anchorAmt / anchorQty else 1.0
    val scaled = anchor != null && abs(factor - 1.0) >= 0.001
    val step = anchorStep(anchor)
    fun setAmt(v: Double) {
        if (v > 0.0) {
            anchorAmt = v
            amtText = fmtNum(v)
        }
    }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier.fillMaxSize().padding(40.dp),
            shape = RoundedCornerShape(24.dp),
            color = Scrim,
        ) {
            Column(Modifier.padding(44.dp)) {
                Text(card.title, style = MaterialTheme.typography.displaySmall, color = Ink)
                val bits = mutableListOf<String>()
                card.meta["time"]?.let { bits.add(it) }
                card.meta["servings"]?.let { bits.add("Serves ${scaleServings(it, factor, scaled)}") }
                if (bits.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        bits.joinToString("  ·  "),
                        style = MaterialTheme.typography.bodyLarge, color = Muted,
                    )
                }
                if (card.body.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    Text(card.body, style = MaterialTheme.typography.bodyLarge, color = Ink)
                }
                if (anchor != null) {
                    Spacer(Modifier.height(14.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            "Scale ${anchor.name}:",
                            style = MaterialTheme.typography.titleMedium, color = Ink,
                        )
                        SmallButton("−", false) { setAmt(anchorAmt - step) }
                        OutlinedTextField(
                            value = amtText,
                            onValueChange = { next ->
                                amtText = next
                                next.toDoubleOrNull()?.let { v -> if (v > 0.0) anchorAmt = v }
                            },
                            modifier = Modifier.width(140.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        )
                        Text(anchor.unit, style = MaterialTheme.typography.bodyLarge, color = Muted)
                        SmallButton("+", false) { setAmt(anchorAmt + step) }
                        if (scaled) {
                            Text(
                                "×${fmtNum((factor * 100).roundToInt() / 100.0)}",
                                style = MaterialTheme.typography.titleMedium, color = Event,
                            )
                            SmallButton("Reset", false) { setAmt(anchorQty) }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                Box(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                    Column {
                        if (imgOk && card.image.isNotEmpty()) {
                            AsyncImage(
                                model = card.image,
                                contentDescription = null,
                                modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp)
                                    .clip(RoundedCornerShape(14.dp)),
                                contentScale = ContentScale.Crop,
                                onError = { imgOk = false },
                            )
                            Spacer(Modifier.height(16.dp))
                        }
                        if (card.items.isNotEmpty()) {
                            SectionLabel("Ingredients")
                            val perCol = ((card.items.size + 2) / 3).coerceAtLeast(1)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(20.dp),
                            ) {
                                card.items.indices.chunked(perCol).forEach { col ->
                                    Column(Modifier.weight(1f)) {
                                        col.forEach { i ->
                                            val item = card.items[i]
                                            CheckRow(scaledLine(item, factor, scaled), item.done) { onToggleItem(i, !item.done) }
                                        }
                                    }
                                }
                            }
                        }
                        if (card.steps.isNotEmpty()) {
                            SectionLabel("Steps — tap one to cook full-screen")
                            card.steps.forEachIndexed { i, step ->
                                Row(
                                    modifier = Modifier.fillMaxWidth()
                                        .clickable { stepIdx = i }.padding(vertical = 10.dp),
                                    verticalAlignment = Alignment.Top,
                                ) {
                                    Surface(
                                        shape = CircleShape, color = Selected,
                                        modifier = Modifier.size(40.dp),
                                    ) {
                                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                            Text("${i + 1}", style = MaterialTheme.typography.bodyLarge, color = Ink)
                                        }
                                    }
                                    Spacer(Modifier.width(14.dp))
                                    Text(step, style = MaterialTheme.typography.bodyLarge, color = Ink)
                                }
                            }
                        }
                        card.lastCookedAt?.let {
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "Cooked ${it.toShortDate()}",
                                style = MaterialTheme.typography.bodyMedium, color = Muted,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SmallButton("✓ Cooked it", false) { onCooked() }
                    SmallButton(if (card.favorite) "★ Saved" else "☆ Save", card.favorite) { onFav(!card.favorite) }
                    SmallButton(if (card.pinned) "📌 Pinned" else "📌 Pin", card.pinned) { onPin(!card.pinned) }
                    SmallButton("Delete", false) { onDelete() }
                    SmallButton("Close", false) { onDismiss() }
                }
            }
        }
    }
    stepIdx?.let { StepFocusDialog(card.steps, it) { stepIdx = null } }
}

private fun fmtNum(q: Double): String =
    if (q % 1.0 == 0.0) q.toLong().toString()
    else "%.2f".format(q).trimEnd('0').trimEnd('.')

private fun fmtAmt(q: Double): String {
    if (q <= 0.0) return "0"
    if (q >= 20.0) return q.roundToInt().toString()
    val r = (q * 4).roundToInt() / 4.0
    if (r == 0.0) return fmtNum(q)
    val whole = r.toInt()
    val glyph = when ((r * 4).roundToInt() % 4) {
        1 -> "¼"
        2 -> "½"
        3 -> "¾"
        else -> ""
    }
    return when {
        glyph.isEmpty() -> whole.toString()
        whole == 0 -> glyph
        else -> "$whole$glyph"
    }
}

private fun scaledLine(item: CardItem, factor: Double, scaled: Boolean): String {
    val q = item.qty
    if (!scaled || q == null || item.name.isEmpty()) return item.text
    val unit = if (item.unit.isNotEmpty()) " ${item.unit}" else ""
    val prep = if (item.prep.isNotEmpty()) ", ${item.prep}" else ""
    return "${fmtAmt(q * factor)}$unit ${item.name}$prep"
}

private fun scaleServings(s: String, factor: Double, scaled: Boolean): String {
    if (!scaled) return s
    return s.replace(Regex("\\d+")) { ceil(it.value.toDouble() * factor).toInt().toString() }
}

private fun anchorStep(anchor: CardItem?): Double {
    if (anchor == null) return 1.0
    if (anchor.unit.isEmpty() || anchor.unit.lowercase() in setOf("count", "whole", "x", "pcs", "pc")) return 1.0
    val q = anchor.qty ?: 0.0
    return when {
        q >= 100.0 -> 10.0
        q >= 20.0 -> 5.0
        q >= 5.0 -> 1.0
        else -> 0.5
    }
}
