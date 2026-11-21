package app.oltnm.ui.screens.notifications

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import app.oltnm.data.filters.FilterStore
import app.oltnm.data.model.GroupedNotification
import app.oltnm.data.model.Notification
import app.oltnm.data.model.Status
import app.oltnm.ui.components.Avatar
import app.oltnm.ui.components.EmptyView
import app.oltnm.ui.components.ErrorBanner
import app.oltnm.ui.components.ErrorView
import app.oltnm.ui.components.LoadMoreButton
import app.oltnm.ui.components.LoadingRow
import app.oltnm.ui.components.LoadingView
import app.oltnm.ui.components.OltnFilterChip
import app.oltnm.ui.components.OltnTopBar
import app.oltnm.ui.components.rememberEmojiText
import app.oltnm.ui.factoryOf
import app.oltnm.ui.theme.LocalOltnSemantics
import app.oltnm.util.HtmlText
import app.oltnm.util.RelativeTime

private enum class NotificationsTab(@StringRes val labelRes: Int) {
    All(R.string.notifications_all),
    Grouped(R.string.notifications_grouped)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    api: MastodonApi,
    onOpenAccount: (String) -> Unit,
    onOpenStatus: (String) -> Unit,
    filterStore: FilterStore? = null
) {
    val viewModel: NotificationsViewModel = viewModel(
        key = "notifications-${api.hashCode()}",
        factory = factoryOf { NotificationsViewModel(api, filterStore) }
    )
    val groupedViewModel: GroupedNotificationsViewModel = viewModel(
        key = "grouped-notifications-${api.hashCode()}",
        factory = factoryOf { GroupedNotificationsViewModel(api, filterStore) }
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val groupedState by groupedViewModel.state.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    var tab by remember { mutableStateOf(NotificationsTab.All) }

    val shouldLoadMore by remember {
        derivedStateOf {
            val layout = listState.layoutInfo
            val lastVisible = layout.visibleItemsInfo.lastOrNull()?.index ?: -1
            layout.totalItemsCount > 0 && lastVisible >= layout.totalItemsCount - 3
        }
    }

    androidx.compose.runtime.LaunchedEffect(tab) {
        if (tab == NotificationsTab.Grouped &&
            groupedState.items.isEmpty() &&
            groupedState.error == null &&
            !groupedState.loading
        ) {
            groupedViewModel.refresh()
        }
    }

    androidx.compose.runtime.LaunchedEffect(shouldLoadMore, tab) {
        if (shouldLoadMore) {
            if (tab == NotificationsTab.Grouped) {
                groupedViewModel.loadMore()
            } else {
                viewModel.loadMore()
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        OltnTopBar(title = { Text(stringResource(R.string.notifications_title)) })

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            NotificationsTab.entries.forEach { item ->
                OltnFilterChip(
                    selected = tab == item,
                    onClick = { tab = item },
                    label = stringResource(item.labelRes)
                )
            }
        }

        if (tab == NotificationsTab.Grouped) {
            groupedState.error?.let { message ->
                if (groupedState.items.isNotEmpty()) {
                    ErrorBanner(
                        message = message,
                        onDismiss = groupedViewModel::clearError
                    )
                }
            }
        } else {
            state.error?.let { message ->
                if (state.items.isNotEmpty()) {
                    ErrorBanner(message = message, onDismiss = viewModel::clearError)
                }
            }
        }

        PullToRefreshBox(
            isRefreshing = if (tab == NotificationsTab.Grouped) {
                groupedState.refreshing
            } else {
                state.refreshing
            },
            onRefresh = if (tab == NotificationsTab.Grouped) {
                groupedViewModel::refresh
            } else {
                viewModel::refresh
            },
            modifier = Modifier.weight(1f).fillMaxWidth()
        ) {
            if (tab == NotificationsTab.Grouped) {
                when {
                    groupedState.loading -> LoadingView()
                    groupedState.items.isEmpty() && groupedState.error != null -> ErrorView(
                        message = groupedState.error.orEmpty(),
                        onRetry = groupedViewModel::refresh
                    )
                    groupedState.items.isEmpty() -> EmptyView(
                        stringResource(R.string.notifications_empty)
                    )
                    else -> LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                        items(groupedState.items, key = { it.group.groupKey }) { item ->
                            GroupedNotificationRow(
                                item = item,
                                onOpenAccount = onOpenAccount,
                                onOpenStatus = onOpenStatus
                            )
                        }
                        item {
                            if (groupedState.loadingMore) {
                                LoadingRow()
                            } else if (groupedState.nextMaxId != null) {
                                LoadMoreButton(onClick = groupedViewModel::loadMore)
                            }
                        }
                    }
                }
            } else {
                when {
                    state.loading -> LoadingView()
                    state.items.isEmpty() && state.error != null -> ErrorView(
                        message = state.error.orEmpty(),
                        onRetry = viewModel::refresh
                    )
                    state.items.isEmpty() -> EmptyView(
                        stringResource(R.string.notifications_empty)
                    )
                    else -> LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                        items(state.items, key = { it.id }) { notification ->
                            NotificationRow(
                                notification = notification,
                                onOpenAccount = onOpenAccount,
                                onOpenStatus = onOpenStatus
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
}

@Composable
private fun GroupedNotificationRow(
    item: GroupedNotification,
    onOpenAccount: (String) -> Unit,
    onOpenStatus: (String) -> Unit
) {
    val group = item.group
    val first = item.accounts.firstOrNull()
    val name = first?.nameOrHandle ?: group.type
    val count = group.notificationsCount.coerceAtLeast(1)
    val others = (count - 1).coerceAtLeast(1)
    val text = when {
        count > 1 && group.type == "favourite" -> if (count == 2) {
            stringResource(R.string.notif_group_favourite_one, name)
        } else {
            stringResource(R.string.notif_group_favourite_many, name, others)
        }
        count > 1 && group.type == "reblog" -> if (count == 2) {
            stringResource(R.string.notif_group_reblog_one, name)
        } else {
            stringResource(R.string.notif_group_reblog_many, name, others)
        }
        count > 1 && group.type == "follow" -> if (count == 2) {
            stringResource(R.string.notif_group_follow_one, name)
        } else {
            stringResource(R.string.notif_group_follow_many, name, others)
        }
        group.type == "follow" -> stringResource(R.string.notif_follow, name)
        group.type == "favourite" -> stringResource(R.string.notif_favourite, name)
        group.type == "reblog" -> stringResource(R.string.notif_reblog, name)
        group.type == "mention" -> stringResource(R.string.notif_mention, name)
        group.type == "poll" -> stringResource(R.string.notif_poll)
        else -> stringResource(R.string.notif_unknown, name)
    }
    val semantics = LocalOltnSemantics.current
    val tint = when (group.type) {
        "reblog" -> semantics.boost
        "favourite" -> semantics.fav
        else -> MaterialTheme.colorScheme.primary
    }
    val time = group.latestPageNotificationAt
        ?.takeIf { it.isNotBlank() }
        ?.let {
            RelativeTime.format(iso = it, nowLabel = stringResource(R.string.time_now))
        }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                val status = item.status
                if (status != null) {
                    onOpenStatus(status.original.id)
                } else {
                    first?.let { onOpenAccount(it.id) }
                }
            }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            item.accounts.take(6).forEachIndexed { index, account ->
                if (index > 0) Spacer(Modifier.width(4.dp))
                Avatar(url = account.avatar, size = 28.dp)
            }
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = tint,
                    modifier = Modifier.weight(1f)
                )
                if (time != null) {
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = time,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            item.status?.let { NotificationStatusPreview(it) }
        }
    }
    HorizontalDivider(
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
    )
}

@Composable
private fun NotificationStatusPreview(status: Status) {
    val content = HtmlText.plain(
        status.original.content,
        status.original.emojis
            .filter { it.url.isNotBlank() }
            .map { it.shortcode }
            .toSet()
    )
    val preview = rememberEmojiText(content, status.original.emojis)
    Text(
        text = preview.text,
        inlineContent = preview.inlineContent,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 3,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(top = 4.dp)
    )
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
            .clickable { notification.status?.let { onOpenStatus(it.original.id) } }
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
            notification.status?.let { NotificationStatusPreview(it) }
        }
    }
    HorizontalDivider(
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
    )
}
