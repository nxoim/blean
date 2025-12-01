package com.nxoim.blean.api.models.feed

import kotlinx.serialization.Serializable

@Serializable
data class ThreadV2(
    val thread: List<ThreadV2Item>,
    val hasOtherReplies: Boolean,
    val threadgate: Threadgate? = null
)