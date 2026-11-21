package app.oltnm.ui.screens.content

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.oltnm.R
import app.oltnm.data.api.MastodonApi
import app.oltnm.data.model.Tag
import app.oltnm.ui.components.EmptyView
import app.oltnm.ui.components.ErrorBanner
import app.oltnm.ui.components.ErrorView
import app.oltnm.ui.components.LoadingView
import app.oltnm.ui.components.OltnGhostButton
import app.oltnm.ui.components.OltnTopBar
import app.oltnm.ui.factoryOf
import app.oltnm.ui.theme.LocalOltnSemantics

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FollowedTagsScreen(
    api: MastodonApi,
    onBack: () -> Unit,
    onOpenTag: (String) -> Unit
) {
    val viewModel: FollowedTagsViewModel = viewModel(
        key = "followed-tags-${api.hashCode()}",
        factory = factoryOf { FollowedTagsViewModel(api) }
    )
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        OltnTopBar(
            title = { Text(stringResource(R.string.followed_tags)) },
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
            if (state.tags.isNotEmpty()) {
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
                state.tags.isEmpty() && state.error != null -> ErrorView(
                    message = state.error.orEmpty(),
                    onRetry = viewModel::refresh
                )
                state.tags.isEmpty() -> EmptyView(stringResource(R.string.followed_tags_empty))
                else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(state.tags, key = { it.name }) { tag ->
                        TagRow(
                            tag = tag,
                            busy = tag.name in state.busy,
                            onOpen = { onOpenTag(tag.name) },
                            onUnfollow = { viewModel.unfollow(tag) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TagRow(
    tag: Tag,
    busy: Boolean,
    onOpen: () -> Unit,
    onUnfollow: () -> Unit
) {
    val tagColor = LocalOltnSemantics.current.tag
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpen)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "#",
                style = MaterialTheme.typography.titleMedium,
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
            OltnGhostButton(
                text = stringResource(R.string.unfollow),
                onClick = onUnfollow,
                enabled = !busy
            )
        }
        HorizontalDivider(
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outlineVariant
        )
    }
}
