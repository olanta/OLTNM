package app.oltnm.data.filters

import app.oltnm.data.model.MastodonFilter
import app.oltnm.data.model.Status
import app.oltnm.util.HtmlText
import java.time.Instant

data class FilteredStatuses(
    val items: List<Status>,
    val warnings: Map<String, String>,
    val nextMaxId: String? = null
)

object FilterMatcher {
    fun apply(
        items: List<Status>,
        filters: List<MastodonFilter>,
        context: String,
        nextMaxId: String? = null,
        now: Instant = Instant.now()
    ): FilteredStatuses {
        if (filters.isEmpty()) return FilteredStatuses(items, emptyMap(), nextMaxId)
        val active = filters.filter { filter ->
            context in filter.context && !isExpired(filter, now)
        }
        if (active.isEmpty()) return FilteredStatuses(items, emptyMap(), nextMaxId)

        val visible = ArrayList<Status>(items.size)
        val warnings = mutableMapOf<String, String>()
        for (item in items) {
            val original = item.original
            val filter = active.firstOrNull { matches(original, it) }
            when {
                filter == null -> visible += item
                filter.filterAction == "hide" -> Unit
                else -> {
                    visible += item
                    warnings[item.id] = filter.title.ifBlank {
                        filter.keywords.firstOrNull()?.keyword.orEmpty()
                    }
                }
            }
        }
        return FilteredStatuses(visible, warnings, nextMaxId)
    }

    fun isHidden(
        status: Status,
        filters: List<MastodonFilter>,
        context: String,
        now: Instant = Instant.now()
    ): Boolean = filters.any { filter ->
        context in filter.context &&
            filter.filterAction == "hide" &&
            !isExpired(filter, now) &&
            matches(status.original, filter)
    }

    private fun isExpired(filter: MastodonFilter, now: Instant): Boolean {
        val expiresAt = filter.expiresAt?.takeIf { it.isNotBlank() } ?: return false
        return runCatching { Instant.parse(expiresAt).isBefore(now) }.getOrDefault(false)
    }

    private fun matches(status: Status, filter: MastodonFilter): Boolean {
        val text = buildString {
            append(HtmlText.plain(status.content))
            append(' ')
            append(status.spoilerText)
            status.mediaAttachments.forEach { attachment ->
                append(' ')
                append(attachment.description.orEmpty())
            }
        }.lowercase()
        return filter.keywords.any { keyword ->
            val needle = keyword.keyword.trim().lowercase()
            if (needle.isEmpty()) return@any false
            if (keyword.wholeWord) {
                Regex(
                    "(?<![\\p{L}\\p{N}_])${Regex.escape(needle)}" +
                        "(?![\\p{L}\\p{N}_])"
                ).containsMatchIn(text)
            } else {
                text.contains(needle)
            }
        }
    }
}
