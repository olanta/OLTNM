package app.oltnm.ui.screens.content

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.oltnm.data.api.MastodonApi
import app.oltnm.data.model.Tag
import app.oltnm.util.userMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FollowedTagsUiState(
    val tags: List<Tag> = emptyList(),
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val busy: Set<String> = emptySet(),
    val nextMaxId: String? = null,
    val error: String? = null
)

class FollowedTagsViewModel(
    private val api: MastodonApi
) : ViewModel() {
    private val _state = MutableStateFlow(FollowedTagsUiState())
    val state: StateFlow<FollowedTagsUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    refreshing = it.tags.isNotEmpty(),
                    loading = it.tags.isEmpty(),
                    error = null
                )
            }
            runCatching { api.followedTags() }
                .onSuccess { page ->
                    _state.update {
                        it.copy(
                            tags = page.items,
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

    fun unfollow(tag: Tag) {
        if (tag.name in _state.value.busy) return
        _state.update {
            it.copy(busy = it.busy + tag.name, error = null)
        }
        viewModelScope.launch {
            runCatching { api.unfollowTag(tag.name) }
                .onSuccess {
                    _state.update {
                        it.copy(tags = it.tags.filterNot { item -> item.name == tag.name })
                    }
                }
                .onFailure { error ->
                    _state.update { it.copy(error = error.userMessage()) }
                }
            _state.update { it.copy(busy = it.busy - tag.name) }
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}
