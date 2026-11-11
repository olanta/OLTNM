package app.oltnm.ui.screens.newsletter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.oltnm.data.api.MastodonApi
import app.oltnm.data.model.Account
import app.oltnm.data.model.NewsletterSource
import app.oltnm.data.model.Status
import app.oltnm.data.model.TrendLink
import app.oltnm.data.store.NewsletterStore
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NewsletterUiState(
    val links: List<TrendLink> = emptyList(),
    val sources: List<NewsletterSource> = emptyList(),
    val posts: List<Status> = emptyList(),
    val loading: Boolean = true,
    val error: String? = null,
    val query: String = "",
    val suggestions: List<Account> = emptyList()
)

class NewsletterViewModel(
    private val api: MastodonApi,
    private val store: NewsletterStore
) : ViewModel() {
    private val _state = MutableStateFlow(NewsletterUiState(sources = store.sources.value))
    val state: StateFlow<NewsletterUiState> = _state.asStateFlow()

    private var searchJob: Job? = null

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            var failed = false
            val links = runCatching { api.trendingLinks(limit = 20) }
                .onFailure { failed = true }
                .getOrDefault(emptyList())
            _state.update { it.copy(links = links) }

            val resolved = resolveSources()
            _state.update { it.copy(sources = resolved) }

            val posts = runCatching { loadSourcePosts(resolved) }
                .onFailure { failed = true }
                .getOrDefault(emptyList())

            _state.update {
                it.copy(
                    posts = posts,
                    loading = false,
                    error = if (failed) it.error ?: "Newsletter partially unavailable" else null
                )
            }
        }
    }

    fun onQueryChange(value: String) {
        _state.update { it.copy(query = value) }
        searchJob?.cancel()
        val query = value.trim().removePrefix("@")
        if (query.isEmpty()) {
            _state.update { it.copy(suggestions = emptyList()) }
            return
        }
        searchJob = viewModelScope.launch {
            delay(400)
            runCatching { api.search(query, limit = 8) }
                .onSuccess { results ->
                    _state.update { current ->
                        current.copy(suggestions = results.accounts.take(5))
                    }
                }
        }
    }

    fun addSource(account: Account) {
        store.add(
            NewsletterSource(
                id = account.id,
                acct = account.acct,
                name = account.nameOrHandle
            )
        )
        _state.update {
            it.copy(
                sources = store.sources.value,
                query = "",
                suggestions = emptyList()
            )
        }
        viewModelScope.launch {
            val posts = runCatching { loadSourcePosts(store.sources.value) }
                .getOrDefault(emptyList())
            _state.update { it.copy(posts = posts) }
        }
    }

    fun removeSource(acct: String) {
        store.remove(acct)
        _state.update { it.copy(sources = store.sources.value) }
        viewModelScope.launch {
            val posts = runCatching { loadSourcePosts(store.sources.value) }
                .getOrDefault(emptyList())
            _state.update { it.copy(posts = posts) }
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    private suspend fun resolveSources(): List<NewsletterSource> {
        val current = store.sources.value
        val resolved = current.map { source ->
            if (source.id.isNotBlank()) {
                source
            } else {
                val found = runCatching {
                    api.search(source.acct, limit = 5).accounts.firstOrNull {
                        it.acct.equals(source.acct, ignoreCase = true)
                    }
                }.getOrNull()
                if (found != null) {
                    NewsletterSource(
                        id = found.id,
                        acct = found.acct,
                        name = found.nameOrHandle
                    )
                } else {
                    source
                }
            }
        }
        if (resolved != current) {
            store.replace(resolved)
        }
        return resolved
    }

    private suspend fun loadSourcePosts(sources: List<NewsletterSource>): List<Status> {
        val resolved = sources.filter { it.id.isNotBlank() }
        if (resolved.isEmpty()) return emptyList()
        val lists = resolved.map { source ->
            runCatching {
                api.accountStatuses(source.id, limit = 8).items
            }.getOrDefault(emptyList())
        }
        return lists.flatten()
            .distinctBy { it.id }
            .sortedByDescending { it.createdAt }
            .take(40)
    }
}
