package app.oltnm

import app.oltnm.data.api.JsonConfig
import app.oltnm.ui.screens.search.DiscoverLanguages
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DiscoverLanguagesTest {
    @Test
    fun hasUniqueCompleteLanguageEntries() {
        val ids = DiscoverLanguages.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
        assertTrue(ids.containsAll(listOf("pt", "es", "ja")))
        DiscoverLanguages.forEach { language ->
            assertTrue(language.label.isNotBlank())
            assertTrue(language.english.isNotBlank())
            assertTrue(language.query.isNotBlank())
            assertTrue(language.tags.isNotEmpty())
            assertTrue(language.keywords.isNotEmpty())
        }
    }

    @Test
    fun convertsLanguageToDiscoverCategory() {
        val language = DiscoverLanguages.first { it.id == "pt" }
        val category = language.toCategory()

        assertEquals(language.id, category.id)
        assertEquals(language.query, category.query)
        assertEquals(language.tags, category.tags)
        assertEquals(language.keywords, category.keywords)
    }

    @Test
    fun includesCuratedSeedHandles() {
        val file = File("src/main/res/raw/discover_seeds.json")
        assertTrue("Missing ${file.absolutePath}", file.exists())
        val seeds = JsonConfig.decode<Map<String, List<String>>>(file.readText())

        assertTrue((seeds["pt"]?.size ?: 0) > 1000)
        assertTrue((seeds["en"]?.size ?: 0) > 100)
        assertEquals(seeds["pt"]?.size, seeds["pt"]?.toSet()?.size)
    }
}
