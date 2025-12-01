package com.nxoim.blean.ui.screens.content.feed.models

import com.nxoim.blean.bskyPrimitives.AccountIdentificator
import com.nxoim.blean.bskyPrimitives.Did

data class SavedFeed(
    val id: String,
    val name: String,
    val isPinned: Boolean,
    val avatarUrl: String?,
    val description: String?,
    val isOnline: Boolean,
    val isValid: Boolean,
    val did: Did,
    val cid: String,
    val authorDid: AccountIdentificator.Did,
    val uri: String, // is string because apparentlu can be equal to "following
    val type: FeedType
)

sealed interface FeedType {
    data object Video : FeedType
    data object NormalPosts : FeedType
    data object Unsupported : FeedType
}