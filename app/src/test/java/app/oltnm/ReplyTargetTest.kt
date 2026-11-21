package app.oltnm

import app.oltnm.data.model.Account
import app.oltnm.data.model.Mention
import app.oltnm.data.model.Status
import app.oltnm.ui.screens.compose.ReplyTarget
import org.junit.Assert.assertEquals
import org.junit.Test

class ReplyTargetTest {
    @Test
    fun mentionsAuthorAndOtherParticipantsWithoutSelf() {
        val status = Status(
            id = "1",
            account = Account(id = "author", acct = "author@example.social"),
            mentions = listOf(
                Mention(id = "other", acct = "other@example.social"),
                Mention(id = "self", acct = "me@example.social"),
                Mention(id = "author", acct = "author@example.social")
            )
        )

        val target = ReplyTarget.from(status, currentAccountId = "self")

        assertEquals(
            "@author@example.social @other@example.social ",
            target.initialText
        )
    }

    @Test
    fun repliesToOwnPostWithoutMentioningSelf() {
        val status = Status(
            id = "1",
            account = Account(id = "self", acct = "me@example.social"),
            mentions = listOf(Mention(id = "other", acct = "other@example.social"))
        )

        val target = ReplyTarget.from(status, currentAccountId = "self")

        assertEquals("@other@example.social ", target.initialText)
    }
}
