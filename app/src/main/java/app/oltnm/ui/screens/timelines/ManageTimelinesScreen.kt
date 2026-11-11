package app.oltnm.ui.screens.timelines

import android.widget.Toast
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.oltnm.R
import app.oltnm.data.api.MastodonApi
import app.oltnm.data.model.MastodonList
import app.oltnm.data.model.TimelineSpec
import app.oltnm.data.model.TimelineTypes
import app.oltnm.di.AppContainer
import app.oltnm.ui.components.OltnFilterChip
import app.oltnm.ui.components.OltnGlassCard
import app.oltnm.ui.components.OltnPrimaryButton
import app.oltnm.ui.components.OltnSectionTitle
import app.oltnm.ui.components.OltnTextField
import app.oltnm.ui.components.OltnTopBar
import app.oltnm.ui.screens.home.timelineLabel
import java.util.UUID

private data class AddType(
    val type: String,
    val label: Int
)

private val ADD_TYPES = listOf(
    AddType(TimelineTypes.HOME, R.string.timeline_home_type),
    AddType(TimelineTypes.LOCAL, R.string.timeline_local),
    AddType(TimelineTypes.FEDERATED, R.string.timeline_federated),
    AddType(TimelineTypes.REMOTE_LOCAL, R.string.timeline_remote_local_type),
    AddType(TimelineTypes.REMOTE_FEDERATED, R.string.timeline_remote_federated_type),
    AddType(TimelineTypes.HASHTAG, R.string.timeline_hashtag_type),
    AddType(TimelineTypes.LIST, R.string.timeline_list_type),
    AddType(TimelineTypes.BOOKMARKS, R.string.bookmarks),
    AddType(TimelineTypes.FAVOURITES, R.string.favourites),
    AddType(TimelineTypes.MENTIONS, R.string.mentions)
)

@Composable
fun ManageTimelinesScreen(
    container: AppContainer,
    api: MastodonApi,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val timelines by container.timelineStore.timelines.collectAsStateWithLifecycle()
    var selectedType by remember { mutableStateOf(TimelineTypes.LOCAL) }
    var host by remember { mutableStateOf("") }
    var tag by remember { mutableStateOf("") }
    var selectedList by remember { mutableStateOf<MastodonList?>(null) }
    var lists by remember { mutableStateOf<List<MastodonList>>(emptyList()) }
    var renaming by remember { mutableStateOf<TimelineSpec?>(null) }
    var renameValue by remember { mutableStateOf("") }
    val localLabel = stringResource(R.string.timeline_local)
    val federatedLabel = stringResource(R.string.timeline_federated)

    LaunchedEffect(Unit) {
        runCatching { api.lists() }.onSuccess { lists = it }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        OltnTopBar(
            title = { Text(stringResource(R.string.manage_timelines)) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = stringResource(R.string.back)
                    )
                }
            }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            OltnSectionTitle(stringResource(R.string.your_timelines))
            Spacer(Modifier.height(8.dp))
            OltnGlassCard(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(8.dp)
            ) {
                timelines.forEachIndexed { index, spec ->
                    val label = timelineLabel(spec)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f).padding(start = 8.dp)
                        )
                        IconButton(
                            onClick = { container.timelineStore.move(spec.id, -1) },
                            enabled = index > 0
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.KeyboardArrowUp,
                                contentDescription = stringResource(R.string.move_up)
                            )
                        }
                        IconButton(
                            onClick = { container.timelineStore.move(spec.id, 1) },
                            enabled = index < timelines.lastIndex
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.KeyboardArrowDown,
                                contentDescription = stringResource(R.string.move_down)
                            )
                        }
                        IconButton(
                            onClick = {
                                renaming = spec
                                renameValue = spec.label.ifBlank { label }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Edit,
                                contentDescription = stringResource(R.string.rename),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(onClick = { container.timelineStore.remove(spec.id) }) {
                            Icon(
                                imageVector = Icons.Rounded.Delete,
                                contentDescription = stringResource(R.string.remove),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
                if (timelines.isEmpty()) {
                    Text(
                        text = stringResource(R.string.empty_timelines),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
            OltnSectionTitle(stringResource(R.string.add_timeline))
            Spacer(Modifier.height(8.dp))
            OltnGlassCard(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ADD_TYPES.forEach { item ->
                        OltnFilterChip(
                            selected = selectedType == item.type,
                            onClick = {
                                selectedType = item.type
                                selectedList = null
                            },
                            label = stringResource(item.label)
                        )
                    }
                }

                when (selectedType) {
                    TimelineTypes.REMOTE_LOCAL,
                    TimelineTypes.REMOTE_FEDERATED
                    -> {
                        OltnTextField(
                            value = host,
                            onValueChange = { host = it },
                            placeholder = stringResource(R.string.timeline_host_hint),
                            modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                        )
                    }
                    TimelineTypes.HASHTAG -> {
                        OltnTextField(
                            value = tag,
                            onValueChange = { tag = it.trimStart('#') },
                            placeholder = stringResource(R.string.timeline_tag_hint),
                            modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                        )
                    }
                    TimelineTypes.LIST -> {
                        if (lists.isEmpty()) {
                            Text(
                                text = stringResource(R.string.no_lists),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 12.dp)
                            )
                        } else {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(top = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                lists.forEach { list ->
                                    OltnFilterChip(
                                        selected = selectedList?.id == list.id,
                                        onClick = { selectedList = list },
                                        label = list.title
                                    )
                                }
                            }
                        }
                    }
                }

                val canAdd = when (selectedType) {
                    TimelineTypes.REMOTE_LOCAL,
                    TimelineTypes.REMOTE_FEDERATED
                    -> host.isNotBlank()
                    TimelineTypes.HASHTAG -> tag.isNotBlank()
                    TimelineTypes.LIST -> selectedList != null
                    else -> true
                }

                Spacer(Modifier.height(14.dp))
                OltnPrimaryButton(
                    text = stringResource(R.string.add_timeline),
                    onClick = {
                        val normalizedHost = host.trim()
                            .removePrefix("https://")
                            .removePrefix("http://")
                            .trimEnd('/')
                            .lowercase()
                        val spec = when (selectedType) {
                            TimelineTypes.HOME,
                            TimelineTypes.LOCAL,
                            TimelineTypes.FEDERATED,
                            TimelineTypes.BOOKMARKS,
                            TimelineTypes.FAVOURITES,
                            TimelineTypes.MENTIONS
                            -> TimelineSpec(
                                id = selectedType,
                                type = selectedType
                            )
                            TimelineTypes.REMOTE_LOCAL -> TimelineSpec(
                                id = UUID.randomUUID().toString(),
                                type = selectedType,
                                host = normalizedHost,
                                label = "$normalizedHost · $localLabel"
                            )
                            TimelineTypes.REMOTE_FEDERATED -> TimelineSpec(
                                id = UUID.randomUUID().toString(),
                                type = selectedType,
                                host = normalizedHost,
                                label = "$normalizedHost · $federatedLabel"
                            )
                            TimelineTypes.HASHTAG -> TimelineSpec(
                                id = UUID.randomUUID().toString(),
                                type = selectedType,
                                value = tag.trim(),
                                label = "#${tag.trim()}"
                            )
                            TimelineTypes.LIST -> TimelineSpec(
                                id = UUID.randomUUID().toString(),
                                type = selectedType,
                                value = selectedList?.id.orEmpty(),
                                label = selectedList?.title.orEmpty()
                            )
                            else -> null
                        }
                        if (spec != null && container.timelineStore.add(spec)) {
                            host = ""
                            tag = ""
                            selectedList = null
                        } else {
                            Toast.makeText(
                                context,
                                R.string.timeline_exists,
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    enabled = canAdd,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    renaming?.let { spec ->
        AlertDialog(
            onDismissRequest = { renaming = null },
            title = { Text(stringResource(R.string.rename_timeline)) },
            text = {
                OltnTextField(
                    value = renameValue,
                    onValueChange = { renameValue = it },
                    placeholder = stringResource(R.string.timeline_label),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        container.timelineStore.rename(spec.id, renameValue)
                        renaming = null
                    }
                ) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = { renaming = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}
