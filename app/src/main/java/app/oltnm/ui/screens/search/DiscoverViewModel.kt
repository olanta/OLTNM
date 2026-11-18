package app.oltnm.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.oltnm.data.api.MastodonApi
import app.oltnm.data.model.Account
import app.oltnm.data.model.Relationship
import app.oltnm.data.model.Status
import app.oltnm.util.userMessage
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DiscoverUiState(
    val categoryId: String = DiscoverCategories.first().id,
    val accounts: List<Account> = emptyList(),
    val relationships: Map<String, Relationship> = emptyMap(),
    val busyIds: Set<String> = emptySet(),
    val loading: Boolean = true,
    val error: String? = null
)

private data class DiscoverCacheEntry(
    val at: Long,
    val accounts: List<Account>
)

class DiscoverViewModel(
    private val api: MastodonApi,
    private val currentAccountId: String
) : ViewModel() {
    private val _state = MutableStateFlow(DiscoverUiState())
    val state: StateFlow<DiscoverUiState> = _state.asStateFlow()

    private val cache = mutableMapOf<String, DiscoverCacheEntry>()
    private var loadSeq = 0

    init {
        load(DiscoverCategories.first().id)
    }

    fun selectCategory(categoryId: String) {
        if (categoryId == _state.value.categoryId && _state.value.accounts.isNotEmpty()) return
        load(categoryId)
    }

    fun retry() {
        load(_state.value.categoryId, force = true)
    }

    fun toggleFollow(accountId: String) {
        if (accountId in _state.value.busyIds) return
        val relationship = _state.value.relationships[accountId] ?: return
        val shouldFollow = !relationship.following && !relationship.requested
        val optimistic = relationship.copy(following = shouldFollow, requested = false)
        _state.update {
            it.copy(
                relationships = it.relationships + (accountId to optimistic),
                busyIds = it.busyIds + accountId,
                error = null
            )
        }
        viewModelScope.launch {
            runCatching {
                if (shouldFollow) api.follow(accountId) else api.unfollow(accountId)
            }
                .onSuccess { updated ->
                    _state.update {
                        it.copy(
                            relationships = it.relationships + (accountId to updated),
                            busyIds = it.busyIds - accountId
                        )
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            relationships = it.relationships + (accountId to relationship),
                            busyIds = it.busyIds - accountId,
                            error = error.userMessage()
                        )
                    }
                }
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    private fun load(categoryId: String, force: Boolean = false) {
        val category = DiscoverCategories.firstOrNull { it.id == categoryId } ?: return
        val now = System.currentTimeMillis()
        val cached = cache[categoryId]
        if (!force && cached != null && now - cached.at < CACHE_TTL_MS) {
            _state.update {
                it.copy(
                    categoryId = categoryId,
                    accounts = cached.accounts,
                    relationships = emptyMap(),
                    loading = false,
                    error = null
                )
            }
            loadRelationships(categoryId, cached.accounts)
            return
        }

        val seq = ++loadSeq
        _state.update {
            it.copy(
                categoryId = categoryId,
                accounts = emptyList(),
                relationships = emptyMap(),
                loading = true,
                error = null
            )
        }
        viewModelScope.launch {
            val result = fetch(category)
            if (seq != loadSeq) return@launch
            if (result == null) {
                _state.update {
                    it.copy(accounts = emptyList(), loading = false, error = DISCOVER_ERROR)
                }
                return@launch
            }
            cache[categoryId] = DiscoverCacheEntry(System.currentTimeMillis(), result)
            _state.update {
                it.copy(accounts = result, loading = false)
            }
            loadRelationships(categoryId, result)
        }
    }

    private suspend fun fetch(category: DiscoverCategory): List<Account>? = coroutineScope {
        val search = async {
            runCatching { api.searchAccounts(category.query, limit = 20, resolve = true) }
        }
        val tags = category.tags.map { tag ->
            async {
                runCatching { api.hashtagTimeline(tag = tag, limit = 20).items }
            }
        }
        val searchResult = search.await()
        val tagResults = tags.awaitAll()
        val anySuccess = searchResult.isSuccess || tagResults.any { it.isSuccess }
        if (!anySuccess) return@coroutineScope null

        val candidates = mutableMapOf<String, DiscoverCandidate>()
        fun candidate(account: Account): DiscoverCandidate = candidates.getOrPut(account.id) { DiscoverCandidate(account) }

        searchResult.getOrNull()?.forEach { account ->
            candidate(account).searchHits += 1
        }
        tagResults.forEach { statuses ->
            statuses.getOrNull()?.forEach { status: Status ->
                val author = status.original.account
                val item = candidate(author)
                item.tagHits += 1
                item.lastSeen = maxOf(item.lastSeen, statusRecency(status.createdAt))
            }
        }

        rankDiscoverCandidates(
            candidates = candidates.values.filter { it.account.id != currentAccountId },
            category = category,
            now = System.currentTimeMillis()
        )
    }

    private fun loadRelationships(categoryId: String, accounts: List<Account>) {
        if (accounts.isEmpty()) {
            _state.update { it.copy(relationships = emptyMap()) }
            return
        }
        viewModelScope.launch {
            runCatching { api.relationships(accounts.map { it.id }) }
                .onSuccess { relationships ->
                    if (_state.value.categoryId != categoryId) return@onSuccess
                    _state.update {
                        it.copy(relationships = relationships.associateBy { relationship -> relationship.id })
                    }
                }
        }
    }

    private companion object {
        const val CACHE_TTL_MS = 30 * 60 * 1000L
        const val DISCOVER_ERROR = "Could not load topic suggestions."
    }
}
