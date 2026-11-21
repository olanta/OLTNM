package app.oltnm.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.oltnm.data.api.MastodonApi
import app.oltnm.data.filters.FilterMatcher
import app.oltnm.data.filters.FilterStore
import app.oltnm.data.model.Account
import app.oltnm.data.model.Relationship
import app.oltnm.data.model.Status
import app.oltnm.util.userMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileUiState(
    val account: Account? = null,
    val relationship: Relationship? = null,
    val items: List<Status> = emptyList(),
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val loadingMore: Boolean = false,
    val followBusy: Boolean = false,
    val actionBusy: Boolean = false,
    val nextMaxId: String? = null,
    val warnings: Map<String, String> = emptyMap(),
    val error: String? = null
)

class ProfileViewModel(
    private val api: MastodonApi,
    private val accountId: String,
    private val filterStore: FilterStore? = null
) : ViewModel() {
    private val _state = MutableStateFlow(ProfileUiState())
    val state: StateFlow<ProfileUiState> = _state.asStateFlow()

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
            runCatching {
                val account = api.account(accountId)
                val page = api.accountStatuses(accountId)
                filterStore?.ensureLoaded(api)
                val filtered = FilterMatcher.apply(
                    items = page.items,
                    filters = filterStore?.filters?.value.orEmpty(),
                    context = "account",
                    nextMaxId = page.nextMaxId
                )
                val relationship = runCatching { api.relationship(accountId) }.getOrNull()
                Triple(account, filtered, relationship)
            }
                .onSuccess { (account, filtered, relationship) ->
                    _state.update {
                        it.copy(
                            account = account,
                            relationship = relationship,
                            items = filtered.items,
                            nextMaxId = filtered.nextMaxId,
                            warnings = filtered.warnings,
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
            runCatching {
                val page = api.accountStatuses(accountId, maxId = snapshot.nextMaxId)
                filterStore?.ensureLoaded(api)
                FilterMatcher.apply(
                    items = page.items,
                    filters = filterStore?.filters?.value.orEmpty(),
                    context = "account",
                    nextMaxId = page.nextMaxId
                )
            }
                .onSuccess { filtered ->
                    _state.update {
                        it.copy(
                            items = (it.items + filtered.items).distinctBy(Status::id),
                            nextMaxId = filtered.nextMaxId,
                            warnings = it.warnings + filtered.warnings,
                            loadingMore = false
                        )
                    }
                }
                .onFailure { error ->
                    _state.update { it.copy(loadingMore = false, error = error.userMessage()) }
                }
        }
    }

    fun toggleFollow() {
        val current = _state.value.relationship ?: return
        if (_state.value.followBusy) return
        val shouldFollow = !current.following && !current.requested
        val optimistic = current.copy(following = shouldFollow, requested = false)
        _state.update { it.copy(relationship = optimistic, followBusy = true, error = null) }
        viewModelScope.launch {
            runCatching {
                if (shouldFollow) api.follow(accountId) else api.unfollow(accountId)
            }
                .onSuccess { relationship ->
                    _state.update {
                        it.copy(relationship = relationship, followBusy = false)
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            relationship = current,
                            followBusy = false,
                            error = error.userMessage()
                        )
                    }
                }
        }
    }

    fun toggleMute() {
        val current = _state.value.relationship ?: return
        if (_state.value.actionBusy) return
        val next = !current.muting
        val optimistic = current.copy(muting = next)
        _state.update { it.copy(relationship = optimistic, actionBusy = true, error = null) }
        viewModelScope.launch {
            runCatching {
                if (next) api.muteAccount(accountId) else api.unmuteAccount(accountId)
            }
                .onSuccess { relationship ->
                    _state.update { it.copy(relationship = relationship, actionBusy = false) }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            relationship = current,
                            actionBusy = false,
                            error = error.userMessage()
                        )
                    }
                }
        }
    }

    fun toggleBlock() {
        val current = _state.value.relationship ?: return
        if (_state.value.actionBusy) return
        val next = !current.blocking
        val optimistic = current.copy(
            blocking = next,
            following = if (next) false else current.following,
            requested = if (next) false else current.requested
        )
        _state.update { it.copy(relationship = optimistic, actionBusy = true, error = null) }
        viewModelScope.launch {
            runCatching {
                if (next) api.blockAccount(accountId) else api.unblockAccount(accountId)
            }
                .onSuccess { relationship ->
                    _state.update { it.copy(relationship = relationship, actionBusy = false) }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            relationship = current,
                            actionBusy = false,
                            error = error.userMessage()
                        )
                    }
                }
        }
    }

    fun toggleFavourite(status: Status) {
        val target = status.original
        val next = !target.favourited
        updateStatus(target.id) {
            it.copy(
                favourited = next,
                favouritesCount = (it.favouritesCount + if (next) 1 else -1).coerceAtLeast(0)
            )
        }
        viewModelScope.launch {
            runCatching { if (next) api.favourite(target.id) else api.unfavourite(target.id) }
                .onSuccess { server -> updateStatus(server.id) { server } }
                .onFailure { error ->
                    updateStatus(target.id) {
                        it.copy(
                            favourited = !next,
                            favouritesCount = (it.favouritesCount + if (next) -1 else 1).coerceAtLeast(0)
                        )
                    }
                    _state.update { it.copy(error = error.userMessage()) }
                }
        }
    }

    fun toggleReblog(status: Status) {
        val target = status.original
        val next = !target.reblogged
        updateStatus(target.id) {
            it.copy(
                reblogged = next,
                reblogsCount = (it.reblogsCount + if (next) 1 else -1).coerceAtLeast(0)
            )
        }
        viewModelScope.launch {
            runCatching { if (next) api.reblog(target.id) else api.unreblog(target.id) }
                .onSuccess { server -> updateStatus(server.id) { server } }
                .onFailure { error ->
                    updateStatus(target.id) {
                        it.copy(
                            reblogged = !next,
                            reblogsCount = (it.reblogsCount + if (next) -1 else 1).coerceAtLeast(0)
                        )
                    }
                    _state.update { it.copy(error = error.userMessage()) }
                }
        }
    }

    fun toggleBookmark(status: Status) {
        val target = status.original
        val next = !target.bookmarked
        updateStatus(target.id) { it.copy(bookmarked = next) }
        viewModelScope.launch {
            runCatching {
                if (next) api.bookmark(target.id) else api.unbookmark(target.id)
            }
                .onSuccess { server -> updateStatus(server.id) { server } }
                .onFailure { error ->
                    updateStatus(target.id) { it.copy(bookmarked = !next) }
                    _state.update { it.copy(error = error.userMessage()) }
                }
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
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
}
