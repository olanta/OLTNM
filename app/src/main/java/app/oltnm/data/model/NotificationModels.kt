package app.oltnm.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NotificationGroup(
    @SerialName("group_key") val groupKey: String = "",
    @SerialName("notifications_count") val notificationsCount: Int = 1,
    val type: String = "",
    @SerialName("most_recent_notification_id") val mostRecentNotificationId: String = "",
    @SerialName("page_min_id") val pageMinId: String? = null,
    @SerialName("page_max_id") val pageMaxId: String? = null,
    @SerialName("latest_page_notification_at") val latestPageNotificationAt: String? = null,
    @SerialName("sample_account_ids") val sampleAccountIds: List<String> = emptyList(),
    @SerialName("status_id") val statusId: String? = null
)

@Serializable
data class GroupedNotificationsResult(
    val accounts: List<Account> = emptyList(),
    val statuses: List<Status> = emptyList(),
    @SerialName("notification_groups") val notificationGroups: List<NotificationGroup> = emptyList()
)

data class GroupedNotification(
    val group: NotificationGroup,
    val accounts: List<Account>,
    val status: Status?
)
