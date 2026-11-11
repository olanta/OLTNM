package app.oltnm.ui.screens.compose

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.oltnm.data.api.GifResult
import app.oltnm.data.api.GiphyApi
import app.oltnm.data.api.MastodonApi
import app.oltnm.data.api.TranslateApi
import app.oltnm.data.model.Emoji
import app.oltnm.data.model.PollRequest
import app.oltnm.data.model.QuotePolicy
import app.oltnm.data.model.StatusRequest
import app.oltnm.data.model.UploadedMedia
import app.oltnm.data.model.Visibility
import app.oltnm.data.store.ComposeDraftData
import app.oltnm.data.store.DraftStore
import app.oltnm.util.CodeImage
import app.oltnm.util.ThreadText
import java.time.Instant
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ComposeError {
    Empty,
    TooLong,
    Translate,
    Upload,
    Poll,
    Schedule,
    ThreadSchedule,
    Generic
}

data class TranslationState(
    val target: String,
    val original: String
)

data class PollDraft(
    val options: List<String> = listOf("", ""),
    val multiple: Boolean = false,
    val expiresInSeconds: Int = 86_400
)

data class MediaFile(
    val fileName: String,
    val bytes: ByteArray,
    val mimeType: String,
    val description: String? = null
)

data class ComposeUiState(
    val text: String = "",
    val visibility: Visibility = Visibility.Public,
    val spoilerEnabled: Boolean = false,
    val spoiler: String = "",
    val sensitive: Boolean = false,
    val quotePolicy: QuotePolicy = QuotePolicy.Public,
    val posting: Boolean = false,
    val posted: Boolean = false,
    val scheduled: Boolean = false,
    val error: ComposeError? = null,
    val media: List<UploadedMedia> = emptyList(),
    val uploading: Boolean = false,
    val translating: Boolean = false,
    val translation: TranslationState? = null,
    val poll: PollDraft? = null,
    val scheduledAtMillis: Long? = null,
    val customEmojis: List<Emoji> = emptyList(),
    val loadingCustomEmojis: Boolean = false,
    val maxChars: Int = MastodonApi.DEFAULT_MAX_CHARS,
    val localVisibilitySupported: Boolean = false
)

class ComposeViewModel(
    private val api: MastodonApi,
    private val giphyApi: GiphyApi,
    private val translateApi: TranslateApi,
    private val draftStore: DraftStore
) : ViewModel() {
    private val _state = MutableStateFlow(ComposeUiState())
    val state: StateFlow<ComposeUiState> = _state.asStateFlow()

    private var draftJob: Job? = null

    init {
        draftStore.load()?.let { draft ->
            _state.update {
                it.copy(
                    text = draft.text,
                    spoiler = draft.spoiler,
                    spoilerEnabled = draft.spoilerEnabled,
                    visibility = runCatching { Visibility.valueOf(draft.visibility) }
                        .getOrDefault(Visibility.Public)
                )
            }
        }
        loadInstanceDetails()
    }

    private fun loadInstanceDetails() {
        viewModelScope.launch {
            runCatching { api.instanceDetails() }
                .onSuccess { details ->
                    val local = details.version?.contains("pleroma", ignoreCase = true) == true ||
                        details.version?.contains("akkoma", ignoreCase = true) == true
                    _state.update {
                        it.copy(
                            maxChars = details.maxCharacters,
                            localVisibilitySupported = local,
                            visibility = if (!local && it.visibility == Visibility.Local) {
                                Visibility.Public
                            } else {
                                it.visibility
                            }
                        )
                    }
                }
        }
    }

    fun onTextChange(value: String) {
        _state.update {
            it.copy(text = value, error = null, translation = null)
        }
        scheduleDraftSave()
    }

    fun onSpoilerChange(value: String) {
        _state.update { it.copy(spoiler = value) }
        scheduleDraftSave()
    }

    fun setSpoilerEnabled(enabled: Boolean) {
        _state.update { it.copy(spoilerEnabled = enabled) }
        scheduleDraftSave()
    }

    fun setVisibility(visibility: Visibility) {
        _state.update { it.copy(visibility = visibility) }
        scheduleDraftSave()
    }

    fun setSensitive(value: Boolean) {
        _state.update { it.copy(sensitive = value) }
    }

    fun setQuotePolicy(value: QuotePolicy) {
        _state.update { it.copy(quotePolicy = value) }
    }

    fun togglePoll() {
        val snapshot = _state.value
        if (snapshot.uploading) return
        if (snapshot.poll != null) {
            _state.update { it.copy(poll = null) }
            return
        }
        if (snapshot.media.isNotEmpty()) return
        _state.update { it.copy(poll = PollDraft()) }
    }

    fun updatePollOption(index: Int, value: String) {
        _state.update { current ->
            val poll = current.poll ?: return@update current
            current.copy(
                poll = poll.copy(
                    options = poll.options.mapIndexed { i, option ->
                        if (i == index) value else option
                    }
                )
            )
        }
    }

    fun addPollOption() {
        _state.update { current ->
            val poll = current.poll ?: return@update current
            if (poll.options.size >= MAX_POLL_OPTIONS) {
                current
            } else {
                current.copy(poll = poll.copy(options = poll.options + ""))
            }
        }
    }

    fun removePollOption(index: Int) {
        _state.update { current ->
            val poll = current.poll ?: return@update current
            if (poll.options.size <= 2) {
                current
            } else {
                current.copy(
                    poll = poll.copy(
                        options = poll.options.filterIndexed { i, _ -> i != index }
                    )
                )
            }
        }
    }

    fun setPollMultiple(value: Boolean) {
        _state.update { current ->
            val poll = current.poll ?: return@update current
            current.copy(poll = poll.copy(multiple = value))
        }
    }

    fun setPollExpiry(seconds: Int) {
        _state.update { current ->
            val poll = current.poll ?: return@update current
            current.copy(poll = poll.copy(expiresInSeconds = seconds))
        }
    }

    fun setScheduledAt(millis: Long?) {
        if (millis == null) {
            _state.update { it.copy(scheduledAtMillis = null, error = null) }
            return
        }
        val minimum = System.currentTimeMillis() + SCHEDULE_MIN_MILLIS
        if (millis < minimum) {
            _state.update { it.copy(error = ComposeError.Schedule) }
            return
        }
        _state.update { it.copy(scheduledAtMillis = millis, error = null) }
    }

    fun updateMediaDescription(id: String, description: String) {
        _state.update { current ->
            current.copy(
                media = current.media.map { item ->
                    if (item.id == id) item.copy(description = description) else item
                }
            )
        }
    }

    fun post() {
        val snapshot = _state.value
        if (snapshot.posting || snapshot.uploading) return

        val poll = snapshot.poll?.let { draft ->
            val options = draft.options.map { it.trim() }.filter { it.isNotEmpty() }
            if (options.size < 2) {
                _state.update { it.copy(error = ComposeError.Poll) }
                return
            }
            PollRequest(
                options = options,
                expiresIn = draft.expiresInSeconds,
                multiple = draft.multiple
            )
        }

        when {
            snapshot.text.isBlank() && snapshot.media.isEmpty() && poll == null -> {
                _state.update { it.copy(error = ComposeError.Empty) }
                return
            }
        }

        val scheduledAt = snapshot.scheduledAtMillis?.let { Instant.ofEpochMilli(it).toString() }
        val visibility = if (
            snapshot.visibility == Visibility.Local && !snapshot.localVisibilitySupported
        ) {
            Visibility.Public
        } else {
            snapshot.visibility
        }
        val quotePolicy = if (
            visibility == Visibility.Private || visibility == Visibility.Direct
        ) {
            QuotePolicy.Nobody.apiValue
        } else {
            snapshot.quotePolicy.apiValue
        }

        viewModelScope.launch {
            _state.update { it.copy(posting = true, error = null) }

            val syncedMedia = snapshot.media.map { item ->
                runCatching {
                    api.updateMediaDescription(item.id, item.description.orEmpty())
                }.getOrDefault(item)
            }

            var text = snapshot.text.trim()
            val generated = mutableListOf<UploadedMedia>()
            if (poll == null) {
                val freeSlots = (MAX_MEDIA - syncedMedia.size).coerceAtLeast(0)
                if (freeSlots > 0) {
                    val blocks = CodeImage.extractCodeBlocks(text).take(freeSlots)
                    for (block in blocks) {
                        val png = CodeImage.renderPng(block) ?: continue
                        runCatching {
                            api.uploadMedia(
                                fileName = "code.png",
                                bytes = png,
                                mimeType = "image/png",
                                description = block.code
                            )
                        }.onSuccess { uploaded ->
                            generated += uploaded
                            text = text.replace(block.raw, "")
                        }
                    }
                    if (generated.isNotEmpty()) {
                        text = text.replace(Regex("\\n{3,}"), "\n\n").trim()
                    }
                }
            }

            val mediaIds = (syncedMedia + generated)
                .take(MAX_MEDIA)
                .map { it.id }

            val parts = if (text.isNotEmpty() && text.length > snapshot.maxChars) {
                ThreadText.split(text, snapshot.maxChars)
            } else {
                listOf(text)
            }

            if (scheduledAt != null && parts.size > 1) {
                _state.update {
                    it.copy(posting = false, error = ComposeError.ThreadSchedule)
                }
                return@launch
            }

            if (scheduledAt != null) {
                val request = StatusRequest(
                    status = parts.first(),
                    visibility = visibility.apiValue,
                    spoilerText = if (snapshot.spoilerEnabled) snapshot.spoiler.trim() else "",
                    sensitive = snapshot.sensitive,
                    mediaIds = mediaIds,
                    poll = poll,
                    scheduledAt = scheduledAt,
                    quoteApprovalPolicy = quotePolicy
                )
                runCatching { api.scheduleStatus(request) }
                    .onSuccess {
                        _state.update {
                            it.copy(posting = false, posted = true, scheduled = true)
                        }
                    }
                    .onFailure {
                        _state.update {
                            it.copy(posting = false, error = ComposeError.Generic)
                        }
                    }
                return@launch
            }

            var replyToId: String? = null
            var postedCount = 0
            var failed = false
            for ((index, part) in parts.withIndex()) {
                val request = StatusRequest(
                    status = part,
                    visibility = visibility.apiValue,
                    spoilerText = if (index == 0 && snapshot.spoilerEnabled) {
                        snapshot.spoiler.trim()
                    } else {
                        ""
                    },
                    inReplyToId = replyToId,
                    sensitive = index == 0 && snapshot.sensitive,
                    mediaIds = if (index == 0) mediaIds else emptyList(),
                    poll = if (index == 0) poll else null,
                    quoteApprovalPolicy = if (index == 0) quotePolicy else null
                )
                val outcome = runCatching { api.postStatus(request) }
                if (outcome.isFailure) {
                    failed = true
                    break
                }
                replyToId = outcome.getOrNull()?.id
                postedCount++
            }

            if (failed) {
                val remaining = if (postedCount == 0) {
                    snapshot.text
                } else {
                    parts.drop(postedCount).joinToString("\n\n")
                }
                _state.update {
                    it.copy(posting = false, text = remaining, error = ComposeError.Generic)
                }
            } else {
                _state.update {
                    it.copy(posting = false, posted = true, scheduled = false)
                }
            }
        }
    }

    fun attachMedia(files: List<MediaFile>) {
        val snapshot = _state.value
        if (files.isEmpty() || snapshot.uploading || snapshot.poll != null) return
        val slots = (MAX_MEDIA - snapshot.media.size).coerceAtLeast(0)
        if (slots == 0) return
        viewModelScope.launch {
            _state.update { it.copy(uploading = true, error = null) }
            var media = _state.value.media
            var failed = false
            files.take(slots).forEach { file ->
                runCatching {
                    api.uploadMedia(
                        fileName = file.fileName,
                        bytes = file.bytes,
                        mimeType = file.mimeType,
                        description = file.description
                    )
                }
                    .onSuccess { uploaded ->
                        media = (media + uploaded).take(MAX_MEDIA)
                        _state.update { it.copy(media = media) }
                    }
                    .onFailure {
                        failed = true
                    }
            }
            _state.update {
                it.copy(
                    uploading = false,
                    error = if (failed) ComposeError.Upload else it.error
                )
            }
        }
    }

    fun attachTextImage(file: MediaFile) {
        val snapshot = _state.value
        if (snapshot.uploading || snapshot.media.size >= MAX_MEDIA || snapshot.poll != null) return
        viewModelScope.launch {
            _state.update { it.copy(uploading = true, error = null) }
            runCatching {
                api.uploadMedia(
                    fileName = file.fileName,
                    bytes = file.bytes,
                    mimeType = file.mimeType,
                    description = file.description
                )
            }
                .onSuccess { uploaded ->
                    val media = if (
                        uploaded.description.isNullOrBlank() &&
                        !file.description.isNullOrBlank()
                    ) {
                        uploaded.copy(description = file.description)
                    } else {
                        uploaded
                    }
                    _state.update {
                        it.copy(
                            uploading = false,
                            media = (it.media + media).take(MAX_MEDIA),
                            text = "",
                            translation = null,
                            error = null
                        )
                    }
                    scheduleDraftSave()
                }
                .onFailure {
                    _state.update { it.copy(uploading = false, error = ComposeError.Upload) }
                }
        }
    }

    fun translate(target: String) {
        val source = _state.value.text
        if (source.isBlank() || _state.value.translating) return
        viewModelScope.launch {
            _state.update { it.copy(translating = true, error = null) }
            runCatching { translateApi.translate(source, target) }
                .onSuccess { result ->
                    _state.update {
                        it.copy(
                            translating = false,
                            text = result.text,
                            translation = TranslationState(target, source)
                        )
                    }
                    scheduleDraftSave()
                }
                .onFailure {
                    _state.update { it.copy(translating = false, error = ComposeError.Translate) }
                }
        }
    }

    fun undoTranslation() {
        val translation = _state.value.translation ?: return
        _state.update { it.copy(text = translation.original, translation = null) }
        scheduleDraftSave()
    }

    fun pickGif(gif: GifResult) {
        val snapshot = _state.value
        if (snapshot.uploading || snapshot.media.size >= MAX_MEDIA || snapshot.poll != null) return
        viewModelScope.launch {
            _state.update { it.copy(uploading = true, error = null) }
            runCatching {
                val download = giphyApi.download(gif.url)
                val uploaded = api.uploadMedia(
                    fileName = "giphy.gif",
                    bytes = download.bytes,
                    mimeType = download.contentType,
                    description = gif.alt.takeIf { it.isNotBlank() }
                )
                when {
                    uploaded.previewUrl.isBlank() && uploaded.url.isBlank() -> {
                        uploaded.copy(previewUrl = gif.preview, url = gif.preview)
                    }
                    uploaded.description.isNullOrBlank() && gif.alt.isNotBlank() -> {
                        uploaded.copy(description = gif.alt)
                    }
                    else -> uploaded
                }
            }
                .onSuccess { media ->
                    _state.update {
                        it.copy(
                            uploading = false,
                            media = (it.media + media).take(MAX_MEDIA)
                        )
                    }
                }
                .onFailure {
                    _state.update { it.copy(uploading = false, error = ComposeError.Upload) }
                }
        }
    }

    fun removeMedia(id: String) {
        _state.update { current ->
            current.copy(media = current.media.filterNot { it.id == id })
        }
    }

    fun loadCustomEmojis() {
        val snapshot = _state.value
        if (snapshot.loadingCustomEmojis || snapshot.customEmojis.isNotEmpty()) return
        viewModelScope.launch {
            _state.update { it.copy(loadingCustomEmojis = true) }
            runCatching { api.customEmojis() }
                .onSuccess { emojis ->
                    _state.update {
                        it.copy(
                            customEmojis = emojis.filter { emoji -> emoji.visibleInPicker },
                            loadingCustomEmojis = false
                        )
                    }
                }
                .onFailure {
                    _state.update { it.copy(loadingCustomEmojis = false) }
                }
        }
    }

    fun consumePosted() {
        val current = _state.value
        _state.value = ComposeUiState(
            maxChars = current.maxChars,
            localVisibilitySupported = current.localVisibilitySupported
        )
        draftStore.clear()
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    private fun scheduleDraftSave() {
        draftJob?.cancel()
        draftJob = viewModelScope.launch {
            delay(600)
            val snapshot = _state.value
            if (snapshot.text.isBlank() && snapshot.spoiler.isBlank()) {
                draftStore.clear()
            } else {
                draftStore.save(
                    ComposeDraftData(
                        text = snapshot.text,
                        spoiler = snapshot.spoiler,
                        spoilerEnabled = snapshot.spoilerEnabled,
                        visibility = snapshot.visibility.name
                    )
                )
            }
        }
    }

    companion object {
        const val MAX_MEDIA = 4
        const val MAX_POLL_OPTIONS = 4
        const val SCHEDULE_MIN_MILLIS = 5 * 60 * 1000L
    }
}
