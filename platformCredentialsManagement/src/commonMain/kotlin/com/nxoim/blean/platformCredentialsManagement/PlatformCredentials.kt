package com.nxoim.blean.platformCredentialsManagement

import com.nxoim.blean.bskyPrimitives.AccountIdentificator
import kotlinx.serialization.Serializable

@Serializable
sealed interface PlatformCredentials {
    val did: AccountIdentificator.Did

    @Serializable
    data class AccessJwt(
        override val did: AccountIdentificator.Did,
        val value: String,
        val pdsUrl: String
    ) : PlatformCredentials

    @Serializable
    data class OAuth(val value: OAuthCredentials) : PlatformCredentials {
        override val did get() = value.did
    }
}