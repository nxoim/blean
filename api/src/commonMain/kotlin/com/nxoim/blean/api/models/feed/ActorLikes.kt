package com.nxoim.blean.api.models.feed

import kotlinx.serialization.Serializable

@Serializable
data class ActorLikes(
    val cursor: String? = null,
    val feed: List<FeedPost>
)