package app.oltnm.ui.screens.notifications

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AlternateEmail
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Poll
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.oltnm.R
import app.oltnm.data.api.MastodonApi
import app.oltnm.data.model.Notification
import app.oltnm.ui.components.Avatar
import app.oltnm.ui.components.EmptyView
import app.oltnm.ui.components.ErrorBanner
import app.oltnm.ui.components.ErrorView
import app.oltnm.ui.components.LoadMoreButton
import app.oltnm.ui.components.LoadingRow
import app.oltnm.ui.components.LoadingView
import app.oltnm.ui.components.OltnTopBar
import app.oltnm.ui.components.rememberOpenUrl
import app.oltnm.ui.factoryOf
import app.oltnm.ui.theme.LocalOltnSemantics
import app.oltnm.util.HtmlText
import app.oltnm.util.RelativeTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    api: MastodonApi,
    onOpenAccount: (String) -> Unit
) {
    val viewModel: NotificationsViewModel = viewModel(key = "notifications-${api.hashCode()}", factory = factoryOf { NotificationsViewModel(api) })
    val state by viewModel.state.collectAsStateWithLifecycle()
    val openUrl = rememberOpenUrl()
    val listState = rememberLazyListState()

    val shouldLoadMore by remember {
        derivedStateOf {
            val layout = listState.layoutInfo
            val lastVisible = layout.visibleItemsInfo.lastOrNull()?.index ?: -1
            layout.totalItemsCount > 0 && lastVisible >= layout.totalItemsCount - 3
        }
    }

    androidx.compose.runtime.LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) viewModel.loadMore()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        OltnTopBar(title = { Text(stringResource(R.string.notifications_title)) })

        state.error?.let { message ->
            if (state.items.isNotEmpty()) {
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
                state.items.isEmpty() && state.error != null -> ErrorView(
                    message = state.error.orEmpty(),
                    onRetry = viewModel::refresh
                )
                state.items.isEmpty() -> EmptyView(stringResource(R.string.notifications_empty))
                else -> LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                    items(state.items, key = { it.id }) { notification ->
                        NotificationRow(
                            notification = notification,
                            onOpenAccount = onOpenAccount,
                            onOpenStatus = { url -> openUrl(url) }
                        )
                    }
                    item {
                        if (state.loadingMore) {
                            LoadingRow()
                        } else if (state.nextMaxId != null) {
                            LoadMoreButton(onClick = viewModel::loadMore)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationRow(
    notification: Notification,
    onOpenAccount: (String) -> Unit,
    onOpenStatus: (String) -> Unit
) {
    val name = notification.account.nameOrHandle
    val text = when (notification.type) {
        "follow" -> stringResource(R.string.notif_follow, name)
        "favourite" -> stringResource(R.string.notif_favourite, name)
        "reblog" -> stringResource(R.string.notif_reblog, name)
        "mention" -> stringResource(R.string.notif_mention, name)
        "poll" -> stringResource(R.string.notif_poll)
        else -> stringResource(R.string.notif_unknown, name)
    }
    val icon = when (notification.type) {
        "follow" -> Icons.Rounded.PersonAdd
        "favourite" -> Icons.Rounded.Favorite
        "reblog" -> Icons.Rounded.Repeat
        "mention" -> Icons.Rounded.AlternateEmail
        "poll" -> Icons.Rounded.Poll
        else -> Icons.Rounded.Notifications
    }
    val semantics = LocalOltnSemantics.current
    val iconTint = when (notification.type) {
        "favourite" -> semantics.fav
        "reblog" -> semantics.boost
        "mention" -> semantics.tag
        "poll" -> semantics.reply
        else -> MaterialTheme.colorScheme.primary
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { notification.status?.displayUrl?.let(onOpenStatus) }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Avatar(
            url = notification.account.avatar,
            size = 42.dp,
            modifier = Modifier.clickable { onOpenAccount(notification.account.id) }
        )
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = RelativeTime.format(
                        iso = notification.createdAt,
                        nowLabel = stringResource(R.string.time_now)
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            notification.status?.let { status ->
                Text(
                    text = HtmlText.plain(status.original.content),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
    HorizontalDivider(
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
    )
}
