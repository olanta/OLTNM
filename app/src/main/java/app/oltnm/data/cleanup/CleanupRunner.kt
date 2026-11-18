package app.oltnm.data.cleanup

import app.oltnm.data.api.MastodonApi
import app.oltnm.data.api.MastodonApiException
import app.oltnm.data.model.CleanupPolicy
import app.oltnm.data.model.CleanupState
import app.oltnm.data.model.Status
import java.time.Instant
import kotlinx.coroutines.delay

object CleanupRunner {
    private const val PAGE_SIZE = 40
    private const val MAX_PAGES = 50
    private const val DELETE_DELAY_MS = 900L
    private const val PAGE_DELAY_MS = 250L

    suspend fun run(
        api: MastodonApi,
        accountId: String,
        policy: CleanupPolicy,
        state: CleanupState,
        now: Long = System.currentTimeMillis(),
        maxDeletes: Int = CLEANUP_MAX_DELETES
    ): CleanupResult {
        val storedCursor = state.cursor
        val completedThrough = state.inspectedThrough ?: 0L
        val floor = storedCursor?.floor ?: completedThrough
        val passCutoff = storedCursor?.cutoff
            ?: (now - policy.minStatusAgeSeconds * 1000L)
        var scanned = 0
        var deleted = 0
        var failed = 0
        var kept = 0
        var stopped = false
        var rateLimited = false

        if (passCutoff <= floor) {
            return CleanupResult(inspectedThrough = floor)
        }

        var maxId = storedCursor?.maxId
        var lastInspectedId: String? = null
        var completed = false

        for (page in 0 until MAX_PAGES) {
            val statuses = api.accountStatuses(
                accountId = accountId,
                maxId = maxId,
                limit = PAGE_SIZE,
                excludeReplies = false
            ).items
            if (statuses.isEmpty()) {
                completed = true
                break
            }

            for (status in statuses) {
                if (deleted >= maxDeletes) {
                    stopped = true
                    break
                }
                val createdAt = parseCreatedAt(status)
                if (createdAt != null && createdAt < floor) {
                    completed = true
                    break
                }
                if (createdAt == null || createdAt > passCutoff) {
                    lastInspectedId = status.id
                    continue
                }

                scanned += 1
                if (!CleanupRules.shouldDelete(status, policy, passCutoff)) {
                    kept += 1
                    lastInspectedId = status.id
                    continue
                }

                try {
                    api.deleteStatus(status.id)
                    deleted += 1
                    lastInspectedId = status.id
                    delay(DELETE_DELAY_MS)
                } catch (error: MastodonApiException) {
                    if (error.statusCode == 401 || error.statusCode == 403) throw error
                    if (error.statusCode == 429) {
                        stopped = true
                        rateLimited = true
                        failed += 1
                        return finish(
                            scanned,
                            deleted,
                            failed,
                            kept,
                            stopped,
                            rateLimited,
                            completedThrough,
                            floor,
                            passCutoff,
                            lastInspectedId,
                            maxId
                        )
                    }
                    failed += 1
                    lastInspectedId = status.id
                } catch (error: Exception) {
                    failed += 1
                    lastInspectedId = status.id
                }
            }

            if (completed || stopped) break
            if (statuses.size < PAGE_SIZE) {
                completed = true
                break
            }
            val last = statuses.lastOrNull()
            if (last == null) {
                completed = true
                break
            }
            maxId = last.id
            if (page == MAX_PAGES - 1) {
                stopped = true
                break
            }
            delay(PAGE_DELAY_MS)
        }

        return finish(
            scanned,
            deleted,
            failed,
            kept,
            stopped,
            rateLimited,
            completedThrough,
            floor,
            passCutoff,
            lastInspectedId,
            maxId
        )
    }

    private fun finish(
        scanned: Int,
        deleted: Int,
        failed: Int,
        kept: Int,
        stopped: Boolean,
        rateLimited: Boolean,
        completedThrough: Long,
        floor: Long,
        passCutoff: Long,
        lastInspectedId: String?,
        maxId: String?
    ): CleanupResult {
        if (!stopped) {
            return CleanupResult(
                scanned = scanned,
                deleted = deleted,
                failed = failed,
                kept = kept,
                stopped = false,
                rateLimited = false,
                cursor = null,
                inspectedThrough = maxOf(floor, passCutoff)
            )
        }
        val resumeId = lastInspectedId ?: maxId
        return CleanupResult(
            scanned = scanned,
            deleted = deleted,
            failed = failed,
            kept = kept,
            stopped = true,
            rateLimited = rateLimited,
            cursor = resumeId?.let {
                app.oltnm.data.model.CleanupCursor(
                    maxId = it,
                    cutoff = passCutoff,
                    floor = floor
                )
            },
            inspectedThrough = completedThrough.takeIf { it > 0 }
        )
    }

    private fun parseCreatedAt(status: Status): Long? = runCatching { Instant.parse(status.createdAt).toEpochMilli() }.getOrNull()
}
