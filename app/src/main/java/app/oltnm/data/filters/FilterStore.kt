package app.oltnm.data.filters

import app.oltnm.data.api.MastodonApi
import app.oltnm.data.model.MastodonFilter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class FilterStore {
    private val _filters = MutableStateFlow<List<MastodonFilter>>(emptyList())
    val filters: StateFlow<List<MastodonFilter>> = _filters.asStateFlow()

    private val mutex = Mutex()
    private var loaded = false

    suspend fun ensureLoaded(api: MastodonApi) {
        if (loaded) return
        mutex.withLock {
            if (loaded) return
            load(api)
        }
    }

    suspend fun refresh(api: MastodonApi) {
        mutex.withLock {
            load(api)
        }
    }

    fun set(filters: List<MastodonFilter>) {
        _filters.value = filters
        loaded = true
    }

    fun add(filter: MastodonFilter) {
        _filters.value = listOf(filter) + _filters.value
        loaded = true
    }

    fun remove(id: String) {
        _filters.value = _filters.value.filterNot { it.id == id }
    }

    private suspend fun load(api: MastodonApi) {
        runCatching { api.filters() }
            .onSuccess {
                _filters.value = it
                loaded = true
            }
    }
}
