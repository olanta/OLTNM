package app.oltnm

import app.oltnm.util.LogRedactor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LogRedactorTest {
    @Test
    fun stripsQueryAndFragmentFromUrls() {
        assertEquals(
            "https://example.social/api/v1/statuses/1",
            LogRedactor.redactUrl(
                "https://example.social/api/v1/statuses/1?access_token=secret#fragment"
            )
        )
    }

    @Test
    fun redactsSecretsWithoutRemovingTheKey() {
        val text = LogRedactor.redactText(
            "GET https://example.social/oauth?code=abc&client_secret=def"
        )

        assertFalse(text.contains("abc"))
        assertFalse(text.contains("def"))
        assertTrue(text.contains("code=[redacted]"))
        assertTrue(text.contains("client_secret=[redacted]"))
    }

    @Test
    fun redactsAuthorizationHeadersAndJsonTokens() {
        val text = LogRedactor.redactText(
            """Authorization: Bearer abc.def.ghi {"access_token":"secret","refresh_token":"other"}"""
        )

        assertFalse(text.contains("abc.def.ghi"))
        assertFalse(text.contains("secret"))
        assertFalse(text.contains("other"))
        assertTrue(text.contains("Bearer [redacted]"))
        assertTrue(text.contains("[redacted]"))
    }

    @Test
    fun keepsSafeDiagnosticText() {
        assertEquals("HTTP GET /api/v1/timelines/home -> 200", LogRedactor.redactText("HTTP GET /api/v1/timelines/home -> 200"))
    }
}
