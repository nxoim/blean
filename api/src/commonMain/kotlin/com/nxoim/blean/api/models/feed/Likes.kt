package com.nxoim.blean.api.models.feed

import com.nxoim.blean.bskyPrimitives.AtUri
import com.nxoim.blean.bskyPrimitives.Cid
import kotlinx.serialization.Serializable

@Serializable
data class Likes(
    val uri: AtUri? = null,
    val cid: Cid? = null,
    val cursor: String? = null,
    val likes: List<Like>
)