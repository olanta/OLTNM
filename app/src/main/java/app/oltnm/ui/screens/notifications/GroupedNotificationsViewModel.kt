package app.oltnm.ui.screens.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.oltnm.data.api.MastodonApi
import app.oltnm.data.api.Page
import app.oltnm.data.filters.FilterMatcher
import app.oltnm.data.filters.FilterStore
import app.oltnm.data.model.GroupedNotification
import app.oltnm.data.model.NotificationGroup
import app.oltnm.data.model.Status
import app.oltnm.data.notifications.NotificationEntry
import app.oltnm.data.notifications.NotificationGrouping
import app.oltnm.util.userMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GroupedNotificationsUiState(
    val items: List<GroupedNotification> = emptyList(),
    val loading: Boolean = false,
    val refreshing: Boolean = false,
    val loadingMore: Boolean = false,
    val nextMaxId: String? = null,
    val error: String? = null,
    val fallback: Boolean = false
)

class GroupedNotificationsViewModel(
    private val api: MastodonApi,
    private val filterStore: FilterStore? = null
) : ViewModel() {
    private val _state = MutableStateFlow(GroupedNotificationsUiState())
    val state: StateFlow<GroupedNotificationsUiState> = _state.asStateFlow()

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
            runCatching { fetch(null) }
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
                        it.copy(
                            loading = false,
                            refreshing = false,
                            error = error.userMessage()
                        )
                    }
                }
        }
    }

    fun loadMore() {
        val snapshot = _state.value
        if (snapshot.loadingMore || snapshot.refreshing || snapshot.nextMaxId == null) return
        viewModelScope.launch {
            _state.update { it.copy(loadingMore = true, error = null) }
            runCatching { fetch(snapshot.nextMaxId) }
                .onSuccess { page ->
                    _state.update {
                        it.copy(
                            items = (it.items + page.items)
                                .distinctBy { item -> item.group.groupKey },
                            nextMaxId = page.nextMaxId,
                            loadingMore = false
                        )
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(loadingMore = false, error = error.userMessage())
                    }
                }
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    private suspend fun fetch(maxId: String?): Page<GroupedNotification> {
        filterStore?.ensureLoaded(api)
        val page = if (!_state.value.fallback) {
            val grouped = runCatching { api.groupedNotifications(maxId = maxId) }
            if (grouped.isSuccess) {
                grouped.getOrThrow()
            } else {
                _state.update { it.copy(fallback = true) }
                fallbackPage(maxId)
            }
        } else {
            fallbackPage(maxId)
        }
        return Page(
            items = page.items.filterNot { item ->
                item.status?.let(::isHidden) == true
            },
            nextMaxId = page.nextMaxId
        )
    }

    private suspend fun fallbackPage(maxId: String?): Page<GroupedNotification> {
        val page = api.notifications(maxId = maxId)
        return Page(
            items = NotificationGrouping.group(page.items).map {
                it.toGroupedNotification()
            },
            nextMaxId = page.nextMaxId
        )
    }

    private fun isHidden(status: Status): Boolean = FilterMatcher.isHidden(
        status = status,
        filters = filterStore?.filters?.value.orEmpty(),
        context = "notifications"
    )
}

private fun NotificationEntry.toGroupedNotification(): GroupedNotification = when (this) {
    is NotificationEntry.Single -> {
        val item = notification
        GroupedNotification(
            group = NotificationGroup(
                groupKey = "converted-${item.id}",
                notificationsCount = 1,
                type = item.type,
                mostRecentNotificationId = item.id,
                pageMinId = item.id,
                pageMaxId = item.id,
                latestPageNotificationAt = item.createdAt,
                sampleAccountIds = listOf(item.account.id),
                statusId = item.status?.id
            ),
            accounts = listOf(item.account),
            status = item.status
        )
    }
    is NotificationEntry.Group -> GroupedNotification(
        group = NotificationGroup(
            groupKey = "converted-$key",
            notificationsCount = notifications.size,
            type = type,
            mostRecentNotificationId = notifications.first().id,
            pageMinId = notifications.last().id,
            pageMaxId = notifications.first().id,
            latestPageNotificationAt = notifications.first().createdAt,
            sampleAccountIds = notifications.map { it.account.id },
            statusId = status?.id
        ),
        accounts = notifications.map { it.account }.distinctBy { it.id },
        status = status
    )
}
