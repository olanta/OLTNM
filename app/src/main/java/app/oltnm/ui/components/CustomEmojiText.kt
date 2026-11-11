@file:OptIn(ExperimentalTextApi::class)

package app.oltnm.ui.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.em
import app.oltnm.data.model.Emoji
import coil3.compose.AsyncImage

private val shortcodePattern = Regex(""":[A-Za-z0-9_-]+:""")

data class EmojiTextData(
    val text: AnnotatedString,
    val inlineContent: Map<String, InlineTextContent>
)

@Composable
fun rememberEmojiText(text: String, emojis: List<Emoji>): EmojiTextData {
    val available = remember(emojis) { emojis.filter { it.url.isNotBlank() } }
    val inlineContent = remember(available) { available.toInlineContent() }
    val byShortcode = remember(available) { available.associateBy { it.shortcode } }
    val annotated = remember(text, byShortcode) {
        buildAnnotatedString {
            var cursor = 0
            shortcodePattern.findAll(text).forEach { match ->
                append(text.substring(cursor, match.range.first))
                val token = match.value
                val emoji = byShortcode[token.substring(1, token.length - 1)]
                if (emoji != null) {
                    appendInlineContent("emoji:${emoji.shortcode}", token)
                } else {
                    append(token)
                }
                cursor = match.range.last + 1
            }
            append(text.substring(cursor))
        }
    }
    return EmojiTextData(annotated, inlineContent)
}

fun List<Emoji>.toInlineContent(): Map<String, InlineTextContent> = filter { it.url.isNotBlank() }.associate { emoji ->
    "emoji:${emoji.shortcode}" to InlineTextContent(
        Placeholder(
            width = 1.15f.em,
            height = 1.15f.em,
            placeholderVerticalAlign = PlaceholderVerticalAlign.Center
        )
    ) {
        AsyncImage(
            model = emoji.url,
            contentDescription = ":${emoji.shortcode}:",
            modifier = Modifier.fillMaxSize()
        )
    }
}
