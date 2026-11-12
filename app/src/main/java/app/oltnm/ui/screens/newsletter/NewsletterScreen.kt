package app.oltnm.ui.screens.newsletter

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.oltnm.R
import app.oltnm.data.api.MastodonApi
import app.oltnm.data.model.Account
import app.oltnm.data.model.NewsletterSource
import app.oltnm.data.model.Status
import app.oltnm.di.AppContainer
import app.oltnm.ui.components.Avatar
import app.oltnm.ui.components.EmptyView
import app.oltnm.ui.components.LoadingView
import app.oltnm.ui.components.OltnSectionTitle
import app.oltnm.ui.components.OltnTextField
import app.oltnm.ui.components.OltnTopBar
import app.oltnm.ui.components.StatusCard
import app.oltnm.ui.components.TrendLinkCard
import app.oltnm.ui.components.rememberOpenUrl
import app.oltnm.ui.factoryOf

@Composable
fun NewsletterScreen(
    container: AppContainer,
    api: MastodonApi,
    onBack: () -> Unit,
    onOpenStatus: (String) -> Unit,
    onReply: (Status) -> Unit
) {
    val viewModel: NewsletterViewModel = viewModel(
        key = "newsletter-${api.hashCode()}",
        factory = factoryOf { NewsletterViewModel(api, container.newsletterStore) }
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val openUrl = rememberOpenUrl()

    Column(modifier = Modifier.fillMaxSize()) {
        OltnTopBar(
            title = { Text(stringResource(R.string.newsletter)) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = stringResource(R.string.back)
                    )
                }
            },
            actions = {
                IconButton(onClick = viewModel::refresh) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = stringResource(R.string.refresh)
                    )
                }
            }
        )

        if (state.loading && state.links.isEmpty() && state.sources.isEmpty()) {
            LoadingView()
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item(key = "news-title") {
                    OltnSectionTitle(stringResource(R.string.trending_news))
                }
                if (state.links.isEmpty()) {
                    item(key = "news-empty") {
                        EmptyView(
                            message = stringResource(R.string.newsletter_no_news),
                            modifier = Modifier.height(160.dp)
                        )
                    }
                } else {
                    items(state.links, key = { it.id.ifBlank { it.url } }) { link ->
                        TrendLinkCard(link = link, onOpenUrl = openUrl)
                    }
                }

                item(key = "sources-title") {
                    Spacer(Modifier.height(8.dp))
                    OltnSectionTitle(stringResource(R.string.news_sources))
                }
                item(key = "sources") {
                    SourcePicker(state = state, viewModel = viewModel)
                }

                item(key = "posts-title") {
                    Spacer(Modifier.height(8.dp))
                    OltnSectionTitle(stringResource(R.string.newsletter_source_posts))
                }
                if (state.posts.isEmpty()) {
                    item(key = "posts-empty") {
                        Text(
                            text = stringResource(R.string.newsletter_no_posts),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    items(state.posts, key = { it.id }) { status ->
                        StatusCard(
                            status = status,
                            onOpenAccount = {},
                            onOpenStatus = { onOpenStatus(it.original.id) },
                            onReply = onReply,
                            onOpenUrl = openUrl,
                            onFavourite = {},
                            onReblog = {},
                            interactive = false
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SourcePicker(
    state: NewsletterUiState,
    viewModel: NewsletterViewModel
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        if (state.sources.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                state.sources.forEach { source ->
                    SourceChip(
                        source = source,
                        onRemove = { viewModel.removeSource(source.acct) }
                    )
                }
            }
        } else {
            Text(
                text = stringResource(R.string.newsletter_no_sources),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        OltnTextField(
            value = state.query,
            onValueChange = viewModel::onQueryChange,
            placeholder = stringResource(R.string.newsletter_add_source),
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
        )

        state.suggestions.forEach { account ->
            SuggestionRow(account = account, onClick = { viewModel.addSource(account) })
        }
    }
}

@Composable
private fun SourceChip(source: NewsletterSource, onRemove: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(start = 10.dp, end = 2.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Rounded.Person,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = source.name.ifBlank { source.acct },
            style = MaterialTheme.typography.labelMedium
        )
        IconButton(
            onClick = onRemove,
            modifier = Modifier.size(28.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = stringResource(R.string.remove),
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
private fun SuggestionRow(account: Account, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Avatar(url = account.avatar, size = 34.dp)
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = account.nameOrHandle,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "@${account.acct}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
