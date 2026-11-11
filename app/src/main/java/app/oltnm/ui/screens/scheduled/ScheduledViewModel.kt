package app.oltnm.ui.screens.scheduled

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.oltnm.data.api.MastodonApi
import app.oltnm.data.model.ScheduledStatus
import app.oltnm.util.userMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ScheduledUiState(
    val items: List<ScheduledStatus> = emptyList(),
    val loading: Boolean = true,
    val error: String? = null
)

class ScheduledViewModel(private val api: MastodonApi) : ViewModel() {
    private val _state = MutableStateFlow(ScheduledUiState())
    val state: StateFlow<ScheduledUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            runCatching { api.scheduledStatuses() }
                .onSuccess { items ->
                    _state.update { it.copy(items = items, loading = false) }
                }
                .onFailure { error ->
                    _state.update { it.copy(loading = false, error = error.userMessage()) }
                }
        }
    }

    fun delete(id: String) {
        viewModelScope.launch {
            runCatching { api.deleteScheduledStatus(id) }
                .onSuccess {
                    _state.update { current ->
                        current.copy(items = current.items.filterNot { it.id == id })
                    }
                }
                .onFailure { error ->
                    _state.update { it.copy(error = error.userMessage()) }
                }
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}
