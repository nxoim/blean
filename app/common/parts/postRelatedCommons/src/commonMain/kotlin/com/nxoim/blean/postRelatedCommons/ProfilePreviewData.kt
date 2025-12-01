package com.nxoim.blean.postRelatedCommons

import com.nxoim.blean.bskyPrimitives.AccountIdentificator
import com.nxoim.blean.postRelatedCommons.models.LabelData
import kotlinx.serialization.Serializable

@Serializable
data class ProfilePreviewData(
    val did: AccountIdentificator.Did,
    val displayName: String?,
    val handle: AccountIdentificator.Handle,
    val avatarUrl: String?,
    val labels: List<LabelData>?,
    val isFollowedByThisUser: Boolean,
    val isMutedByUser: Boolean,
    val isBlockedByUser: Boolean
)