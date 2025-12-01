package com.nxoim.blean.postRelatedCommons.models

import com.nxoim.blean.bskyPrimitives.AtUri
import kotlinx.serialization.Serializable

@Serializable
data class FeedChunk(
    val nextCursor: ChunkCursor,
    val postUris: List<AtUri>
)

@Serializable
sealed interface ChunkCursor {
    @Serializable
    data object None : ChunkCursor {
        override fun toString(): String = ""
    }

    @Serializable
    data class Next(val value: String?) : ChunkCursor {
        override fun toString(): String = value ?: ""
    }
}