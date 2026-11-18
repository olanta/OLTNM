package app.oltnm.data.cleanup

import app.oltnm.data.model.CleanupCursor
import app.oltnm.data.model.CleanupPolicy
import app.oltnm.data.model.CleanupState
import app.oltnm.data.model.Status
import java.time.Instant

val CleanupMinAgeSeconds = listOf(
    604_800L,
    1_209_600L,
    2_629_746L,
    5_259_492L,
    7_889_238L,
    15_778_476L,
    31_556_952L,
    63_113_904L
)

val DefaultCleanupPolicy = CleanupPolicy()

const val CLEANUP_INTERVAL_MILLIS = 24L * 60L * 60L * 1000L
const val CLEANUP_MAX_DELETES = 30
const val CLEANUP_RATE_LIMIT_COOLDOWN_MILLIS = 30L * 60L * 1000L

data class CleanupResult(
    val scanned: Int = 0,
    val deleted: Int = 0,
    val failed: Int = 0,
    val kept: Int = 0,
    val stopped: Boolean = false,
    val rateLimited: Boolean = false,
    val cursor: CleanupCursor? = null,
    val inspectedThrough: Long? = null
)

object CleanupRules {
    fun shouldDelete(status: Status, policy: CleanupPolicy, cutoffMillis: Long): Boolean {
        val createdAt = runCatching { Instant.parse(status.createdAt).toEpochMilli() }.getOrNull()
            ?: return false
        if (createdAt >= cutoffMillis) return false
        if (policy.keepPinned && status.pinned) return false
        if (policy.keepDirect && status.visibility == "direct") return false
        if (policy.keepFavourited && status.favourited) return false
        if (policy.keepBookmarked && status.bookmarked) return false
        if (policy.keepPolls && status.poll != null) return false
        if (policy.keepMedia && status.mediaAttachments.isNotEmpty()) return false
        if (policy.minFavourites > 0 && status.favouritesCount >= policy.minFavourites) return false
        if (policy.minBoosts > 0 && status.reblogsCount >= policy.minBoosts) return false
        return true
    }

    fun due(policy: CleanupPolicy, state: CleanupState, now: Long = System.currentTimeMillis()): Boolean {
        if (!policy.enabled) return false
        if (cooldownActive(state.rateLimitedUntil, now)) return false
        if (state.resumePending || state.cursor != null) return true
        val lastRun = state.lastRun ?: return true
        return now - lastRun >= CLEANUP_INTERVAL_MILLIS
    }

    fun cooldownActive(until: Long?, now: Long = System.currentTimeMillis()): Boolean = until != null && now < until

    fun cooldownUntil(result: CleanupResult, now: Long = System.currentTimeMillis()): Long = if (result.rateLimited || result.deleted >= CLEANUP_MAX_DELETES) {
        now + CLEANUP_RATE_LIMIT_COOLDOWN_MILLIS
    } else {
        0L
    }

    fun policyWidened(previous: CleanupPolicy, next: CleanupPolicy): Boolean {
        val keepFlags = listOf(
            previous.keepPinned to next.keepPinned,
            previous.keepDirect to next.keepDirect,
            previous.keepFavourited to next.keepFavourited,
            previous.keepBookmarked to next.keepBookmarked,
            previous.keepPolls to next.keepPolls,
            previous.keepMedia to next.keepMedia
        )
        if (keepFlags.any { (before, after) -> before && !after }) return true
        fun widened(before: Int, after: Int): Boolean = before > 0 && (after == 0 || after > before)
        return widened(previous.minFavourites, next.minFavourites) ||
            widened(previous.minBoosts, next.minBoosts)
    }
}
