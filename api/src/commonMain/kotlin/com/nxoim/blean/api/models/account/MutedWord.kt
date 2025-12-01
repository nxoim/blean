package com.nxoim.blean.api.models.account

import com.nxoim.blean.api.models.commonParts.TimestampISO8601
import kotlinx.serialization.Serializable

@Serializable
data class MutedWord(
    val id: String,
    val value: String,
    val targets: List<String>,
    val actorTarget: String? = "all",
    val expiresAt: TimestampISO8601? = null,
) {
    val isTargetingAllActors = actorTarget == "all"
    val isTargetingExcludeFollowing = actorTarget == "exclude-following"

    val isTargetingTags = targets.contains("tag")
    val isTargetingContent = targets.contains("content")
}