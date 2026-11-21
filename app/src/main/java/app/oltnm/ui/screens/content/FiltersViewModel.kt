package app.oltnm.ui.screens.content

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.oltnm.data.api.MastodonApi
import app.oltnm.data.filters.FilterStore
import app.oltnm.data.model.FilterKeywordRequest
import app.oltnm.data.model.FilterRequest
import app.oltnm.data.model.MastodonFilter
import app.oltnm.util.userMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

val FilterContexts = listOf("home", "public", "notifications", "thread", "account")

data class FiltersUiState(
    val filters: List<MastodonFilter> = emptyList(),
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val busy: Set<String> = emptySet(),
    val title: String = "",
    val keyword: String = "",
    val contexts: Set<String> = setOf("home", "notifications"),
    val action: String = "hide",
    val creating: Boolean = false,
    val error: String? = null
) {
    val canCreate: Boolean
        get() = title.isNotBlank() &&
            keyword.isNotBlank() &&
            contexts.isNotEmpty() &&
            !creating
}

class FiltersViewModel(
    private val api: MastodonApi,
    private val filterStore: FilterStore? = null
) : ViewModel() {
    private val _state = MutableStateFlow(FiltersUiState())
    val state: StateFlow<FiltersUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    refreshing = it.filters.isNotEmpty(),
                    loading = it.filters.isEmpty(),
                    error = null
                )
            }
            runCatching { api.filters() }
                .onSuccess { filters ->
                    filterStore?.set(filters)
                    _state.update {
                        it.copy(
                            filters = filters,
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

    fun onTitleChange(value: String) {
        _state.update { it.copy(title = value, error = null) }
    }

    fun onKeywordChange(value: String) {
        _state.update { it.copy(keyword = value, error = null) }
    }

    fun toggleContext(context: String) {
        _state.update { current ->
            current.copy(
                contexts = if (context in current.contexts) {
                    current.contexts - context
                } else {
                    current.contexts + context
                }
            )
        }
    }

    fun setAction(action: String) {
        _state.update { it.copy(action = action) }
    }

    fun create() {
        val snapshot = _state.value
        if (!snapshot.canCreate) return
        _state.update { it.copy(creating = true, error = null) }
        viewModelScope.launch {
            runCatching {
                api.createFilter(
                    FilterRequest(
                        title = snapshot.title.trim(),
                        context = snapshot.contexts.toList(),
                        filterAction = snapshot.action,
                        keywords = listOf(
                            FilterKeywordRequest(keyword = snapshot.keyword.trim())
                        )
                    )
                )
            }
                .onSuccess { filter ->
                    filterStore?.add(filter)
                    _state.update {
                        it.copy(
                            filters = listOf(filter) + it.filters,
                            title = "",
                            keyword = "",
                            creating = false
                        )
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(creating = false, error = error.userMessage())
                    }
                }
        }
    }

    fun delete(filter: MastodonFilter) {
        if (filter.id in _state.value.busy) return
        _state.update {
            it.copy(busy = it.busy + filter.id, error = null)
        }
        viewModelScope.launch {
            runCatching { api.deleteFilter(filter.id) }
                .onSuccess {
                    filterStore?.remove(filter.id)
                    _state.update {
                        it.copy(filters = it.filters.filterNot { item -> item.id == filter.id })
                    }
                }
                .onFailure { error ->
                    _state.update { it.copy(error = error.userMessage()) }
                }
            _state.update { it.copy(busy = it.busy - filter.id) }
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}
