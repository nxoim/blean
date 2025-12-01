package com.nxoim.blean.api.models.account

import kotlinx.serialization.Serializable

@Serializable
data class SavedFeedItem(
    val id: String,
    val type: String,
    /**
     * Either an AT uri or name or something idk. Can contain "following" and at://
     */
    val value: String,
    val pinned: Boolean
) {
    val isFeed = type == "feed"
    val isList = type == "list"
    val isTimeline = type == "timeline"
}