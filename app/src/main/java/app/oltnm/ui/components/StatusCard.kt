@file:OptIn(ExperimentalTextApi::class)

package app.oltnm.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.oltnm.R
import app.oltnm.data.model.Emoji
import app.oltnm.data.model.Status
import app.oltnm.ui.theme.LocalOltnSemantics
import app.oltnm.util.HtmlText
import app.oltnm.util.RelativeTime

@Composable
fun StatusCard(
    status: Status,
    onOpenAccount: (String) -> Unit,
    onOpenStatus: (Status) -> Unit,
    onReply: (Status) -> Unit,
    onOpenUrl: (String) -> Unit,
    onFavourite: (Status) -> Unit,
    onReblog: (Status) -> Unit,
    modifier: Modifier = Modifier,
    interactive: Boolean = true
) {
    val displayed = status.original
    val semantics = LocalOltnSemantics.current
    var revealed by remember(displayed.id) {
        mutableStateOf(displayed.spoilerText.isBlank())
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onOpenStatus(displayed) }
    ) {
        if (status.reblog != null) {
            Row(
                modifier = Modifier.padding(start = 16.dp, top = 10.dp, end = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.Repeat,
                    contentDescription = null,
                    tint = semantics.boost,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.boosted_by, status.account.nameOrHandle),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = semantics.boost
                )
            }
        }

        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Avatar(
                url = displayed.account.avatar,
                size = 44.dp,
                modifier = Modifier.clickable { onOpenAccount(displayed.account.id) }
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onOpenAccount(displayed.account.id) }
                    ) {
                        val accountName = rememberEmojiText(
                            displayed.account.nameOrHandle,
                            displayed.account.emojis
                        )
                        Text(
                            text = accountName.text,
                            inlineContent = accountName.inlineContent,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "@${displayed.account.acct}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = RelativeTime.format(
                            iso = displayed.createdAt,
                            nowLabel = stringResource(R.string.time_now)
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(Modifier.height(8.dp))

                if (displayed.spoilerText.isNotBlank() && !revealed) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(semantics.replySoft)
                            .clickable { revealed = true }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.WarningAmber,
                            contentDescription = null,
                            tint = semantics.reply,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.content_warning),
                                style = MaterialTheme.typography.labelMedium,
                                color = semantics.reply
                            )
                            Text(
                                text = displayed.spoilerText,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = stringResource(R.string.tap_to_show),
                            style = MaterialTheme.typography.labelSmall,
                            color = semantics.reply
                        )
                    }
                } else {
                    if (displayed.spoilerText.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.WarningAmber,
                                contentDescription = null,
                                tint = semantics.reply,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = displayed.spoilerText,
                                style = MaterialTheme.typography.bodySmall,
                                color = semantics.reply
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                    }

                    StatusBody(displayed, onOpenUrl)
                }

                StatusActions(
                    status = displayed,
                    onOpenUrl = onOpenUrl,
                    onReply = { onReply(displayed) },
                    onFavourite = { onFavourite(status) },
                    onReblog = { onReblog(status) },
                    interactive = interactive
                )
            }
        }

        HorizontalDivider(
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outlineVariant
        )
    }
}

@Composable
private fun StatusBody(status: Status, onOpenUrl: (String) -> Unit) {
    val body = HtmlText.plain(
        status.content,
        status.emojis.filter { it.url.isNotBlank() }.map { it.shortcode }.toSet()
    )
    if (body.isNotBlank()) {
        val emojiText = rememberStatusText(body, status.emojis)
        Text(
            text = emojiText.text,
            inlineContent = emojiText.inlineContent,
            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 23.sp),
            modifier = Modifier.fillMaxWidth()
        )
    }
    MediaGrid(media = status.mediaAttachments, onOpenUrl = onOpenUrl)
    PollView(poll = status.poll)
}

@Composable
private fun rememberStatusText(body: String, emojis: List<Emoji>): EmojiTextData {
    val semantics = LocalOltnSemantics.current
    val mentionColor = MaterialTheme.colorScheme.onSurfaceVariant
    val available = remember(emojis) { emojis.filter { it.url.isNotBlank() } }
    val inlineContent = remember(available) { available.toInlineContent() }
    val emojiByShortcode = remember(available) { available.associateBy { it.shortcode } }
    val annotated = remember(body, semantics.link, semantics.tag, mentionColor, emojiByShortcode) {
        buildAnnotatedString {
            val pattern = Regex(
                """(https?://\S+|#[\p{L}\p{N}_]+|@[\p{L}\p{N}_@.]+|:[A-Za-z0-9_-]+:)"""
            )
            var cursor = 0
            pattern.findAll(body).forEach { match ->
                append(body.substring(cursor, match.range.first))
                val token = match.value
                val emoji = if (token.startsWith(":")) {
                    emojiByShortcode[token.removeSurrounding(":")]
                } else {
                    null
                }
                if (emoji != null) {
                    appendInlineContent("emoji:${emoji.shortcode}", token)
                } else {
                    val color = when {
                        token.startsWith("#") -> semantics.tag
                        token.startsWith("@") -> mentionColor
                        token.startsWith(":") -> null
                        else -> semantics.link
                    }
                    if (color != null) {
                        withStyle(SpanStyle(color = color)) { append(token) }
                    } else {
                        append(token)
                    }
                }
                cursor = match.range.last + 1
            }
            append(body.substring(cursor))
        }
    }
    return EmojiTextData(annotated, inlineContent)
}

@Composable
private fun StatusActions(
    status: Status,
    onOpenUrl: (String) -> Unit,
    onReply: () -> Unit,
    onFavourite: () -> Unit,
    onReblog: () -> Unit,
    interactive: Boolean
) {
    val semantics = LocalOltnSemantics.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        if (interactive) {
            StatusAction(
                icon = Icons.Rounded.ChatBubbleOutline,
                count = status.repliesCount,
                tint = semantics.reply,
                active = false,
                contentDescription = stringResource(R.string.action_reply),
                onClick = onReply
            )
            StatusAction(
                icon = Icons.Rounded.Repeat,
                count = status.reblogsCount,
                tint = semantics.boost,
                active = status.reblogged,
                contentDescription = stringResource(R.string.action_boost),
                onClick = onReblog
            )
            StatusAction(
                icon = if (status.favourited) {
                    Icons.Rounded.Favorite
                } else {
                    Icons.Rounded.FavoriteBorder
                },
                count = status.favouritesCount,
                tint = semantics.fav,
                active = status.favourited,
                contentDescription = stringResource(R.string.action_favourite),
                onClick = onFavourite
            )
        }
        Spacer(Modifier.weight(1f))
        IconButton(onClick = { status.displayUrl?.let(onOpenUrl) }) {
            Icon(
                imageVector = Icons.Rounded.Share,
                contentDescription = stringResource(R.string.share),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun StatusAction(
    icon: ImageVector,
    count: Int,
    tint: Color,
    active: Boolean,
    contentDescription: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (active) tint.copy(alpha = 0.14f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (active) tint else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
        if (count > 0) {
            Spacer(Modifier.width(5.dp))
            Text(
                text = formatCount(count),
                style = MaterialTheme.typography.labelSmall,
                color = if (active) tint else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
