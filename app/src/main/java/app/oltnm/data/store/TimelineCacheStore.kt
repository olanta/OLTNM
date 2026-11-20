package app.oltnm.data.store

import android.content.Context
import app.oltnm.data.api.JsonConfig
import app.oltnm.data.model.Status
import java.io.File
import java.security.MessageDigest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

@Serializable
data class CachedTimelinePage(
    val items: List<Status> = emptyList(),
    val nextMaxId: String? = null
)

class TimelineCacheStore internal constructor(private val directory: File) {
    constructor(context: Context) : this(File(context.filesDir, "timeline-cache"))

    suspend fun read(key: String): CachedTimelinePage? = withContext(Dispatchers.IO) {
        val file = fileFor(key)
        if (!file.exists()) return@withContext null
        try {
            JsonConfig.json.decodeFromString(CachedTimelinePage.serializer(), file.readText())
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            null
        }
    }

    suspend fun write(key: String, items: List<Status>, nextMaxId: String?) = withContext(Dispatchers.IO) {
        if (items.isEmpty()) return@withContext
        try {
            directory.mkdirs()
            val target = fileFor(key)
            val temp = File(directory, "${target.name}.tmp")
            temp.writeText(
                JsonConfig.json.encodeToString(
                    CachedTimelinePage.serializer(),
                    CachedTimelinePage(items = items, nextMaxId = nextMaxId)
                )
            )
            temp.copyTo(target, overwrite = true)
            temp.delete()
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            Unit
        }
    }

    private fun fileFor(key: String): File {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(key.toByteArray())
            .joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }
        return File(directory, "$digest.json")
    }
}
