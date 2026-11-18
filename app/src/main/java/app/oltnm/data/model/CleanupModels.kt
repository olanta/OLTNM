package app.oltnm.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CleanupPolicy(
    val enabled: Boolean = false,
    @SerialName("min_status_age") val minStatusAgeSeconds: Long = 1_209_600L,
    val keepPinned: Boolean = true,
    val keepDirect: Boolean = true,
    val keepFavourited: Boolean = true,
    val keepBookmarked: Boolean = true,
    val keepPolls: Boolean = false,
    val keepMedia: Boolean = false,
    val minFavourites: Int = 0,
    val minBoosts: Int = 0
)

@Serializable
data class CleanupCursor(
    val maxId: String,
    val cutoff: Long,
    val floor: Long
)

@Serializable
data class CleanupState(
    val cursor: CleanupCursor? = null,
    @SerialName("inspected_through") val inspectedThrough: Long? = null,
    @SerialName("last_run") val lastRun: Long? = null,
    @SerialName("rate_limited_until") val rateLimitedUntil: Long? = null,
    @SerialName("resume_pending") val resumePending: Boolean = false
)

@Serializable
data class CleanupAccountState(
    val policy: CleanupPolicy = CleanupPolicy(),
    val state: CleanupState = CleanupState()
)
