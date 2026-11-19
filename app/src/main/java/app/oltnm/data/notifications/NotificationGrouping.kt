package app.oltnm.data.notifications

import app.oltnm.data.model.Notification
import app.oltnm.data.model.Status

sealed interface NotificationEntry {
    val key: String

    data class Single(val notification: Notification) : NotificationEntry {
        override val key: String = "single-${notification.id}"
    }

    data class Group(
        val type: String,
        val status: Status?,
        val notifications: List<Notification>
    ) : NotificationEntry {
        override val key: String =
            "group-$type-${status?.id ?: notifications.firstOrNull()?.id.orEmpty()}"
    }
}

object NotificationGrouping {
    fun group(notifications: List<Notification>): List<NotificationEntry> {
        val grouped = linkedMapOf<String, MutableList<Notification>>()
        val order = mutableListOf<String>()

        notifications.forEach { notification ->
            val key = groupKey(notification)
            if (key == null) {
                order += "single-${notification.id}"
            } else {
                if (key !in grouped) {
                    grouped[key] = mutableListOf()
                    order += key
                }
                grouped.getValue(key) += notification
            }
        }

        return order.mapNotNull { key ->
            if (key.startsWith("single-")) {
                val id = key.removePrefix("single-")
                notifications.firstOrNull { it.id == id }?.let { NotificationEntry.Single(it) }
            } else {
                val items = grouped[key].orEmpty()
                when {
                    items.isEmpty() -> null
                    items.size == 1 -> NotificationEntry.Single(items.first())
                    else -> NotificationEntry.Group(
                        type = items.first().type,
                        status = items.first().status,
                        notifications = items
                    )
                }
            }
        }
    }

    private fun groupKey(notification: Notification): String? {
        if (notification.type == "follow") return "follow"
        val statusId = notification.status?.id?.takeIf { it.isNotBlank() } ?: return null
        return when (notification.type) {
            "favourite", "reblog" -> "${notification.type}:$statusId"
            else -> null
        }
    }
}
