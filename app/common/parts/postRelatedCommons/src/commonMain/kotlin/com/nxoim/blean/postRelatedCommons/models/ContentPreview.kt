package com.nxoim.blean.postRelatedCommons.models

import com.nxoim.blean.bskyPrimitives.AtUri
import com.nxoim.blean.postRelatedCommons.ProfilePreviewData
import kotlinx.serialization.Serializable

@Serializable
sealed interface ContentPreview {
    @Serializable
    data class Link(
        val url: String,
        val description: String?,
        val thumbnailCdnUrl: String?,
//        val thumbnailBlob: ThumbnailBlob?,
        val title: String?
    ) : ContentPreview

    @Serializable
    data class FeedGenerator(
        val uri: AtUri,
        val title: String,
        val description: TextAndFacets?,
        val creator: ProfilePreviewData,
        val avatarUrl: String?,
        val likeCount: Int
    ) : ContentPreview

    @Serializable
    data class StarterPack(
        val uri: AtUri,
        val title: String?,
        val description: TextAndFacets?,
        val creator: ProfilePreviewData,
        val userCount: Int = 0
    ) : ContentPreview

    @Serializable
    data class List(
        val uri: AtUri,
        val title: String,
        val description: TextAndFacets?,
        val creator: ProfilePreviewData,
        val purpose: Purpose,
        val avatarUrl: String?,
        val entryCount: Int?
    ) : ContentPreview {
        sealed interface Purpose {
            data object Mod : Purpose
            data object Curate : Purpose
            data object Reference : Purpose
        }
    }

    @Serializable
    data class Labeler(
        val uri: AtUri,
        val title: String,
        val description: TextAndFacets?,
        val creator: ProfilePreviewData,
    ) : ContentPreview

    @Serializable
    data class Unsupported(val uri: AtUri?) : ContentPreview
}
