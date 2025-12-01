package com.nxoim.blean.client

import com.nxoim.blean.models.LoggedInUserBasicDetails
import kotlinx.coroutines.flow.StateFlow

/**
 * Represents an account and provides access to the client state
 * and some basic info about the account.
 */
class Account(
    val basicDetails: StateFlow<LoggedInUserBasicDetails>,
    val client: StateFlow<BleanClient>,
)