package com.nxoim.blean.api.models.feed

import com.nxoim.blean.api.models.commonParts.TimestampISO8601
import com.nxoim.blean.bskyPrimitives.AtUri
import com.nxoim.blean.bskyPrimitives.Cid
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Threadgate(
    val uri: AtUri? = null,
    val cid: Cid? = null,
    val record: ThreadgateRecord? = null,
    val lists: List<ThreadgateList>? = null
)

@Serializable
data class ThreadgateRecord(
    val allow: List<ThreadgateRule>? = null,
    val createdAt: TimestampISO8601,
    val post: AtUri,
//    val hiddenReplies: List<String> // TODO figure out the type
)

@Serializable
sealed interface ThreadgateRule {
    @Serializable
    @SerialName("app.bsky.feed.threadgate#mentionRule")
    data object OnlyMentionedUsers : ThreadgateRule

    @Serializable
    @SerialName("app.bsky.feed.threadgate#followingRule")
    data object OnlyFollowing : ThreadgateRule

    @Serializable
    @SerialName("app.bsky.feed.threadgate#followerRule")
    data object OnlyFollowers : ThreadgateRule

    @Serializable
    @SerialName("app.bsky.feed.threadgate#listRule")
    data class OnlyListMembers(val list: AtUri) : ThreadgateRule
}