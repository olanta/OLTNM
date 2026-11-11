package app.oltnm

import androidx.compose.ui.graphics.Color
import app.oltnm.data.model.TextColorChoice
import app.oltnm.data.model.TextSizeChoice
import app.oltnm.ui.theme.color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AccessibilityTest {
    @Test
    fun defaultTextColourFollowsTheTheme() {
        assertNull(TextColorChoice.Default.color(isDark = true))
        assertNull(TextColorChoice.Default.color(isDark = false))
    }

    @Test
    fun highContrastFlipsWithTheTheme() {
        assertEquals(Color.White, TextColorChoice.HighContrast.color(isDark = true))
        assertEquals(Color.Black, TextColorChoice.HighContrast.color(isDark = false))
    }

    @Test
    fun textSizesAreOrdered() {
        assertTrue(TextSizeChoice.Small.scale < TextSizeChoice.Default.scale)
        assertTrue(TextSizeChoice.Default.scale < TextSizeChoice.Large.scale)
        assertTrue(TextSizeChoice.Large.scale < TextSizeChoice.ExtraLarge.scale)
    }
}
