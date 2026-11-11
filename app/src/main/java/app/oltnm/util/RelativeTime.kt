package app.oltnm.util

import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object RelativeTime {
    private val monthDay = DateTimeFormatter.ofPattern("MMM d").withZone(ZoneId.systemDefault())

    fun format(iso: String, now: Instant = Instant.now(), nowLabel: String = "now"): String {
        val instant = runCatching { Instant.parse(iso) }.getOrNull() ?: return ""
        val seconds = Duration.between(instant, now).seconds.coerceAtLeast(0)
        return when {
            seconds < 60 -> nowLabel
            seconds < 3_600 -> "${seconds / 60}m"
            seconds < 86_400 -> "${seconds / 3_600}h"
            seconds < 604_800 -> "${seconds / 86_400}d"
            else -> monthDay.format(instant)
        }
    }
}
