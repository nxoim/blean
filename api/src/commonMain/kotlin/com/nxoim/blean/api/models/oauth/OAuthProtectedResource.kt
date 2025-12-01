package com.nxoim.blean.api.models.oauth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OAuthProtectedResource(
    @SerialName("resource") val resource: String,
    @SerialName("authorization_servers") val authorizationServers: List<String>,
    @SerialName("scopes_supported") val scopesSupported: List<String>,
    @SerialName("bearer_methods_supported") val bearerMethodsSupported: List<String>,
    @SerialName("resource_documentation") val resourceDocumentation: String,
)