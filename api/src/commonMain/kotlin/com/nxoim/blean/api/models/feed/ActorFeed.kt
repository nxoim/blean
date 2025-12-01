package com.nxoim.blean.api.models.feed

import kotlinx.serialization.Serializable

@Serializable
data class ActorFeed(val cursor: String? = null, val feed: List<FeedPost>)
