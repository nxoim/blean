package com.nxoim.blean.api.models.feed

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@kotlinx.serialization.Serializable
data class VideoRecordEmbedBlob(
    val mimeType: String,
    val size: Int,
    val ref: VideoRecordEmbedBlobRef
)

@Serializable
data class VideoRecordEmbedBlobRef(
    @SerialName("\$link") val link: String
)

