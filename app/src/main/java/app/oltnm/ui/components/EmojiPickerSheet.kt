package app.oltnm.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.oltnm.R
import app.oltnm.data.model.Emoji
import coil3.compose.AsyncImage

private val SKIN_TONES = listOf(
    "\uD83C\uDFFB",
    "\uD83C\uDFFC",
    "\uD83C\uDFFD",
    "\uD83C\uDFFE",
    "\uD83C\uDFFF"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmojiPickerSheet(
    customEmojis: List<Emoji>,
    loadingCustom: Boolean,
    onDismiss: () -> Unit,
    onPick: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    val groups = remember(context, locale) {
        EmojiCatalog.load(context, locale.language)
    }
    var query by remember { mutableStateOf("") }
    var tone by remember { mutableIntStateOf(0) }
    val needle = query.trim().lowercase(locale)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = stringResource(R.string.emoji_picker),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            OltnTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = stringResource(R.string.emoji_search),
                leadingIcon = {
                    Icon(Icons.Rounded.Search, contentDescription = null)
                },
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = stringResource(R.string.skin_tone),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                (0..SKIN_TONES.size).forEach { index ->
                    val sample = if (index == 0) "\u270B" else "\u270B" + SKIN_TONES[index - 1]
                    val selected = tone == index
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (selected) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainer
                                }
                            )
                            .border(
                                width = 1.dp,
                                color = if (selected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    Color.Transparent
                                },
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable { tone = index },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = sample, fontSize = 16.sp)
                    }
                }
            }

            LazyVerticalGrid(
                columns = GridCells.Adaptive(44.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .heightIn(min = 220.dp, max = 520.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                val customMatches = if (needle.isBlank()) {
                    customEmojis
                } else {
                    customEmojis.filter { it.shortcode.lowercase(locale).contains(needle) }
                }

                if (customMatches.isNotEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        EmojiGroupLabel(stringResource(R.string.emoji_custom))
                    }
                    items(customMatches, key = { it.shortcode }) { emoji ->
                        CustomEmojiCell(emoji) { onPick(":${emoji.shortcode}: ") }
                    }
                }

                if (loadingCustom && customEmojis.isEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                        }
                    }
                }

                if (needle.isBlank()) {
                    groups.forEach { group ->
                        item(
                            span = { GridItemSpan(maxLineSpan) },
                            key = "group-${group.id}"
                        ) {
                            EmojiGroupLabel(group.label)
                        }
                        items(group.emojis.size) { index ->
                            val item = group.emojis[index]
                            val value = preview(item, tone)
                            EmojiCell(value) { onPick(value) }
                        }
                    }
                } else {
                    val matches = groups.flatMap { group ->
                        group.emojis.filter { item ->
                            item.l.contains(needle, ignoreCase = true) ||
                                item.t.any { tag -> tag.contains(needle, ignoreCase = true) }
                        }
                    }
                    if (matches.isEmpty() && customMatches.isEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Text(
                                text = stringResource(R.string.emoji_empty),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    } else {
                        items(matches) { item ->
                            val value = preview(item, tone)
                            EmojiCell(value) { onPick(value) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmojiGroupLabel(label: String) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 6.dp)
    )
}

@Composable
private fun EmojiCell(emoji: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text = emoji, fontSize = 24.sp)
    }
}

@Composable
private fun CustomEmojiCell(emoji: Emoji, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = emoji.staticUrl.ifBlank { emoji.url },
            contentDescription = ":${emoji.shortcode}:",
            modifier = Modifier.size(28.dp)
        )
    }
}

private fun preview(item: EmojiItemData, tone: Int): String {
    if (tone <= 0) return item.c
    return item.s.getOrNull(tone - 1) ?: item.c
}
