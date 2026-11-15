package app.oltnm

import app.oltnm.data.api.JsonConfig
import app.oltnm.data.model.ReportRequest
import app.oltnm.data.model.StatusRequest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StatusRequestTest {
    @Test
    fun serializesReplyTarget() {
        val json = JsonConfig.encodeStatusRequest(
            StatusRequest(
                status = "Hello",
                visibility = "direct",
                inReplyToId = "42"
            )
        )

        assertTrue(json.contains("\"in_reply_to_id\":\"42\""))
    }

    @Test
    fun omitsReplyTargetForNewPosts() {
        val json = JsonConfig.encodeStatusRequest(StatusRequest(status = "Hello"))

        assertFalse(json.contains("in_reply_to_id"))
    }

    @Test
    fun alwaysSerializesEditableFields() {
        val json = JsonConfig.encodeStatusRequest(StatusRequest(status = "Hello"))

        assertTrue(json.contains("\"visibility\":\"public\""))
        assertTrue(json.contains("\"spoiler_text\":\"\""))
        assertTrue(json.contains("\"sensitive\":false"))
        assertTrue(json.contains("\"media_ids\":[]"))
    }

    @Test
    fun serializesReportRequest() {
        val json = JsonConfig.json.encodeToString(
            ReportRequest.serializer(),
            ReportRequest(
                accountId = "42",
                statusIds = listOf("99"),
                comment = "spam"
            )
        )

        assertTrue(json.contains("\"account_id\":\"42\""))
        assertTrue(json.contains("\"status_ids\":[\"99\"]"))
        assertTrue(json.contains("\"comment\":\"spam\""))
    }
}
