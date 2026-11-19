package app.oltnm

import app.oltnm.data.model.Notification
import app.oltnm.data.model.Status
import app.oltnm.data.notifications.NotificationEntry
import app.oltnm.data.notifications.NotificationGrouping
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationGroupingTest {
    @Test
    fun groupsLikesAndBoostsByStatusSeparately() {
        val entries = NotificationGrouping.group(
            listOf(
                notification("1", "favourite", "status-a"),
                notification("2", "reblog", "status-a"),
                notification("3", "favourite", "status-a"),
                notification("4", "reblog", "status-a"),
                notification("5", "favourite", "status-b")
            )
        )

        val groups = entries.filterIsInstance<NotificationEntry.Group>()
        assertEquals(2, groups.size)
        assertEquals("favourite", groups[0].type)
        assertEquals(2, groups[0].notifications.size)
        assertEquals("reblog", groups[1].type)
        assertEquals(2, groups[1].notifications.size)
        assertTrue(entries.last() is NotificationEntry.Single)
    }

    @Test
    fun keepsMentionsAndSingletonsSeparate() {
        val entries = NotificationGrouping.group(
            listOf(
                notification("1", "mention", "status-a"),
                notification("2", "favourite", "status-b")
            )
        )

        assertTrue(entries[0] is NotificationEntry.Single)
        assertTrue(entries[1] is NotificationEntry.Single)
    }

    @Test
    fun groupsFollowNotifications() {
        val entries = NotificationGrouping.group(
            listOf(
                notification("1", "follow", null),
                notification("2", "follow", null)
            )
        )

        val group = entries.single() as NotificationEntry.Group
        assertEquals("follow", group.type)
        assertEquals(2, group.notifications.size)
    }

    @Test
    fun keepsMissingStatusNotificationsSingle() {
        val entries = NotificationGrouping.group(
            listOf(
                notification("1", "favourite", null),
                notification("2", "favourite", null)
            )
        )

        assertEquals(2, entries.filterIsInstance<NotificationEntry.Single>().size)
    }

    @Test
    fun preservesTheNewestGroupOrder() {
        val entries = NotificationGrouping.group(
            listOf(
                notification("1", "favourite", "status-a"),
                notification("2", "favourite", "status-b"),
                notification("3", "favourite", "status-a"),
                notification("4", "favourite", "status-b")
            )
        )

        assertEquals("group-favourite-status-a", entries[0].key)
        assertEquals("group-favourite-status-b", entries[1].key)
    }

    private fun notification(id: String, type: String, statusId: String?): Notification = Notification(
        id = id,
        type = type,
        status = statusId?.let { Status(id = it) }
    )
}
