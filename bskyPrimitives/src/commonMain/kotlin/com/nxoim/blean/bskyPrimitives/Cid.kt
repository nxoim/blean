package com.nxoim.blean.bskyPrimitives

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@Serializable(with = Cid.Serializer::class)
@JvmInline
value class Cid(private val value: String) {
    override fun toString(): String = value

    object Serializer : StringWrapperSerializer<Cid>("Cid", ::Cid)
}