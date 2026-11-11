package app.oltnm.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.oltnm.data.api.MastodonApi
import app.oltnm.data.model.SearchResults
import app.oltnm.data.model.Status
import app.oltnm.util.userMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SearchUiState(
    val query: String = "",
    val loading: Boolean = false,
    val results: SearchResults? = null,
    val error: String? = null
)

class SearchViewModel(private val api: MastodonApi) : ViewModel() {
    private val _state = MutableStateFlow(SearchUiState())
    val state: StateFlow<SearchUiState> = _state.asStateFlow()

    fun onQueryChange(value: String) {
        _state.update { it.copy(query = value, error = null) }
    }

    fun search() {
        val query = _state.value.query.trim()
        if (query.isEmpty() || _state.value.loading) return
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            runCatching { api.search(query) }
                .onSuccess { results ->
                    _state.update { it.copy(loading = false, results = results) }
                }
                .onFailure { error ->
                    _state.update { it.copy(loading = false, error = error.userMessage()) }
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
            val results = current.results ?: return@update current
            current.copy(
                results = results.copy(
                    statuses = results.statuses.map { item ->
                        when {
                            item.id == id -> transform(item)
                            item.reblog?.id == id -> item.copy(reblog = item.reblog?.let(transform))
                            else -> item
                        }
                    }
                )
            )
        }
    }
}
