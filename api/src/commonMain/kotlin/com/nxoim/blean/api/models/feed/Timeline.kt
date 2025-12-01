package com.nxoim.blean.api.models.feed

import kotlinx.serialization.Serializable

@Serializable
data class Timeline(
    val feed: List<FeedPost>,
    val cursor: String? = null
)