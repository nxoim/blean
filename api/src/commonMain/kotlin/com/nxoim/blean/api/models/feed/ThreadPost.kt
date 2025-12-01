package com.nxoim.blean.api.models.feed

import com.nxoim.blean.bskyPrimitives.AtUri
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
sealed interface ThreadPost {
    @Serializable
    @SerialName("app.bsky.feed.defs#threadViewPost")
    data class ThreadPostView(
        val post: PostView.Visible,
        val parent: ThreadPost? = null,
        val replies: List<ThreadPost>? = null
    ) : ThreadPost

    @Serializable
    @SerialName("app.bsky.feed.defs#notFoundPost")
    data class NotFound(
        val uri: AtUri,
        val notFound: Boolean = true
    ) : ThreadPost

    @Serializable
    @SerialName("app.bsky.feed.defs#blockedPost")
    data class Blocked(
        val uri: AtUri,
        val blocked: Boolean,
        val author: ProfileInfo.Blocked
    ) : ThreadPost
}