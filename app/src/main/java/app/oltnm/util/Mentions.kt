package app.oltnm.util

object Mentions {
    private val queryPattern = Regex("""(^|[\s\u00A0(])@([\p{L}\p{N}_.-]+)$""")
    private val applyPattern = Regex("""@([\p{L}\p{N}_.-]*)$""")

    fun query(text: String, cursor: Int): String? {
        if (cursor !in 0..text.length) return null
        val prefix = text.substring(0, cursor)
        val match = queryPattern.find(prefix) ?: return null
        return match.groupValues[2].takeIf { it.isNotEmpty() }
    }

    fun insert(text: String, cursor: Int, acct: String): MentionInsertion? {
        if (cursor !in 0..text.length) return null
        val prefix = text.substring(0, cursor)
        val match = applyPattern.find(prefix) ?: return null
        val start = match.range.first
        val handle = acct.trim().removePrefix("@")
        if (handle.isBlank()) return null
        val inserted = "@$handle "
        return MentionInsertion(
            text = text.replaceRange(start, cursor, inserted),
            cursor = start + inserted.length
        )
    }
}

data class MentionInsertion(
    val text: String,
    val cursor: Int
)
