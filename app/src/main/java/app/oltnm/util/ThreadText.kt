package app.oltnm.util

object ThreadText {
    fun split(text: String, limit: Int): List<String> {
        val trimmed = text.trim()
        if (limit <= 0 || trimmed.length <= limit) return listOf(trimmed)

        val parts = mutableListOf<String>()
        var rest = trimmed
        while (rest.length > limit) {
            val cut = findCut(rest, limit)
            parts += rest.substring(0, cut).trim()
            rest = rest.substring(cut).trim()
        }
        if (rest.isNotEmpty()) parts += rest
        return parts.filter { it.isNotEmpty() }
    }

    private fun findCut(rest: String, limit: Int): Int {
        val window = rest.substring(0, minOf(rest.length, limit + 1))
        val minimum = (limit * 0.4).toInt()

        val paragraph = window.lastIndexOf("\n\n")
        if (paragraph >= minimum) return paragraph + 2
        val newline = window.lastIndexOf('\n')
        if (newline >= minimum) return newline + 1

        val sentence = maxOf(
            window.lastIndexOf(". "),
            window.lastIndexOf("! "),
            window.lastIndexOf("? ")
        )
        if (sentence >= minimum) return sentence + 1

        val space = window.lastIndexOf(' ')
        if (space >= minimum) return space
        return limit
    }
}
