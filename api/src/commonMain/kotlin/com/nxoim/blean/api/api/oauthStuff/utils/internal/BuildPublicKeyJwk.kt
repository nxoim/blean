package com.nxoim.blean.api.api.oauthStuff.utils.internal

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@OptIn(ExperimentalEncodingApi::class)
fun buildPublicKeyJwk(x: ByteArray, y: ByteArray): JsonObject = buildJsonObject {
    put("kty", "EC")
    put("crv", "P-256")
    put("x", Base64.encode(x))
    put("y", Base64.encode(y))
}