package com.nxoim.blean.api.models.commonParts

import kotlinx.serialization.Serializable

@Serializable
data class MediaSizePx(
    val width: Int,
    val height: Int
)
