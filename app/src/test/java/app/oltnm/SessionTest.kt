package app.oltnm

import app.oltnm.data.model.Session
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class SessionTest {
    @Test
    fun keyCombinesHostAndAccount() {
        assertEquals("mastodon.social#42", session("mastodon.social", "42").key)
    }

    @Test
    fun handleDoesNotDuplicateRemoteServers() {
        assertEquals("@user@mastodon.social", session("mastodon.social", "42").handle)
        assertEquals(
            "@user@example.social",
            session("mastodon.social", "42", acct = "user@example.social").handle
        )
    }

    @Test
    fun keysAreUniquePerHostAndAccount() {
        assertNotEquals(
            session("mastodon.social", "42").key,
            session("mastodon.social", "43").key
        )
        assertNotEquals(
            session("mastodon.social", "42").key,
            session("fosstodon.org", "42").key
        )
    }

    private fun session(host: String, accountId: String, acct: String = "user"): Session = Session(
        host = host,
        clientId = "client",
        clientSecret = "secret",
        accessToken = "token",
        accountId = accountId,
        acct = acct,
        displayName = "User",
        avatar = ""
    )
}
