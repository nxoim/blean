package com.nxoim.blean.api.models.feed

import com.nxoim.blean.api.models.commonParts.UriString
import com.nxoim.blean.bskyPrimitives.AccountIdentificator
import kotlinx.serialization.Serializable

@Serializable
data class Creator(
    val did: AccountIdentificator.Did,
    val handle: AccountIdentificator.Handle,
    val displayName: String,
    val description: String,
    val avatar: UriString,
)