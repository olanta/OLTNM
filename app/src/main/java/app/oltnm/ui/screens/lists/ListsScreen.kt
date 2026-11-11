package app.oltnm.ui.screens.lists

import android.widget.Toast
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.List
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.oltnm.R
import app.oltnm.data.api.MastodonApi
import app.oltnm.data.model.MastodonList
import app.oltnm.data.model.TimelineSpec
import app.oltnm.data.model.TimelineTypes
import app.oltnm.di.AppContainer
import app.oltnm.ui.components.EmptyView
import app.oltnm.ui.components.ErrorView
import app.oltnm.ui.components.LoadingView
import app.oltnm.ui.components.OltnTopBar
import java.util.UUID

@Composable
fun ListsScreen(
    container: AppContainer,
    api: MastodonApi,
    onBack: () -> Unit,
    onOpenList: (MastodonList) -> Unit
) {
    val context = LocalContext.current
    var lists by remember { mutableStateOf<List<MastodonList>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var reloadKey by remember { mutableIntStateOf(0) }

    LaunchedEffect(reloadKey) {
        loading = true
        error = null
        runCatching { api.lists() }
            .onSuccess { lists = it }
            .onFailure { error = it.message }
        loading = false
    }

    Column(modifier = Modifier.fillMaxSize()) {
        OltnTopBar(
            title = { Text(stringResource(R.string.lists)) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = stringResource(R.string.back)
                    )
                }
            },
            actions = {
                IconButton(onClick = { reloadKey++ }) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = stringResource(R.string.refresh)
                    )
                }
            }
        )

        when {
            loading -> LoadingView()
            error != null -> ErrorView(message = error.orEmpty(), onRetry = { reloadKey++ })
            lists.isEmpty() -> EmptyView(stringResource(R.string.empty_lists))
            else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(lists, key = { it.id }) { list ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenList(list) }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.List,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = list.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (list.exclusive) {
                                Text(
                                    text = stringResource(R.string.list_exclusive),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        IconButton(
                            onClick = {
                                val added = container.timelineStore.add(
                                    TimelineSpec(
                                        id = UUID.randomUUID().toString(),
                                        type = TimelineTypes.LIST,
                                        value = list.id,
                                        label = list.title
                                    )
                                )
                                Toast.makeText(
                                    context,
                                    if (added) R.string.timeline_added else R.string.timeline_exists,
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PlaylistAdd,
                                contentDescription = stringResource(R.string.add_timeline)
                            )
                        }
                    }
                }
            }
        }
    }
}
