package app.oltnm.util

object HtmlText {
    private val breakTag = Regex("(?i)<br\\s*/?>")
    private val paragraphEnd = Regex("(?i)</p\\s*>")
    private val blockEnd = Regex("(?i)</(li|div|blockquote|tr)\\s*>")
    private val imageTag = Regex("(?i)<img\\b[^>]*>")
    private val altAttribute = Regex("(?i)\\balt\\s*=\\s*[\"']([^\"']*)[\"']")
    private val anyTag = Regex("<[^>]*>")
    private val trailingSpaces = Regex("[ \\t]+\\n")
    private val manyNewlines = Regex("\\n{3,}")

    fun plain(html: String, emojiShortcodes: Set<String> = emptySet()): String {
        if (html.isBlank()) return ""
        var text = imageTag.replace(html) { match ->
            val alt = altAttribute.find(match.value)?.groupValues?.get(1)
            val shortcode = alt?.trim()?.removeSurrounding(":")
            if (shortcode != null && shortcode in emojiShortcodes) alt else ""
        }
        text = breakTag.replace(text, "\n")
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
