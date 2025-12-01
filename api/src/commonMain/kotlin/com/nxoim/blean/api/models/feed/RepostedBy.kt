package com.nxoim.blean.api.models.feed

import com.nxoim.blean.bskyPrimitives.AtUri
import com.nxoim.blean.bskyPrimitives.Cid
import kotlinx.serialization.Serializable

@Serializable
data class RepostedBy(
    val uri: AtUri,
    val cid: Cid? = null,
    val cursor: String? = null,
    val repostedBy: List<ProfileInfo.Basic2>
)