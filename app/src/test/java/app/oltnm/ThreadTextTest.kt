package app.oltnm

import app.oltnm.util.ThreadText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ThreadTextTest {
    @Test
    fun keepsShortPostsAsOnePart() {
        assertEquals(listOf("hello"), ThreadText.split("  hello  ", 500))
    }

    @Test
    fun splitsLongPostsWithinTheLimit() {
        val text = "First paragraph with a few words.\n\n" +
            "Second paragraph also has some words and keeps going."
        val parts = ThreadText.split(text, 40)
        assertTrue(parts.size > 1)
        assertTrue(parts.all { it.length <= 40 })
        assertEquals("First paragraph with a few words.", parts.first())
    }

    @Test
    fun prefersParagraphBreaks() {
        val parts = ThreadText.split("First paragraph.\n\nSecond paragraph.", 20)
        assertEquals(listOf("First paragraph.", "Second paragraph."), parts)
    }

    @Test
    fun handlesEmptyInput() {
        assertEquals(listOf(""), ThreadText.split("", 10))
    }
}
