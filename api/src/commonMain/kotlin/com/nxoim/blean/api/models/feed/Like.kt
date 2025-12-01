package com.nxoim.blean.api.models.feed

import com.nxoim.blean.api.models.commonParts.TimestampISO8601
import kotlinx.serialization.Serializable

@Serializable
data class Like(
    val indexedAt: TimestampISO8601,
    val createdAt: TimestampISO8601,
    val actor: ProfileInfo
)