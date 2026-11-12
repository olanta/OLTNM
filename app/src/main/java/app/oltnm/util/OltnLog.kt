package app.oltnm.util

import android.util.Log
import app.oltnm.BuildConfig
import java.util.ArrayDeque

object OltnLog {
    private const val TAG = "OLTNM"
    private const val MAX_ENTRIES = 300
    private const val MAX_ENTRY_LENGTH = 2000

    private val entries = ArrayDeque<String>()
    private val lock = Any()

    fun d(message: String) {
        if (BuildConfig.DEBUG) log(Log.DEBUG, "D", message, null)
    }

    fun i(message: String) {
        log(Log.INFO, "I", message, null)
    }

    fun w(message: String, error: Throwable? = null) {
        log(Log.WARN, "W", message, error)
    }

    fun e(message: String, error: Throwable? = null) {
        log(Log.ERROR, "E", message, error)
    }

    fun recent(): List<String> = synchronized(lock) { entries.toList() }

    fun dump(): String = recent().joinToString("\n")

    fun clear() {
        synchronized(lock) { entries.clear() }
    }

    private fun log(priority: Int, level: String, message: String, error: Throwable?) {
        val safeMessage = LogRedactor.redactText(message).take(MAX_ENTRY_LENGTH)
        val stack = error
            ?.let { LogRedactor.redactText(Log.getStackTraceString(it)) }
            ?.take(MAX_ENTRY_LENGTH)
        val full = listOfNotNull(safeMessage, stack).joinToString("\n")
        Log.println(priority, TAG, full)
        synchronized(lock) {
            entries.addLast("${System.currentTimeMillis()} $level $full")
            while (entries.size > MAX_ENTRIES) entries.removeFirst()
        }
    }
}
