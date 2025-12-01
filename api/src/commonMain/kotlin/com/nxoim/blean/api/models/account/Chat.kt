package com.nxoim.blean.api.models.account

import kotlinx.serialization.Serializable

@Serializable
data class Chat(val allowIncoming: String) {
    val all = allowIncoming == "all"
    val none = allowIncoming == "none"
    val following = allowIncoming == "following"
}