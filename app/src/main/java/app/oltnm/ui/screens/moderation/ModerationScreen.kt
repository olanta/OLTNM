package app.oltnm.ui.screens.moderation

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.oltnm.R
import app.oltnm.data.api.MastodonApi
import app.oltnm.data.model.Account
import app.oltnm.ui.components.Avatar
import app.oltnm.ui.components.EmptyView
import app.oltnm.ui.components.ErrorBanner
import app.oltnm.ui.components.ErrorView
import app.oltnm.ui.components.LoadingView
import app.oltnm.ui.components.OltnFilterChip
import app.oltnm.ui.components.OltnGhostButton
import app.oltnm.ui.components.OltnPrimaryButton
import app.oltnm.ui.components.OltnTextField
import app.oltnm.ui.components.OltnTopBar
import app.oltnm.ui.components.rememberEmojiText
import app.oltnm.ui.factoryOf
import app.oltnm.util.HtmlText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModerationScreen(
    api: MastodonApi,
    onBack: () -> Unit,
    onOpenAccount: (String) -> Unit
) {
    val viewModel: ModerationViewModel = viewModel(
        key = "moderation-${api.hashCode()}",
        factory = factoryOf { ModerationViewModel(api) }
    )
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        OltnTopBar(
            title = { Text(stringResource(R.string.moderation_title)) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = stringResource(R.string.back)
                    )
                }
            }
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ModerationTab.entries.forEach { tab ->
                OltnFilterChip(
                    selected = state.tab == tab,
                    onClick = { viewModel.selectTab(tab) },
                    label = stringResource(tab.labelRes())
                )
            }
        }

        state.error?.let { message ->
            if (state.itemsCount > 0) {
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
                state.itemsCount == 0 && state.error != null -> ErrorView(
                    message = state.error.orEmpty(),
                    onRetry = viewModel::refresh
                )
                else -> when (state.tab) {
                    ModerationTab.FollowRequests -> FollowRequestsList(
                        accounts = state.followRequests,
                        busy = state.busy,
                        onOpenAccount = onOpenAccount,
                        onAccept = viewModel::authorize,
                        onReject = viewModel::reject
                    )
                    ModerationTab.Mutes -> MutesList(
                        accounts = state.mutes,
                        busy = state.busy,
                        onOpenAccount = onOpenAccount,
                        onUnmute = viewModel::unmute
                    )
                    ModerationTab.Domains -> DomainsList(
                        domains = state.domains,
                        query = state.domainQuery,
                        busy = state.busy,
                        onQueryChange = viewModel::onDomainQueryChange,
                        onBlock = viewModel::blockDomain,
                        onUnblock = viewModel::unblockDomain
                    )
                }
            }
        }
    }
}

@Composable
private fun FollowRequestsList(
    accounts: List<Account>,
    busy: Set<String>,
    onOpenAccount: (String) -> Unit,
    onAccept: (Account) -> Unit,
    onReject: (Account) -> Unit
) {
    if (accounts.isEmpty()) {
        EmptyView(stringResource(R.string.moderation_empty_requests))
        return
    }
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(accounts, key = { it.id }) { account ->
            ModerationAccountRow(
                account = account,
                busy = account.id in busy,
                onOpenAccount = onOpenAccount
            ) {
                OltnGhostButton(
                    text = stringResource(R.string.moderation_reject),
                    onClick = { onReject(account) },
                    enabled = account.id !in busy
                )
                Spacer(Modifier.width(8.dp))
                OltnPrimaryButton(
                    text = stringResource(R.string.moderation_accept),
                    onClick = { onAccept(account) },
                    enabled = account.id !in busy
                )
            }
        }
    }
}

@Composable
private fun MutesList(
    accounts: List<Account>,
    busy: Set<String>,
    onOpenAccount: (String) -> Unit,
    onUnmute: (Account) -> Unit
) {
    if (accounts.isEmpty()) {
        EmptyView(stringResource(R.string.moderation_empty_mutes))
        return
    }
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(accounts, key = { it.id }) { account ->
            ModerationAccountRow(
                account = account,
                busy = account.id in busy,
                onOpenAccount = onOpenAccount
            ) {
                OltnGhostButton(
                    text = stringResource(R.string.unmute),
                    onClick = { onUnmute(account) },
                    enabled = account.id !in busy
                )
            }
        }
    }
}

@Composable
private fun ModerationAccountRow(
    account: Account,
    busy: Boolean,
    onOpenAccount: (String) -> Unit,
    actions: @Composable () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onOpenAccount(account.id) }
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.Top
        ) {
            Avatar(url = account.avatar, size = 44.dp)
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                val name = rememberEmojiText(account.nameOrHandle, account.emojis)
                Text(
                    text = name.text,
                    inlineContent = name.inlineContent,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "@${account.acct}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val bio = HtmlText.plain(
                    account.note,
                    account.emojis.filter { it.url.isNotBlank() }.map { it.shortcode }.toSet()
                )
                if (bio.isNotBlank()) {
                    Text(
                        text = bio,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 10.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (busy) {
                Text(
                    text = stringResource(R.string.loading),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                actions()
            }
        }
        HorizontalDivider(
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outlineVariant
        )
    }
}

@Composable
private fun DomainsList(
    domains: List<String>,
    query: String,
    busy: Set<String>,
    onQueryChange: (String) -> Unit,
    onBlock: () -> Unit,
    onUnblock: (String) -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item(key = "domain-input") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OltnTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    placeholder = stringResource(R.string.moderation_domain_hint),
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                OltnPrimaryButton(
                    text = stringResource(R.string.moderation_block_domain),
                    onClick = onBlock,
                    enabled = query.isNotBlank() &&
                        query.trim().lowercase() !in busy
                )
            }
        }
        if (domains.isEmpty()) {
            item(key = "empty") {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.moderation_empty_domains),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(domains, key = { it }) { domain ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = domain,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    OltnGhostButton(
                        text = stringResource(R.string.moderation_unblock),
                        onClick = { onUnblock(domain) },
                        enabled = domain !in busy
                    )
                }
                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant
                )
            }
        }
    }
}

private fun ModerationTab.labelRes(): Int = when (this) {
    ModerationTab.FollowRequests -> R.string.moderation_follow_requests
    ModerationTab.Mutes -> R.string.moderation_mutes
    ModerationTab.Domains -> R.string.moderation_domains
}
