package app.oltnm.data.store

import android.content.Context
import app.oltnm.data.api.JsonConfig
import app.oltnm.data.model.TimelineSpec
import app.oltnm.data.model.TimelineTypes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class TimelineStore(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private val _timelines = MutableStateFlow(loadTimelines())
    val timelines: StateFlow<List<TimelineSpec>> = _timelines.asStateFlow()

    private val _selectedId = MutableStateFlow(prefs.getString(KEY_SELECTED, null))
    val selectedId: StateFlow<String?> = _selectedId.asStateFlow()

    fun add(spec: TimelineSpec): Boolean {
        val current = _timelines.value
        if (current.size >= MAX_TIMELINES) return false
        if (current.any { sameTarget(it, spec) }) return false
        save(current + spec)
        select(spec.id)
        return true
    }

    fun remove(id: String) {
        val current = _timelines.value
        val next = current.filterNot { it.id == id }
        save(next)
        if (_selectedId.value == id) {
            _selectedId.value = next.firstOrNull()?.id
            prefs.edit().putString(KEY_SELECTED, _selectedId.value).apply()
        }
    }

    fun move(id: String, delta: Int) {
        val current = _timelines.value.toMutableList()
        val index = current.indexOfFirst { it.id == id }
        if (index < 0) return
        val target = (index + delta).coerceIn(0, current.lastIndex)
        if (target == index) return
        val item = current.removeAt(index)
        current.add(target, item)
        save(current)
    }

    fun rename(id: String, label: String) {
        val next = _timelines.value.map {
            if (it.id == id) it.copy(label = label.trim()) else it
        }
        save(next)
    }

    fun select(id: String) {
        _selectedId.value = id
        prefs.edit().putString(KEY_SELECTED, id).apply()
    }

    private fun save(list: List<TimelineSpec>) {
        prefs.edit()
            .putString(KEY_TIMELINES, JsonConfig.json.encodeToString(TIMELINE_LIST, list))
            .apply()
        _timelines.value = list
    }

    private fun loadTimelines(): List<TimelineSpec> {
        val encoded = prefs.getString(KEY_TIMELINES, null)
        if (encoded.isNullOrBlank()) return DEFAULTS
        return runCatching {
            JsonConfig.json.decodeFromString(TIMELINE_LIST, encoded)
        }.getOrDefault(DEFAULTS)
    }

    private fun sameTarget(a: TimelineSpec, b: TimelineSpec): Boolean = a.type == b.type && a.host == b.host && a.value == b.value

    private companion object {
        const val PREFS = "oltnm.timelines"
        const val KEY_TIMELINES = "timelines"
        const val KEY_SELECTED = "selected"
        const val MAX_TIMELINES = 10

        val TIMELINE_LIST =
            kotlinx.serialization.builtins.ListSerializer(TimelineSpec.serializer())

        val DEFAULTS = listOf(
            TimelineSpec(id = "home", type = TimelineTypes.HOME),
            TimelineSpec(id = "local", type = TimelineTypes.LOCAL),
            TimelineSpec(id = "federated", type = TimelineTypes.FEDERATED)
        )
    }
}
