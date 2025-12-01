package com.nxoim.blean.api.models.feed

import com.nxoim.blean.api.models.commonParts.UriString
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ExternalMediaEmbed(
    val uri: UriString,
    val title: String,
    val description: String,
    val thumb: ExternalThumbnailBlob? = null
)

@Serializable
data class ExternalPostRecordEmbed(
    val uri: UriString,
    val title: String,
    val description: String,
    val thumb: UriString? = null,
)

// TODO Expected class kotlinx.serialization.json.JsonObject (Kotlin reflection is not
//  available) as the serialized body of com.nxoim.blean.apiCore.models.feed.parts.ExternalThumbnailBlob,
//  but had class kotlinx.serialization.json.JsonLiteral (Kotlin reflection is not available)
@Serializable
data class ExternalThumbnailBlob(
    val mimeType: String,
    val size: Int,
    val ref: ExternalThumbnailBlobRef
)

@Serializable
data class ExternalThumbnailBlobRef(
    @SerialName("\$link") val link: String
)

