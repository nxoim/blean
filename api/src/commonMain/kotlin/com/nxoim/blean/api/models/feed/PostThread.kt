package com.nxoim.blean.api.models.feed

import kotlinx.serialization.Serializable

@Serializable
data class PostThread(
    val thread: ThreadPost,
    val threadgate: Threadgate? = null
)