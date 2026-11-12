package app.oltnm.data.store

import android.content.Context
import app.oltnm.data.api.JsonConfig
import app.oltnm.data.model.PendingLogin
import app.oltnm.data.model.Session
import app.oltnm.util.OltnLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.builtins.ListSerializer

class SessionStore(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private val _sessions = MutableStateFlow(readSessions())
    val sessions: StateFlow<List<Session>> = _sessions.asStateFlow()

    private val _session = MutableStateFlow(readCurrent(_sessions.value))
    val session: StateFlow<Session?> = _session.asStateFlow()

    fun current(): Session? = _session.value

    fun saveSession(value: Session) {
        val next = _sessions.value.filterNot { it.key == value.key } + value
        persist(next)
        _sessions.value = next
        OltnLog.i("Account added: ${value.handle}")
        selectSession(value.key)
    }

    fun selectSession(key: String) {
        val target = _sessions.value.firstOrNull { it.key == key } ?: return
        prefs.edit().putString(KEY_CURRENT, key).apply()
        _session.value = target
        OltnLog.d("Account selected: ${target.handle}")
    }

    fun removeSession(key: String) {
        val removed = _sessions.value.firstOrNull { it.key == key }
        val next = _sessions.value.filterNot { it.key == key }
        persist(next)
        _sessions.value = next
        removed?.let { OltnLog.i("Account removed: ${it.handle}") }
        if (_session.value?.key == key) {
            val replacement = next.firstOrNull()
            if (replacement == null) {
                prefs.edit().remove(KEY_CURRENT).apply()
                _session.value = null
            } else {
                selectSession(replacement.key)
            }
        }
    }

    fun removeCurrent() {
        _session.value?.let { removeSession(it.key) }
    }

    fun clear() {
        prefs.edit()
            .remove(KEY_SESSIONS)
            .remove(KEY_CURRENT)
            .remove(KEY_SESSION)
            .apply()
        _sessions.value = emptyList()
        _session.value = null
        OltnLog.i("All accounts removed")
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

    private fun readSessions(): List<Session> {
        val encoded = prefs.getString(KEY_SESSIONS, null)
        if (encoded != null) {
            return runCatching {
                JsonConfig.json.decodeFromString(SESSION_LIST, encoded)
            }.getOrDefault(emptyList())
        }
        val legacyEncoded = prefs.getString(KEY_SESSION, null) ?: return emptyList()
        val legacy = runCatching {
            JsonConfig.json.decodeFromString(Session.serializer(), legacyEncoded)
        }.getOrNull() ?: return emptyList()
        val migrated = listOf(legacy)
        prefs.edit()
            .putString(KEY_SESSIONS, JsonConfig.json.encodeToString(SESSION_LIST, migrated))
            .putString(KEY_CURRENT, legacy.key)
            .remove(KEY_SESSION)
            .apply()
        return migrated
    }

    private fun readCurrent(sessions: List<Session>): Session? {
        if (sessions.isEmpty()) return null
        val key = prefs.getString(KEY_CURRENT, null)
        return sessions.firstOrNull { it.key == key } ?: sessions.first()
    }

    private fun persist(list: List<Session>) {
        prefs.edit()
            .putString(KEY_SESSIONS, JsonConfig.json.encodeToString(SESSION_LIST, list))
            .apply()
    }

    private companion object {
        const val PREFS = "oltnm.session"
        const val KEY_SESSION = "session"
        const val KEY_SESSIONS = "sessions"
        const val KEY_CURRENT = "current"
        const val KEY_PENDING = "pending_login"
        val SESSION_LIST = ListSerializer(Session.serializer())
    }
}
