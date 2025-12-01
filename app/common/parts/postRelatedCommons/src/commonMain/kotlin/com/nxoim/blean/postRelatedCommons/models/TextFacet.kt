package com.nxoim.blean.postRelatedCommons.models

import kotlinx.serialization.Serializable

@Serializable
sealed interface TextFacet {
    @Serializable
    data class Did(
        val did: com.nxoim.blean.bskyPrimitives.Did,
        val firstCharIndex: Int,
        val lastCharIndex: Int
    ) : TextFacet

    @Serializable
    data class Link(
        val url: String,
        val firstCharIndex: Int,
        val lastCharIndex: Int
    ) : TextFacet

    @Serializable
    data class Tag(
        val tag: String,
        val firstCharIndex: Int,
        val lastCharIndex: Int
    ) : TextFacet
}