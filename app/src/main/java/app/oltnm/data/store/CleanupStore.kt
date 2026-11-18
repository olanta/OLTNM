package app.oltnm.data.store

import android.content.Context
import app.oltnm.data.api.JsonConfig
import app.oltnm.data.model.CleanupAccountState
import app.oltnm.data.model.CleanupPolicy
import app.oltnm.data.model.CleanupState

class CleanupStore(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun load(sessionKey: String): CleanupAccountState {
        val encoded = prefs.getString(sessionKey, null) ?: return CleanupAccountState()
        return runCatching {
            JsonConfig.json.decodeFromString(CleanupAccountState.serializer(), encoded)
        }.getOrDefault(CleanupAccountState())
    }

    fun savePolicy(sessionKey: String, policy: CleanupPolicy) {
        val current = load(sessionKey)
        save(sessionKey, current.copy(policy = policy))
    }

    fun saveState(sessionKey: String, state: CleanupState) {
        val current = load(sessionKey)
        save(sessionKey, current.copy(state = state))
    }

    private fun save(sessionKey: String, value: CleanupAccountState) {
        prefs.edit()
            .putString(
                sessionKey,
                JsonConfig.json.encodeToString(CleanupAccountState.serializer(), value)
            )
            .apply()
    }

    private companion object {
        const val PREFS = "oltnm.cleanup"
    }
}
