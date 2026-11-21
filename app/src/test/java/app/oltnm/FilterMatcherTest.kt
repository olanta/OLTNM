package app.oltnm

import app.oltnm.data.filters.FilterMatcher
import app.oltnm.data.model.Account
import app.oltnm.data.model.FilterKeyword
import app.oltnm.data.model.MastodonFilter
import app.oltnm.data.model.Status
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FilterMatcherTest {
    private val now = Instant.parse("2026-11-19T12:00:00Z")

    @Test
    fun hidesMatchingStatuses() {
        val status = status("1", "This contains spoilers")
        val filter = filter("hide", "spoilers")

        val result = FilterMatcher.apply(
            items = listOf(status),
            filters = listOf(filter),
            context = "home",
            now = now
        )

        assertTrue(result.items.isEmpty())
    }

    @Test
    fun warnsForWarnFilters() {
        val status = status("1", "This contains spoilers")
        val filter = filter("warn", "spoilers")

        val result = FilterMatcher.apply(
            items = listOf(status),
            filters = listOf(filter),
            context = "home",
            now = now
        )

        assertEquals(listOf(status), result.items)
        assertEquals("Test filter", result.warnings["1"])
    }

    @Test
    fun respectsContextExpiryAndWholeWord() {
        val status = status("1", "spoilers everywhere")
        val expired = filter("hide", "spoilers", expiresAt = "2020-01-01T00:00:00Z")
        val otherContext = filter("hide", "spoilers", context = "public")
        val partial = filter("hide", "spoil", wholeWord = true)

        assertFalse(
            FilterMatcher.apply(listOf(status), listOf(otherContext), "home", now = now)
                .items.isEmpty()
        )
        assertFalse(
            FilterMatcher.apply(listOf(status), listOf(expired), "home", now = now)
                .items.isEmpty()
        )
        assertFalse(
            FilterMatcher.apply(listOf(status), listOf(partial), "home", now = now)
                .items.isEmpty()
        )
    }

    @Test
    fun filtersBoostedContentAndKeepsPagination() {
        val boosted = Status(
            id = "wrapper",
            account = Account(id = "a"),
            reblog = status("original", "spoilers")
        )
        val filter = filter("hide", "spoilers")

        val result = FilterMatcher.apply(
            items = listOf(boosted),
            filters = listOf(filter),
            context = "home",
            nextMaxId = "42",
            now = now
        )

        assertTrue(result.items.isEmpty())
        assertEquals("42", result.nextMaxId)
    }

    private fun status(id: String, content: String) = Status(
        id = id,
        content = "<p>$content</p>",
        account = Account(id = "account")
    )

    private fun filter(
        action: String,
        keyword: String,
        context: String = "home",
        wholeWord: Boolean = false,
        expiresAt: String? = null
    ) = MastodonFilter(
        id = "filter-$keyword",
        title = "Test filter",
        context = listOf(context),
        filterAction = action,
        expiresAt = expiresAt,
        keywords = listOf(
            FilterKeyword(
                id = "keyword-$keyword",
                keyword = keyword,
                wholeWord = wholeWord
            )
        )
    )
}
