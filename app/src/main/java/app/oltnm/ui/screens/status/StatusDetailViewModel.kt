package app.oltnm.ui.screens.status

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.oltnm.data.api.MastodonApi
import app.oltnm.data.model.Status
import app.oltnm.util.userMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class StatusDetailUiState(
    val status: Status? = null,
    val ancestors: List<Status> = emptyList(),
    val descendants: List<Status> = emptyList(),
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val error: String? = null
)

class StatusDetailViewModel(
    private val api: MastodonApi,
    private val statusId: String,
    private val interactive: Boolean
) : ViewModel() {
    private val _state = MutableStateFlow(StatusDetailUiState())
    val state: StateFlow<StatusDetailUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        if (_state.value.refreshing) return
        viewModelScope.launch {
            _state.update {
                it.copy(
                    loading = it.status == null,
                    refreshing = it.status != null,
                    error = null
                )
            }
            val statusResult = runCatching { api.status(statusId) }
            statusResult
                .onSuccess { status ->
                    _state.update {
                        it.copy(status = status, loading = false, refreshing = false)
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            loading = false,
                            refreshing = false,
                            error = error.userMessage()
                        )
                    }
                }
            if (statusResult.isFailure) return@launch
            runCatching { api.statusContext(statusId) }
                .onSuccess { context ->
                    _state.update {
                        it.copy(
                            ancestors = context.ancestors,
                            descendants = context.descendants
                        )
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(error = it.error ?: error.userMessage())
                    }
                }
        }
    }

    fun toggleFavourite(status: Status) {
        if (!interactive) return
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
                            favouritesCount = (it.favouritesCount + if (next) -1 else 1)
                                .coerceAtLeast(0)
                        )
                    }
                    _state.update { it.copy(error = error.userMessage()) }
                }
        }
    }

    fun toggleReblog(status: Status) {
        if (!interactive) return
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
                            reblogsCount = (it.reblogsCount + if (next) -1 else 1)
                                .coerceAtLeast(0)
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
            fun apply(status: Status): Status = when {
                status.id == id -> transform(status)
                status.reblog?.id == id -> status.copy(reblog = status.reblog?.let(transform))
                else -> status
            }
            current.copy(
                status = current.status?.let(::apply),
                ancestors = current.ancestors.map(::apply),
                descendants = current.descendants.map(::apply)
            )
        }
    }
}
