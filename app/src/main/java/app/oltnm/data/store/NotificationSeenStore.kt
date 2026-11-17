package app.oltnm.data.store

import android.content.Context

class NotificationSeenStore(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun lastSeen(sessionKey: String): String? = prefs.getString(sessionKey, null)

    fun markSeen(sessionKey: String, notificationId: String) {
        prefs.edit().putString(sessionKey, notificationId).apply()
    }

    fun clear(sessionKey: String) {
        prefs.edit().remove(sessionKey).apply()
    }

    private companion object {
        const val PREFS = "oltnm.notifications"
    }
}
