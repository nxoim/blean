package com.nxoim.blean.api.api.oauthStuff.models

import com.nxoim.blean.api.api.oauthStuff.utils.internal.hashS256
import com.nxoim.blean.api.api.oauthStuff.utils.internal.hashS256Blocking
import com.nxoim.blean.api.api.oauthStuff.utils.toCodeChallenge
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

/**
 * Can be used to generate code challenge ([toCodeChallenge])
 */
@Serializable
@JvmInline
value class OAuthCodeVerifier(val value: String) {
    fun hashS256Blocking() = value.hashS256Blocking()
    suspend fun hashS256() = value.hashS256()
    override fun toString() = value
}



