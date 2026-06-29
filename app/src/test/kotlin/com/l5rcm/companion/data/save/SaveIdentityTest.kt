package com.l5rcm.companion.data.save

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SaveIdentityTest {

    @Test
    fun readsUuidWhenPresent() {
        assertEquals(
            "3f2a9c10-7b4e-4a2d-9f1c-2e5b8d6a0c11",
            SaveIdentity.uuidOf("""{"name":"X","uuid":"3f2a9c10-7b4e-4a2d-9f1c-2e5b8d6a0c11"}"""),
        )
    }

    @Test
    fun returnsNullWhenAbsentBlankNullOrUnsafe() {
        assertNull(SaveIdentity.uuidOf("""{"name":"X"}"""))                 // absent
        assertNull(SaveIdentity.uuidOf("""{"name":"X","uuid":null}"""))     // json null
        assertNull(SaveIdentity.uuidOf("""{"name":"X","uuid":""}"""))       // blank -> not safe
        assertNull(SaveIdentity.uuidOf("""{"uuid":"../../etc/passwd"}"""))  // path traversal
    }

    @Test(expected = MissingUuidException::class)
    fun requireUuidThrowsWhenMissing() {
        SaveIdentity.requireUuid("""{"name":"X"}""")
    }

    @Test
    fun requireUuidReturnsWhenPresent() {
        assertEquals("abc-123", SaveIdentity.requireUuid("""{"uuid":"abc-123","name":"X"}"""))
    }
}
