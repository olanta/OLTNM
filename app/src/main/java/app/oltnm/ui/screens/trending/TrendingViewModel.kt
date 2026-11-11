package app.oltnm.ui.screens.trending

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.oltnm.data.api.MastodonApi
import app.oltnm.data.model.Status
import app.oltnm.data.model.Tag
import app.oltnm.data.model.TrendLink
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TrendingUiState(
    val posts: List<Status> = emptyList(),
    val tags: List<Tag> = emptyList(),
    val links: List<TrendLink> = emptyList(),
    val loading: Boolean = true
)

class TrendingViewModel(private val api: MastodonApi) : ViewModel() {
    private val _state = MutableStateFlow(TrendingUiState())
    val state: StateFlow<TrendingUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true) }
            val posts = runCatching { api.trendingStatuses() }.getOrDefault(emptyList())
            val tags = runCatching { api.trendingTags() }.getOrDefault(emptyList())
            val links = runCatching { api.trendingLinks() }.getOrDefault(emptyList())
            _state.update {
                it.copy(
                    posts = posts,
                    tags = tags,
                    links = links,
                    loading = false
                )
            }
        }
    }
}
