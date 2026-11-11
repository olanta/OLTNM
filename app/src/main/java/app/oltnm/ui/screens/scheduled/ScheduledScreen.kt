package app.oltnm.ui.screens.scheduled

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
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.oltnm.R
import app.oltnm.data.api.MastodonApi
import app.oltnm.data.model.ScheduledStatus
import app.oltnm.ui.components.EmptyView
import app.oltnm.ui.components.ErrorView
import app.oltnm.ui.components.LoadingView
import app.oltnm.ui.components.OltnTopBar
import app.oltnm.ui.factoryOf
import java.text.DateFormat
import java.time.Instant
import java.util.Date

@Composable
fun ScheduledScreen(
    api: MastodonApi,
    onBack: () -> Unit
) {
    val viewModel: ScheduledViewModel = viewModel(
        key = "scheduled-${api.hashCode()}",
        factory = factoryOf { ScheduledViewModel(api) }
    )
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        OltnTopBar(
            title = { Text(stringResource(R.string.scheduled_posts)) },
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

        when {
            state.loading -> LoadingView()
            state.items.isEmpty() && state.error != null -> ErrorView(
                message = state.error.orEmpty(),
                onRetry = viewModel::refresh
            )
            state.items.isEmpty() -> EmptyView(stringResource(R.string.no_scheduled))
            else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                state.error?.let { message ->
                    item(key = "error") {
                        Text(
                            text = message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
                items(state.items, key = { it.id }) { status ->
                    ScheduledRow(
                        status = status,
                        onDelete = { viewModel.delete(status.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ScheduledRow(status: ScheduledStatus, onDelete: () -> Unit) {
    val locale = LocalConfiguration.current.locales[0]
    val formatted = runCatching {
        val millis = Instant.parse(status.scheduledAt).toEpochMilli()
        DateFormat.getDateTimeInstance(
            DateFormat.MEDIUM,
            DateFormat.SHORT,
            locale
        ).format(Date(millis))
    }.getOrDefault(status.scheduledAt)

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.schedule_for, formatted),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.padding(top = 4.dp))
                val spoiler = status.params?.spoilerText.orEmpty()
                if (spoiler.isNotBlank()) {
                    Text(
                        text = spoiler,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                Text(
                    text = status.params?.text.orEmpty()
                        .ifBlank { stringResource(R.string.media_only) },
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Rounded.Delete,
                    contentDescription = stringResource(R.string.delete),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
        HorizontalDivider(
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outlineVariant
        )
    }
}
