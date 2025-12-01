package com.nxoim.blean.api.api.oauthStuff.utils

import com.nxoim.blean.api.api.oauthStuff.models.DPoPProof
import com.nxoim.blean.api.api.oauthStuff.models.EllipticCurvePublicKeyCoordinates
import com.nxoim.blean.api.api.oauthStuff.models.SignedJWTMessage
import com.nxoim.blean.api.api.oauthStuff.models.UnsignedJWTMessage
import com.nxoim.blean.api.api.oauthStuff.utils.internal.buildHeaderJson
import com.nxoim.blean.api.api.oauthStuff.utils.internal.generateRandomValueEncodedInBase64
import com.nxoim.blean.api.api.oauthStuff.utils.internal.hashS256Blocking
import io.ktor.utils.io.core.toByteArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

inline fun DPoPProof.Companion.generateForResourceRequest(
    url: String,
    method: String, // TODO enum? Use ktors utils?
    dPoPNonce: String,
    accessToken: String,
    authorizationServerIssuer: String,
    publicKey: EllipticCurvePublicKeyCoordinates,
    timeEpochSeconds: Long,
    sign: (UnsignedJWTMessage) -> SignedJWTMessage,
): DPoPProof {
    val headerJson = buildHeaderJson(publicKey.x, publicKey.y)

    val payloadJson = buildJsonObject {
        put("iss", authorizationServerIssuer)
        put("iat", timeEpochSeconds)
        put("exp", timeEpochSeconds + 60)
        put("jti", generateRandomValueEncodedInBase64())
        put("htm", method)
        put("htu", url)
        put("ath", accessToken.hashS256Blocking().toString())
        put("nonce", dPoPNonce)
    }

    return generateDPoPProof(headerJson, payloadJson, sign)
}

// todo explain the purpose cause wtf
inline fun DPoPProof.Companion.generateForTokenRequest(
    clientId: String,
    tokenEndpoint: String,
    tokenEndpointDpopNonce: String,
    publicKey: EllipticCurvePublicKeyCoordinates,
    timeEpochSeconds: Long,
    sign: (UnsignedJWTMessage) -> SignedJWTMessage
): DPoPProof {
    val headerJson = buildHeaderJson(publicKey.x, publicKey.y)

    val payloadJson = buildJsonObject {
        put("iss", clientId)
        put("sub", clientId)
        put("htu", tokenEndpoint)
        put("htm", "POST") // Typically POST for token requests
        put("exp", timeEpochSeconds + 60)
        put("jti", generateRandomValueEncodedInBase64())
        put("iat", timeEpochSeconds)
        put("nonce", tokenEndpointDpopNonce)
    }

    return generateDPoPProof(headerJson, payloadJson, sign)
}

@OptIn(ExperimentalEncodingApi::class)
inline fun generateDPoPProof(
    headerJson: JsonObject,
    payloadJson: JsonObject,
    sign: (UnsignedJWTMessage) -> SignedJWTMessage
): DPoPProof {
    val headerBase64 = Base64.encode(headerJson.toString().toByteArray())
    val payloadBase64 = Base64.encode(payloadJson.toString().toByteArray())
    val jwtMessage = "$headerBase64.$payloadBase64"
    val signedJwtMessage = sign(UnsignedJWTMessage(jwtMessage))
    val jwtSignature = Base64.encode(signedJwtMessage.value)
    return DPoPProof("$headerBase64.$payloadBase64.$jwtSignature")
}