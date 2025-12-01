package com.nxoim.blean.api.models.feed

import kotlinx.serialization.Serializable

@Serializable
data class FeedPost(
    val post: PostView.Visible,
    val reply: Reply? = null,
    val reason: PostReason? = null,
    val feedContext: String? = null // Context provided by feed generator that may be passed back alongside interactions.
) {
    init {
        if (feedContext != null) require(feedContext.length <= 2000) {
            "feedContext must be less than 2000 characters"
        }
    }
}

