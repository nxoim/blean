package com.nxoim.blean.api.models.feed

import kotlinx.serialization.Serializable

@Serializable
data class ActorFeeds(
    val cursor: String? = null,
    val feeds: List<FeedDetails>
)