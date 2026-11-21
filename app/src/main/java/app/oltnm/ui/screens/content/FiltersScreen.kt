package app.oltnm.ui.screens.content

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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.oltnm.R
import app.oltnm.data.api.MastodonApi
import app.oltnm.data.filters.FilterStore
import app.oltnm.data.model.MastodonFilter
import app.oltnm.ui.components.ErrorBanner
import app.oltnm.ui.components.LoadingView
import app.oltnm.ui.components.OltnFilterChip
import app.oltnm.ui.components.OltnGhostButton
import app.oltnm.ui.components.OltnGlassCard
import app.oltnm.ui.components.OltnPrimaryButton
import app.oltnm.ui.components.OltnSectionTitle
import app.oltnm.ui.components.OltnTextField
import app.oltnm.ui.components.OltnTopBar
import app.oltnm.ui.factoryOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FiltersScreen(
    api: MastodonApi,
    onBack: () -> Unit,
    filterStore: FilterStore? = null
) {
    val viewModel: FiltersViewModel = viewModel(
        key = "filters-${api.hashCode()}",
        factory = factoryOf { FiltersViewModel(api, filterStore) }
    )
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        OltnTopBar(
            title = { Text(stringResource(R.string.filters_title)) },
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
            if (state.filters.isNotEmpty()) {
                ErrorBanner(message = message, onDismiss = viewModel::clearError)
            }
        }

        PullToRefreshBox(
            isRefreshing = state.refreshing,
            onRefresh = viewModel::refresh,
            modifier = Modifier.weight(1f).fillMaxWidth()
        ) {
            if (state.loading) {
                LoadingView()
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    item(key = "filter-form") {
                        FilterForm(
                            title = state.title,
                            keyword = state.keyword,
                            contexts = state.contexts,
                            action = state.action,
                            creating = state.creating,
                            canCreate = state.canCreate,
                            onTitleChange = viewModel::onTitleChange,
                            onKeywordChange = viewModel::onKeywordChange,
                            onToggleContext = viewModel::toggleContext,
                            onAction = viewModel::setAction,
                            onCreate = viewModel::create
                        )
                    }
                    if (state.filters.isEmpty()) {
                        item(key = "empty") {
                            Text(
                                text = stringResource(R.string.filters_empty),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    } else {
                        items(state.filters, key = { it.id }) { filter ->
                            FilterRow(
                                filter = filter,
                                busy = filter.id in state.busy,
                                onDelete = { viewModel.delete(filter) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterForm(
    title: String,
    keyword: String,
    contexts: Set<String>,
    action: String,
    creating: Boolean,
    canCreate: Boolean,
    onTitleChange: (String) -> Unit,
    onKeywordChange: (String) -> Unit,
    onToggleContext: (String) -> Unit,
    onAction: (String) -> Unit,
    onCreate: () -> Unit
) {
    OltnGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        contentPadding = PaddingValues(16.dp)
    ) {
        Text(
            text = stringResource(R.string.filters_add),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )
        OltnTextField(
            value = title,
            onValueChange = onTitleChange,
            placeholder = stringResource(R.string.filters_title_hint),
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
        )
        OltnTextField(
            value = keyword,
            onValueChange = onKeywordChange,
            placeholder = stringResource(R.string.filters_keyword_hint),
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        )
        Spacer(Modifier.height(4.dp))
        OltnSectionTitle(stringResource(R.string.filters_contexts))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(top = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterContexts.forEach { context ->
                OltnFilterChip(
                    selected = context in contexts,
                    onClick = { onToggleContext(context) },
                    label = filterContextLabel(context)
                )
            }
        }
        OltnSectionTitle(stringResource(R.string.filters_action))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            OltnFilterChip(
                selected = action == "hide",
                onClick = { onAction("hide") },
                label = stringResource(R.string.filters_action_hide)
            )
            OltnFilterChip(
                selected = action == "warn",
                onClick = { onAction("warn") },
                label = stringResource(R.string.filters_action_warn)
            )
        }
        OltnPrimaryButton(
            text = stringResource(R.string.filters_create),
            onClick = onCreate,
            enabled = canCreate,
            loading = creating,
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
        )
    }
}

@Composable
private fun FilterRow(
    filter: MastodonFilter,
    busy: Boolean,
    onDelete: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = filter.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                if (filter.keywords.isNotEmpty()) {
                    Text(
                        text = filter.keywords.joinToString(", ") { it.keyword },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                val contextLabels = filter.context.map { filterContextLabel(it) }
                Text(
                    text = contextLabels.joinToString(", "),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(
                        if (filter.filterAction == "warn") {
                            R.string.filters_action_warn
                        } else {
                            R.string.filters_action_hide
                        }
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(Modifier.width(8.dp))
            OltnGhostButton(
                text = stringResource(R.string.delete),
                onClick = onDelete,
                enabled = !busy
            )
        }
        HorizontalDivider(
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outlineVariant
        )
    }
}

@Composable
private fun filterContextLabel(context: String): String = when (context) {
    "home" -> stringResource(R.string.filters_context_home)
    "public" -> stringResource(R.string.filters_context_public)
    "notifications" -> stringResource(R.string.filters_context_notifications)
    "thread" -> stringResource(R.string.filters_context_thread)
    "account" -> stringResource(R.string.filters_context_account)
    else -> context
}
