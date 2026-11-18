package app.oltnm

import app.oltnm.util.Mentions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MentionsTest {
    @Test
    fun findsPartialMentionAtCursor() {
        assertEquals("ali", Mentions.query("hi @ali", 7))
        assertEquals("al", Mentions.query("(@al", 4))
        assertNull(Mentions.query("email@example", 13))
        assertNull(Mentions.query("hi @", 4))
    }

    @Test
    fun replacesPartialWithLocalAccount() {
        val insertion = Mentions.insert("hi @al", 6, "alice")

        assertEquals("hi @alice ", insertion?.text)
        assertEquals(10, insertion?.cursor)
    }

    @Test
    fun replacesPartialWithRemoteAccount() {
        val insertion = Mentions.insert("@al", 3, "@alice@example.social")

        assertEquals("@alice@example.social ", insertion?.text)
        assertEquals(22, insertion?.cursor)
    }
}
