package com.nxoim.blean.api.models.feed

import com.nxoim.blean.api.models.commonParts.ImageWithBlob
import com.nxoim.blean.api.models.commonParts.MediaSizePx
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface PostRecordBlobEmbed {
    @Serializable
    @SerialName("app.bsky.embed.images")
    data class Images(
        val images: List<ImageWithBlob>
    ) : PostRecordBlobEmbed

    @Serializable
    @SerialName("app.bsky.embed.video")
    data class VideoView(
        val alt: String? = null,
        val aspectRatio: MediaSizePx? = null,
        val video: VideoRecordEmbedBlob,
//        val thumb
    ) : PostRecordBlobEmbed {
        init {
            if (alt != null) require(alt.length <= 10000) {
                "alt must be less than 10000 characters"
            }
        }
    }

    @Serializable
    @SerialName("app.bsky.embed.external")
    data class External(
        val external: ExternalMediaEmbed
    ) : PostRecordBlobEmbed

    @Serializable
    @SerialName("app.bsky.embed.record")
    data class RecordView(
        val record: RecordInEmbedInRecordInPost
    ) : PostRecordBlobEmbed

    @Serializable
    @SerialName("app.bsky.embed.recordWithMedia")
    data class RecordWithMediaView(
        val record: RecordInEmbedInRecordInPost, // TODO may be incorrecr
        val media: PostRecordBlobEmbed
    ) : PostRecordBlobEmbed

    @Serializable
    data class Unsupported(@SerialName("\$type") val type: String) : PostRecordBlobEmbed
}