package com.nxoim.blean.api.models.oauth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PushedAuthorizationRequest(
    @SerialName("request_uri") val requestUri: String,
    val expiresIn: Int = -1
)