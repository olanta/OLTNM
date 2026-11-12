package app.oltnm

import app.oltnm.data.model.Account
import app.oltnm.data.model.Status
import app.oltnm.data.model.Visibility
import app.oltnm.ui.screens.compose.ReplyTarget
import org.junit.Assert.assertEquals
import org.junit.Test

class ReplyTargetTest {
    @Test
    fun usesOriginalStatusAndVisibility() {
        val target = ReplyTarget.from(
            Status(
                id = "boost",
                account = Account(acct = "booster"),
                visibility = "public",
                reblog = Status(
                    id = "42",
                    account = Account(acct = "alice@example.social"),
                    visibility = "unlisted"
                )
            )
        )

        assertEquals("42", target.statusId)
        assertEquals("alice@example.social", target.acct)
        assertEquals(Visibility.Unlisted, target.visibility)
    }

    @Test
    fun prefillsMentionWithSingleAt() {
        assertEquals(
            "@alice@example.social ",
            ReplyTarget("1", "alice@example.social", Visibility.Public).initialText
        )
        assertEquals("@alice ", ReplyTarget("1", "@alice", Visibility.Public).initialText)
        assertEquals("", ReplyTarget("1", "  ", Visibility.Public).initialText)
    }

    @Test
    fun mapsUnknownVisibilityToPublic() {
        assertEquals(
            Visibility.Public,
            ReplyTarget.from(Status(id = "1", visibility = "unknown")).visibility
        )
    }
}
