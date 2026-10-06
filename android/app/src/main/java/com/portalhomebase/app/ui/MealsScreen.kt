package com.portalhomebase.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.portalhomebase.app.data.BoardState
import com.portalhomebase.app.data.Card
import com.portalhomebase.app.data.MealSlot
import com.portalhomebase.app.ui.theme.Ink
import com.portalhomebase.app.ui.theme.CardBg
import com.portalhomebase.app.ui.theme.Muted
import com.portalhomebase.app.ui.theme.Outline
import com.portalhomebase.app.ui.theme.Panel
import com.portalhomebase.app.ui.theme.Accent
import kotlinx.coroutines.launch

@Composable
fun MealsScreen(state: BoardState, modifier: Modifier = Modifier) {
    val cards by state.cards.collectAsState()
    val scope = rememberCoroutineScope()
    var openRecipe by remember { mutableStateOf<Card?>(null) }
    var pendingDelete by remember { mutableStateOf<Card?>(null) }
    val plans = cards.filter { it.type == "mealplan" }
    val recipes = remember(cards) { cards.filter { it.type == "recipe" }.associateBy { it.id } }

    if (plans.isEmpty()) {
        Surface(shape = RoundedCornerShape(18.dp), color = CardBg) {
            Text(
                "No meal plan yet — ask a Muse to post one.",
                style = MaterialTheme.typography.bodyLarge, color = Muted,
                modifier = Modifier.padding(24.dp),
            )
        }
    } else {
        Column(
            modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            plans.forEach { plan ->
                PlanGrid(plan, recipes) { openRecipe = it }
            }
        }
    }

    openRecipe?.let { recipe ->
        val live = cards.find { it.id == recipe.id } ?: recipe
        RecipeDetailDialog(
            card = live,
            onToggleItem = { i, done -> scope.launch { state.runAction { toggleItem(live.id, i, done) } } },
            onCooked = { scope.launch { state.runAction { cooked(live.id) } } },
            onFav = { fav -> scope.launch { state.runAction { setFavorite(live.id, fav) } } },
            onPin = { pin -> scope.launch { state.runAction { setPinned(live.id, pin) } } },
            onDelete = { pendingDelete = live; openRecipe = null },
            onDismiss = { openRecipe = null },
        )
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
private fun PlanGrid(plan: Card, recipes: Map<String, Card>, onOpenRecipe: (Card) -> Unit) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Panel),
    ) {
        Column(Modifier.padding(24.dp)) {
            Text(plan.title, style = MaterialTheme.typography.titleLarge, color = Ink)
            if (plan.body.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(plan.body, style = MaterialTheme.typography.bodyMedium, color = Muted)
            }
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.width(140.dp))
                Text(
                    "Lunch", style = MaterialTheme.typography.bodySmall, color = Muted,
                    modifier = Modifier.weight(1f).padding(start = 14.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Dinner", style = MaterialTheme.typography.bodySmall, color = Muted,
                    modifier = Modifier.weight(1f).padding(start = 14.dp),
                )
            }
            Spacer(Modifier.height(8.dp))
            plan.plan.forEach { day ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 5.dp),
                ) {
                    Text(
                        day.day, style = MaterialTheme.typography.titleMedium, color = Ink,
                        modifier = Modifier.width(140.dp),
                    )
                    MealCell(day.lunch, Modifier.weight(1f), recipes, onOpenRecipe)
                    Spacer(Modifier.width(8.dp))
                    MealCell(day.dinner, Modifier.weight(1f), recipes, onOpenRecipe)
                }
            }
        }
    }
}

@Composable
private fun MealCell(slot: MealSlot?, modifier: Modifier, recipes: Map<String, Card>, onOpenRecipe: (Card) -> Unit) {
    val linked = slot?.ref?.isNotEmpty() == true && recipes.containsKey(slot.ref)
    val gone = slot?.ref?.isNotEmpty() == true && !recipes.containsKey(slot.ref)
    Box(
        modifier.heightIn(min = 64.dp).clip(RoundedCornerShape(12.dp))
            .then(if (linked) Modifier.clickable { onOpenRecipe(recipes[slot!!.ref]!!) } else Modifier)
            .background(
                if (linked) Accent.copy(alpha = 0.35f)
                else Outline.copy(alpha = 0.35f),
            )
            .padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        when {
            slot == null -> Text("—", style = MaterialTheme.typography.bodyLarge, color = Muted)
            gone -> Text(
                "${slot.label} · gone", style = MaterialTheme.typography.bodyLarge, color = Muted,
                maxLines = 2, overflow = TextOverflow.Ellipsis,
            )
            else -> Text(
                (if (linked) "→ " else "") + slot.label,
                style = MaterialTheme.typography.bodyLarge,
                color = if (linked) Ink else Muted,
                maxLines = 2, overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
