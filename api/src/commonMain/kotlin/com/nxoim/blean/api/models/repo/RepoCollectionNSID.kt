package com.nxoim.blean.api.models.repo

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class RepoCollectionNSID(val id: String) {
    @SerialName(likeId)
    Like(likeId),
    @SerialName(postId)
    Post(postId)
}

private const val likeId = "app.bsky.feed.like"
private const val postId = "app.bsky.feed.post"