package app.oltnm.ui.screens.trending

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Refresh
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.oltnm.R
import app.oltnm.data.api.MastodonApi
import app.oltnm.data.model.Tag
import app.oltnm.ui.components.EmptyView
import app.oltnm.ui.components.LoadingView
import app.oltnm.ui.components.OltnFilterChip
import app.oltnm.ui.components.OltnTopBar
import app.oltnm.ui.components.StatusCard
import app.oltnm.ui.components.TrendLinkCard
import app.oltnm.ui.components.rememberOpenUrl
import app.oltnm.ui.factoryOf
import app.oltnm.ui.theme.LocalOltnSemantics

private enum class TrendingTab {
    Posts,
    Hashtags,
    News
}

@Composable
fun TrendingScreen(
    api: MastodonApi,
    onBack: () -> Unit,
    onOpenHashtag: (String) -> Unit,
    onOpenStatus: (String) -> Unit
) {
    val viewModel: TrendingViewModel = viewModel(
        key = "trending-${api.hashCode()}",
        factory = factoryOf { TrendingViewModel(api) }
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val openUrl = rememberOpenUrl()
    var tab by remember { mutableStateOf(TrendingTab.Posts) }

    Column(modifier = Modifier.fillMaxSize()) {
        OltnTopBar(
            title = { Text(stringResource(R.string.trending)) },
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

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OltnFilterChip(
                selected = tab == TrendingTab.Posts,
                onClick = { tab = TrendingTab.Posts },
                label = stringResource(R.string.trending_posts)
            )
            OltnFilterChip(
                selected = tab == TrendingTab.Hashtags,
                onClick = { tab = TrendingTab.Hashtags },
                label = stringResource(R.string.trending_hashtags)
            )
            OltnFilterChip(
                selected = tab == TrendingTab.News,
                onClick = { tab = TrendingTab.News },
                label = stringResource(R.string.trending_news)
            )
        }

        when {
            state.loading -> LoadingView()
            else -> when (tab) {
                TrendingTab.Posts -> if (state.posts.isEmpty()) {
                    EmptyView(stringResource(R.string.empty_trending))
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(state.posts, key = { it.id }) { status ->
                            StatusCard(
                                status = status,
                                onOpenAccount = {},
                                onOpenStatus = { onOpenStatus(it.original.id) },
                                onOpenUrl = openUrl,
                                onFavourite = {},
                                onReblog = {},
                                interactive = false
                            )
                        }
                    }
                }
                TrendingTab.Hashtags -> if (state.tags.isEmpty()) {
                    EmptyView(stringResource(R.string.empty_trending))
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(state.tags, key = { it.name }) { tag ->
                            HashtagRow(tag = tag, onOpen = { onOpenHashtag(tag.name) })
                        }
                    }
                }
                TrendingTab.News -> if (state.links.isEmpty()) {
                    EmptyView(stringResource(R.string.empty_trending))
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(state.links, key = { it.id.ifBlank { it.url } }) { link ->
                            TrendLinkCard(link = link, onOpenUrl = openUrl)
                        }
                    }
                }
            }
        }
    }
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
