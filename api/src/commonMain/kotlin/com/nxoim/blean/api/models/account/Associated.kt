package com.nxoim.blean.api.models.account

import kotlinx.serialization.Serializable

// todo move to common models?
@Serializable
data class Associated(
    val lists: Int? = null,
    val feedgens: Int? = null,
    val starterPacks: Int? = null,
    val labeler: Boolean? = null,
    val chat: Chat? = null
)