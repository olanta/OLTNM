package app.oltnm.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.oltnm.data.api.MastodonApi
import app.oltnm.data.model.Account
import app.oltnm.data.model.Status
import app.oltnm.util.userMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileUiState(
    val account: Account? = null,
    val items: List<Status> = emptyList(),
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val loadingMore: Boolean = false,
    val nextMaxId: String? = null,
    val error: String? = null
)

class ProfileViewModel(
    private val api: MastodonApi,
    private val accountId: String
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
                account to page
            }
                .onSuccess { (account, page) ->
                    _state.update {
                        it.copy(
                            account = account,
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
            runCatching { api.accountStatuses(accountId, maxId = snapshot.nextMaxId) }
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
                    _state.update { it.copy(loadingMore = false, error = error.userMessage()) }
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
