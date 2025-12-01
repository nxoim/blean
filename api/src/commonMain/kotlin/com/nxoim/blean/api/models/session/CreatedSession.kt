package com.nxoim.blean.api.models.session

import com.nxoim.blean.bskyPrimitives.AccountIdentificator
import com.nxoim.blean.bskyPrimitives.Did
import kotlinx.serialization.Serializable

@Serializable
data class CreatedSession(
    val accessJwt: String,
    val refreshJwt: String,
    val handle: AccountIdentificator.Handle,
    val did: Did,
//         val didDoc: DidDoc?  = DidDoc(), todo DID document
    val email: String? = null,
    val emailConfirmed: Boolean? = null,
    val emailAuthFactor: Boolean? = null,
    val active: Boolean? = null,
    val status: String? = null
) {
    val takendown = status == "takendown"
    val suspended = status == "suspended"
    val deactivated = status == "deactivated"
    val _false = status == "false"
}


