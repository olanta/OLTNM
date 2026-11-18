package app.oltnm

import app.oltnm.data.cleanup.CLEANUP_MAX_DELETES
import app.oltnm.data.cleanup.CLEANUP_RATE_LIMIT_COOLDOWN_MILLIS
import app.oltnm.data.cleanup.CleanupResult
import app.oltnm.data.cleanup.CleanupRules
import app.oltnm.data.cleanup.DefaultCleanupPolicy
import app.oltnm.data.model.CleanupState
import app.oltnm.data.model.Status
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CleanupRulesTest {
    private val now = 1_700_000_000_000L
    private val day = 24 * 60 * 60 * 1000L
    private val policy = DefaultCleanupPolicy.copy(minStatusAgeSeconds = 14 * 24 * 60 * 60L)
    private val cutoff = now - policy.minStatusAgeSeconds * 1000L

    @Test
    fun deletesOnlyOldIneligiblePosts() {
        assertTrue(CleanupRules.shouldDelete(status(20), policy, cutoff))
        assertFalse(CleanupRules.shouldDelete(status(2), policy, cutoff))
    }

    @Test
    fun keepsConfiguredExceptions() {
        assertFalse(CleanupRules.shouldDelete(status(20, pinned = true), policy, cutoff))
        assertFalse(CleanupRules.shouldDelete(status(20, visibility = "direct"), policy, cutoff))
        assertFalse(CleanupRules.shouldDelete(status(20, favourited = true), policy, cutoff))
        assertFalse(CleanupRules.shouldDelete(status(20, bookmarked = true), policy, cutoff))
    }

    @Test
    fun keepsPostsAboveInteractionThresholds() {
        val next = policy.copy(minFavourites = 5, minBoosts = 2)
        assertFalse(CleanupRules.shouldDelete(status(20, favourites = 5), next, cutoff))
        assertTrue(CleanupRules.shouldDelete(status(20, favourites = 4), next, cutoff))
        assertFalse(CleanupRules.shouldDelete(status(20, boosts = 2), next, cutoff))
        assertTrue(CleanupRules.shouldDelete(status(20, boosts = 1), next, cutoff))
    }

    @Test
    fun dueRequiresEnabledPolicyAndInterval() {
        assertFalse(CleanupRules.due(policy.copy(enabled = false), CleanupState(), now))
        assertTrue(CleanupRules.due(policy.copy(enabled = true), CleanupState(), now))
        assertFalse(
            CleanupRules.due(
                policy.copy(enabled = true),
                CleanupState(lastRun = now - 1000),
                now
            )
        )
        assertTrue(
            CleanupRules.due(
                policy.copy(enabled = true),
                CleanupState(lastRun = now - day),
                now
            )
        )
    }

    @Test
    fun cooldownCoversFullBatchesAndRateLimits() {
        val full = CleanupResult(deleted = CLEANUP_MAX_DELETES)
        assertTrue(CleanupRules.cooldownUntil(full, now) >= now + CLEANUP_RATE_LIMIT_COOLDOWN_MILLIS)
        assertTrue(CleanupRules.cooldownActive(now + 1000, now))
        assertFalse(CleanupRules.cooldownActive(now - 1000, now))
    }

    @Test
    fun wideningPolicyInvalidatesTheCursor() {
        assertTrue(CleanupRules.policyWidened(policy, policy.copy(keepPinned = false)))
        assertTrue(
            CleanupRules.policyWidened(
                policy.copy(minFavourites = 5),
                policy.copy(minFavourites = 10)
            )
        )
        assertFalse(CleanupRules.policyWidened(policy, policy.copy(keepPinned = true)))
    }

    private fun status(
        ageDays: Int,
        pinned: Boolean = false,
        visibility: String = "public",
        favourited: Boolean = false,
        bookmarked: Boolean = false,
        favourites: Int = 0,
        boosts: Int = 0
    ): Status = Status(
        createdAt = java.time.Instant.ofEpochMilli(now - ageDays * day).toString(),
        pinned = pinned,
        visibility = visibility,
        favourited = favourited,
        bookmarked = bookmarked,
        favouritesCount = favourites,
        reblogsCount = boosts
    )
}
