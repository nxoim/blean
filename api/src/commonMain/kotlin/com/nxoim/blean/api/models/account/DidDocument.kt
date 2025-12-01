package com.nxoim.blean.api.models.account

import com.nxoim.blean.api.models.commonParts.UriString
import com.nxoim.blean.bskyPrimitives.AtUri
import com.nxoim.blean.bskyPrimitives.Did
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DidDocument(
    @SerialName("@context") val context: List<String>, // urls
    val alsoKnownAs: List<AtUri>,
    val id: Did,
    val service: List<DidDocumentService>,
    val verificationMethod: List<DidDocumentVerificationMethod>
)

@Serializable
data class DidDocumentService(
    val id: String,
    val serviceEndpoint: UriString,
    val type: String,
)

@Serializable
data class DidDocumentVerificationMethod(
    val controller: Did,
    val id: String,
    val publicKeyMultibase: String,
    val type: String
)