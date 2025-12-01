package com.nxoim.blean.api.models.account

import com.nxoim.blean.api.models.commonParts.Label
import com.nxoim.blean.api.models.commonParts.TimestampISO8601
import com.nxoim.blean.bskyPrimitives.AtUri
import com.nxoim.blean.bskyPrimitives.Cid
import kotlinx.serialization.Serializable

@Serializable
data class JoinedViaStarterPack(
    val uri: AtUri,
    val cid: Cid,
//    val record: String, // TODO record
    val creator: Creator,
    val listItemCount: Int? = null,
    val joinedWeekCount: Int? = null,
    val joinedAllTimeCount: Int? = null,
    val labels: List<Label>? = null,
    val indexedAt: TimestampISO8601
)
