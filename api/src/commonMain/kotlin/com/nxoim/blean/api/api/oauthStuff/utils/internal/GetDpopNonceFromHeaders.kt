package com.nxoim.blean.api.api.oauthStuff.utils.internal

import io.ktor.client.statement.HttpResponse

fun HttpResponse.getDpopNonceFromHeaders() = headers["dpop-nonce"]