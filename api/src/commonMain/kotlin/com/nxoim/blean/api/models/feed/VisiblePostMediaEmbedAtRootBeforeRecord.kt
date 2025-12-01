package com.nxoim.blean.api.models.feed

import com.nxoim.blean.api.models.commonParts.ImageWithCDNUrls
import com.nxoim.blean.api.models.commonParts.MediaSizePx
import com.nxoim.blean.api.models.commonParts.UriString
import com.nxoim.blean.bskyPrimitives.Cid
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface VisiblePostMediaEmbedAtRootBeforeRecord {
    @Serializable
    @SerialName("app.bsky.embed.images#view")
    data class Images(
        val images: List<ImageWithCDNUrls>
    ) : VisiblePostMediaEmbedAtRootBeforeRecord

    @Serializable
    @SerialName("app.bsky.embed.video#view")
    data class VideoView(
        val cid: Cid,
        val playlist: UriString,
        val thumbnail: UriString? = null, // uri
        val alt: String? = null,
        val captions: List<VideoCaptions>? = null,
        val aspectRatio: MediaSizePx? = null
    ) : VisiblePostMediaEmbedAtRootBeforeRecord {
        init {
            if (alt != null) require(alt.length <= 10000) {
                "alt must be less than 10000 characters"
            }
        }
    }

    @Serializable
    @SerialName("app.bsky.embed.external#view")
    data class External(
        val external: ExternalPostRecordEmbed
    ) : VisiblePostMediaEmbedAtRootBeforeRecord

    @Serializable
    @SerialName("app.bsky.embed.record#view")
    data class RecordView(
        val record: EmbedRecord
    ) : VisiblePostMediaEmbedAtRootBeforeRecord

    @Serializable
    @SerialName("app.bsky.embed.recordWithMedia#view")
    data class RecordWithMediaView(
        val record: EmbedRecord,
        val media: VisiblePostMediaEmbedAtRootBeforeRecord
    ) : VisiblePostMediaEmbedAtRootBeforeRecord

    @Serializable
    data class Unsupported(
        @SerialName("\$type") private val type: String
    ) : VisiblePostMediaEmbedAtRootBeforeRecord
}

// im not sure what needs to be null here
@Serializable
data class VideoCaptions(
    val ref: MediaMetadata
)

// im not sure what needs to be null here
@Serializable
data class MediaMetadata(
    @SerialName("\$link") val link: String? = null,
    val mimeType: String? = null,
    val size: Int? = null
)
