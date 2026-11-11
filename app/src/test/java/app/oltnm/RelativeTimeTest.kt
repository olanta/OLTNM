package app.oltnm

import app.oltnm.util.RelativeTime
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class RelativeTimeTest {
    private val now = Instant.parse("2026-01-10T12:00:00Z")

    @Test
    fun showsNowForRecentPosts() {
        assertEquals("now", RelativeTime.format("2026-01-10T11:59:30Z", now))
    }

    @Test
    fun showsMinutesHoursAndDays() {
        assertEquals("5m", RelativeTime.format("2026-01-10T11:55:00Z", now))
        assertEquals("3h", RelativeTime.format("2026-01-10T09:00:00Z", now))
        assertEquals("2d", RelativeTime.format("2026-01-08T12:00:00Z", now))
    }

    @Test
    fun fallsBackToEmptyForInvalidDates() {
        assertEquals("", RelativeTime.format("not-a-date", now))
    }
}
