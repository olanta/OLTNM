package app.oltnm.ui.screens.content

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.oltnm.R
import app.oltnm.data.api.MastodonApi
import app.oltnm.data.model.Announcement
import app.oltnm.ui.components.EmptyView
import app.oltnm.ui.components.ErrorBanner
import app.oltnm.ui.components.ErrorView
import app.oltnm.ui.components.LoadingView
import app.oltnm.ui.components.OltnGhostButton
import app.oltnm.ui.components.OltnGlassCard
import app.oltnm.ui.components.OltnTopBar
import app.oltnm.ui.components.rememberEmojiText
import app.oltnm.ui.factoryOf
import app.oltnm.util.HtmlText
import app.oltnm.util.RelativeTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnnouncementsScreen(
    api: MastodonApi,
    onBack: () -> Unit
) {
    val viewModel: AnnouncementsViewModel = viewModel(
        key = "announcements-${api.hashCode()}",
        factory = factoryOf { AnnouncementsViewModel(api) }
    )
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        OltnTopBar(
            title = { Text(stringResource(R.string.announcements)) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = stringResource(R.string.back)
                    )
                }
            }
        )

        state.error?.let { message ->
            if (state.announcements.isNotEmpty()) {
                ErrorBanner(message = message, onDismiss = viewModel::clearError)
            }
        }

        PullToRefreshBox(
            isRefreshing = state.refreshing,
            onRefresh = viewModel::refresh,
            modifier = Modifier.weight(1f).fillMaxWidth()
        ) {
            when {
                state.loading -> LoadingView()
                state.announcements.isEmpty() && state.error != null -> ErrorView(
                    message = state.error.orEmpty(),
                    onRetry = viewModel::refresh
                )
                state.announcements.isEmpty() -> EmptyView(stringResource(R.string.announcements_empty))
                else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(state.announcements, key = { it.id }) { announcement ->
                        AnnouncementCard(
                            announcement = announcement,
                            busy = announcement.id in state.busy,
                            onDismiss = { viewModel.dismiss(announcement) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AnnouncementCard(
    announcement: Announcement,
    busy: Boolean,
    onDismiss: () -> Unit
) {
    val content = HtmlText.plain(
        announcement.content,
        announcement.emojis.filter { it.url.isNotBlank() }.map { it.shortcode }.toSet()
    )
    val text = rememberEmojiText(content, announcement.emojis)
    OltnGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        if (!announcement.read) {
            Text(
                text = stringResource(R.string.announcements_new),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Text(
            text = text.text,
            inlineContent = text.inlineContent,
            style = MaterialTheme.typography.bodyMedium
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val published = announcement.publishedAt
                ?.takeIf { it.isNotBlank() }
                ?.let {
                    RelativeTime.format(
                        iso = it,
                        nowLabel = stringResource(R.string.time_now)
                    )
                }
            if (!published.isNullOrBlank()) {
                Text(
                    text = published,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.weight(1f))
            OltnGhostButton(
                text = stringResource(R.string.announcements_dismiss),
                onClick = onDismiss,
                enabled = !busy
            )
        }
    }
}
