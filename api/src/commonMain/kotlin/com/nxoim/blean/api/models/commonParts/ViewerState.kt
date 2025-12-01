package com.nxoim.blean.api.models.commonParts

import com.nxoim.blean.api.models.account.BlockingByList
import com.nxoim.blean.api.models.account.KnownFollowers
import com.nxoim.blean.api.models.account.MutedByList
import com.nxoim.blean.bskyPrimitives.AtUri
import kotlinx.serialization.Serializable

@Serializable
data class ViewerState(
    val labels: List<Label>? = null,
    val muted: Boolean? = null,
    val mutedByList: MutedByList? = null,
    val blockedBy: Boolean? = null,
    val blocking: AtUri? = null,
    val blockingByList: BlockingByList? = null,
    val following: AtUri? = null,
    val followedBy: AtUri? = null,
    val knownFollowers: KnownFollowers? = null
) {
    val isBlockingManually = blocking.toString().contains("/app.bsky.graph.block/")
    val isBlockingByList = blockingByList != null || blocking.toString().contains("/app.bsky.graph.list/")
}


