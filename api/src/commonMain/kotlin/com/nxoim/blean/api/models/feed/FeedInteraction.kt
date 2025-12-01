@file:OptIn(ExperimentalSerializationApi::class)

package com.nxoim.blean.api.models.feed

import com.nxoim.blean.bskyPrimitives.AtUri
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * https://docs.bsky.app/docs/api/app-bsky-feed-send-interactions
 */
@Serializable
data class FeedInteraction(
    val item: AtUri,
    val event: Interaction,
    val feedContext: String? = null,
) {
    init {
        feedContext?.let {
            require(it.length <= 2000) {
                "FeedInteractions feedContext must be no more than 2000 characters"
            }
        }
    }
}

@Serializable
data class FeedInteractions(
    val interactions: List<FeedInteraction>
)

// received unknown interactions
//@Serializable
//data class UnknownInteractions(
//    @SerialName("_unknown_") val unknown:
//)

@Serializable
data object UnknownInteractions

@Serializable
enum class Interaction {
    @SerialName("app.bsky.feed.defs#requestLess")
    RequestLess,
    @SerialName("app.bsky.feed.defs#requestMore")
    RequestMore,
    @SerialName("app.bsky.feed.defs#clickthroughItem")
    ClickthroughItem,
    @SerialName("app.bsky.feed.defs#clickthroughAuthor")
    ClickthroughAuthor,
    @SerialName("app.bsky.feed.defs#clickthroughReposter")
    ClickthroughReposter,
    @SerialName("app.bsky.feed.defs#clickthroughEmbed")
    ClickthroughEmbed,
    @SerialName("app.bsky.feed.defs#interactionSeen")
    Seen,
    @SerialName("app.bsky.feed.defs#interactionLike")
    Like,
    @SerialName("app.bsky.feed.defs#interactionRepost")
    Repost,
    @SerialName("app.bsky.feed.defs#interactionReply")
    Reply,
    @SerialName("app.bsky.feed.defs#interactionQuote")
    Quote,
    @SerialName("app.bsky.feed.defs#interactionShare")
    Share
}