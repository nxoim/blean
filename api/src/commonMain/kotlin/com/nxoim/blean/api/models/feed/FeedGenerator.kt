package com.nxoim.blean.api.models.feed

import kotlinx.serialization.Serializable

@Serializable
data class FeedGenerator(
    val view: FeedDetails,
    val isOnline: Boolean,
    val isValid: Boolean
)