package com.nxoim.blean.api.api.oauthStuff.utils.internal

import io.ktor.client.statement.HttpResponse

fun HttpResponse.headersContainUseDpopNonceError() =
    this.headers
        .get("WWW-Authenticate")
        ?.contains("use_dpop_nonce") == true