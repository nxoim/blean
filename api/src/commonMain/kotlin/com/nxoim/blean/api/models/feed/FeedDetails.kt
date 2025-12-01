package com.nxoim.blean.api.models.feed

import com.nxoim.blean.api.models.commonParts.Label
import com.nxoim.blean.api.models.commonParts.TimestampISO8601
import com.nxoim.blean.api.models.commonParts.UriString
import com.nxoim.blean.bskyPrimitives.Cid
import com.nxoim.blean.bskyPrimitives.Did
import kotlinx.serialization.Serializable

/**
 * @param contentMode Treat posts as normal posts if contentMode is null
 */
@Serializable
data class FeedDetails(
    val uri: UriString,
    val cid: Cid,
    val did: Did,
    val creator: Creator,
    val displayName: String,
    val description: String? = null,
    val descriptionFacets: List<Facet>? = null,
    val avatar: UriString? = null,
    val likeCount: Int? = null,
    val acceptsInteractions: Boolean? = null,
    val contentMode: String? = null,
    val labels: List<Label>? = null,
    val viewer: FeedViewer? = null,
    val indexedAt: TimestampISO8601
) {
    init {
        require(description?.length!! <= 3000) {
            "description must be less than or equal to 3000 characters"
        }
    }

    val isDefaultKindOfFeed = contentMode == null
    val isVideoFeed = contentMode == "app.bsky.feed.defs#contentModeVideo"
}

