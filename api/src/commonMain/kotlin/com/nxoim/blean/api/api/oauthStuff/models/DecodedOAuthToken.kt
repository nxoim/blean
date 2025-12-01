package com.nxoim.blean.api.api.oauthStuff.models

import com.nxoim.blean.bskyPrimitives.Did
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DecodedOAuthToken(
    val aud: Did,
    val iat: Long,
    val exp: Long,
    val sub: Did,
//    val cnf: ConfirmationOfProofOfPosession // "val jkt"
    @SerialName("client_id") val clientId: String,
    val scope: String,
    val iss: String, // issuer
)

inline val DecodedOAuthToken.scopes get() = scope.split(" ")

inline val DecodedOAuthToken.pdsUrlOrThrow
    get() = pdsUrl ?: error("aud wasnt did web and therefore couldnt turn it into a pds url")

inline val DecodedOAuthToken.pdsUrl
    get() = run {
        if (this.aud is Did.Web)
            "https://${this.aud.valueAfterPrefix()}"
        else
            null
    }