package com.nxoim.blean.api.models.feed

import com.nxoim.blean.api.models.account.ListPurpose
import com.nxoim.blean.api.models.commonParts.Label
import com.nxoim.blean.api.models.commonParts.TimestampISO8601
import com.nxoim.blean.api.models.commonParts.UriString
import com.nxoim.blean.api.models.commonParts.ViewerState
import com.nxoim.blean.bskyPrimitives.Cid
import kotlinx.serialization.Serializable

@Serializable
data class ThreadgateList(
    val uri: UriString,
    val cid: Cid,
    val name: String,
    val purpose: ListPurpose? = null,
    val avatarUrl: String? = null,
    val listItemCount: Int? = null,
    val labels: List<Label>? = null,
    val viewer: ViewerState? = null,
    val indexedAt: TimestampISO8601? = null
) {
    init {
        require(name.isNotEmpty() && name.length <= 64) {
            "List name must be between 1 and 64 characters"
        }
    }
}
