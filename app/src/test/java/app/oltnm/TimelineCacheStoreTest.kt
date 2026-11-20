package app.oltnm

import app.oltnm.data.model.Status
import app.oltnm.data.store.TimelineCacheStore
import java.nio.file.Files
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TimelineCacheStoreTest {
    @Test
    fun writesAndReadsTimelinePage() = runBlocking {
        val directory = Files.createTempDirectory("oltnm-timeline-cache").toFile()
        try {
            val store = TimelineCacheStore(directory)
            val status = Status(
                id = "1",
                content = "<p>Hello</p>",
                createdAt = "2026-11-19T12:00:00Z"
            )

            store.write("account|home|", listOf(status), "20")

            val page = store.read("account|home|")
            assertEquals(listOf(status), page?.items)
            assertEquals("20", page?.nextMaxId)
            assertTrue(directory.listFiles().orEmpty().isNotEmpty())
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun keepsKeysSeparateAndIgnoresCorruptFiles() = runBlocking {
        val directory = Files.createTempDirectory("oltnm-timeline-cache").toFile()
        try {
            val store = TimelineCacheStore(directory)
            store.write("one", listOf(Status(id = "1")), null)
            store.write("two", listOf(Status(id = "2")), null)

            assertEquals("1", store.read("one")?.items?.single()?.id)
            assertEquals("2", store.read("two")?.items?.single()?.id)

            directory.listFiles().orEmpty().forEach { it.writeText("not json") }
            assertTrue(store.read("one") == null)
        } finally {
            directory.deleteRecursively()
        }
    }
}
