package app.oltnm

import app.oltnm.util.Instances
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class InstancesTest {
    @Test
    fun normalizesSchemeCaseAndTrailingSlash() {
        assertEquals("mastodon.social", Instances.normalize("https://Mastodon.Social/"))
        assertEquals("mastodon.social", Instances.normalize("  mastodon.social  "))
        assertEquals("fosstodon.org", Instances.normalize("http://fosstodon.org"))
    }

    @Test
    fun rejectsInvalidInstances() {
        assertNull(Instances.normalize(""))
        assertNull(Instances.normalize("https://"))
        assertNull(Instances.normalize("localhost"))
        assertNull(Instances.normalize("example.com/path"))
        assertNull(Instances.normalize("two words.com"))
    }

    @Test
    fun buildsAuthorizeUrlWithEscapedRedirect() {
        val url = Instances.authorizeUrl(
            host = "mastodon.social",
            clientId = "client-123",
            redirectUri = "oltnm://oauth",
            scopes = "read write follow"
        )
        assertTrue(url.startsWith("https://mastodon.social/oauth/authorize?"))
        assertTrue(url.contains("client_id=client-123"))
        assertTrue(url.contains("redirect_uri=oltnm%3A%2F%2Foauth"))
        assertTrue(url.contains("response_type=code"))
        assertTrue(url.contains("scope=read+write+follow"))
        assertTrue(url.contains("force_login=true"))
    }
}
