package com.nxoim.blean.bskyPrimitives

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.getOrThrow
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

// might go through major changes
@Serializable(with = Did.Serializer::class)
sealed interface Did {
    fun valueAfterPrefix() = toString().removePrefix(
        when(this) {
            is Plc -> plcPrefix
            is Web -> webPrefix
        }
    )

    @Serializable(with = Plc.Serializer::class)
    @JvmInline
    value class Plc(private val string: String) : Did {
        override fun toString() = string

        object Serializer : StringWrapperSerializer<Plc>("Did.Plc", { parse(it).getOrThrow() as Plc })
    }

    @Serializable(with = Web.Serializer::class)
    @JvmInline
    value class Web(private val string: String) : Did {
        override fun toString() = string

        object Serializer : StringWrapperSerializer<Web>("Did.Web", { parse(it).getOrThrow() as Web })
    }

    object Serializer : StringWrapperSerializer<Did>("Did", { parse(it).getOrThrow() })

    companion object {
        fun parse(value: String): Result<Did, IllegalStateException> = when {
            value.startsWith(plcPrefix) -> Ok(Plc(value))
            value.startsWith(webPrefix) -> Ok(Web(value))
            else -> Err(IllegalStateException("Invalid did that cannot be parsed"))
        }
    }
}

private const val webPrefix = "did:web:"
private const val plcPrefix = "did:plc:"