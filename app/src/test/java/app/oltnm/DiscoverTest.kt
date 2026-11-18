package app.oltnm

import app.oltnm.data.model.Account
import app.oltnm.data.model.Status
import app.oltnm.ui.screens.search.DiscoverCandidate
import app.oltnm.ui.screens.search.DiscoverCategories
import app.oltnm.ui.screens.search.rankDiscoverCandidates
import app.oltnm.ui.screens.search.scoreDiscoverCandidate
import app.oltnm.ui.screens.search.statusRecency
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DiscoverTest {
    private val now = 1_700_000_000_000L
    private val art = DiscoverCategories.first()

    @Test
    fun categoriesHaveUniqueIdsAndSources() {
        assertEquals(DiscoverCategories.size, DiscoverCategories.map { it.id }.toSet().size)
        DiscoverCategories.forEach { category ->
            assertTrue(category.tags.isNotEmpty())
            assertTrue(category.keywords.isNotEmpty())
            assertTrue(category.query.isNotBlank())
        }
    }

    @Test
    fun relevantAccountsRankHigher() {
        val relevant = DiscoverCandidate(
            account = account("artist", "artist illustration design"),
            tagHits = 4,
            searchHits = 1,
            lastSeen = now - 2 * DAY
        )
        val plain = DiscoverCandidate(account("plain", ""))
        val ranked = rankDiscoverCandidates(listOf(plain, relevant), art, now)

        assertEquals("artist", ranked.first().id)
    }

    @Test
    fun scoresRecencyAndKeywordMatches() {
        val candidate = DiscoverCandidate(
            account = account("artist", "I paint and draw"),
            tagHits = 2,
            searchHits = 1,
            lastSeen = now
        )
        val older = candidate.copy(lastSeen = now - 45 * DAY)

        assertTrue(scoreDiscoverCandidate(candidate, art, now) > scoreDiscoverCandidate(older, art, now))
    }

    @Test
    fun parsesStatusTimestamps() {
        val status = Status(createdAt = "2023-11-14T22:13:20Z")
        assertEquals(1_700_000_000_000L, statusRecency(status.createdAt))
    }

    private fun account(id: String, note: String): Account = Account(
        id = id,
        username = id,
        acct = "$id@example.social",
        displayName = id,
        note = note,
        followersCount = 10,
        statusesCount = 10
    )

    private companion object {
        const val DAY = 24 * 60 * 60 * 1000L
    }
}
