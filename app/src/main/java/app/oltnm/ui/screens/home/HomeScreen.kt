package app.oltnm.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.oltnm.R
import app.oltnm.data.api.MastodonApi
import app.oltnm.data.model.Session
import app.oltnm.data.model.TimelineSpec
import app.oltnm.data.model.TimelineTypes
import app.oltnm.di.AppContainer
import app.oltnm.ui.components.Avatar
import app.oltnm.ui.components.EmptyView
import app.oltnm.ui.components.ErrorBanner
import app.oltnm.ui.components.ErrorView
import app.oltnm.ui.components.LoadMoreButton
import app.oltnm.ui.components.LoadingRow
import app.oltnm.ui.components.LoadingView
import app.oltnm.ui.components.OltnMark
import app.oltnm.ui.components.OltnTopBar
import app.oltnm.ui.components.StatusCard
import app.oltnm.ui.components.rememberOpenUrl
import app.oltnm.ui.factoryOf

@Composable
fun TimelineScreen(
    container: AppContainer,
    api: MastodonApi,
    session: Session,
    onOpenAccount: (String) -> Unit,
    onOpenStatus: (String, String?) -> Unit,
    onManageTimelines: () -> Unit
) {
    val timelines by container.timelineStore.timelines.collectAsStateWithLifecycle()
    val selectedId by container.timelineStore.selectedId.collectAsStateWithLifecycle()
    val selected = timelines.firstOrNull { it.id == selectedId } ?: timelines.firstOrNull()
    var refreshSignal by remember { mutableIntStateOf(0) }

    LaunchedEffect(timelines, selectedId) {
        if (selected != null && selected.id != selectedId) {
            container.timelineStore.select(selected.id)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        OltnTopBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OltnMark(size = 24.dp, animated = false)
                    Spacer(Modifier.width(7.dp))
                    Text(
                        text = "OLTN",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = session.host,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            actions = {
                IconButton(onClick = { refreshSignal++ }) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = stringResource(R.string.refresh)
                    )
                }
                Box(
                    modifier = Modifier
                        .padding(end = 10.dp)
                        .size(34.dp)
                        .clip(CircleShape)
                        .clickable { onOpenAccount(session.accountId) }
                ) {
                    Avatar(url = session.avatar, size = 34.dp)
                }
            }
        )

        TimelineChips(
            timelines = timelines,
            selectedId = selected?.id,
            onSelect = { container.timelineStore.select(it.id) },
            onManage = onManageTimelines
        )

        if (selected == null) {
            Column(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                EmptyView(stringResource(R.string.empty_timelines))
                TextButton(onClick = onManageTimelines) {
                    Text(stringResource(R.string.manage_timelines))
                }
            }
        } else {
            val remote = selected.type == TimelineTypes.REMOTE_LOCAL ||
                selected.type == TimelineTypes.REMOTE_FEDERATED
            val remoteHost = if (remote) selected.host else null
            val remoteApi = if (remote) {
                val host = selected.host.orEmpty()
                remember(selected.id, host) { container.publicApiForHost(host) }
            } else {
                null
            }
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                TimelineView(
                    spec = selected,
                    ownApi = api,
                    remoteApi = remoteApi,
                    interactive = !remote,
                    refreshSignal = refreshSignal,
                    onOpenAccount = onOpenAccount,
                    onOpenStatus = { id -> onOpenStatus(id, remoteHost) }
                )
            }
        }
    }
}

@Composable
private fun TimelineChips(
    timelines: List<TimelineSpec>,
    selectedId: String?,
    onSelect: (TimelineSpec) -> Unit,
    onManage: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        timelines.forEach { spec ->
            val selected = spec.id == selectedId
            Text(
                text = timelineLabel(spec),
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(
                        if (selected) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                        } else {
                            Color.Transparent
                        }
                    )
                    .clickable { onSelect(spec) }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            )
        }
        IconButton(onClick = onManage) {
            Icon(
                imageVector = Icons.Rounded.Tune,
                contentDescription = stringResource(R.string.manage_timelines)
            )
        }
        if (timelines.isEmpty()) {
            TextButton(onClick = onManage) {
                Text(stringResource(R.string.add_timeline))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimelineView(
    spec: TimelineSpec,
    ownApi: MastodonApi,
    remoteApi: MastodonApi?,
    interactive: Boolean,
    refreshSignal: Int,
    onOpenAccount: (String) -> Unit,
    onOpenStatus: (String) -> Unit
) {
    val viewModel: TimelineViewModel = viewModel(
        key = "timeline-${spec.id}-${spec.host}-${ownApi.hashCode()}",
        factory = factoryOf { TimelineViewModel(spec, ownApi, remoteApi) }
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val openUrl = rememberOpenUrl()
    val listState = rememberLazyListState()

    LaunchedEffect(refreshSignal) {
        if (refreshSignal > 0) viewModel.refresh()
    }

    val shouldLoadMore by remember {
        derivedStateOf {
            val layout = listState.layoutInfo
            val lastVisible = layout.visibleItemsInfo.lastOrNull()?.index ?: -1
            layout.totalItemsCount > 0 && lastVisible >= layout.totalItemsCount - 3
        }
    }
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) viewModel.loadMore()
    }

    val accountCallback: (String) -> Unit = if (interactive) {
        onOpenAccount
    } else {
        { accountId ->
            val account = state.items
                .asSequence()
                .map { it.original.account }
                .firstOrNull { it.id == accountId }
            account?.url?.takeIf { it.isNotBlank() }?.let(openUrl)
        }
    }

    PullToRefreshBox(
        isRefreshing = state.refreshing,
        onRefresh = viewModel::refresh,
        modifier = Modifier.fillMaxSize()
    ) {
        when {
            state.loading -> LoadingView()
            state.items.isEmpty() && state.error != null -> ErrorView(
                message = state.error.orEmpty(),
                onRetry = viewModel::refresh
            )
            state.items.isEmpty() -> EmptyView(
                message = stringResource(R.string.empty_timeline)
            )
            else -> LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                state.error?.let { message ->
                    item(key = "error") {
                        ErrorBanner(
                            message = message,
                            onDismiss = viewModel::clearError
                        )
                    }
                }
                items(state.items, key = { it.id }) { status ->
                    StatusCard(
                        status = status,
                        onOpenAccount = accountCallback,
                        onOpenStatus = { onOpenStatus(it.original.id) },
                        onOpenUrl = openUrl,
                        onFavourite = viewModel::toggleFavourite,
                        onReblog = viewModel::toggleReblog,
                        interactive = interactive
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

@Composable
fun StatusTimelineScreen(
    api: MastodonApi,
    spec: TimelineSpec,
    title: String,
    onBack: () -> Unit,
    onOpenAccount: (String) -> Unit,
    onOpenStatus: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        OltnTopBar(
            title = { Text(title) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = stringResource(R.string.back)
                    )
                }
            }
        )
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            TimelineView(
                spec = spec,
                ownApi = api,
                remoteApi = null,
                interactive = true,
                refreshSignal = 0,
                onOpenAccount = onOpenAccount,
                onOpenStatus = onOpenStatus
            )
        }
    }
}
