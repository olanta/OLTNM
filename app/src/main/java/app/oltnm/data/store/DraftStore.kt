package app.oltnm.data.store

import android.content.Context
import app.oltnm.data.api.JsonConfig
import app.oltnm.data.model.Visibility
import kotlinx.serialization.Serializable

@Serializable
data class ComposeDraftData(
    val text: String = "",
    val spoiler: String = "",
    val spoilerEnabled: Boolean = false,
    val visibility: String = Visibility.Public.name
)

class DraftStore(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun load(): ComposeDraftData? {
        val encoded = prefs.getString(KEY, null) ?: return null
        return runCatching {
            JsonConfig.json.decodeFromString(ComposeDraftData.serializer(), encoded)
        }.getOrNull()
    }

    fun save(draft: ComposeDraftData) {
        prefs.edit()
            .putString(
                KEY,
                JsonConfig.json.encodeToString(ComposeDraftData.serializer(), draft)
            )
            .apply()
    }

    fun clear() {
        prefs.edit().remove(KEY).apply()
    }

    private companion object {
        const val PREFS = "oltnm.draft"
        const val KEY = "compose"
    }
}
