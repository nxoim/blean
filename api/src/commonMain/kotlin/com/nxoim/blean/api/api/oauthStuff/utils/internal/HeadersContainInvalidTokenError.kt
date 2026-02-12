package com.nxoim.blean.api.api.oauthStuff.utils.internal

import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode

fun HttpResponse.containsInvalidTokenError(): Boolean {
    if (status == HttpStatusCode.Unauthorized) return true

    return headers
        .getAll(invalidTokenHeaders.first)
        ?.any { header ->
            invalidTokenHeaders.second.any { invalid ->
                header.contains(invalid, ignoreCase = true)
            }
        } == true
}

val invalidTokenHeaders = "WWW-Authenticate" to listOf(
    "invalid_token",
    "invalid_dpop_proof"
)
