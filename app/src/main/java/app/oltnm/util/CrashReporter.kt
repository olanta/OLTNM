package app.oltnm.util

import android.content.Context
import android.util.Log
import app.oltnm.BuildConfig
import java.io.File
import java.time.Instant

object CrashReporter {
    private const val DIRECTORY = "crashes"
    private const val MAX_REPORTS = 10
    private const val TAG = "OLTNM"

    @Volatile
    private var installed = false

    fun install(context: Context) {
        if (installed) return
        installed = true
        val appContext = context.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            runCatching { write(appContext, thread, error) }
                .onFailure { Log.e(TAG, "Could not write crash report", it) }
            previous?.uncaughtException(thread, error)
        }
    }

    fun reports(context: Context): List<File> {
        val directory = File(context.applicationContext.filesDir, DIRECTORY)
        return directory.listFiles()
            .orEmpty()
            .filter { it.isFile }
            .sortedByDescending { it.lastModified() }
    }

    fun readAll(context: Context): String = reports(context)
        .map { file ->
            val body = runCatching { file.readText() }.getOrDefault("")
            "== ${file.name} ==\n${LogRedactor.redactText(body)}"
        }
        .joinToString("\n\n")

    fun clear(context: Context) {
        reports(context).forEach { it.delete() }
    }

    private fun write(context: Context, thread: Thread, error: Throwable) {
        val directory = File(context.filesDir, DIRECTORY).apply { mkdirs() }
        val file = File(directory, "crash-${System.currentTimeMillis()}.log")
        val report = buildString {
            appendLine("time=${Instant.now()}")
            appendLine("version=${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
            appendLine("thread=${thread.name}")
            appendLine("exception=${error.javaClass.name}")
            appendLine(LogRedactor.redactText(Log.getStackTraceString(error)))
        }
        file.writeText(report)
        directory.listFiles()
            .orEmpty()
            .sortedByDescending { it.lastModified() }
            .drop(MAX_REPORTS)
            .forEach { it.delete() }
    }
}
