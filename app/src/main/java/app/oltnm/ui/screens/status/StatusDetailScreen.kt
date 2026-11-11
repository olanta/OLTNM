package app.oltnm.ui.screens.status

import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.oltnm.R
import app.oltnm.data.api.MastodonApi
import app.oltnm.ui.components.ErrorBanner
import app.oltnm.ui.components.ErrorView
import app.oltnm.ui.components.LoadingView
import app.oltnm.ui.components.OltnSectionTitle
import app.oltnm.ui.components.OltnTopBar
import app.oltnm.ui.components.StatusCard
import app.oltnm.ui.components.rememberOpenUrl
import app.oltnm.ui.factoryOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatusDetailScreen(
    api: MastodonApi,
    statusId: String,
    interactive: Boolean,
    onBack: () -> Unit,
    onOpenAccount: (String) -> Unit,
    onOpenStatus: (String) -> Unit
) {
    val viewModel: StatusDetailViewModel = viewModel(
        key = "status-detail-${api.hashCode()}-$statusId",
        factory = factoryOf { StatusDetailViewModel(api, statusId, interactive) }
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val openUrl = rememberOpenUrl()

    val accountCallback: (String) -> Unit = if (interactive) {
        onOpenAccount
    } else {
        { accountId ->
            val statuses = listOfNotNull(state.status) + state.ancestors + state.descendants
            statuses.asSequence()
                .map { it.original.account }
                .firstOrNull { it.id == accountId }
                ?.url
                ?.takeIf { it.isNotBlank() }
                ?.let(openUrl)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        OltnTopBar(
            title = { Text(stringResource(R.string.status_title)) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = stringResource(R.string.back)
                    )
                }
            }
        )

        PullToRefreshBox(
            isRefreshing = state.refreshing,
            onRefresh = viewModel::refresh,
            modifier = Modifier.weight(1f).fillMaxWidth()
        ) {
            when {
                state.loading && state.status == null -> LoadingView()
                state.status == null && state.error != null -> ErrorView(
                    message = state.error.orEmpty(),
                    onRetry = viewModel::refresh
                )
                state.status == null -> LoadingView()
                else -> {
                    val status = state.status
                    if (status == null) {
                        LoadingView()
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            state.error?.let { message ->
                                item(key = "error") {
                                    ErrorBanner(
                                        message = message,
                                        onDismiss = viewModel::clearError
                                    )
                                }
                            }
                            item(key = "status") {
                                StatusCard(
                                    status = status,
                                    onOpenAccount = accountCallback,
                                    onOpenStatus = {},
                                    onOpenUrl = openUrl,
                                    onFavourite = viewModel::toggleFavourite,
                                    onReblog = viewModel::toggleReblog,
                                    interactive = interactive
                                )
                            }
                            if (state.ancestors.isNotEmpty()) {
                                item(key = "ancestors-title") {
                                    OltnSectionTitle(
                                        text = stringResource(R.string.ancestors_title),
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                                    )
                                }
                                items(state.ancestors, key = { "ancestor-${it.id}" }) { ancestor ->
                                    StatusCard(
                                        status = ancestor,
                                        onOpenAccount = accountCallback,
                                        onOpenStatus = { onOpenStatus(it.original.id) },
                                        onOpenUrl = openUrl,
                                        onFavourite = viewModel::toggleFavourite,
                                        onReblog = viewModel::toggleReblog,
                                        interactive = interactive
                                    )
                                }
                            }
                            if (state.descendants.isNotEmpty()) {
                                item(key = "descendants-title") {
                                    OltnSectionTitle(
                                        text = stringResource(R.string.descendants_title),
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                                    )
                                }
                                items(state.descendants, key = { "descendant-${it.id}" }) { descendant ->
                                    StatusCard(
                                        status = descendant,
                                        onOpenAccount = accountCallback,
                                        onOpenStatus = { onOpenStatus(it.original.id) },
                                        onOpenUrl = openUrl,
                                        onFavourite = viewModel::toggleFavourite,
                                        onReblog = viewModel::toggleReblog,
                                        interactive = interactive
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
