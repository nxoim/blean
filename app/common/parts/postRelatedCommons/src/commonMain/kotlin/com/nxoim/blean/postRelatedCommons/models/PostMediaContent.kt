package com.nxoim.blean.postRelatedCommons.models

import kotlinx.serialization.Serializable

@Serializable
sealed interface PostMediaContent {
    @Serializable
    data class ImageWithCDNLinks(
        val thumb: String,
        val fullsize: String,
        val altText: String?,
        val widthPx: Int?,
        val heightPx: Int?,
    ) : PostMediaContent

    @Serializable
    data class Video(
        val cid: String,
        val playlistUri: String,
        val thumbnail: String?,
        val altText: String?,
        val widthPx: Int?,
        val heightPx: Int?,
        val captions: List<VideoCaptions>?
    ) : PostMediaContent

    @Serializable
    data object Unsupported : PostMediaContent
}

@Serializable
data class VideoCaptions(
    val url: String
)

@Serializable
data class TextAndFacets(
    val text: String,
    val facets: List<TextFacet>?
)

@Serializable
data class ThumbnailBlob(
    var link: String,
    var mimeType: String,
    var size: Int
)


