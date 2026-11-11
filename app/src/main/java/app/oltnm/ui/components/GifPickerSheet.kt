package app.oltnm.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.oltnm.R
import app.oltnm.data.api.GifPage
import app.oltnm.data.api.GifResult
import app.oltnm.data.api.GiphyApi
import app.oltnm.ui.theme.LocalOltnSemantics
import coil3.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GifPickerSheet(
    giphyApi: GiphyApi,
    onDismiss: () -> Unit,
    onPick: (GifResult) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val openUrl = rememberOpenUrl()
    val mediaBg = LocalOltnSemantics.current.mediaBg
    val language = LocalConfiguration.current.locales[0].language

    var query by remember { mutableStateOf("") }
    var page by remember { mutableStateOf<GifPage?>(null) }
    var loading by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }

    suspend fun load(value: String, offset: Int) {
        loading = true
        failed = false
        runCatching {
            if (value.isBlank()) {
                giphyApi.trending(offset = offset, lang = language)
            } else {
                giphyApi.search(query = value, offset = offset, lang = language)
            }
        }.onSuccess { page = it }
            .onFailure { failed = true }
        loading = false
    }

    LaunchedEffect(query) {
        delay(450)
        load(query, 0)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = stringResource(R.string.gif),
                style = MaterialTheme.typography.titleLarge
            )
            OltnTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = stringResource(R.string.gif_search),
                leadingIcon = {
                    Icon(Icons.Rounded.Search, contentDescription = null)
                },
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
            )

            val current = page
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 200.dp, max = 440.dp)
                    .padding(top = 10.dp)
            ) {
                when {
                    failed -> GifStateText(stringResource(R.string.gif_error))
                    current == null -> Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                    current.items.isEmpty() -> GifStateText(stringResource(R.string.gif_none))
                    else -> LazyVerticalGrid(
                        columns = GridCells.Adaptive(120.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(current.items, key = { it.id }) { gif ->
                            Box(
                                modifier = Modifier
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(mediaBg)
                                    .clickable { onPick(gif) }
                            ) {
                                AsyncImage(
                                    model = gif.preview,
                                    contentDescription = gif.alt,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val currentPage = page
                TextButton(
                    onClick = {
                        scope.launch {
                            load(query, maxOf(0, (currentPage?.offset ?: 0) - GiphyApi.PER_PAGE))
                        }
                    },
                    enabled = currentPage != null && currentPage.offset > 0 && !loading
                ) {
                    Text(stringResource(R.string.prev))
                }
                if (loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                }
                TextButton(
                    onClick = {
                        scope.launch {
                            load(query, (currentPage?.offset ?: 0) + GiphyApi.PER_PAGE)
                        }
                    },
                    enabled = currentPage != null &&
                        !loading &&
                        currentPage.offset + currentPage.count < currentPage.total
                ) {
                    Text(stringResource(R.string.next))
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { openUrl("https://giphy.com") }) {
                    Text(
                        text = stringResource(R.string.gif_powered_by),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun GifStateText(message: String) {
    Box(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
