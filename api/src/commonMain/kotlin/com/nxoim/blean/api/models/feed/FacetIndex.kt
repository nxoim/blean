package com.nxoim.blean.api.models.feed

import kotlinx.serialization.Serializable

@Serializable
data class FacetIndex(
    val byteStart: Int,
    val byteEnd: Int
)