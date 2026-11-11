package app.oltnm

import app.oltnm.data.model.LanguageChoice
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LanguageChoiceTest {
    @Test
    fun usesTheExpectedLanguageTags() {
        assertNull(LanguageChoice.System.tag)
        assertEquals("en", LanguageChoice.English.tag)
        assertEquals("pt-BR", LanguageChoice.PortugueseBrazil.tag)
    }

    @Test
    fun resolvesTheBrazilianPortugueseLocale() {
        val locale = Locale.forLanguageTag(LanguageChoice.PortugueseBrazil.tag.orEmpty())
        assertEquals("pt", locale.language)
        assertEquals("BR", locale.country)
    }
}
