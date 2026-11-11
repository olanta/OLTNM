package app.oltnm.ui.screens.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.oltnm.data.api.MastodonApi
import app.oltnm.data.model.Notification
import app.oltnm.util.userMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NotificationsUiState(
    val items: List<Notification> = emptyList(),
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val loadingMore: Boolean = false,
    val nextMaxId: String? = null,
    val error: String? = null
)

class NotificationsViewModel(private val api: MastodonApi) : ViewModel() {
    private val _state = MutableStateFlow(NotificationsUiState())
    val state: StateFlow<NotificationsUiState> = _state.asStateFlow()

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
            runCatching { api.notifications() }
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
            runCatching { api.notifications(maxId = snapshot.nextMaxId) }
                .onSuccess { page ->
                    _state.update {
                        it.copy(
                            items = (it.items + page.items).distinctBy(Notification::id),
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

    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}
