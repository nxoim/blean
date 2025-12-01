package com.nxoim.blean.postRelatedCommons.models

import com.nxoim.blean.bskyPrimitives.AtUri

typealias AnyPostContainer = PostContainer<PostType>
typealias AnyAvailablePostContainer = PostContainer.Available<PostType>

sealed interface PostContainer<out T : PostType> {
    val uri: AtUri

    data class Available<T : PostType> (
        val value: T,
    ) : PostContainer<T> {
        override val uri = value.uri
    }

    sealed interface Unavailable : PostContainer<Nothing> {
        data class Detached(override val uri: AtUri) : Unavailable

        data class Blocked(
            override val uri: AtUri,
            val blockReason: BlockReason
        ) : Unavailable

        data class NotFound(override val uri: AtUri) : Unavailable

        data class Unsupported(override val uri: AtUri) : Unavailable
    }
}

sealed interface BlockReason {
    data object AuthorByTheUserManual : BlockReason
    data class AuthorByTheUserFromModList(
        val uri: AtUri,
        val title: String?
    ) : BlockReason
    data object UserByTheAuthor : BlockReason
    data object Unknown : BlockReason
}