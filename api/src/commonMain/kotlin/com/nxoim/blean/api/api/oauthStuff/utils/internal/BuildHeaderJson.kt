package com.nxoim.blean.api.api.oauthStuff.utils.internal

import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

fun buildHeaderJson(
    x: ByteArray,
    y: ByteArray
) = buildJsonObject {
    put("alg", "ES256")
    put("typ", "dpop+jwt")
    put("jwk", buildPublicKeyJwk(x, y))
}
