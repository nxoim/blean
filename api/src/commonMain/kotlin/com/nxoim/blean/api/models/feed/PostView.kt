package com.nxoim.blean.api.models.feed

import com.nxoim.blean.api.models.commonParts.Label
import com.nxoim.blean.api.models.commonParts.TimestampISO8601
import com.nxoim.blean.bskyPrimitives.AtUri
import com.nxoim.blean.bskyPrimitives.Cid
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface PostView {
    val uri: AtUri

    @Serializable
    @SerialName("app.bsky.feed.defs#postView")
    data class Visible(
        override val uri: AtUri,
        val cid: Cid,
        val author: ProfileInfo.Basic,
        val record: PostRecord,
        val embed: VisiblePostMediaEmbedAtRootBeforeRecord? = null,
        val replyCount: Int? = null,
        val repostCount: Int? = null,
        val likeCount: Int? = null,
        val quoteCount: Int? = null,
        val indexedAt: TimestampISO8601,
        val viewer: FeedViewer? = null,
        val labels: List<Label>? = null,
        val threadgate: Threadgate? = null
    ) : PostView

    @Serializable
    @SerialName("app.bsky.feed.defs#notFoundPost")
    data class NotFound(
        override val uri: AtUri,
        val notFound: Boolean = true// why name it notfound in app.bsky.feed.defs.notFoundPost
    ) : PostView

    @Serializable
    @SerialName("app.bsky.feed.defs#blockedPost")
    data class Blocked(
        override val uri: AtUri,
        val blocked: Boolean,
        val author: ProfileInfo.Blocked
    ) : PostView
}



