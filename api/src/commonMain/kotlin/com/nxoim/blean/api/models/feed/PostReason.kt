package com.nxoim.blean.api.models.feed

import com.nxoim.blean.api.models.commonParts.TimestampISO8601
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// TODO rename to feed post reason or something
@Serializable
sealed interface PostReason {
    @Serializable
    @SerialName("app.bsky.feed.defs#reasonRepost")
    data class Repost(
        val by: LikeReasonBy,
        val indexedAt: TimestampISO8601
    ) : PostReason

    @Serializable
    @SerialName("app.bsky.feed.defs#pinReason")
    data class Pin(
        val by: LikeReasonBy,
        val indexedAt: TimestampISO8601
    ) : PostReason
}


