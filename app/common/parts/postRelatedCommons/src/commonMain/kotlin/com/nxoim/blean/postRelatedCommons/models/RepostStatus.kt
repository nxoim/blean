package com.nxoim.blean.postRelatedCommons.models

import com.nxoim.blean.bskyPrimitives.AtUri

sealed interface RepostStatus {
    @ConsistentCopyVisibility
    data class Reposted private constructor(val status: ActionStatus) : RepostStatus { // todo quoted or not
        companion object {
            val Pending = Reposted(ActionStatus.Pending)
            @Suppress("FunctionName")
            fun NotPending(reference: AtUri) = Reposted(ActionStatus.Done(InteractionReference(reference)))
            val Failed = Reposted(ActionStatus.Failed)
        }
    }

    @ConsistentCopyVisibility
    data class NotReposted private constructor(val status: ActionStatus?): RepostStatus {
        companion object {
            val Pending = NotReposted(ActionStatus.Pending)
            val NotPending = NotReposted(null)
            val Failed = NotReposted(ActionStatus.Failed)
        }
    }
}

val RepostStatus.optimisticIsReposted
    get() = this == RepostStatus.Reposted.Pending ||
            (this as? RepostStatus.Reposted)?.status is ActionStatus.Done