package app.oltnm.data.api

data class Page<T>(
    val items: List<T>,
    val nextMaxId: String?
)

object Pagination {
    private val maxIdPattern = Regex("""[?&]max_id=([^&>]+)""")

    fun nextMaxId(linkHeader: String?): String? {
        if (linkHeader.isNullOrBlank()) return null
        val next = linkHeader
            .split(',')
            .firstOrNull { it.contains("rel=\"next\"") }
            ?: return null
        return maxIdPattern.find(next)?.groupValues?.getOrNull(1)
    }
}
