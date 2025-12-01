@file:OptIn(ExperimentalAtomicApi::class)

package com.nxoim.blean.bskyPrimitives

import kotlinx.serialization.Serializable
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.random.Random
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * https://atproto.com/specs/record-key
 */
// the interface by itself is not serializable because theres
// no reliable way to deserialize the key into it's type
sealed interface RecordKey {
    val value: String

    companion object {
        private val recordKeyRegex = Regex("^[a-zA-Z0-9._:~-]{1,512}$")
        private val strictNsidRegex =
            Regex("^[a-z]([a-z0-9-]{0,61}[a-z0-9])?(\\.[a-z0-9]([a-z0-9-]{0,61}[a-z0-9])?)+$")

        fun validateBase(key: String) {
            require(key.length <= 512) { "Key too long" }
            require(key != "." && key != "..") { "Key cannot be . or .." }
            require(recordKeyRegex.matches(key)) { "Invalid characters in key: $key" }
        }
    }

    @Serializable(Tid.Serializer::class)
    data class Tid(override val value: String) : RecordKey, Comparable<Tid> {
        constructor(raw: String, sanitize: Boolean) : this(
            (if (sanitize) raw.replace("-", "") else raw).lowercase()
        )

        init {
            require(value.length == 13) { "TID must be 13 chars" }
            validateBase(value)
            require(value.all { it in s23Chars }) { "TID must be base32-sortable" }
        }

        override fun compareTo(other: Tid): Int = value.compareTo(other.value)

        val timestamp by lazy { s32decode(value.take(11)) }
        val clockId by lazy { s32decode(value.substring(11, 13)).toInt() }
        val formatted by lazy {
            "${value.take(4)}-" +
                    "${value.substring(4, 7)}-" +
                    "${value.substring(7, 11)}-" +
                    value.substring(11, 13)
        }

        override fun toString() = value

        object Serializer : StringWrapperSerializer<Tid>("Tid", ::Tid)

        companion object {
            private val clockId10Bits = Random.nextInt(1024)
            private val state = AtomicReference(State(0L, 0))

            @OptIn(ExperimentalTime::class)
            fun next(nowMillis: Long = Clock.System.now().toEpochMilliseconds()): Tid {
                var captured: State

                while (true) {
                    val current = state.load()
                    val nextState =
                        if (nowMillis > current.lastTs) {
                            State(nowMillis, 0)
                        } else {
                            State(current.lastTs, current.count + 1)
                        }

                    if (state.compareAndSet(current, nextState)) {
                        captured = nextState
                        break
                    }
                }

                val timestamp = (captured.lastTs * 1000) + captured.count
                val timeStr = s32encode(timestamp).padStart(11, '2')
                val clockStr = s32encode(clockId10Bits.toLong()).padStart(2, '2')

                return Tid(timeStr + clockStr)
            }

            fun fromTime(timestamp: Long, clockId: Int): Tid {
                val timeStr = s32encode(timestamp).padStart(11, '2')
                val clockStr = s32encode(clockId.toLong()).padStart(2, '2')
                return Tid(timeStr + clockStr)
            }
        }
    }

    @Serializable(NSID.Serializer::class)
    data class NSID(override val value: String) : RecordKey {

        init {
            validateBase(value)
            require(strictNsidRegex.matches(value)) {
                "Invalid NSID format. Must be lowercase, domain-like, max 63 chars per segment. Got: $value"
            }
        }

        override fun toString() = value
        object Serializer : StringWrapperSerializer<NSID>("RecordKey.NSID", ::NSID)
    }

    @Serializable(Literal.Serializer::class)
    data class Literal(override val value: String) : RecordKey {
        init { validateBase(value) }
        override fun toString() = value
        object Serializer : StringWrapperSerializer<Literal>("RecordKey.Literal", ::Literal)
    }

    @Serializable(Any.Serializer::class)
    data class Any(override val value: String) : RecordKey {
        init { validateBase(value) }
        override fun toString() = value
        object Serializer : StringWrapperSerializer<Any>("RecordKey.Any", ::Any)
    }
}

////////////////////////////////////////////////////////////////////////////////////////////

private data class State(val lastTs: Long, val count: Int)

private const val s23Chars = "234567abcdefghijklmnopqrstuvwxyz"
private fun s32encode(i: Long): String {
    if (i == 0L) return "2"
    var v = i
    val sb = StringBuilder()
    while (v > 0) {
        val index = (v % 32).toInt()
        sb.append(s23Chars[index])
        v /= 32
    }
    return sb.reverse().toString()
}

private fun s32decode(s: String): Long {
    var result = 0L
    for (c in s) {
        val idx = s23Chars.indexOf(c)
        require(idx >= 0) { "Invalid s32 char: $c" }
        result = result * 32 + idx
    }
    return result
}