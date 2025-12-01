package com.nxoim.blean.api.models.feed

import com.nxoim.blean.bskyPrimitives.AtUri
import kotlinx.serialization.Serializable

@Serializable
data class FeedViewer(
    val repost: AtUri? = null,
    val like: AtUri? = null,
    val threadMuted: Boolean? = null,
    val replyDisabled: Boolean? = null,
    val embeddingDisabled: Boolean? = null,
    val pinned: Boolean? = null
)