package com.nxoim.blean.models

import com.nxoim.blean.bskyPrimitives.AccountIdentificator

/**
 * To be used in limited viewing scenarios like auth screen when soft logged out
 */
data class LoggedInUserBasicDetails(
    val did: AccountIdentificator.Did,
    val handle: AccountIdentificator.Handle,
    val displayName: String?,
    val avatarUrl: String?
)