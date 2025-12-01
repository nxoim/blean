package com.nxoim.blean.api.models.oauth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Jwks(
    @SerialName("keys") val keys: List<Jwk>
)

@Serializable
data class Jwk(
    @SerialName("kty") val kty: String,
    @SerialName("use") val use: String? = null,
    @SerialName("key_ops") val keyOps: List<String>? = null,
    @SerialName("alg") val alg: String? = null,
    @SerialName("kid") val kid: String? = null,
    @SerialName("x5u") val x5u: String? = null,
    @SerialName("x5c") val x5c: List<String>? = null,
    @SerialName("x5t") val x5t: String? = null,
    @SerialName("x5t#S256") val x5tS256: String? = null
)
