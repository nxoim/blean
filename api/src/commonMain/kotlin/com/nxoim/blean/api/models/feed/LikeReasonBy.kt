package com.nxoim.blean.api.models.feed

import com.nxoim.blean.api.models.commonParts.Label
import com.nxoim.blean.api.models.commonParts.TimestampISO8601
import com.nxoim.blean.api.models.commonParts.UriString
import com.nxoim.blean.api.models.commonParts.ViewerState
import com.nxoim.blean.bskyPrimitives.AccountIdentificator
import com.nxoim.blean.bskyPrimitives.Did
import kotlinx.serialization.Serializable

@Serializable
data class LikeReasonBy(
    val did: Did,
    val handle: AccountIdentificator.Handle,
    val displayName: String? = null,
    val avatar: UriString? = null,
    val associated: ProfileAssociated? = null,
    val viewer: ViewerState? = null,
    val labels: List<Label>? = null,
    val createdAt: TimestampISO8601
)