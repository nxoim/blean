package com.nxoim.blean.api.api.oauthStuff.models

import kotlinx.serialization.Serializable

@Serializable
sealed interface AuthorizationFromWebviewResult {
    @Serializable
    data class Success(
        val code: String,
        val state: String,
        /**
         * Issuer
         */
        val iss: String
    ) : AuthorizationFromWebviewResult

    @Serializable
    data class Error(
        val error: String,
        val errorDescription: String
    ) : AuthorizationFromWebviewResult
}
