@file:OptIn(ExperimentalContracts::class)

package com.nxoim.blean.byteCount

import kotlinx.serialization.Serializable
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.jvm.JvmInline

@JvmInline
@Serializable
value class ByteCount(val bytes: Long) {
    val inWholeKilobytes: Long get() = bytes / Kilobyte.bytes
    val inWholeMegabytes: Long get() = bytes / Megabyte.bytes
    val inWholeGigabytes: Long get() = bytes / Gigabyte.bytes
    val inWholeTerabytes: Long get() = bytes / Terabyte.bytes
    val inWholePetabytes: Long get() = bytes / Petabyte.bytes

    val inWholeKibibytes: Long get() = bytes / Kibibyte.bytes
    val inWholeMebibytes: Long get() = bytes / Mebibyte.bytes
    val inWholeGibibytes: Long get() = bytes / Gibibyte.bytes
    val inWholeTebibytes: Long get() = bytes / Tebibyte.bytes
    val inWholePebibytes: Long get() = bytes / Pebibyte.bytes

    override fun toString() = "$bytes bytes"

    operator fun plus(other: ByteCount): ByteCount {
        val result = this.bytes + other.bytes
        // if signs of operands are same but result's sign is different.
        return if ((this.bytes xor result) and (other.bytes xor result) < 0) {
            if (this.bytes > 0) ByteCount.Max else ByteCount.Min
        } else {
            ByteCount(result)
        }
    }

    operator fun minus(other: ByteCount): ByteCount {
        val result = this.bytes - other.bytes
        // true if x and y have different signs, AND x and result have different signs.
        return if (((this.bytes xor other.bytes) < 0) && ((this.bytes xor result) < 0)) {
            if (other.bytes < 0) Max else Min
        } else
            ByteCount(result)
    }

    operator fun times(scale: Long): ByteCount {
        if (bytes == 0L || scale == 0L) return Zero
        try {
            return ByteCount(multiplyExact(bytes, scale))
        } catch (e: ArithmeticException) {
            val positiveResultExpected = (bytes > 0) == (scale > 0)
            return if (positiveResultExpected) Max else Min
        }
    }
    operator fun times(other: ByteCount): ByteCount = times(other.bytes)
    operator fun times(scale: Int): ByteCount = times(scale.toLong())
    operator fun times(scale: Short): ByteCount = times(scale.toLong())

    operator fun rem(other: Long): ByteCount {
        if (other == 0L) {
            throw IllegalArgumentException("Modulo by zero is undefined.")
        }
        return ByteCount(this.bytes % other)
    }
    operator fun rem(other: ByteCount): ByteCount = rem(other.bytes)
    operator fun rem(other: Int): ByteCount = rem(other.toLong())
    operator fun rem(other: Short): ByteCount = rem(other.toLong())

    operator fun unaryPlus(): ByteCount = ByteCount(+this.bytes)
    operator fun unaryMinus(): ByteCount {
        if (bytes == Long.MIN_VALUE) return Max
        return ByteCount(-bytes)
    }
    operator fun compareTo(other: ByteCount) = this.bytes.compareTo(other.bytes)

    inline fun <T> toBinaryComponents(block: (kibibytes: Long, bytes: Long) -> T): T {
        contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }

        var remaining = this.bytes
        val kibibytes = remaining / Kibibyte.bytes
        remaining %= Kibibyte.bytes

        return block(kibibytes, remaining)
    }

    inline fun <T> toBinaryComponents(block: (mebibytes: Long, kibibytes: Long, bytes: Long) -> T): T {
        contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
        var remaining = this.bytes
        val mebibytes = remaining / Mebibyte.bytes
        remaining %= Mebibyte.bytes
        val kibibytes = remaining / Kibibyte.bytes
        remaining %= Kibibyte.bytes

        return block(mebibytes, kibibytes, remaining)
    }

    inline fun <T> toBinaryComponents(block: (gibibytes: Long, mebibytes: Long, kibibytes: Long, bytes: Long) -> T): T {
        contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }

        var remaining = this.bytes
        val gibibytes = remaining / Gibibyte.bytes
        remaining %= Gibibyte.bytes
        val mebibytes = remaining / Mebibyte.bytes
        remaining %= Mebibyte.bytes
        val kibibytes = remaining / Kibibyte.bytes
        remaining %= Kibibyte.bytes

        return block(gibibytes, mebibytes, kibibytes, remaining)
    }

    inline fun <T> toBinaryComponents(block: (tebibytes: Long, gibibytes: Long, mebibytes: Long, kibibytes: Long, bytes: Long) -> T): T {
        contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }

        var remaining = this.bytes
        val tebibytes = remaining / Tebibyte.bytes
        remaining %= Tebibyte.bytes
        val gibibytes = remaining / Gibibyte.bytes
        remaining %= Gibibyte.bytes
        val mebibytes = remaining / Mebibyte.bytes
        remaining %= Mebibyte.bytes
        val kibibytes = remaining / Kibibyte.bytes
        remaining %= Kibibyte.bytes

        return block(tebibytes, gibibytes, mebibytes, kibibytes, remaining)
    }

    inline fun <T> toBinaryComponents(block: (pebibytes: Long, tebibytes: Long, gibibytes: Long, mebibytes: Long, kibibytes: Long, bytes: Long) -> T): T {
        contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }

        var remaining = this.bytes
        val pebibytes = remaining / Pebibyte.bytes
        remaining %= Pebibyte.bytes
        val tebibytes = remaining / Tebibyte.bytes
        remaining %= Tebibyte.bytes
        val gibibytes = remaining / Gibibyte.bytes
        remaining %= Gibibyte.bytes
        val mebibytes = remaining / Mebibyte.bytes
        remaining %= Mebibyte.bytes
        val kibibytes = remaining / Kibibyte.bytes
        remaining %= Kibibyte.bytes

        return block(pebibytes, tebibytes, gibibytes, mebibytes, kibibytes, remaining)
    }

    inline fun <T> toDecimalComponents(block: (kilobytes: Long, bytes: Long) -> T): T {
        contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }

        var remaining = this.bytes
        val kilobytes = remaining / Kilobyte.bytes
        remaining %= Kilobyte.bytes

        return block(kilobytes, remaining)
    }

    inline fun <T> toDecimalComponents(block: (megabytes: Long, kilobytes: Long, bytes: Long) -> T): T {
        contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }

        var remaining = this.bytes
        val megabytes = remaining / Megabyte.bytes
        remaining %= Megabyte.bytes
        val kilobytes = remaining / Kilobyte.bytes
        remaining %= Kilobyte.bytes

        return block(megabytes, kilobytes, remaining)
    }

    inline fun <T> toDecimalComponents(block: (gigabytes: Long, megabytes: Long, kilobytes: Long, bytes: Long) -> T): T {
        contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }

        var remaining = this.bytes
        val gigabytes = remaining / Gigabyte.bytes
        remaining %= Gigabyte.bytes
        val megabytes = remaining / Megabyte.bytes
        remaining %= Megabyte.bytes
        val kilobytes = remaining / Kilobyte.bytes
        remaining %= Kilobyte.bytes

        return block(gigabytes, megabytes, kilobytes, remaining)
    }

    inline fun <T> toDecimalComponents(block: (terabytes: Long, gigabytes: Long, megabytes: Long, kilobytes: Long, bytes: Long) -> T): T {
        contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }

        var remaining = this.bytes
        val terabytes = remaining / Terabyte.bytes
        remaining %= Terabyte.bytes
        val gigabytes = remaining / Gigabyte.bytes
        remaining %= Gigabyte.bytes
        val megabytes = remaining / Megabyte.bytes
        remaining %= Megabyte.bytes
        val kilobytes = remaining / Kilobyte.bytes
        remaining %= Kilobyte.bytes

        return block(terabytes, gigabytes, megabytes, kilobytes, remaining)
    }

    inline fun <T> toDecimalComponents(block: (petabytes: Long, terabytes: Long, gigabytes: Long, megabytes: Long, kilobytes: Long, bytes: Long) -> T): T {
        contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }

        var remaining = this.bytes
        val petabytes = remaining / Petabyte.bytes
        remaining %= Petabyte.bytes
        val terabytes = remaining / Terabyte.bytes
        remaining %= Terabyte.bytes
        val gigabytes = remaining / Gigabyte.bytes
        remaining %= Gigabyte.bytes
        val megabytes = remaining / Megabyte.bytes
        remaining %= Megabyte.bytes
        val kilobytes = remaining / Kilobyte.bytes
        remaining %= Kilobyte.bytes

        return block(petabytes, terabytes, gigabytes, megabytes, kilobytes, remaining)
    }

    companion object {
        val Min = ByteCount(Long.MIN_VALUE)
        val Zero = ByteCount(0)
        val Max = ByteCount(Long.MAX_VALUE)

        val Kilobyte = ByteCount(1_000)
        val Megabyte = ByteCount(1_000_000)
        val Gigabyte = ByteCount(1_000_000_000)
        val Terabyte = ByteCount(1_000_000_000_000)
        val Petabyte = ByteCount(1_000_000_000_000_000)

        val Kibibyte = ByteCount(1024)
        val Mebibyte = ByteCount(1_048_576)
        val Gibibyte = ByteCount(1_073_741_824)
        val Tebibyte = ByteCount(1_099_511_627_776)
        val Pebibyte = ByteCount(1_125_899_906_842_624)
    }
}


val Short.bytes: ByteCount get() = ByteCount(this.toLong())
val Int.bytes: ByteCount get() = ByteCount(this.toLong())
val Long.bytes: ByteCount get() = ByteCount(this)

val Short.kilobytes: ByteCount get() = ByteCount.Kilobyte * this
val Int.kilobytes: ByteCount get() = ByteCount.Kilobyte * this
val Long.kilobytes: ByteCount get() = ByteCount.Kilobyte * this

val Short.megabytes: ByteCount get() = ByteCount.Megabyte * this
val Int.megabytes: ByteCount get() = ByteCount.Megabyte * this
val Long.megabytes: ByteCount get() = ByteCount.Megabyte * this

val Short.gigabytes: ByteCount get() = ByteCount.Gigabyte * this
val Int.gigabytes: ByteCount get() = ByteCount.Gigabyte * this
val Long.gigabytes: ByteCount get() = ByteCount.Gigabyte * this

val Short.terabytes: ByteCount get() = ByteCount.Terabyte * this
val Int.terabytes: ByteCount get() = ByteCount.Terabyte * this
val Long.terabytes: ByteCount get() = ByteCount.Terabyte * this

val Short.petabytes: ByteCount get() = ByteCount.Petabyte * this
val Int.petabytes: ByteCount get() = ByteCount.Petabyte * this
val Long.petabytes: ByteCount get() = ByteCount.Petabyte * this


val Short.kibibytes: ByteCount get() = ByteCount.Kibibyte * this
val Int.kibibytes: ByteCount get() = ByteCount.Kibibyte * this
val Long.kibibytes: ByteCount get() = ByteCount.Kibibyte * this

val Short.mebibytes: ByteCount get() = ByteCount.Mebibyte * this
val Int.mebibytes: ByteCount get() = ByteCount.Mebibyte * this
val Long.mebibytes: ByteCount get() = ByteCount.Mebibyte * this


val Short.gibibytes: ByteCount get() = ByteCount.Gibibyte * this
val Int.gibibytes: ByteCount get() = ByteCount.Gibibyte * this
val Long.gibibytes: ByteCount get() = ByteCount.Gibibyte * this


val Short.tebibytes: ByteCount get() = ByteCount.Tebibyte * this
val Int.tebibytes: ByteCount get() = ByteCount.Tebibyte * this
val Long.tebibytes: ByteCount get() = ByteCount.Tebibyte * this

val Short.pebibytes: ByteCount get() = ByteCount.Pebibyte * this
val Int.pebibytes: ByteCount get() = ByteCount.Pebibyte * this
val Long.pebibytes: ByteCount get() = ByteCount.Pebibyte * this

private fun multiplyExact(a: Long, b: Long): Long {
    if ((a == Long.MIN_VALUE && b == -1L) || b == Long.MIN_VALUE && a == -1L)
        throw ArithmeticException()

    val result = a * b
    if (a != 0L && result / a != b) throw ArithmeticException()

    return result
}