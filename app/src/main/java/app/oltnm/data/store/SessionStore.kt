package app.oltnm.data.store

import android.content.Context
import app.oltnm.data.api.JsonConfig
import app.oltnm.data.model.PendingLogin
import app.oltnm.data.model.Session
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SessionStore(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private val _session = MutableStateFlow(readSession())
    val session: StateFlow<Session?> = _session.asStateFlow()

    fun current(): Session? = _session.value

    fun saveSession(value: Session) {
        prefs.edit()
            .putString(KEY_SESSION, JsonConfig.json.encodeToString(Session.serializer(), value))
            .apply()
        _session.value = value
    }

    fun clear() {
        prefs.edit().remove(KEY_SESSION).apply()
        _session.value = null
    }

    fun savePending(value: PendingLogin) {
        prefs.edit()
            .putString(KEY_PENDING, JsonConfig.json.encodeToString(PendingLogin.serializer(), value))
            .apply()
    }

    fun pending(): PendingLogin? {
        val encoded = prefs.getString(KEY_PENDING, null) ?: return null
        return runCatching {
            JsonConfig.json.decodeFromString(PendingLogin.serializer(), encoded)
        }.getOrNull()
    }

    fun clearPending() {
        prefs.edit().remove(KEY_PENDING).apply()
    }

    private fun readSession(): Session? {
        val encoded = prefs.getString(KEY_SESSION, null) ?: return null
        return runCatching {
            JsonConfig.json.decodeFromString(Session.serializer(), encoded)
        }.getOrNull()
    }

    private companion object {
        const val PREFS = "oltnm.session"
        const val KEY_SESSION = "session"
        const val KEY_PENDING = "pending_login"
    }
}
