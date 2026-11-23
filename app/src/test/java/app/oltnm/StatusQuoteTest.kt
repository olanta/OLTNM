package app.oltnm

import app.oltnm.data.api.JsonConfig
import app.oltnm.data.model.Status
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StatusQuoteTest {
    @Test
    fun readsDirectQuoteStatus() {
        val status = JsonConfig.decode<Status>(
            """
            {
              "id": "1",
              "content": "<p>quoting</p>",
              "quote": {
                "id": "2",
                "content": "<p>quoted</p>",
                "account": {"id": "a", "acct": "someone@example.social"}
              }
            }
            """.trimIndent()
        )

        assertEquals("2", status.quotedStatus?.id)
        assertEquals("quoted", status.quotedStatus?.content?.replace(Regex("<[^>]+>"), ""))
    }

    @Test
    fun readsNestedAcceptedQuoteStatus() {
        val status = JsonConfig.decode<Status>(
            """
            {
              "id": "1",
              "content": "<p>quoting</p>",
              "quote": {
                "state": "accepted",
                "quoted_status": {
                  "id": "2",
                  "content": "<p>quoted</p>",
                  "account": {"id": "a", "acct": "someone@example.social"}
                }
              }
            }
            """.trimIndent()
        )

        assertEquals("2", status.quotedStatus?.id)
    }

    @Test
    fun ignoresPendingQuoteStatus() {
        val status = JsonConfig.decode<Status>(
            """
            {
              "id": "1",
              "quote": {
                "state": "pending",
                "quoted_status": {
                  "id": "2",
                  "content": "<p>quoted</p>",
                  "account": {"id": "a"}
                }
              }
            }
            """.trimIndent()
        )

        assertNull(status.quotedStatus)
    }
}
