package com.nxoim.blean.api.models.feed

import com.nxoim.blean.api.models.commonParts.UriString
import com.nxoim.blean.bskyPrimitives.Did
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface FacetFeature {
    @Serializable
    @SerialName("app.bsky.richtext.facet#mention")
    data class Mention(val did: Did) : FacetFeature

    @Serializable
    @SerialName("app.bsky.richtext.facet#link")
    data class Link(val uri: UriString) : FacetFeature

    @Serializable
    @SerialName("app.bsky.richtext.facet#tag")
    data class Tag(val tag: String) : FacetFeature {
        init {
            require(tag.length <= 640) {
                "Tag length must be less than or equal to 640 characters."
            }
        }
    }

    @Serializable
    data object Unsupported : FacetFeature
}