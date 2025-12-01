package com.nxoim.blean.api.models.feed

import com.nxoim.blean.api.models.commonParts.TimestampISO8601
import com.nxoim.blean.bskyPrimitives.AtUri
import kotlinx.serialization.Serializable

@Serializable
data class StarterPackRecord(
    val createdAt: TimestampISO8601,
    val description: String? = null,
    val feeds: List<StarterPackFeedItem>? = null,
    val descriptionFacets: List<Facet>? = null,
    val list: AtUri,
    val name: String,
    val updatedAt: TimestampISO8601? = null
) {
    init {
        if (description != null) {
            require(description.length <= 3000) {
                "description must be less than 3000 characters"
            }
        }
    }
}

@Serializable
data class StarterPackFeedItem(
    val uri: AtUri,
)