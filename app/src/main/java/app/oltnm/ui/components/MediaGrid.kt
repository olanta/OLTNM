package app.oltnm.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.oltnm.R
import app.oltnm.data.model.MediaAttachment
import app.oltnm.ui.theme.LocalOltnSemantics
import coil3.compose.AsyncImage

@Composable
fun MediaGrid(
    media: List<MediaAttachment>,
    onOpenUrl: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (media.isEmpty()) return
    val mediaBg = LocalOltnSemantics.current.mediaBg
    val shown = media.take(4)

    Column(
        modifier = modifier.padding(top = 10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (shown.size == 1) {
            MediaThumb(
                attachment = shown.first(),
                modifier = Modifier.fillMaxWidth().aspectRatio(4f / 3f),
                mediaBg = mediaBg,
                single = true,
                onOpenUrl = onOpenUrl
            )
        } else {
            shown.chunked(2).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    row.forEach { attachment ->
                        MediaThumb(
                            attachment = attachment,
                            modifier = Modifier.weight(1f).aspectRatio(1.3f),
                            mediaBg = mediaBg,
                            onOpenUrl = onOpenUrl
                        )
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
        if (media.size > shown.size) {
            Text(
                text = stringResource(R.string.more_media, media.size - shown.size),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MediaThumb(
    attachment: MediaAttachment,
    modifier: Modifier,
    mediaBg: Color,
    single: Boolean = false,
    onOpenUrl: (String) -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(mediaBg)
            .clickable { onOpenUrl(attachment.bestUrl) }
    ) {
        AsyncImage(
            model = attachment.bestPreview,
            contentDescription = attachment.description,
            contentScale = if (single) ContentScale.Fit else ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        if (attachment.type != "image") {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.24f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayCircle,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.92f),
                    modifier = Modifier.size(40.dp)
                )
            }
        }
    }
}
