package app.oltnm.ui.screens.content

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.oltnm.data.api.MastodonApi
import app.oltnm.data.model.Announcement
import app.oltnm.util.userMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AnnouncementsUiState(
    val announcements: List<Announcement> = emptyList(),
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val busy: Set<String> = emptySet(),
    val error: String? = null
)

class AnnouncementsViewModel(
    private val api: MastodonApi
) : ViewModel() {
    private val _state = MutableStateFlow(AnnouncementsUiState())
    val state: StateFlow<AnnouncementsUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    refreshing = it.announcements.isNotEmpty(),
                    loading = it.announcements.isEmpty(),
                    error = null
                )
            }
            runCatching { api.announcements() }
                .onSuccess { announcements ->
                    _state.update {
                        it.copy(
                            announcements = announcements,
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

    fun dismiss(announcement: Announcement) {
        if (announcement.id in _state.value.busy) return
        _state.update {
            it.copy(busy = it.busy + announcement.id, error = null)
        }
        viewModelScope.launch {
            runCatching { api.dismissAnnouncement(announcement.id) }
                .onSuccess {
                    _state.update {
                        it.copy(
                            announcements = it.announcements.filterNot { item ->
                                item.id == announcement.id
                            }
                        )
                    }
                }
                .onFailure { error ->
                    _state.update { it.copy(error = error.userMessage()) }
                }
            _state.update { it.copy(busy = it.busy - announcement.id) }
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}
