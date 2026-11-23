package app.oltnm.ui.screens.search

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.oltnm.R
import app.oltnm.data.api.MastodonApi
import app.oltnm.data.discover.DiscoverSeedData
import app.oltnm.data.model.Account
import app.oltnm.data.model.Relationship
import app.oltnm.data.model.Status
import app.oltnm.data.model.Tag
import app.oltnm.ui.components.Avatar
import app.oltnm.ui.components.EmptyView
import app.oltnm.ui.components.ErrorView
import app.oltnm.ui.components.LoadingView
import app.oltnm.ui.components.OltnFilterChip
import app.oltnm.ui.components.OltnGhostButton
import app.oltnm.ui.components.OltnPrimaryButton
import app.oltnm.ui.components.OltnTextField
import app.oltnm.ui.components.OltnTopBar
import app.oltnm.ui.components.StatusCard
import app.oltnm.ui.components.formatCount
import app.oltnm.ui.components.rememberEmojiText
import app.oltnm.ui.components.rememberOpenUrl
import app.oltnm.ui.factoryOf
import app.oltnm.ui.theme.LocalOltnSemantics
import app.oltnm.util.HtmlText

private enum class SearchTab(@StringRes val labelRes: Int) {
    Discover(R.string.discover),
    Accounts(R.string.search_accounts),
    Hashtags(R.string.search_hashtags),
    Posts(R.string.search_posts)
}

@Composable
fun SearchScreen(
    api: MastodonApi,
    currentAccountId: String,
    instanceHost: String,
    onOpenAccount: (String) -> Unit,
    onOpenStatus: (String) -> Unit,
    onOpenTag: (String) -> Unit,
    onReply: (Status) -> Unit,
    onQuote: (Status) -> Unit
) {
    val context = LocalContext.current
    val seedGroups = remember(context) { DiscoverSeedData.load(context) }
    val appLanguage = LocalConfiguration.current.locales[0].language
    val initialLanguageId = if (appLanguage.equals("pt", ignoreCase = true)) "pt" else "en"
    val viewModel: SearchViewModel = viewModel(
        key = "search-${api.hashCode()}",
        factory = factoryOf { SearchViewModel(api) }
    )
    val discoverViewModel: DiscoverViewModel = viewModel(
        key = "discover-${api.hashCode()}",
        factory = factoryOf {
            DiscoverViewModel(
                api = api,
                currentAccountId = currentAccountId,
                instanceHost = instanceHost,
                seedGroups = seedGroups,
                initialLanguageId = initialLanguageId
            )
        }
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val discoverState by discoverViewModel.state.collectAsStateWithLifecycle()
    val openUrl = rememberOpenUrl()
    var tab by remember { mutableStateOf(SearchTab.Discover) }
    val submitSearch: () -> Unit = {
        tab = SearchTab.Accounts
        viewModel.search()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        OltnTopBar(title = { Text(stringResource(R.string.search_title)) })

        OltnTextField(
            value = state.query,
            onValueChange = viewModel::onQueryChange,
            placeholder = stringResource(R.string.search_hint),
            leadingIcon = {
                Icon(Icons.Rounded.Search, contentDescription = null)
            },
            trailingIcon = {
                IconButton(onClick = submitSearch) {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = stringResource(R.string.search_title)
                    )
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { submitSearch() }),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SearchTab.entries.forEach { item ->
                OltnFilterChip(
                    selected = tab == item,
                    onClick = { tab = item },
                    label = stringResource(item.labelRes)
                )
            }
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth().padding(top = 12.dp)) {
            if (tab == SearchTab.Discover) {
                DiscoverContent(
                    state = discoverState,
                    onMode = discoverViewModel::selectMode,
                    onCategory = discoverViewModel::selectCategory,
                    onLanguage = discoverViewModel::selectLanguage,
                    onRetry = discoverViewModel::retry,
                    onFollow = discoverViewModel::toggleFollow,
                    onSeedFollow = discoverViewModel::toggleSeed,
                    onSeedResolve = discoverViewModel::resolveSeed,
                    onShowMoreSeeds = discoverViewModel::showMoreSeeds,
                    onOpenAccount = onOpenAccount
                )
            } else {
                val results = state.results
                when {
                    state.loading -> LoadingView()
                    results == null && state.error != null -> ErrorView(
                        message = state.error.orEmpty(),
                        onRetry = viewModel::search
                    )
                    results == null -> EmptyView(stringResource(R.string.search_empty))
                    else -> when (tab) {
                        SearchTab.Accounts -> if (results.accounts.isEmpty()) {
                            EmptyView(stringResource(R.string.search_no_results))
                        } else {
                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                items(results.accounts, key = { it.id }) { account ->
                                    AccountRow(account = account, onOpenAccount = onOpenAccount)
                                }
                            }
                        }
                        SearchTab.Hashtags -> if (results.hashtags.isEmpty()) {
                            EmptyView(stringResource(R.string.search_no_results))
                        } else {
                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                items(results.hashtags, key = { it.name }) { tag ->
                                    HashtagRow(tag = tag, onOpen = { onOpenTag(tag.name) })
                                }
                            }
                        }
                        SearchTab.Posts -> if (results.statuses.isEmpty()) {
                            EmptyView(stringResource(R.string.search_no_results))
                        } else {
                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                items(results.statuses, key = { it.id }) { status ->
                                    StatusCard(
                                        status = status,
                                        onOpenAccount = onOpenAccount,
                                        onOpenStatus = { onOpenStatus(it.original.id) },
                                        onReply = onReply,
                                        onQuote = onQuote,
                                        onOpenUrl = openUrl,
                                        onFavourite = viewModel::toggleFavourite,
                                        onReblog = viewModel::toggleReblog,
                                        onBookmark = viewModel::toggleBookmark
                                    )
                                }
                            }
                        }
                        SearchTab.Discover -> Unit
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DiscoverContent(
    state: DiscoverUiState,
    onMode: (DiscoverMode) -> Unit,
    onCategory: (String) -> Unit,
    onLanguage: (String) -> Unit,
    onRetry: () -> Unit,
    onFollow: (String) -> Unit,
    onSeedFollow: (String) -> Unit,
    onSeedResolve: (String) -> Unit,
    onShowMoreSeeds: () -> Unit,
    onOpenAccount: (String) -> Unit
) {
    var languageMenu by remember { mutableStateOf(false) }
    val selectedLanguage = DiscoverLanguages.firstOrNull { it.id == state.languageId }
        ?: DiscoverLanguages.first()
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = stringResource(R.string.discover_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DiscoverMode.entries.forEach { mode ->
                OltnFilterChip(
                    selected = state.mode == mode,
                    onClick = { onMode(mode) },
                    label = stringResource(
                        when (mode) {
                            DiscoverMode.Topics -> R.string.discover_topics
                            DiscoverMode.Languages -> R.string.discover_languages
                        }
                    )
                )
            }
        }

        if (state.mode == DiscoverMode.Topics) {
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DiscoverCategories.forEach { category ->
                    OltnFilterChip(
                        selected = category.id == state.topicId,
                        onClick = { onCategory(category.id) },
                        label = stringResource(category.label)
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.discover_language),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(8.dp))
                Box {
                    OltnGhostButton(
                        text = "${selectedLanguage.label} · ${selectedLanguage.english}",
                        onClick = { languageMenu = true }
                    )
                    DropdownMenu(
                        expanded = languageMenu,
                        onDismissRequest = { languageMenu = false },
                        modifier = Modifier.heightIn(max = 420.dp)
                    ) {
                        DiscoverLanguages.forEach { language ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = "${language.label} · ${language.english}",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                },
                                onClick = {
                                    languageMenu = false
                                    onLanguage(language.id)
                                }
                            )
                        }
                    }
                }
            }
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            val hasRows = state.accounts.isNotEmpty() || state.seedHandles.isNotEmpty()
            when {
                state.loading -> LoadingView()
                !hasRows && state.error != null -> ErrorView(
                    message = state.error.orEmpty(),
                    onRetry = onRetry
                )
                !hasRows -> EmptyView(stringResource(R.string.discover_empty))
                else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                    state.error?.let { message ->
                        item(key = "discover-error") {
                            Text(
                                text = message,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }
                    items(state.accounts, key = { it.id }) { account ->
                        DiscoverAccountRow(
                            account = account,
                            relationship = state.relationships[account.id],
                            busy = account.id in state.busyIds,
                            onFollow = { onFollow(account.id) },
                            onOpenAccount = onOpenAccount
                        )
                    }
                    items(state.visibleSeedHandles, key = { "seed:$it" }) { handle ->
                        val account = state.seedAccounts[handle]
                        DiscoverSeedRow(
                            handle = handle,
                            account = account,
                            relationship = account?.let { state.relationships[it.id] },
                            busy = handle in state.seedBusy ||
                                (account != null && account.id in state.busyIds),
                            onResolve = { onSeedResolve(handle) },
                            onFollow = { onSeedFollow(handle) },
                            onOpenAccount = onOpenAccount
                        )
                    }
                    if (state.seedHandles.size > state.seedLimit) {
                        item(key = "discover-more-seeds") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                OltnGhostButton(
                                    text = stringResource(
                                        R.string.discover_show_more,
                                        state.seedHandles.size - state.seedLimit
                                    ),
                                    onClick = onShowMoreSeeds
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DiscoverSeedRow(
    handle: String,
    account: Account?,
    relationship: Relationship?,
    busy: Boolean,
    onResolve: () -> Unit,
    onFollow: () -> Unit,
    onOpenAccount: (String) -> Unit
) {
    LaunchedEffect(handle) {
        if (account == null) onResolve()
    }
    val following = relationship?.following == true || relationship?.requested == true
    val label = when {
        relationship == null -> R.string.follow
        relationship.requested -> R.string.follow_requested
        relationship.following -> R.string.unfollow
        relationship.followedBy -> R.string.follow_back
        else -> R.string.follow
    }
    val username = handle.substringBefore("@")
    val followButton: @Composable () -> Unit = {
        if (following) {
            OltnGhostButton(
                text = stringResource(label),
                onClick = onFollow,
                enabled = !busy
            )
        } else {
            OltnPrimaryButton(
                text = stringResource(label),
                onClick = onFollow,
                enabled = !busy
            )
        }
    }
    Column(modifier = Modifier.fillMaxWidth()) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            if (maxWidth < 360.dp) {
                Column {
                    DiscoverSeedInfo(
                        handle = handle,
                        username = username,
                        account = account,
                        onOpenAccount = onOpenAccount,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        followButton()
                    }
                }
            } else {
                Row(verticalAlignment = Alignment.Top) {
                    DiscoverSeedInfo(
                        handle = handle,
                        username = username,
                        account = account,
                        onOpenAccount = onOpenAccount,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    followButton()
                }
            }
        }
        HorizontalDivider(
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outlineVariant
        )
    }
}

@Composable
private fun DiscoverSeedInfo(
    handle: String,
    username: String,
    account: Account?,
    onOpenAccount: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.clickable(enabled = account != null) {
            account?.let { onOpenAccount(it.id) }
        },
        verticalAlignment = Alignment.Top
    ) {
        Avatar(url = account?.avatar.orEmpty(), size = 44.dp)
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            val name = rememberEmojiText(
                account?.nameOrHandle ?: username,
                account?.emojis.orEmpty()
            )
            Text(
                text = name.text,
                inlineContent = name.inlineContent,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "@${account?.acct ?: handle}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (account != null && account.followersCount > 0) {
                Text(
                    text = "${formatCount(account.followersCount)} ${
                        stringResource(R.string.profile_followers)
                    }",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun DiscoverAccountRow(
    account: Account,
    relationship: Relationship?,
    busy: Boolean,
    onFollow: () -> Unit,
    onOpenAccount: (String) -> Unit
) {
    val following = relationship?.following == true || relationship?.requested == true
    val label = when {
        relationship == null -> R.string.follow
        relationship.requested -> R.string.follow_requested
        relationship.following -> R.string.unfollow
        relationship.followedBy -> R.string.follow_back
        else -> R.string.follow
    }
    val followButton: @Composable () -> Unit = {
        if (following) {
            OltnGhostButton(
                text = stringResource(label),
                onClick = onFollow,
                enabled = !busy
            )
        } else {
            OltnPrimaryButton(
                text = stringResource(label),
                onClick = onFollow,
                enabled = relationship != null && !busy
            )
        }
    }
    Column(modifier = Modifier.fillMaxWidth()) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            if (maxWidth < 360.dp) {
                Column {
                    DiscoverAccountInfo(
                        account = account,
                        onOpenAccount = onOpenAccount,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        followButton()
                    }
                }
            } else {
                Row(verticalAlignment = Alignment.Top) {
                    DiscoverAccountInfo(
                        account = account,
                        onOpenAccount = onOpenAccount,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    followButton()
                }
            }
        }
        HorizontalDivider(
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outlineVariant
        )
    }
}

@Composable
private fun DiscoverAccountInfo(
    account: Account,
    onOpenAccount: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.clickable { onOpenAccount(account.id) },
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
            if (account.followersCount > 0) {
                Text(
                    text = "${formatCount(account.followersCount)} ${
                        stringResource(R.string.profile_followers)
                    }",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            val bio = HtmlText.plain(
                account.note,
                account.emojis.filter { it.url.isNotBlank() }.map { it.shortcode }.toSet()
            )
            if (bio.isNotBlank()) {
                val bioText = rememberEmojiText(bio, account.emojis)
                Text(
                    text = bioText.text,
                    inlineContent = bioText.inlineContent,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 3.dp)
                )
            }
        }
    }
}

@Composable
private fun AccountRow(account: Account, onOpenAccount: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenAccount(account.id) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Avatar(url = account.avatar, size = 48.dp)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            val accountName = rememberEmojiText(account.nameOrHandle, account.emojis)
            Text(
                text = accountName.text,
                inlineContent = accountName.inlineContent,
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
            val note = HtmlText.plain(
                account.note,
                account.emojis.filter { it.url.isNotBlank() }.map { it.shortcode }.toSet()
            )
            val noteText = rememberEmojiText(note, account.emojis)
            if (note.isNotBlank()) {
                Text(
                    text = noteText.text,
                    inlineContent = noteText.inlineContent,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    HorizontalDivider(
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outlineVariant
    )
}

@Composable
private fun HashtagRow(tag: Tag, onOpen: () -> Unit) {
    val tagColor = LocalOltnSemantics.current.tag
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "#",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = tagColor
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = tag.name,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = tagColor,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    HorizontalDivider(
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outlineVariant
    )
}
