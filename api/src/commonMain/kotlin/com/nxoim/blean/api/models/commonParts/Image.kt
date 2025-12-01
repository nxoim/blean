package com.nxoim.blean.api.models.commonParts

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ImageWithCDNUrls(
    val thumb: UriString,
    val fullsize: UriString,
    val alt: String,
    val aspectRatio: MediaSizePx? = null
)

@Serializable
data class ImageWithBlob(
    val alt: String,
    val aspectRatio: MediaSizePx? = null,
    val image: ImageBlob
)

@Serializable
data class ImageBlob(
    val mimeType: String,
    val size: Int,
    val ref: ImageBlobRef
)

@Serializable
data class ImageBlobRef(
    @SerialName("\$link") val link: String
)