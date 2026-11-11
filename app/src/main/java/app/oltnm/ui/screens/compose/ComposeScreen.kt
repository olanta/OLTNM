package app.oltnm.ui.screens.compose

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.Audiotrack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.EmojiEmotions
import androidx.compose.material.icons.rounded.GifBox
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Poll
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.oltnm.R
import app.oltnm.data.api.GiphyApi
import app.oltnm.data.api.MastodonApi
import app.oltnm.data.api.TranslateApi
import app.oltnm.data.api.languageDisplayName
import app.oltnm.data.model.QuotePolicy
import app.oltnm.data.model.Visibility
import app.oltnm.data.store.DraftStore
import app.oltnm.data.store.PreferencesStore
import app.oltnm.ui.components.EmojiPickerSheet
import app.oltnm.ui.components.GifPickerSheet
import app.oltnm.ui.components.OltnFilterChip
import app.oltnm.ui.components.OltnGlassCard
import app.oltnm.ui.components.OltnPrimaryButton
import app.oltnm.ui.components.OltnSectionTitle
import app.oltnm.ui.components.OltnTextField
import app.oltnm.ui.components.OltnTextFieldValue
import app.oltnm.ui.components.OltnTopBar
import app.oltnm.ui.components.TranslateTargetSheet
import app.oltnm.ui.factoryOf
import app.oltnm.util.CodeImage
import app.oltnm.util.TextImage
import app.oltnm.util.TextImageStyle
import app.oltnm.util.ThreadText
import coil3.compose.AsyncImage
import java.text.DateFormat
import java.util.Calendar
import java.util.Date
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ComposeScreen(
    api: MastodonApi,
    giphyApi: GiphyApi,
    translateApi: TranslateApi,
    draftStore: DraftStore,
    preferences: PreferencesStore,
    onPosted: () -> Unit
) {
    val viewModel: ComposeViewModel = viewModel(
        key = "compose-${api.hashCode()}",
        factory = factoryOf { ComposeViewModel(api, giphyApi, translateApi, draftStore) }
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val fontFamily by preferences.fontFamily.collectAsStateWithLifecycle()
    val textSize by preferences.textSize.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val imageTextColor = MaterialTheme.colorScheme.onSurface.toArgb()
    val imageBackground = MaterialTheme.colorScheme.surface.toArgb()
    val imageBorder = MaterialTheme.colorScheme.outlineVariant.toArgb()
    val imageAccent = MaterialTheme.colorScheme.primary.toArgb()

    var input by remember { mutableStateOf(TextFieldValue()) }
    var emojiOpen by remember { mutableStateOf(false) }
    var gifOpen by remember { mutableStateOf(false) }
    var translateOpen by remember { mutableStateOf(false) }

    val mediaLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        scope.launch {
            val files = withContext(Dispatchers.IO) {
                uris.mapNotNull { uri ->
                    val bytes = runCatching {
                        context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    }.getOrNull() ?: return@mapNotNull null
                    val mimeType = context.contentResolver.getType(uri)
                        ?: "application/octet-stream"
                    val name = queryDisplayName(context, uri) ?: "upload"
                    MediaFile(name, bytes, mimeType)
                }
            }
            viewModel.attachMedia(files)
        }
    }

    val openSchedulePicker: () -> Unit = {
        val now = Calendar.getInstance()
        val dateDialog = DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val timeDialog = TimePickerDialog(
                    context,
                    { _, hour, minute ->
                        val chosen = Calendar.getInstance().apply {
                            set(year, month, dayOfMonth, hour, minute, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        viewModel.setScheduledAt(chosen.timeInMillis)
                    },
                    now.get(Calendar.HOUR_OF_DAY),
                    now.get(Calendar.MINUTE),
                    true
                )
                timeDialog.show()
            },
            now.get(Calendar.YEAR),
            now.get(Calendar.MONTH),
            now.get(Calendar.DAY_OF_MONTH)
        )
        dateDialog.datePicker.minDate = System.currentTimeMillis()
        dateDialog.show()
    }

    LaunchedEffect(state.text) {
        if (state.text != input.text) {
            input = TextFieldValue(state.text, TextRange(state.text.length))
        }
    }

    LaunchedEffect(state.posted) {
        if (state.posted) {
            Toast.makeText(
                context,
                if (state.scheduled) R.string.scheduled else R.string.posted,
                Toast.LENGTH_SHORT
            ).show()
            viewModel.consumePosted()
            onPosted()
        }
    }

    val insert: (String) -> Unit = { snippet ->
        val start = minOf(input.selection.start, input.selection.end).coerceIn(0, input.text.length)
        val end = maxOf(input.selection.start, input.selection.end).coerceIn(0, input.text.length)
        val newText = input.text.replaceRange(start, end, snippet)
        input = TextFieldValue(newText, TextRange(start + snippet.length))
        viewModel.onTextChange(newText)
    }

    val errorText = composeErrorText(state.error, state.maxChars)
    val threadParts = remember(state.text, state.maxChars) {
        val trimmed = state.text.trim()
        if (trimmed.length > state.maxChars) {
            ThreadText.split(trimmed, state.maxChars).size
        } else {
            1
        }
    }
    val overLimit = threadParts > 1
    val codeBlocks = remember(state.text, state.poll) {
        if (state.poll == null) CodeImage.extractCodeBlocks(state.text) else emptyList()
    }
    val canPost = (state.text.isNotBlank() || state.media.isNotEmpty() || state.poll != null) &&
        !state.posting &&
        !state.uploading &&
        (state.scheduledAtMillis == null || threadParts == 1)
    val postLabel = if (state.scheduledAtMillis != null) {
        stringResource(R.string.schedule)
    } else {
        stringResource(R.string.compose_post)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        OltnTopBar(
            title = { Text(stringResource(R.string.compose_title)) },
            actions = {
                OltnPrimaryButton(
                    text = postLabel,
                    onClick = viewModel::post,
                    enabled = canPost,
                    loading = state.posting,
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            OltnGlassCard(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Switch(
                        checked = state.spoilerEnabled,
                        onCheckedChange = viewModel::setSpoilerEnabled
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.compose_content_warning),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                if (state.spoilerEnabled) {
                    OltnTextField(
                        value = state.spoiler,
                        onValueChange = viewModel::onSpoilerChange,
                        placeholder = stringResource(R.string.compose_spoiler_hint),
                        modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
                    )
                }

                OltnTextFieldValue(
                    value = input,
                    onValueChange = {
                        input = it
                        viewModel.onTextChange(it.text)
                    },
                    placeholder = stringResource(R.string.compose_hint),
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = if (overLimit) {
                            "$threadParts ${stringResource(R.string.thread_count)}"
                        } else {
                            "${state.text.length}/${state.maxChars}"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = if (overLimit) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        textAlign = TextAlign.End
                    )
                }
                if (overLimit) {
                    Text(
                        text = stringResource(R.string.thread_auto_hint),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                        textAlign = TextAlign.End
                    )
                }

                if (state.media.isNotEmpty() || state.uploading) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        state.media.forEach { media ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surfaceContainer)
                                ) {
                                    if (media.type == "audio") {
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Audiotrack,
                                                contentDescription = media.description,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(26.dp)
                                            )
                                        }
                                    } else {
                                        AsyncImage(
                                            model = media.previewUrl.ifBlank { media.url },
                                            contentDescription = media.description,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                        if (media.type == "video" || media.type == "gifv") {
                                            Icon(
                                                imageVector = Icons.Rounded.PlayCircle,
                                                contentDescription = null,
                                                tint = Color.White.copy(alpha = 0.9f),
                                                modifier = Modifier.align(Alignment.Center).size(28.dp)
                                            )
                                        }
                                    }
                                }
                                Spacer(Modifier.width(8.dp))
                                OltnTextField(
                                    value = media.description.orEmpty(),
                                    onValueChange = {
                                        viewModel.updateMediaDescription(media.id, it)
                                    },
                                    placeholder = stringResource(R.string.attach_alt),
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = { viewModel.removeMedia(media.id) }) {
                                    Icon(
                                        imageVector = Icons.Rounded.Close,
                                        contentDescription = stringResource(R.string.remove),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                        if (state.uploading) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.uploading_media),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Switch(
                            checked = state.sensitive,
                            onCheckedChange = viewModel::setSensitive
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.sensitive_media),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                if (codeBlocks.isNotEmpty() && state.poll == null) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Code,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.code_as_image),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                state.translation?.let { translation ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Translate,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "${stringResource(R.string.translated)} — " +
                                languageDisplayName(translation.target),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = viewModel::undoTranslation) {
                            Text(stringResource(R.string.undo))
                        }
                    }
                }

                state.poll?.let { poll ->
                    PollEditor(
                        poll = poll,
                        onOptionChange = viewModel::updatePollOption,
                        onAddOption = viewModel::addPollOption,
                        onRemoveOption = viewModel::removePollOption,
                        onMultipleChange = viewModel::setPollMultiple,
                        onExpiryChange = viewModel::setPollExpiry,
                        onRemovePoll = viewModel::togglePoll
                    )
                }

                state.scheduledAtMillis?.let { millis ->
                    ScheduleNotice(
                        millis = millis,
                        onRemove = { viewModel.setScheduledAt(null) }
                    )
                    if (threadParts > 1) {
                        Text(
                            text = stringResource(R.string.schedule_no_thread),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ComposeToolButton(
                        icon = Icons.Rounded.AttachFile,
                        label = stringResource(R.string.attach_media),
                        enabled = state.poll == null &&
                            state.media.size < ComposeViewModel.MAX_MEDIA &&
                            !state.uploading,
                        onClick = {
                            mediaLauncher.launch(arrayOf("image/*", "video/*", "audio/*"))
                        }
                    )
                    ComposeToolButton(
                        icon = Icons.Rounded.Image,
                        label = stringResource(R.string.text_as_image),
                        enabled = state.text.isNotBlank() &&
                            state.poll == null &&
                            state.media.size < ComposeViewModel.MAX_MEDIA &&
                            !state.uploading,
                        onClick = {
                            val text = state.text.trim()
                            if (text.isNotBlank()) {
                                scope.launch {
                                    val bytes = withContext(Dispatchers.Default) {
                                        TextImage.render(
                                            context = context,
                                            text = text,
                                            style = TextImageStyle(
                                                font = fontFamily,
                                                textScale = textSize.scale,
                                                textColor = imageTextColor,
                                                backgroundColor = imageBackground,
                                                borderColor = imageBorder,
                                                accentColor = imageAccent
                                            )
                                        )
                                    }
                                    if (bytes != null) {
                                        viewModel.attachTextImage(
                                            MediaFile(
                                                fileName = "text-image.png",
                                                bytes = bytes,
                                                mimeType = "image/png",
                                                description = text.take(1500)
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    )
                    ComposeToolButton(
                        icon = Icons.Rounded.Poll,
                        label = stringResource(R.string.add_poll),
                        active = state.poll != null,
                        enabled = state.media.isEmpty() && !state.uploading,
                        onClick = viewModel::togglePoll
                    )
                    ComposeToolButton(
                        icon = Icons.Rounded.EmojiEmotions,
                        label = stringResource(R.string.emoji_picker),
                        onClick = {
                            viewModel.loadCustomEmojis()
                            emojiOpen = true
                        }
                    )
                    ComposeToolButton(
                        icon = Icons.Rounded.GifBox,
                        label = stringResource(R.string.gif),
                        enabled = state.poll == null &&
                            state.media.size < ComposeViewModel.MAX_MEDIA &&
                            !state.uploading,
                        onClick = { gifOpen = true }
                    )
                    ComposeToolButton(
                        icon = Icons.Rounded.Translate,
                        label = stringResource(R.string.translate_to),
                        active = state.translation != null,
                        loading = state.translating,
                        enabled = state.text.isNotBlank(),
                        onClick = { translateOpen = true }
                    )
                    ComposeToolButton(
                        icon = Icons.Rounded.Schedule,
                        label = stringResource(R.string.schedule),
                        active = state.scheduledAtMillis != null,
                        enabled = threadParts == 1,
                        onClick = openSchedulePicker
                    )
                    Spacer(Modifier.weight(1f))
                    if (state.uploading) {
                        Text(
                            text = stringResource(R.string.uploading_media),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                errorText?.let { message ->
                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
            OltnSectionTitle(stringResource(R.string.visibility_label))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Visibility.entries
                    .filter { it != Visibility.Local || state.localVisibilitySupported }
                    .forEach { visibility ->
                        OltnFilterChip(
                            selected = state.visibility == visibility,
                            onClick = { viewModel.setVisibility(visibility) },
                            label = visibilityLabel(visibility)
                        )
                    }
            }

            val quoteRestricted = state.visibility == Visibility.Private ||
                state.visibility == Visibility.Direct
            Spacer(Modifier.height(16.dp))
            OltnSectionTitle(stringResource(R.string.quote_policy))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val effectivePolicy = if (quoteRestricted) {
                    QuotePolicy.Nobody
                } else {
                    state.quotePolicy
                }
                QuotePolicy.entries.forEach { policy ->
                    OltnFilterChip(
                        selected = effectivePolicy == policy,
                        onClick = {
                            if (!quoteRestricted) viewModel.setQuotePolicy(policy)
                        },
                        label = quotePolicyLabel(policy),
                        enabled = !quoteRestricted
                    )
                }
            }
            if (quoteRestricted) {
                Text(
                    text = stringResource(R.string.quote_policy_private),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (emojiOpen) {
        EmojiPickerSheet(
            customEmojis = state.customEmojis,
            loadingCustom = state.loadingCustomEmojis,
            onDismiss = { emojiOpen = false },
            onPick = insert
        )
    }
    if (gifOpen) {
        GifPickerSheet(
            giphyApi = giphyApi,
            onDismiss = { gifOpen = false },
            onPick = { gif ->
                gifOpen = false
                viewModel.pickGif(gif)
            }
        )
    }
    if (translateOpen) {
        TranslateTargetSheet(
            onDismiss = { translateOpen = false },
            onPick = { target ->
                translateOpen = false
                viewModel.translate(target)
            }
        )
    }
}

@Composable
private fun ComposeToolButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    active: Boolean = false,
    loading: Boolean = false,
    enabled: Boolean = true
) {
    IconButton(
        onClick = onClick,
        enabled = enabled && !loading,
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(
                if (active) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                } else {
                    Color.Transparent
                }
            )
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.primary
            )
        } else {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (active) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

private val PollExpiryOptions = listOf(
    300 to R.string.poll_5m,
    1_800 to R.string.poll_30m,
    3_600 to R.string.poll_1h,
    21_600 to R.string.poll_6h,
    86_400 to R.string.poll_1d,
    259_200 to R.string.poll_3d,
    604_800 to R.string.poll_7d
)

@Composable
private fun PollEditor(
    poll: PollDraft,
    onOptionChange: (Int, String) -> Unit,
    onAddOption: () -> Unit,
    onRemoveOption: (Int) -> Unit,
    onMultipleChange: (Boolean) -> Unit,
    onExpiryChange: (Int) -> Unit,
    onRemovePoll: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Rounded.Poll,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.poll),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onRemovePoll) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = stringResource(R.string.remove),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        poll.options.forEachIndexed { index, option ->
            OltnTextField(
                value = option,
                onValueChange = { onOptionChange(index, it) },
                placeholder = stringResource(R.string.poll_option, index + 1),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                trailingIcon = if (poll.options.size > 2) {
                    {
                        IconButton(onClick = { onRemoveOption(index) }) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = stringResource(R.string.remove),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                } else {
                    null
                }
            )
        }
        if (poll.options.size < ComposeViewModel.MAX_POLL_OPTIONS) {
            TextButton(onClick = onAddOption) {
                Text(stringResource(R.string.poll_add_option))
            }
        }
        Row(
            modifier = Modifier.padding(top = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Switch(checked = poll.multiple, onCheckedChange = onMultipleChange)
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.poll_multiple),
                style = MaterialTheme.typography.bodySmall
            )
        }
        Text(
            text = stringResource(R.string.poll_expiry),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(top = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            PollExpiryOptions.forEach { (seconds, labelRes) ->
                OltnFilterChip(
                    selected = poll.expiresInSeconds == seconds,
                    onClick = { onExpiryChange(seconds) },
                    label = stringResource(labelRes)
                )
            }
        }
    }
}

@Composable
private fun ScheduleNotice(millis: Long, onRemove: () -> Unit) {
    val locale = LocalConfiguration.current.locales[0]
    val formatted = remember(millis, locale) {
        DateFormat.getDateTimeInstance(
            DateFormat.SHORT,
            DateFormat.SHORT,
            locale
        ).format(Date(millis))
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Rounded.Schedule,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = stringResource(R.string.schedule_for, formatted),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        TextButton(onClick = onRemove) {
            Text(stringResource(R.string.remove))
        }
    }
}

private fun queryDisplayName(context: Context, uri: Uri): String? {
    context.contentResolver.query(
        uri,
        arrayOf(OpenableColumns.DISPLAY_NAME),
        null,
        null,
        null
    )?.use { cursor ->
        if (cursor.moveToFirst()) {
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0) return cursor.getString(index)
        }
    }
    return uri.lastPathSegment
}

@Composable
private fun composeErrorText(error: ComposeError?, maxChars: Int): String? = when (error) {
    ComposeError.Empty -> stringResource(R.string.compose_empty_error)
    ComposeError.TooLong -> stringResource(R.string.compose_too_long, maxChars)
    ComposeError.Translate -> stringResource(R.string.error_translate)
    ComposeError.Upload -> stringResource(R.string.error_upload)
    ComposeError.Poll -> stringResource(R.string.poll_invalid)
    ComposeError.Schedule -> stringResource(R.string.schedule_invalid)
    ComposeError.ThreadSchedule -> stringResource(R.string.schedule_no_thread)
    ComposeError.Generic -> stringResource(R.string.error_generic)
    null -> null
}

@Composable
private fun visibilityLabel(visibility: Visibility): String = when (visibility) {
    Visibility.Public -> stringResource(R.string.visibility_public)
    Visibility.Local -> stringResource(R.string.visibility_local)
    Visibility.Unlisted -> stringResource(R.string.visibility_unlisted)
    Visibility.Private -> stringResource(R.string.visibility_private)
    Visibility.Direct -> stringResource(R.string.visibility_direct)
}

@Composable
private fun quotePolicyLabel(policy: QuotePolicy): String = when (policy) {
    QuotePolicy.Public -> stringResource(R.string.quote_policy_public)
    QuotePolicy.Followers -> stringResource(R.string.quote_policy_followers)
    QuotePolicy.Nobody -> stringResource(R.string.quote_policy_nobody)
}
