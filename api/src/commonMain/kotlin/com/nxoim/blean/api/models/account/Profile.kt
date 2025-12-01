package com.nxoim.blean.api.models.account

import com.nxoim.blean.api.models.commonParts.Label
import com.nxoim.blean.api.models.commonParts.TimestampISO8601
import com.nxoim.blean.api.models.commonParts.UriString
import com.nxoim.blean.api.models.commonParts.ViewerState
import com.nxoim.blean.bskyPrimitives.AccountIdentificator
import kotlinx.serialization.Serializable

// seemingly mirrors GetProfile.Response.Success
@Serializable
data class Profile(
    val did: AccountIdentificator.Did,
    val handle: AccountIdentificator.Handle,
    val displayName: String? = null,
    val description: String? = null,
    val avatar: UriString? = null,
    val banner: UriString? = null,
    val followersCount: Int? = null,
    val followsCount: Int? = null,
    val postsCount: Int? = null,
    val associated: Associated? = null,
    val joinedViaStarterPack: JoinedViaStarterPack? = null,
    val indexedAt: TimestampISO8601? = null,
    val createdAt: TimestampISO8601? = null,
    val viewer: ViewerState? = null,
    val labels: List<Label>? = null
) {
    init {
        if (displayName != null) require(displayName.length <= 640) {
            "displayName must be less than or equal to 640 characters"
        }
        if (description != null) require(description.length <= 2560) {
            "description must be less than or equal to 640 characters"
        }
    }
}