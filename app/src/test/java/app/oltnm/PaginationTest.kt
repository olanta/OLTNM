package app.oltnm

import app.oltnm.data.api.Pagination
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PaginationTest {
    @Test
    fun returnsNullWithoutHeader() {
        assertNull(Pagination.nextMaxId(null))
        assertNull(Pagination.nextMaxId(""))
    }

    @Test
    fun returnsNullWithoutNextLink() {
        val header = "<https://example.com/api/v1/timelines/home?limit=20&max_id=10>; rel=\"prev\""
        assertNull(Pagination.nextMaxId(header))
    }

    @Test
    fun extractsMaxIdFromNextLink() {
        val header = "<https://example.com/api/v1/timelines/home?limit=20&max_id=100>; rel=\"next\", " +
            "<https://example.com/api/v1/timelines/home?limit=20&since_id=200>; rel=\"prev\""
        assertEquals("100", Pagination.nextMaxId(header))
    }
}
