package com.nxoim.blean.client.clientModels

import com.nxoim.blean.api.models.feed.PostView
import com.nxoim.blean.api.models.feed.SearchedPosts
import kotlinx.serialization.Serializable

/**
 *  ⚠️This is a duplicate of api's model. The api models
 *  guarantee PostView.Visible to be part of [com.nxoim.blean.api.models.feed.SearchedPosts], however
 *  the client intends to cache post content individually, and that
 *  means if a post author gets blocked or whatever - the post
 *  will become unavailable, and we wont be able to deserialize into
 *  the api model. The duplicate allows for retrieval of the most recent
 *  post data
 */
@Serializable
data class SearchChunk(
    val cursor: String? = null,
    val hitsTotal: Int? = null,
    val posts: List<PostView>
)

fun SearchedPosts.toSearchChunk() = SearchChunk(
    cursor = cursor,
    posts = posts
)