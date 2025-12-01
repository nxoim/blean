package com.nxoim.blean.api.models.account

import kotlinx.serialization.Serializable

// TODO Possible values: &lt; 5 what???
@Serializable
data class KnownFollowers(
    val count: Int,
    val followers: List<Follower>
)