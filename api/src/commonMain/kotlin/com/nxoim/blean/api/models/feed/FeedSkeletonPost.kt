package com.nxoim.blean.api.models.feed

import kotlinx.serialization.Serializable

@Serializable
data class FeedSkeletonPost(
    val post: PostView.Visible,
    val reason: PostReason? = null,
    val feedContext: String? = null
) {
    init {
        if (feedContext != null) require(feedContext.length <= 2000) {
            "feedContext must be less than 2000 characters"
        }
    }
}