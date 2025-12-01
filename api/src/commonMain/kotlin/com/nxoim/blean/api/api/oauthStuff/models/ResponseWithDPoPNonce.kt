package com.nxoim.blean.api.api.oauthStuff.models

data class ResponseWithDPoPNonce<T>(
    val dpopNonce: String,
    val response: T
)