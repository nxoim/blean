package com.nxoim.blean.platformCredentialsManagement

import com.nxoim.blean.api.api.oauthStuff.models.ECDSAP256InBase64Keys
import com.nxoim.blean.bskyPrimitives.AccountIdentificator
import kotlinx.serialization.Serializable

@Serializable
data class OAuthCredentials(
    val did: AccountIdentificator.Did,
    val accessToken: String,
    val refreshToken: String,
    val authorizationServerUrl: String,
    val clientId: String,
    val keyPair: ECDSAP256InBase64Keys
)