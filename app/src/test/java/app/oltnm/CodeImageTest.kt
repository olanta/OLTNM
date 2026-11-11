package app.oltnm

import app.oltnm.util.CodeImage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CodeImageTest {
    @Test
    fun extractsFencedBlocksWithLanguage() {
        val blocks = CodeImage.extractCodeBlocks(
            "before\n```kotlin\nfun main() {}\n```\nafter"
        )
        assertEquals(1, blocks.size)
        assertEquals("kotlin", blocks.first().lang)
        assertEquals("fun main() {}", blocks.first().code)
        assertTrue(blocks.first().raw.startsWith("```kotlin"))
    }

    @Test
    fun extractsMultipleBlocks() {
        val text = "```js\nconst x = 1\n```\n\n```\nplain\n```"
        val blocks = CodeImage.extractCodeBlocks(text)
        assertEquals(2, blocks.size)
        assertEquals("", blocks[1].lang)
        assertEquals("plain", blocks[1].code)
    }

    @Test
    fun ignoresUnclosedBlocks() {
        assertTrue(CodeImage.extractCodeBlocks("```kt\nnot closed").isEmpty())
    }
}
