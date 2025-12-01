package com.nxoim.blean.api.models.feed

import com.nxoim.blean.bskyPrimitives.AtUri
import com.nxoim.blean.bskyPrimitives.Cid
import kotlinx.serialization.Serializable

@Serializable
data class Quotes(
    val uri: AtUri,
    val cid: Cid? = null,
    val cursor: String? = null,
    val posts: List<PostView.Visible>
)