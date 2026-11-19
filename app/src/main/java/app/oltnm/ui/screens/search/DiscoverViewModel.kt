package app.oltnm.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.oltnm.data.api.MastodonApi
import app.oltnm.data.model.Account
import app.oltnm.data.model.Relationship
import app.oltnm.data.model.Status
import app.oltnm.util.userMessage
import java.net.URI
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

enum class DiscoverMode {
    Topics,
    Languages
}

data class DiscoverUiState(
    val mode: DiscoverMode = DiscoverMode.Topics,
    val topicId: String = DiscoverCategories.first().id,
    val languageId: String = "en",
    val accounts: List<Account> = emptyList(),
    val relationships: Map<String, Relationship> = emptyMap(),
    val busyIds: Set<String> = emptySet(),
    val seedHandles: List<String> = emptyList(),
    val seedAccounts: Map<String, Account?> = emptyMap(),
    val seedLimit: Int = 80,
    val seedBusy: Set<String> = emptySet(),
    val loading: Boolean = true,
    val error: String? = null
) {
    val categoryId: String
        get() = if (mode == DiscoverMode.Topics) topicId else languageId

    val visibleSeedHandles: List<String>
        get() = seedHandles.take(seedLimit)
}

private data class DiscoverCacheEntry(
    val at: Long,
    val accounts: List<Account>
)

class DiscoverViewModel(
    private val api: MastodonApi,
    private val currentAccountId: String,
    private val instanceHost: String,
    seedGroups: Map<String, List<String>>,
    initialLanguageId: String = "en"
) : ViewModel() {
    private val _state = MutableStateFlow(DiscoverUiState(languageId = initialLanguageId))
    val state: StateFlow<DiscoverUiState> = _state.asStateFlow()

    private val cache = mutableMapOf<String, DiscoverCacheEntry>()
    private val seeds = seedGroups.mapValues { (_, handles) ->
        handles.map { it.trim().removePrefix("@") }
            .filter { it.isNotBlank() }
            .distinct()
    }
    private val seedAccountCache = mutableMapOf<String, Account?>()
    private val seedResolving = mutableSetOf<String>()
    private val seedSemaphore = Semaphore(SEED_CONCURRENCY)
    private var loadSeq = 0

    init {
        load(DiscoverMode.Topics, DiscoverCategories.first().id)
    }

    fun selectMode(mode: DiscoverMode) {
        if (mode == _state.value.mode) return
        val categoryId = if (mode == DiscoverMode.Topics) {
            _state.value.topicId
        } else {
            _state.value.languageId
        }
        load(mode, categoryId)
    }

    fun selectCategory(categoryId: String) {
        if (
            _state.value.mode == DiscoverMode.Topics &&
            categoryId == _state.value.topicId &&
            _state.value.accounts.isNotEmpty()
        ) {
            return
        }
        load(DiscoverMode.Topics, categoryId)
    }

    fun selectLanguage(languageId: String) {
        if (
            _state.value.mode == DiscoverMode.Languages &&
            languageId == _state.value.languageId &&
            _state.value.accounts.isNotEmpty()
        ) {
            return
        }
        load(DiscoverMode.Languages, languageId)
    }

    fun retry() {
        load(_state.value.mode, _state.value.categoryId, force = true)
    }

    fun showMoreSeeds() {
        _state.update { it.copy(seedLimit = it.seedLimit + SEED_PAGE) }
    }

    fun toggleFollow(accountId: String) {
        if (accountId in _state.value.busyIds) return
        val relationship = _state.value.relationships[accountId] ?: return
        followAccount(accountId, relationship)
    }

    fun resolveSeed(handle: String) {
        if (seedAccountCache.containsKey(handle) || handle in seedResolving) return
        seedResolving += handle
        viewModelScope.launch {
            try {
                resolveSeedInternal(handle)
            } finally {
                seedResolving -= handle
            }
        }
    }

    fun toggleSeed(handle: String) {
        if (handle in _state.value.seedBusy) return
        _state.update { it.copy(seedBusy = it.seedBusy + handle, error = null) }
        viewModelScope.launch {
            try {
                if (seedAccountCache[handle] == null) seedAccountCache.remove(handle)
                val account = resolveSeedInternal(handle)
                    ?: throw IllegalStateException("Could not find @$handle.")
                val relationship = ensureRelationship(account.id)
                    ?: Relationship(id = account.id)
                followAccount(account.id, relationship)
            } catch (error: Exception) {
                _state.update { it.copy(error = error.userMessage()) }
            } finally {
                _state.update { it.copy(seedBusy = it.seedBusy - handle) }
            }
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    private fun followAccount(accountId: String, relationship: Relationship) {
        if (accountId in _state.value.busyIds) return
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

    private suspend fun resolveSeedInternal(handle: String): Account? {
        seedAccountCache[handle]?.let { return it }
        return seedSemaphore.withPermit {
            seedAccountCache[handle]?.let { return@withPermit it }
            val account = resolveSeedHandle(handle)
            seedAccountCache[handle] = account
            _state.update {
                it.copy(seedAccounts = it.seedAccounts + (handle to account))
            }
            if (account != null) ensureRelationship(account.id)
            account
        }
    }

    private suspend fun resolveSeedHandle(handle: String): Account? {
        val acct = handle.trim().removePrefix("@").lowercase()
        val at = acct.lastIndexOf('@')
        if (at <= 0 || at == acct.length - 1) return null
        val username = acct.substring(0, at)
        val host = acct.substring(at + 1)
        return runCatching {
            api.searchAccounts(acct, limit = 5, resolve = true).firstOrNull { account ->
                account.acct.equals(acct, ignoreCase = true) ||
                    (
                        account.username.equals(username, ignoreCase = true) &&
                            hostOf(account.url) == host
                        )
            }
        }.getOrNull()
    }

    private suspend fun ensureRelationship(accountId: String): Relationship? {
        _state.value.relationships[accountId]?.let { return it }
        return runCatching { api.relationship(accountId) }
            .getOrNull()
            ?.also { relationship ->
                _state.update {
                    it.copy(relationships = it.relationships + (accountId to relationship))
                }
            }
    }

    private fun load(
        mode: DiscoverMode,
        categoryId: String,
        force: Boolean = false
    ) {
        val category = categoryFor(mode, categoryId) ?: return
        val cacheKey = "${mode.name}:${category.id}"
        val now = System.currentTimeMillis()
        val cached = cache[cacheKey]
        val seedHandles = seeds[category.id].orEmpty()
        val topicId = if (mode == DiscoverMode.Topics) category.id else _state.value.topicId
        val languageId = if (mode == DiscoverMode.Languages) category.id else _state.value.languageId
        if (!force && cached != null && now - cached.at < CACHE_TTL_MS) {
            _state.update {
                it.copy(
                    mode = mode,
                    topicId = topicId,
                    languageId = languageId,
                    accounts = cached.accounts,
                    seedHandles = filterSeeds(seedHandles, cached.accounts),
                    seedLimit = SEED_PAGE,
                    loading = false,
                    error = null
                )
            }
            loadRelationships(cached.accounts)
            return
        }

        val seq = ++loadSeq
        _state.update {
            it.copy(
                mode = mode,
                topicId = topicId,
                languageId = languageId,
                accounts = emptyList(),
                seedHandles = filterSeeds(seedHandles, emptyList()),
                seedLimit = SEED_PAGE,
                loading = true,
                error = null
            )
        }
        viewModelScope.launch {
            val result = fetch(category)
            if (seq != loadSeq) return@launch
            if (result == null) {
                _state.update {
                    it.copy(
                        accounts = emptyList(),
                        seedHandles = filterSeeds(seedHandles, emptyList()),
                        loading = false,
                        error = DISCOVER_ERROR
                    )
                }
                return@launch
            }
            cache[cacheKey] = DiscoverCacheEntry(System.currentTimeMillis(), result)
            _state.update {
                it.copy(
                    accounts = result,
                    seedHandles = filterSeeds(seedHandles, result),
                    loading = false
                )
            }
            loadRelationships(result)
        }
    }

    private fun categoryFor(mode: DiscoverMode, categoryId: String): DiscoverCategory? = when (mode) {
        DiscoverMode.Topics -> DiscoverCategories.firstOrNull { it.id == categoryId }
        DiscoverMode.Languages -> DiscoverLanguages.firstOrNull { it.id == categoryId }?.toCategory()
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
        fun candidate(account: Account): DiscoverCandidate = candidates.getOrPut(account.id) {
            DiscoverCandidate(account)
        }

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

    private fun loadRelationships(accounts: List<Account>) {
        if (accounts.isEmpty()) return
        viewModelScope.launch {
            runCatching { api.relationships(accounts.map { it.id }) }
                .onSuccess { relationships ->
                    _state.update {
                        it.copy(
                            relationships = it.relationships +
                                relationships.associateBy { relationship -> relationship.id }
                        )
                    }
                }
        }
    }

    private fun filterSeeds(handles: List<String>, accounts: List<Account>): List<String> {
        val known = accounts.flatMap { account ->
            val acct = account.acct.lowercase()
            if (acct.contains('@')) {
                listOf(acct)
            } else {
                listOf(acct, "$acct@${instanceHost.lowercase()}")
            }
        }.toSet()
        return handles.filter { it.lowercase() !in known }
    }

    private fun hostOf(url: String): String? = runCatching { URI(url).host?.lowercase() }.getOrNull()

    private companion object {
        const val CACHE_TTL_MS = 30 * 60 * 1000L
        const val DISCOVER_ERROR = "Could not load topic suggestions."
        const val SEED_PAGE = 80
        const val SEED_CONCURRENCY = 4
    }
}
