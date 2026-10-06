package com.portalhomebase.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.portalhomebase.app.data.BoardState
import com.portalhomebase.app.data.Card
import com.portalhomebase.app.ui.theme.AlertCard
import com.portalhomebase.app.ui.theme.ButtonBg
import com.portalhomebase.app.ui.theme.Danger
import com.portalhomebase.app.ui.theme.Ink
import com.portalhomebase.app.ui.theme.CardBg
import com.portalhomebase.app.ui.theme.Event
import com.portalhomebase.app.ui.theme.Muted
import com.portalhomebase.app.ui.theme.OnSuccess
import com.portalhomebase.app.ui.theme.Outline
import com.portalhomebase.app.ui.theme.Selected
import com.portalhomebase.app.ui.theme.Star
import com.portalhomebase.app.ui.theme.Success
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

// (time imports used by toDateTime/toShortDate below)

@Composable
fun BoardScreen(
    state: BoardState,
    modifier: Modifier = Modifier,
    onOpenMeals: () -> Unit,
    header: (@Composable () -> Unit)? = null,
) {
    val cards by state.cards.collectAsState()
    val scope = rememberCoroutineScope()
    var focusSteps by remember { mutableStateOf<List<String>>(emptyList()) }
    var focusIdx by remember { mutableIntStateOf(0) }
    var focusOpen by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<Card?>(null) }
    // Board is for household cards — notes, lists, alerts, meal plans.
    // Recipes live only in the Recipes tab (pinned first there), so the
    // Board can never fill up with food.
    val pinned = remember(cards) { cards.filter { it.pinned && it.type != "recipe" } }
    val rest = remember(cards) { cards.filter { !it.pinned && it.type != "recipe" } }
    val shown = remember(cards) { pinned + rest }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(360.dp),
        modifier = modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        if (header != null) {
            item(span = { GridItemSpan(maxLineSpan) }) { header() }
        }
        if (shown.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Surface(shape = RoundedCornerShape(18.dp), color = CardBg) {
                    Text(
                        "No cards yet — ask a Muse to post one.",
                        style = MaterialTheme.typography.bodyLarge, color = Muted,
                        modifier = Modifier.padding(24.dp),
                    )
                }
            }
        } else {
            items(shown, key = { it.id }) { card ->
                CardView(
                    card = card,
                    onToggleItem = { i, done -> scope.launch { state.runAction { toggleItem(card.id, i, done) } } },
                    onStep = { idx ->
                        focusSteps = card.steps
                        focusIdx = idx
                        focusOpen = true
                    },
                    onCooked = { scope.launch { state.runAction { cooked(card.id) } } },
                    onFav = { fav -> scope.launch { state.runAction { setFavorite(card.id, fav) } } },
                    onPin = { pin -> scope.launch { state.runAction { setPinned(card.id, pin) } } },
                    onDelete = { pendingDelete = card },
                    onCardTap = { tapped ->
                        if (tapped.type == "mealplan") onOpenMeals()
                    },
                )
            }
        }
    }

    if (focusOpen && focusSteps.isNotEmpty()) {
        StepFocusDialog(focusSteps, focusIdx) { focusOpen = false }
    }

    pendingDelete?.let { card ->
        Dialog(onDismissRequest = { pendingDelete = null }) {
            Surface(shape = RoundedCornerShape(18.dp), color = CardBg) {
                Column(Modifier.padding(32.dp)) {
                    Text("Delete “${card.title}”?", style = MaterialTheme.typography.titleMedium, color = Ink)
                    Spacer(Modifier.height(24.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        DialogButton("Cancel", Modifier.weight(1f)) { pendingDelete = null }
                        DialogButton("Delete", Modifier.weight(1f), danger = true) {
                            scope.launch { state.runAction { deleteCard(card.id) } }
                            pendingDelete = null
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DialogButton(label: String, modifier: Modifier, primary: Boolean = false, danger: Boolean = false, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier.height(72.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = when {
                danger -> Danger
                primary -> Selected
                else -> CardBg
            },
        ),
        border = BorderStroke(1.dp, Outline),
    ) {
        Text(label, style = MaterialTheme.typography.titleMedium, color = Ink)
    }
}

@Composable
private fun CardView(
    card: Card,
    onToggleItem: (Int, Boolean) -> Unit,
    onStep: (Int) -> Unit,
    onCooked: () -> Unit,
    onFav: (Boolean) -> Unit,
    onPin: (Boolean) -> Unit,
    onDelete: () -> Unit,
    onCardTap: (Card) -> Unit,
) {
    var imgOk by remember(card.id, card.image) { mutableStateOf(true) }
    val tappable = card.type == "recipe" || card.type == "mealplan"
    Card(
        modifier = if (tappable) Modifier.clickable { onCardTap(card) } else Modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (card.type == "alert") AlertCard else CardBg,
        ),
    ) {
        Column(Modifier.padding(22.dp)) {
            if (imgOk && card.image.isNotEmpty()) {
                AsyncImage(
                    model = card.image,
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth().heightIn(max = 240.dp).clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop,
                    onError = { imgOk = false },
                )
                Spacer(Modifier.height(16.dp))
            }
            Text(
                card.title,
                style = if (card.type == "alert") MaterialTheme.typography.titleLarge.copy(fontSize = 27.sp)
                else MaterialTheme.typography.titleLarge,
                color = Ink,
            )
            if (card.type == "recipe") {
                val bits = mutableListOf<String>()
                card.meta["time"]?.let { bits.add(it) }
                card.meta["servings"]?.let { bits.add("Serves $it") }
                if (bits.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text(bits.joinToString("  ·  "), style = MaterialTheme.typography.bodyMedium, color = Muted)
                }
            }
            if (card.body.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(card.body, style = MaterialTheme.typography.bodyMedium, color = Ink)
            }
            if (card.type == "mealplan") {
                Spacer(Modifier.height(8.dp))
                val linked = card.plan.sumOf { d ->
                    (if (d.lunch?.ref?.isNotEmpty() == true) 1 else 0) +
                        (if (d.dinner?.ref?.isNotEmpty() == true) 1 else 0)
                }
                Text(
                    "${card.plan.size} days · $linked recipes linked →",
                    style = MaterialTheme.typography.bodyLarge, color = Event,
                )
            }
            if (card.items.isNotEmpty() && (card.type == "list" || card.type == "recipe")) {
                if (card.type == "recipe") SectionLabel("Ingredients")
                card.items.forEachIndexed { i, item ->
                    CheckRow(item.text, item.done) { onToggleItem(i, !item.done) }
                }
            }
            if (card.steps.isNotEmpty() && card.type == "recipe") {
                SectionLabel("Steps")
                card.steps.forEachIndexed { i, step ->
                    Row(
                        modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp)
                            .clickable { onStep(i) }.padding(vertical = 10.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Surface(
                            shape = CircleShape, color = Selected,
                            modifier = Modifier.size(34.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Text("${i + 1}", style = MaterialTheme.typography.bodyMedium, color = Ink)
                            }
                        }
                        Spacer(Modifier.width(14.dp))
                        Text(step, style = MaterialTheme.typography.bodyMedium, color = Ink)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "from ${card.source} · ${card.ts.toDateTime()}",
                style = MaterialTheme.typography.labelSmall, color = Muted,
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (card.type == "recipe") {
                    SmallButton("✓ Cooked it", false) { onCooked() }
                    SmallButton(if (card.favorite) "★ Saved" else "☆ Save", card.favorite) { onFav(!card.favorite) }
                }
                SmallButton(if (card.pinned) "📌 Pinned" else "📌 Pin", card.pinned) { onPin(!card.pinned) }
                SmallButton("Delete", false) { onDelete() }
            }
            card.lastCookedAt?.let {
                Spacer(Modifier.height(6.dp))
                Text("Cooked ${it.toShortDate()}", style = MaterialTheme.typography.labelSmall, color = Muted)
            }
        }
    }
}

@Composable
fun SectionLabel(text: String) {
    Spacer(Modifier.height(16.dp))
    Text(text, style = MaterialTheme.typography.bodySmall, color = Muted)
    Spacer(Modifier.height(6.dp))
}

@Composable
fun CheckRow(text: String, done: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp)
            .clickable { onToggle() }.padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            shape = RoundedCornerShape(9.dp),
            color = if (done) Success else Color.Transparent,
            border = BorderStroke(2.dp, if (done) Success else Selected),
            modifier = Modifier.size(30.dp),
        ) {
            if (done) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text("✓", color = OnSuccess, fontSize = 20.sp)
                }
            }
        }
        Spacer(Modifier.width(14.dp))
        Text(
            text, style = MaterialTheme.typography.bodyLarge,
            color = if (done) Muted else Ink,
            textDecoration = if (done) TextDecoration.LineThrough else null,
        )
    }
}

@Composable
fun SmallButton(label: String, highlighted: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.height(56.dp),
        shape = RoundedCornerShape(11.dp),
        colors = ButtonDefaults.buttonColors(containerColor = ButtonBg),
        border = BorderStroke(1.dp, if (highlighted) Star else Outline),
    ) {
        Text(label, fontSize = 17.sp, color = if (highlighted) Star else Ink)
    }
}

private fun Long.toDateTime(): String =
    Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("M/d/yyyy, h:mm a", Locale.US))

fun Long.toShortDate(): String =
    Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("MMM d", Locale.US))
