package com.nxoim.blean.api.models.feed

import kotlinx.serialization.Serializable

@Serializable
data class SearchedPosts(
    val cursor: String? = null,
    val hitsTotal: Int? = null,
    val posts: List<PostView.Visible>
)

enum class SearchSort(val requestParameterBody: String) {
    Top("top"),
    Latest("latest")
}