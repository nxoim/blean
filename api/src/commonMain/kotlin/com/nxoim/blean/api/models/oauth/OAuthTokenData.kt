package com.nxoim.blean.api.models.oauth

import com.nxoim.blean.bskyPrimitives.Did
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * @param expiresIn seconds until expiration
 */
@Serializable
data class OAuthTokenData(
    @SerialName("access_token") val accessToken: String,
    @SerialName("token_type") val tokenType: String,
    @SerialName("refresh_token") val refreshToken: String?,
    val scope: String,
    @SerialName("expires_in") val expiresIn: Int = -1,
    val sub: Did,
)