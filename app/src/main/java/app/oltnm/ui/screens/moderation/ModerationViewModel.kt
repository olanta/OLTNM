package app.oltnm.ui.screens.moderation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.oltnm.data.api.MastodonApi
import app.oltnm.data.model.Account
import app.oltnm.util.userMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ModerationTab {
    FollowRequests,
    Mutes,
    Domains
}

data class ModerationUiState(
    val tab: ModerationTab = ModerationTab.FollowRequests,
    val followRequests: List<Account> = emptyList(),
    val mutes: List<Account> = emptyList(),
    val domains: List<String> = emptyList(),
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val busy: Set<String> = emptySet(),
    val domainQuery: String = "",
    val error: String? = null
) {
    val itemsCount: Int
        get() = when (tab) {
            ModerationTab.FollowRequests -> followRequests.size
            ModerationTab.Mutes -> mutes.size
            ModerationTab.Domains -> domains.size
        }
}

class ModerationViewModel(
    private val api: MastodonApi
) : ViewModel() {
    private val _state = MutableStateFlow(ModerationUiState())
    val state: StateFlow<ModerationUiState> = _state.asStateFlow()

    private val loaded = mutableSetOf<ModerationTab>()
    private var loadSeq = 0

    init {
        load(ModerationTab.FollowRequests)
    }

    fun selectTab(tab: ModerationTab) {
        if (tab == _state.value.tab) return
        _state.update { it.copy(tab = tab, error = null) }
        if (tab !in loaded) load(tab)
    }

    fun refresh() {
        load(_state.value.tab, force = true)
    }

    fun onDomainQueryChange(value: String) {
        _state.update { it.copy(domainQuery = value, error = null) }
    }

    fun authorize(account: Account) {
        runAction(
            key = account.id,
            request = { api.authorizeFollowRequest(account.id) },
            onSuccess = {
                _state.update {
                    it.copy(
                        followRequests = it.followRequests.filterNot { item ->
                            item.id == account.id
                        }
                    )
                }
            }
        )
    }

    fun reject(account: Account) {
        runAction(
            key = account.id,
            request = { api.rejectFollowRequest(account.id) },
            onSuccess = {
                _state.update {
                    it.copy(
                        followRequests = it.followRequests.filterNot { item ->
                            item.id == account.id
                        }
                    )
                }
            }
        )
    }

    fun unmute(account: Account) {
        runAction(
            key = account.id,
            request = { api.unmuteAccount(account.id) },
            onSuccess = {
                _state.update {
                    it.copy(mutes = it.mutes.filterNot { item -> item.id == account.id })
                }
            }
        )
    }

    fun unblockDomain(domain: String) {
        runAction(
            key = domain,
            request = { api.unblockDomain(domain) },
            onSuccess = {
                _state.update {
                    it.copy(domains = it.domains.filterNot { item -> item == domain })
                }
            }
        )
    }

    fun blockDomain() {
        val domain = _state.value.domainQuery.trim().lowercase()
        if (domain.isBlank() || domain in _state.value.busy) return
        runAction(
            key = domain,
            request = { api.blockDomain(domain) },
            onSuccess = {
                _state.update {
                    it.copy(
                        domains = (it.domains + domain).distinct().sorted(),
                        domainQuery = ""
                    )
                }
            }
        )
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    private fun load(tab: ModerationTab, force: Boolean = false) {
        if (!force && tab in loaded) return
        val seq = ++loadSeq
        val hasItems = _state.value.itemsCount > 0
        _state.update {
            it.copy(
                loading = !hasItems,
                refreshing = hasItems,
                error = null
            )
        }
        viewModelScope.launch {
            try {
                when (tab) {
                    ModerationTab.FollowRequests -> {
                        val items = api.followRequests(limit = 80).items
                        if (seq != loadSeq) return@launch
                        loaded += tab
                        _state.update {
                            it.copy(
                                followRequests = items,
                                loading = false,
                                refreshing = false
                            )
                        }
                    }
                    ModerationTab.Mutes -> {
                        val items = api.mutes(limit = 80).items
                        if (seq != loadSeq) return@launch
                        loaded += tab
                        _state.update {
                            it.copy(
                                mutes = items,
                                loading = false,
                                refreshing = false
                            )
                        }
                    }
                    ModerationTab.Domains -> {
                        val items = api.domainBlocks()
                        if (seq != loadSeq) return@launch
                        loaded += tab
                        _state.update {
                            it.copy(
                                domains = items.sorted(),
                                loading = false,
                                refreshing = false
                            )
                        }
                    }
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                if (seq != loadSeq) return@launch
                _state.update {
                    it.copy(
                        loading = false,
                        refreshing = false,
                        error = error.userMessage()
                    )
                }
            }
        }
    }

    private fun runAction(
        key: String,
        request: suspend () -> Unit,
        onSuccess: () -> Unit
    ) {
        if (key in _state.value.busy) return
        _state.update {
            it.copy(busy = it.busy + key, error = null)
        }
        viewModelScope.launch {
            runCatching { request() }
                .onSuccess { onSuccess() }
                .onFailure { error ->
                    _state.update { it.copy(error = error.userMessage()) }
                }
            _state.update { it.copy(busy = it.busy - key) }
        }
    }
}
