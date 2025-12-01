package com.nxoim.blean.api.models.account

import kotlinx.serialization.Serializable

@Serializable
data class Suggestions(
    val cursor: String? = null,
    val actors: List<Profile>
)