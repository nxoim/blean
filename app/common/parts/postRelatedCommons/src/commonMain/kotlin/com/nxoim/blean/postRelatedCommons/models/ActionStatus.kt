package com.nxoim.blean.postRelatedCommons.models

import com.nxoim.blean.bskyPrimitives.AtUri
import kotlin.jvm.JvmInline

sealed interface ActionStatus {
    data object Pending : ActionStatus
    data class Done(val reference: InteractionReference<AtUri>) : ActionStatus
    data object Failed : ActionStatus
}

@JvmInline
value class InteractionReference<T : Any>(val value: T)