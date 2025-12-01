package com.nxoim.blean.commonThingsDumpster

import com.nxoim.blean.byteCount.ByteCount
import com.nxoim.blean.byteCount.bytes
import com.nxoim.blean.byteCount.kilobytes
import com.nxoim.blean.byteCount.mebibytes
import com.nxoim.blean.byteCount.petabytes
import com.nxoim.blean.byteCount.tebibytes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ByteCountTests {
    @Test
    fun `normal add`() {
        assertEquals(1500L.bytes, (500L.bytes + 1000L.bytes))
    }

    @Test
    fun `overflow positive + positive saturates to Max`() {
        val a = ByteCount.Max - ByteCount(10L)
        val b = ByteCount(20L)
        assertEquals(ByteCount.Max, a + b)
    }

    @Test
    fun `overflow negative + negative saturates to Min`() {
        val a = ByteCount.Min + ByteCount(10L)
        val b = ByteCount(-20L)
        assertEquals(ByteCount.Min, a + b)
    }

    @Test
    fun `mix signs no overflow`() {
        val a = 1_000L.bytes
        val b = (-500L).bytes
        assertEquals(500L.bytes, a + b)
    }

    @Test
    fun `normal subtract`() {
        assertEquals(200L.bytes, (1000L.bytes - 800L.bytes))
    }

    @Test
    fun `underflow positive - negative saturates to Max`() {
        val a = ByteCount.Max - ByteCount(5L)
        val b = (-10L).bytes
        assertEquals(ByteCount.Max, a - b)
    }

    @Test
    fun `underflow negative - positive saturates to Min`() {
        val a = ByteCount.Min + ByteCount(5L)
        val b = 10L.bytes
        assertEquals(ByteCount.Min, a - b)
    }

    @Test
    fun `zero shortcut`() {
        assertEquals(ByteCount.Zero, 0L.bytes * 123456789L)
        assertEquals(ByteCount.Zero, 123456L.bytes * 0L)
    }

    @Test
    fun `normal multiply`() {
        assertEquals(2_000L.bytes, (1000L.bytes * 2L))
    }

    @Test
    fun `minValue times -1 gives Max`() {
        assertEquals(ByteCount.Max, ByteCount.Min * (-1L))
    }

    @Test
    fun `normal modulo`() {
        assertEquals(1L.bytes, 10L.bytes % 3L)
        assertEquals((-1L).bytes, (-10L).bytes % 3L)
    }

    @Test
    fun `modulo by zero throws`() {
        assertFailsWith<IllegalArgumentException> { 42L.bytes % 0L }
    }

    @Test
    fun `unary plus is identity`() {
        val x = 12345L.bytes
        assertEquals(x, +x)
    }

    @Test
    fun `unary minus flips sign`() {
        val x = 500L.bytes
        assertEquals((-500L).bytes, -x)
    }

    @Test
    fun `unary minus minValue gives Max`() {
        assertEquals(ByteCount.Max, -ByteCount.Min)
    }

    @Test
    fun compareTo() {
        assertTrue(100L.bytes < 200L.bytes)
        assertTrue(200L.bytes > 100L.bytes)
        assertEquals(0, 50L.bytes.compareTo(50L.bytes))
    }

    @Test
    fun `decimal whole units`() {
        val bytes = 2_345L
        assertEquals(bytes / 1_000, ByteCount(bytes).inWholeKilobytes)
        assertEquals(bytes / 1_000_000, ByteCount(bytes).inWholeMegabytes)
    }

    @Test
    fun `binary whole units`() {
        val bytes = 5_000L
        assertEquals(bytes / 1024, ByteCount(bytes).inWholeKibibytes)
        assertEquals(bytes / 1_048_576, ByteCount(bytes).inWholeMebibytes)
    }

    @Test
    fun `binary 2-component decomposition`() {
        2500L.bytes.toBinaryComponents { ki, b ->
            assertEquals(2, ki)
            assertEquals(452, b)
        }
    }

    @Test
    fun `binary 3-component decomposition`() {
        val value = (3L * 1_073_741_824) + (2L * 1_048_576) + (5L * 1024) + 7L
        value.bytes.toBinaryComponents { gi, me, ki, b ->
            assertEquals(3, gi)
            assertEquals(2, me)
            assertEquals(5, ki)
            assertEquals(7, b)
        }
    }

    @Test
    fun `decimal 3-component decomposition`() {
        val value = (4L * 1_000_000) + (6L * 1_000) + 123L
        value.bytes.toDecimalComponents { mb, kb, b ->
            assertEquals(4, mb)
            assertEquals(6, kb)
            assertEquals(123, b)
        }
    }

    @Test
    fun `times with ByteCount`() {
        assertEquals(200L.bytes, 100L.bytes * 2L.bytes)
    }

    @Test
    fun `extensions on IntShort`() {
        assertEquals(3_000L.bytes, 3.kilobytes)
        assertEquals(2_097_152L.bytes, 2.mebibytes)
    }

    @Test
    fun `add zero identity`() {
        val x = 123L.bytes
        assertEquals(x, x + ByteCount.Zero)
        assertEquals(x, ByteCount.Zero + x)
        assertEquals(x, x - ByteCount.Zero)
    }

    @Test
    fun `multiply by negative one`() {
        val x = 789L.bytes
        assertEquals<ByteCount>(-x, x * ByteCount(-1L))
    }

    @Test
    fun `addition is commutative`() {
        val a = 100L.bytes
        val b = 200L.bytes
        assertEquals(a + b, b + a)
    }

    @Test
    fun `multiplication is commutative`() {
        val a = 3L.bytes
        val b = 4L.bytes
        assertEquals(a * b, b * a)
    }

    @Test
    fun `addition is associative`() {
        val a = 1L.bytes
        val b = 2L.bytes
        val c = 3L.bytes
        assertEquals((a + b) + c, a + (b + c))
    }

    @Test
    fun `max plus zero stays max`() {
        assertEquals(ByteCount.Max, ByteCount.Max + ByteCount.Zero)
    }

    @Test
    fun `min plus zero stays min`() {
        assertEquals(ByteCount.Min, ByteCount.Min + ByteCount.Zero)
    }

    @Test
    fun `max plus one saturates to max`() {
        assertEquals(ByteCount.Max, ByteCount.Max + 1L.bytes)
    }

    @Test
    fun `min minus one saturates to min`() {
        assertEquals(ByteCount.Min, ByteCount.Min - 1L.bytes)
    }

    @Test
    fun `min minus negative one increments by one`() {
        val expected = (Long.MIN_VALUE + 1).bytes
        assertEquals(expected, ByteCount.Min - (-1L).bytes)
    }

    @Test
    fun `rem with ByteCount divisor`() {
        assertEquals(2L.bytes, 8L.bytes % 3L.bytes)
    }

    @Test
    fun `rem with negative divisor`() {
        assertEquals(2L.bytes, 8L.bytes % -3L)
        assertEquals((-2L).bytes, (-8L).bytes % 3L)
    }

    @Test
    fun `binary decomposition of zero`() {
        0L.bytes.toBinaryComponents { ki, b ->
            assertEquals(0, ki)
            assertEquals(0, b)
        }
    }

    @Test
    fun `decimal decomposition of zero`() {
        0L.bytes.toDecimalComponents { kb, b ->
            assertEquals(0, kb)
            assertEquals(0, b)
        }
    }

    @Test
    fun `binary decomposition of negative value`() {
        (-2500L).bytes.toBinaryComponents { ki, b ->
            assertEquals(-2, ki)
            assertEquals(-452, b)
        }
    }

    @Test
    fun `toString formatting`() {
        assertEquals("123 bytes", 123L.bytes.toString())
        assertEquals("-5 bytes", (-5L).bytes.toString())
    }

    @Test
    fun `equals and hashCode consistency`() {
        val x = 555L.bytes
        val y = ByteCount(x.bytes)
        assertTrue(x == y)
        assertEquals(x.hashCode(), y.hashCode())
    }

    @Test
    fun `whole gigabytes, terabytes, petabytes`() {
        val bytes = 5L * ByteCount.Gigabyte.bytes + 123L
        assertEquals(5, ByteCount(bytes).inWholeGigabytes)
        val tBytes = 2L * ByteCount.Terabyte.bytes + 42L
        assertEquals(2, ByteCount(tBytes).inWholeTerabytes)
        val pBytes = 3L * ByteCount.Petabyte.bytes + 7L
        assertEquals(3, ByteCount(pBytes).inWholePetabytes)
    }

    @Test
    fun `extensions on Short and Int for all units`() {
        assertEquals(2L * ByteCount.Tebibyte.bytes, 2.tebibytes.bytes)
        assertEquals(4L * ByteCount.Kilobyte.bytes, 4.kilobytes.bytes)
        assertEquals(7L * ByteCount.Petabyte.bytes, 7.petabytes.bytes)
    }
}