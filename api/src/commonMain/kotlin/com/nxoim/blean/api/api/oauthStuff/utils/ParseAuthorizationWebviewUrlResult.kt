package com.nxoim.blean.api.api.oauthStuff.utils

import com.nxoim.blean.api.api.oauthStuff.models.AuthorizationFromWebviewResult
import io.ktor.http.decodeURLQueryComponent

//The client uses URL query parameters (state and iss) to look up and verify session information.
// Using the code query parameter, the client then makes an initial token
// request to the Authorization Server’s token endpoint. The client completes the
// PKCE flow by including the earlier value in the code_verifier field. Confidential clients need
// to include a client assertion JWT in the token request; see the "Confidential Client" section.
// The Authorization Server validates the request and returns a set of tokens, as well as a sub field indicating
// the account identifier (DID) for this session, and the scope that is covered by the issued access token.
fun parseAuthorizationWebviewUrlResult(url: String): AuthorizationFromWebviewResult {
    val result = url.removeRange(0, url.indexOf("?") + 1)
    val parameters = result
        .split("&")
        .associate {
            val (key, value) = it.split("=")
            key to value
        }

    val code = parameters["code"]
    val state = parameters["state"]
    val iss = parameters["iss"]

    return if (code != null && state != null && iss != null) {
        AuthorizationFromWebviewResult.Success(
            code = code,
            state = state,
            iss = iss.decodeURLQueryComponent()
        )
    } else {
        AuthorizationFromWebviewResult.Error(
            error = parameters["error"] ?: "Unknown error",
            errorDescription = parameters["error_description"] ?: "Unknown error description"
        )
    }
}