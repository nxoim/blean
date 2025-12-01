@file:OptIn(ExperimentalTime::class)

import com.nxoim.blean.bskyPrimitives.RecordKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.ExperimentalTime

class RecordKeyTest {

    @Test
    fun testCreatesNewTid() {
        val tid = RecordKey.Tid.next()
        val str = tid.toString()
        // Kotlin strings are non-nullable by default, checking generic logic
        assertEquals(13, str.length)
        RecordKey.Tid(str)
    }

    @Test
    fun testParsesTid() {
        val tid = RecordKey.Tid.next()
        val str = tid.toString()
        val parsed = RecordKey.Tid(str)

        assertEquals(parsed, tid)
        assertEquals(parsed.value, tid.value)
    }

    @Test
    fun testThrowsIfInvalid() {
        // Too short
        assertFailsWith<IllegalArgumentException> { RecordKey.Tid("") }
        // Bad chars
        assertFailsWith<IllegalArgumentException> { RecordKey.Tid("222222222222!") }
        // Too long
        assertFailsWith<IllegalArgumentException> { RecordKey.Tid("22222222222222") }
    }

    @Test
    fun testSanitizesInput() {
        val raw = "3kgo-dwha-t2222"
        val tid = RecordKey.Tid(raw, sanitize = true)
        assertEquals("3kgodwhat2222", tid.value)
    }

    @Test
    fun testNextStr() {
        val str = RecordKey.Tid.next().toString()
        assertEquals(13, str.length)
    }

    @Test
    fun testNextLargerThanPrev() {
        val t1 = RecordKey.Tid.next()
        val t2 = RecordKey.Tid.next()

        assertTrue(t2 > t1, "T2 ($t2) should be > T1 ($t1)")
    }

    @Test
    fun testOrderingNewestFirst() {
        val older = RecordKey.Tid.next()
        // busy wait slightly to ensure different timestamps if clock is coarse
        // or just rely on atomic counter
        val newer = RecordKey.Tid.next()

        val list = mutableListOf(older, newer)
        list.sortDescending() // newest first

        assertEquals(newer, list[0])
        assertEquals(older, list[1])
    }

    @Test
    fun testOrderingOldestFirst() {
        val older = RecordKey.Tid.next()
        val newer = RecordKey.Tid.next()

        val list = mutableListOf(newer, older)
        list.sort() // oldest first (natural order)

        assertEquals(older, list[0])
        assertEquals(newer, list[1])
    }

    @Test
    fun testIsTid() {
        val tid = RecordKey.Tid.next()
        RecordKey.Tid(tid.toString())
        assertFails { RecordKey.Tid("") }
        assertFails { RecordKey.Tid("invalid")}
    }

    @Test
    fun testEquals() {
        val t1 = RecordKey.Tid.next()
        assertTrue(t1 == t1)

        val t2 = RecordKey.Tid(t1.toString())
        assertTrue(t1 == t2)

        val t3 = RecordKey.Tid.next()
        assertFalse(t1 == t3)
    }

    @Test
    fun testNewerOlderThan() {
        val old = RecordKey.Tid.next()
        val new = RecordKey.Tid.next()

        assertTrue(new > old)
        assertFalse(old > new)

        assertTrue(old < new)
        assertFalse(new < old)
    }

    @Test
    fun testStrictNSID() {
        // Valid
        RecordKey.NSID("com.example.foo")
        RecordKey.NSID("app.bsky.feed.post")

        // Invalid Cases
        assertFailsWith<IllegalArgumentException> { RecordKey.NSID("App.Bsky.Feed.Post") } // Uppercase
        assertFailsWith<IllegalArgumentException> { RecordKey.NSID("-app.bsky") } // Leading dash
        assertFailsWith<IllegalArgumentException> { RecordKey.NSID("app.bsky-") } // Trailing dash
        assertFailsWith<IllegalArgumentException> { RecordKey.NSID("app..bsky") } // Double dot
    }
}