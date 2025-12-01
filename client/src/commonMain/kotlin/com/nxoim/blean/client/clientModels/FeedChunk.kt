package com.nxoim.blean.client.clientModels

import com.nxoim.blean.api.models.feed.Feed
import com.nxoim.blean.api.models.feed.PostReason
import com.nxoim.blean.api.models.feed.PostView
import com.nxoim.blean.api.models.feed.Reply
import kotlinx.serialization.Serializable

/**
 *  ⚠️This is a duplicate of api's model. The api models
 *  guarantee PostView.Visible to be part of [com.nxoim.blean.api.models.feed.Feed], however
 *  the client intends to cache post content individually, and that
 *  means if a post author gets blocked or whatever - the post
 *  will become unavailable, and we wont be able to deserialize into
 *  the api model. The duplicate allows for retrieval of the most recent
 *  post data
 */
@Serializable
data class FeedChunk(
    val cursor: String? = null,
    val feed: List<FeedPost>
)

@Serializable
data class FeedPost(
    val post: PostView,
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

fun Feed.toFeedChunk() = FeedChunk(
    cursor = cursor,
    feed = feed.map {
        FeedPost(
            post = it.post,
            reply = it.reply,
            reason = it.reason,
            feedContext = it.feedContext
        )
    }
)