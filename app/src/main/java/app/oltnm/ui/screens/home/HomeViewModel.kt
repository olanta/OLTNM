package app.oltnm.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.oltnm.data.api.MastodonApi
import app.oltnm.data.api.Page
import app.oltnm.data.model.Status
import app.oltnm.data.model.TimelineSpec
import app.oltnm.data.model.TimelineTypes
import app.oltnm.util.userMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TimelineUiState(
    val items: List<Status> = emptyList(),
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val loadingMore: Boolean = false,
    val nextMaxId: String? = null,
    val error: String? = null
)

class TimelineViewModel(
    private val spec: TimelineSpec,
    private val ownApi: MastodonApi,
    private val remoteApi: MastodonApi?
) : ViewModel() {
    private val isRemote = spec.type == TimelineTypes.REMOTE_LOCAL ||
        spec.type == TimelineTypes.REMOTE_FEDERATED
    private val sourceApi = if (isRemote) remoteApi ?: ownApi else ownApi

    private val _state = MutableStateFlow(TimelineUiState())
    val state: StateFlow<TimelineUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        if (_state.value.refreshing) return
        viewModelScope.launch {
            _state.update {
                it.copy(
                    refreshing = it.items.isNotEmpty(),
                    loading = it.items.isEmpty(),
                    error = null
                )
            }
            runCatching { fetch(null) }
                .onSuccess { page ->
                    _state.update {
                        it.copy(
                            items = page.items,
                            nextMaxId = page.nextMaxId,
                            loading = false,
                            refreshing = false
                        )
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(loading = false, refreshing = false, error = error.userMessage())
                    }
                }
        }
    }

    fun loadMore() {
        val snapshot = _state.value
        if (snapshot.loadingMore || snapshot.refreshing || snapshot.nextMaxId == null) return
        viewModelScope.launch {
            _state.update { it.copy(loadingMore = true, error = null) }
            runCatching { fetch(snapshot.nextMaxId) }
                .onSuccess { page ->
                    _state.update {
                        it.copy(
                            items = (it.items + page.items).distinctBy(Status::id),
                            nextMaxId = page.nextMaxId,
                            loadingMore = false
                        )
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(loadingMore = false, error = error.userMessage())
                    }
                }
        }
    }

    fun toggleFavourite(status: Status) {
        if (isRemote) {
            toggleRemoteFavourite(status)
            return
        }
        val target = status.original
        val next = !target.favourited
        updateStatus(target.id) {
            it.copy(
                favourited = next,
                favouritesCount = (it.favouritesCount + if (next) 1 else -1).coerceAtLeast(0)
            )
        }
        viewModelScope.launch {
            runCatching { if (next) ownApi.favourite(target.id) else ownApi.unfavourite(target.id) }
                .onSuccess { server -> updateStatus(server.id) { server } }
                .onFailure { error ->
                    updateStatus(target.id) {
                        it.copy(
                            favourited = !next,
                            favouritesCount = (it.favouritesCount + if (next) -1 else 1)
                                .coerceAtLeast(0)
                        )
                    }
                    _state.update { it.copy(error = error.userMessage()) }
                }
        }
    }

    fun toggleReblog(status: Status) {
        if (isRemote) {
            toggleRemoteReblog(status)
            return
        }
        val target = status.original
        val next = !target.reblogged
        updateStatus(target.id) {
            it.copy(
                reblogged = next,
                reblogsCount = (it.reblogsCount + if (next) 1 else -1).coerceAtLeast(0)
            )
        }
        viewModelScope.launch {
            runCatching { if (next) ownApi.reblog(target.id) else ownApi.unreblog(target.id) }
                .onSuccess { server -> updateStatus(server.id) { server } }
                .onFailure { error ->
                    updateStatus(target.id) {
                        it.copy(
                            reblogged = !next,
                            reblogsCount = (it.reblogsCount + if (next) -1 else 1)
                                .coerceAtLeast(0)
                        )
                    }
                    _state.update { it.copy(error = error.userMessage()) }
                }
        }
    }

    fun toggleBookmark(status: Status) {
        if (isRemote) {
            toggleRemoteBookmark(status)
            return
        }
        val target = status.original
        val next = !target.bookmarked
        updateStatus(target.id) { it.copy(bookmarked = next) }
        viewModelScope.launch {
            runCatching {
                if (next) ownApi.bookmark(target.id) else ownApi.unbookmark(target.id)
            }
                .onSuccess { server -> updateStatus(server.id) { server } }
                .onFailure { error ->
                    updateStatus(target.id) { it.copy(bookmarked = !next) }
                    _state.update { it.copy(error = error.userMessage()) }
                }
        }
    }

    private suspend fun resolveLocal(status: Status): Status? {
        val url = status.displayUrl ?: return null
        return runCatching { ownApi.search(url).statuses.firstOrNull() }.getOrNull()
    }

    private fun toggleRemoteFavourite(status: Status) {
        val target = status.original
        viewModelScope.launch {
            val local = resolveLocal(target)
            if (local == null) {
                _state.update { it.copy(error = REMOTE_RESOLVE_ERROR) }
                return@launch
            }
            val next = !local.favourited
            updateStatus(target.id) {
                it.copy(
                    favourited = next,
                    favouritesCount = (local.favouritesCount + if (next) 1 else -1)
                        .coerceAtLeast(0)
                )
            }
            runCatching { if (next) ownApi.favourite(local.id) else ownApi.unfavourite(local.id) }
                .onSuccess { server ->
                    updateStatus(target.id) {
                        it.copy(
                            favourited = server.favourited,
                            favouritesCount = server.favouritesCount
                        )
                    }
                }
                .onFailure { error ->
                    updateStatus(target.id) {
                        it.copy(
                            favourited = local.favourited,
                            favouritesCount = local.favouritesCount
                        )
                    }
                    _state.update { it.copy(error = error.userMessage()) }
                }
        }
    }

    private fun toggleRemoteReblog(status: Status) {
        val target = status.original
        viewModelScope.launch {
            val local = resolveLocal(target)
            if (local == null) {
                _state.update { it.copy(error = REMOTE_RESOLVE_ERROR) }
                return@launch
            }
            val next = !local.reblogged
            updateStatus(target.id) {
                it.copy(
                    reblogged = next,
                    reblogsCount = (local.reblogsCount + if (next) 1 else -1).coerceAtLeast(0)
                )
            }
            runCatching { if (next) ownApi.reblog(local.id) else ownApi.unreblog(local.id) }
                .onSuccess { server ->
                    updateStatus(target.id) {
                        it.copy(
                            reblogged = server.reblogged,
                            reblogsCount = server.reblogsCount
                        )
                    }
                }
                .onFailure { error ->
                    updateStatus(target.id) {
                        it.copy(
                            reblogged = local.reblogged,
                            reblogsCount = local.reblogsCount
                        )
                    }
                    _state.update { it.copy(error = error.userMessage()) }
                }
        }
    }

    private fun toggleRemoteBookmark(status: Status) {
        val target = status.original
        viewModelScope.launch {
            val local = resolveLocal(target)
            if (local == null) {
                _state.update { it.copy(error = REMOTE_RESOLVE_ERROR) }
                return@launch
            }
            val next = !local.bookmarked
            updateStatus(target.id) { it.copy(bookmarked = next) }
            runCatching { if (next) ownApi.bookmark(local.id) else ownApi.unbookmark(local.id) }
                .onSuccess { server ->
                    updateStatus(target.id) { it.copy(bookmarked = server.bookmarked) }
                }
                .onFailure { error ->
                    updateStatus(target.id) { it.copy(bookmarked = local.bookmarked) }
                    _state.update { it.copy(error = error.userMessage()) }
                }
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    private suspend fun fetch(maxId: String?): Page<Status> = when (spec.type) {
        TimelineTypes.HOME -> ownApi.homeTimeline(maxId = maxId)
        TimelineTypes.LOCAL -> ownApi.publicTimeline(local = true, maxId = maxId)
        TimelineTypes.FEDERATED -> ownApi.publicTimeline(local = false, maxId = maxId)
        TimelineTypes.REMOTE_LOCAL -> sourceApi.publicTimeline(local = true, maxId = maxId)
        TimelineTypes.REMOTE_FEDERATED -> sourceApi.publicTimeline(local = false, maxId = maxId)
        TimelineTypes.HASHTAG -> ownApi.hashtagTimeline(
            tag = spec.value.orEmpty(),
            maxId = maxId
        )
        TimelineTypes.LIST -> ownApi.listTimeline(
            listId = spec.value.orEmpty(),
            maxId = maxId
        )
        TimelineTypes.BOOKMARKS -> ownApi.bookmarks(maxId = maxId)
        TimelineTypes.FAVOURITES -> ownApi.favourites(maxId = maxId)
        TimelineTypes.MENTIONS -> {
            val page = ownApi.notifications(
                maxId = maxId,
                types = listOf("mention")
            )
            Page(
                items = page.items.mapNotNull { it.status }.distinctBy(Status::id),
                nextMaxId = page.nextMaxId
            )
        }
        else -> Page(emptyList(), null)
    }

    private fun updateStatus(id: String, transform: (Status) -> Status) {
        _state.update { current ->
            current.copy(
                items = current.items.map { item ->
                    when {
                        item.id == id -> transform(item)
                        item.reblog?.id == id -> item.copy(reblog = item.reblog?.let(transform))
                        else -> item
                    }
                }
            )
        }
    }

    private companion object {
        const val REMOTE_RESOLVE_ERROR = "Could not find this post on your instance."
    }
}
