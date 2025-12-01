package com.nxoim.blean.api.api.oauthStuff.utils

import io.ktor.http.URLBuilder
import io.ktor.http.takeFrom

fun buildAuthorizationWebviewUrl(
    authorizationEndpoint: String,
    clientId: String,
    requestUri: String
) = URLBuilder()
    .apply {
        takeFrom(authorizationEndpoint)
        parameters.append("client_id", clientId)
        parameters.append("request_uri", requestUri)
    }
    .buildString()