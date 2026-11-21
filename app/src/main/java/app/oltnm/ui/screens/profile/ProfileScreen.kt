package app.oltnm.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.oltnm.R
import app.oltnm.data.api.MastodonApi
import app.oltnm.data.filters.FilterStore
import app.oltnm.data.model.Account
import app.oltnm.data.model.Relationship
import app.oltnm.data.model.Status
import app.oltnm.ui.components.Avatar
import app.oltnm.ui.components.ErrorBanner
import app.oltnm.ui.components.ErrorView
import app.oltnm.ui.components.LoadMoreButton
import app.oltnm.ui.components.LoadingRow
import app.oltnm.ui.components.LoadingView
import app.oltnm.ui.components.OltnGhostButton
import app.oltnm.ui.components.OltnPrimaryButton
import app.oltnm.ui.components.OltnTopBar
import app.oltnm.ui.components.StatusCard
import app.oltnm.ui.components.formatCount
import app.oltnm.ui.components.rememberEmojiText
import app.oltnm.ui.components.rememberOpenUrl
import app.oltnm.ui.factoryOf
import app.oltnm.util.HtmlText
import coil3.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    api: MastodonApi,
    accountId: String,
    currentAccountId: String,
    onBack: () -> Unit,
    onOpenAccount: (String) -> Unit,
    onOpenStatus: (String) -> Unit,
    onReply: (Status) -> Unit,
    filterStore: FilterStore? = null
) {
    val viewModel: ProfileViewModel = viewModel(
        key = "profile-${api.hashCode()}-$accountId",
        factory = factoryOf { ProfileViewModel(api, accountId, filterStore) }
    )
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
        OltnTopBar(
            title = { Text(stringResource(R.string.profile_title)) },
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
                state.account == null && state.error != null -> ErrorView(
                    message = state.error.orEmpty(),
                    onRetry = viewModel::refresh
                )
                else -> LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                    state.account?.let { account ->
                        item(key = "header") {
                            ProfileHeader(
                                account = account,
                                relationship = state.relationship,
                                showActions = account.id != currentAccountId,
                                followBusy = state.followBusy,
                                actionBusy = state.actionBusy,
                                onFollow = viewModel::toggleFollow,
                                onMute = viewModel::toggleMute,
                                onBlock = viewModel::toggleBlock
                            )
                        }
                    }
                    if (state.items.isEmpty()) {
                        item(key = "empty") {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stringResource(R.string.empty_timeline),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        items(state.items, key = { it.id }) { status ->
                            StatusCard(
                                status = status,
                                onOpenAccount = onOpenAccount,
                                onOpenStatus = { onOpenStatus(it.original.id) },
                                onReply = onReply,
                                onOpenUrl = openUrl,
                                onFavourite = viewModel::toggleFavourite,
                                onReblog = viewModel::toggleReblog,
                                onBookmark = viewModel::toggleBookmark,
                                filterWarning = state.warnings[status.id]
                            )
                        }
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
private fun ProfileHeader(
    account: Account,
    relationship: Relationship?,
    showActions: Boolean,
    followBusy: Boolean,
    actionBusy: Boolean,
    onFollow: () -> Unit,
    onMute: () -> Unit,
    onBlock: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
        ) {
            if (account.header.isNotBlank()) {
                AsyncImage(
                    model = account.header,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Avatar(url = account.avatar, size = 68.dp)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                val accountName = rememberEmojiText(account.nameOrHandle, account.emojis)
                Text(
                    text = accountName.text,
                    inlineContent = accountName.inlineContent,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "@${account.acct}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (account.bot) {
                    Text(
                        text = stringResource(R.string.bot_account),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            if (showActions && relationship != null) {
                Box {
                    IconButton(
                        onClick = { menuOpen = true },
                        enabled = !actionBusy
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MoreVert,
                            contentDescription = stringResource(R.string.moderation_title)
                        )
                    }
                    DropdownMenu(
                        expanded = menuOpen,
                        onDismissRequest = { menuOpen = false }
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    stringResource(
                                        if (relationship.muting) R.string.unmute else R.string.mute
                                    )
                                )
                            },
                            onClick = {
                                menuOpen = false
                                onMute()
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    stringResource(
                                        if (relationship.blocking) R.string.unblock else R.string.block
                                    )
                                )
                            },
                            onClick = {
                                menuOpen = false
                                onBlock()
                            }
                        )
                    }
                }
            }
        }

        if (showActions && relationship != null && !relationship.blocking) {
            ProfileFollowRow(
                relationship = relationship,
                followBusy = followBusy,
                onFollow = onFollow
            )
        }

        val note = HtmlText.plain(
            account.note,
            account.emojis.filter { it.url.isNotBlank() }.map { it.shortcode }.toSet()
        )
        val noteText = rememberEmojiText(note, account.emojis)
        if (note.isNotBlank()) {
            Text(
                text = noteText.text,
                inlineContent = noteText.inlineContent,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            ProfileStat(account.statusesCount, stringResource(R.string.profile_statuses))
            ProfileStat(account.followersCount, stringResource(R.string.profile_followers))
            ProfileStat(account.followingCount, stringResource(R.string.profile_following))
        }

        HorizontalDivider(
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
        )
    }
}

@Composable
private fun ProfileFollowRow(
    relationship: Relationship,
    followBusy: Boolean,
    onFollow: () -> Unit
) {
    val status = when {
        relationship.following && relationship.followedBy -> R.string.follow_mutual
        relationship.following -> R.string.follow_following
        relationship.followedBy -> R.string.follow_follows_you
        relationship.requested -> R.string.follow_requested
        else -> null
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (status != null) {
            Text(
                text = stringResource(status),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.weight(1f))
        val label = when {
            relationship.following -> R.string.unfollow
            relationship.requested -> R.string.cancel_follow_request
            relationship.followedBy -> R.string.follow_back
            else -> R.string.follow
        }
        if (relationship.following || relationship.requested) {
            OltnGhostButton(
                text = stringResource(label),
                onClick = onFollow,
                enabled = !followBusy
            )
        } else {
            OltnPrimaryButton(
                text = stringResource(label),
                onClick = onFollow,
                enabled = !followBusy
            )
        }
    }
}

@Composable
private fun ProfileStat(value: Int, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = formatCount(value),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
