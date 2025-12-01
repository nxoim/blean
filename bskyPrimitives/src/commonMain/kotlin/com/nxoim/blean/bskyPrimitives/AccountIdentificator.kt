package com.nxoim.blean.bskyPrimitives

import com.github.michaelbull.result.getOrThrow
import com.nxoim.blean.bskyPrimitives.Did.Companion.parse
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@Serializable
sealed interface AccountIdentificator {
    @Serializable(Handle.Serializer::class)
    @JvmInline
    value class Handle(private val value: String) : AccountIdentificator {
        override fun toString() = value

        object Serializer : StringWrapperSerializer<Handle>(
            "AccountIdentificator.Handle",
            ::Handle
        )
    }

    @Serializable(Did.Serializer::class)
    @JvmInline
    value class Did(val value: com.nxoim.blean.bskyPrimitives.Did) : AccountIdentificator {
        override fun toString() = value.toString()

        object Serializer : StringWrapperSerializer<Did>(
            "AccountIdentificator.Did",
            { Did(parse(it).getOrThrow()) }
        )
    }
}