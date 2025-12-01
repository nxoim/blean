package com.nxoim.blean.api.api.oauthStuff.utils.internal

import io.ktor.client.statement.HttpResponse

fun HttpResponse.headersContainInvalidTokenError() =
    this.headers
        .get("WWW-Authenticate")
        ?.let { (it.contains("invalid_dpop_proof") || it.contains("invalid_token")) } == true