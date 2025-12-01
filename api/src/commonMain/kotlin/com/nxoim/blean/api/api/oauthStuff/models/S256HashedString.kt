package com.nxoim.blean.api.api.oauthStuff.models

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@Serializable
@JvmInline
value class S256HashedString(private val value: String) {
    override fun toString() = value
}