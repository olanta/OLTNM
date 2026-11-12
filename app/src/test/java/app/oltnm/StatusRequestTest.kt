package app.oltnm

import app.oltnm.data.api.JsonConfig
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
}
