package com.nxoim.blean.postRelatedCommons.models

import com.nxoim.blean.bskyPrimitives.AtUri

sealed interface LikeStatus {
    @ConsistentCopyVisibility
    data class Liked private constructor(val status: ActionStatus) : LikeStatus {
        companion object {
            val Pending = Liked(ActionStatus.Pending)
            @Suppress("FunctionName")
            fun NotPending(reference: AtUri) = Liked(ActionStatus.Done(InteractionReference(reference)))
            val Failed = Liked(ActionStatus.Failed)
        }
    }

    @ConsistentCopyVisibility
    data class NotLiked private constructor(val status: ActionStatus?) : LikeStatus {
        companion object {
            val Pending = NotLiked(ActionStatus.Pending)
            val NotPending = NotLiked(null)
            val Failed = NotLiked(ActionStatus.Failed)
        }
    }
}

val LikeStatus.optimisticIsLiked
    get() = this == LikeStatus.Liked.Pending ||
            (this as? LikeStatus.Liked)?.status is ActionStatus.Done