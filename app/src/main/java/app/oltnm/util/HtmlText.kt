package app.oltnm.util

object HtmlText {
    private val breakTag = Regex("(?i)<br\\s*/?>")
    private val paragraphEnd = Regex("(?i)</p\\s*>")
    private val blockEnd = Regex("(?i)</(li|div|blockquote|tr)\\s*>")
    private val anyTag = Regex("<[^>]*>")
    private val trailingSpaces = Regex("[ \\t]+\\n")
    private val manyNewlines = Regex("\\n{3,}")

    fun plain(html: String): String {
        if (html.isBlank()) return ""
        var text = breakTag.replace(html, "\n")
        text = paragraphEnd.replace(text, "\n\n")
        text = blockEnd.replace(text, "\n")
        text = anyTag.replace(text, "")
        text = decodeEntities(text)
        return text
            .replace(trailingSpaces, "\n")
            .replace(manyNewlines, "\n\n")
            .trim()
    }

    private fun decodeEntities(text: String): String = text
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .replace("&apos;", "'")
        .replace("&nbsp;", " ")
}
