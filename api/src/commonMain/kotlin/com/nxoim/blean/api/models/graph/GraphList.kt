package com.nxoim.blean.api.models.graph

import com.nxoim.blean.api.models.account.Associated
import com.nxoim.blean.api.models.account.ListPurpose
import com.nxoim.blean.api.models.commonParts.Label
import com.nxoim.blean.api.models.commonParts.TimestampISO8601
import com.nxoim.blean.api.models.commonParts.UriString
import com.nxoim.blean.api.models.commonParts.ViewerState
import com.nxoim.blean.api.models.feed.Facet
import com.nxoim.blean.api.models.feed.ListViewer
import com.nxoim.blean.api.models.feed.ProfileInfo
import com.nxoim.blean.bskyPrimitives.AtUri
import com.nxoim.blean.bskyPrimitives.Cid
import com.nxoim.blean.bskyPrimitives.Did
import kotlinx.serialization.Serializable

@Serializable
data class GraphList(
    val uri: AtUri,
    val cid: Cid,
    val creator: ProfileInfo,
    val name: String,
    val purpose: ListPurpose,
    val description: String? = null,
    val descriptionFacets: List<Facet>? = null,
    val avatar: UriString? = null,
    val listItemCount: Int? = null,
    val labels: List<String>? = null,
    val viewer: ListViewer? = null,
    val indexedAt: TimestampISO8601
) {
    init {
        require(name.isNotBlank() && name.length <= 64) {
            "name must be non-empty and <= 64 characters"
        }

        if (description != null) require(description.length <= 3000) {
            "description must be less than 3000 characters"
        }
    }
}

@Serializable
data class GraphListItem(
    val uri: AtUri,
    val subject: GraphListItemSubject,
)

@Serializable
data class GraphListItemSubject(
    val did: Did,
    val handle: String,
    val displayName: String? = null,
    val description: String? = null,
    val avatar: UriString? = null,
    val associated: Associated? = null,
    val indexedAt: TimestampISO8601? = null,
    val viewer: ViewerState? = null,
    val labels: List<Label>? = null
) {
    init {
        if (displayName != null) require(displayName.length <= 640) {
            "displayName must be less than 640 characters"
        }
        if (description != null) require(description.length <= 2560) {
            "description must be less than 2560 characters"
        }
    }
}