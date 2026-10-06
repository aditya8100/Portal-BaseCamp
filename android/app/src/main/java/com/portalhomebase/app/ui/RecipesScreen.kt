package com.portalhomebase.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.portalhomebase.app.data.BoardState
import com.portalhomebase.app.data.Card
import com.portalhomebase.app.ui.theme.Accent
import com.portalhomebase.app.ui.theme.CardBg
import com.portalhomebase.app.ui.theme.Ink
import com.portalhomebase.app.ui.theme.Muted
import com.portalhomebase.app.ui.theme.Star
import kotlinx.coroutines.launch

// Recipe library: every recipe card in one browsable list so recipes never
// get buried under other card types on the Board. Tap opens the full detail.
@Composable
fun RecipesScreen(state: BoardState, modifier: Modifier = Modifier) {
    val cards by state.cards.collectAsState()
    val scope = rememberCoroutineScope()
    var openRecipe by remember { mutableStateOf<Card?>(null) }
    var pendingDelete by remember { mutableStateOf<Card?>(null) }
    var query by remember { mutableStateOf("") }
    val recipes = remember(cards) {
        cards.filter { it.type == "recipe" }
            .sortedWith(
                compareByDescending<Card> { it.pinned }
                    .thenByDescending { it.favorite }.thenByDescending { it.ts },
            )
    }
    val filtered = remember(recipes, query) {
        if (query.isBlank()) recipes
        else recipes.filter { r ->
            r.title.contains(query, ignoreCase = true) ||
                r.body.contains(query, ignoreCase = true) ||
                r.items.any { it.text.contains(query, ignoreCase = true) }
        }
    }

    if (recipes.isEmpty()) {
        Text(
            "No recipes yet — ask a Muse to post one.",
            style = MaterialTheme.typography.bodyLarge, color = Muted,
        )
    } else {
        Column(modifier = modifier.fillMaxSize()) {
            TextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search ${recipes.size} recipes…", color = Muted) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = CardBg,
                    unfocusedContainerColor = CardBg,
                    focusedTextColor = Ink,
                    unfocusedTextColor = Ink,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = Accent,
                ),
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        Text(
                            "✕", style = MaterialTheme.typography.titleMedium, color = Muted,
                            modifier = Modifier.clickable { query = "" }.padding(12.dp),
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            if (filtered.isEmpty()) {
                Text(
                    "No matches for “$query”.",
                    style = MaterialTheme.typography.bodyLarge, color = Muted,
                )
            } else {
                Column(
                    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    filtered.forEach { recipe ->
                        RecipeRow(recipe) { openRecipe = recipe }
                    }
                }
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
private fun RecipeRow(recipe: Card, onOpen: () -> Unit) {
    val bits = mutableListOf<String>()
    recipe.meta["time"]?.let { bits.add(it) }
    recipe.meta["servings"]?.let { bits.add("Serves $it") }
    if (recipe.items.isNotEmpty()) bits.add("${recipe.items.size} ingredients")
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        modifier = Modifier.clickable(onClick = onOpen),
    ) {
        Row(
            Modifier.padding(horizontal = 24.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (recipe.pinned) {
                Text("📌 ", style = MaterialTheme.typography.titleLarge)
            } else if (recipe.favorite) {
                Text("★ ", style = MaterialTheme.typography.titleLarge, color = Star)
            }
            Column(Modifier.weight(1f)) {
                Text(
                    recipe.title, style = MaterialTheme.typography.titleLarge, color = Ink,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                if (bits.isNotEmpty()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        bits.joinToString(" · "), style = MaterialTheme.typography.bodyMedium,
                        color = Muted, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Text("→", style = MaterialTheme.typography.titleLarge, color = Accent)
        }
    }
}
