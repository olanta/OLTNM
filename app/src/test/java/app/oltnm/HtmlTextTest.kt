package app.oltnm

import app.oltnm.util.HtmlText
import org.junit.Assert.assertEquals
import org.junit.Test

class HtmlTextTest {
    @Test
    fun stripsTagsAndDecodesEntities() {
        val html = "<p>Hello <strong>world</strong> &amp; friends</p>"
        assertEquals("Hello world & friends", HtmlText.plain(html))
    }

    @Test
    fun keepsParagraphAndLineBreaks() {
        val html = "<p>first</p><p>second<br/>third</p>"
        assertEquals("first\n\nsecond\nthird", HtmlText.plain(html))
    }

    @Test
    fun returnsEmptyForBlankInput() {
        assertEquals("", HtmlText.plain(""))
        assertEquals("", HtmlText.plain("   "))
    }
}
