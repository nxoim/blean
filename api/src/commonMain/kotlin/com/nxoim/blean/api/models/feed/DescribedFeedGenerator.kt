package com.nxoim.blean.api.models.feed

import com.nxoim.blean.bskyPrimitives.Did
import kotlinx.serialization.Serializable

@Serializable
data class DescribedFeedGenerator(
    val did: Did? = null,
    val feeds: List<String>
)