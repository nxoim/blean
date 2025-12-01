package com.nxoim.blean.api.models.feed

import com.nxoim.blean.bskyPrimitives.AtUri
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("app.bsky.unspecced.getPostThreadV2#threadItem")
data class ThreadV2Item(
    val uri: AtUri,
    val depth: Int,
    val value: ThreadV2Post
)

@Serializable
sealed interface ThreadV2Post {
    @Serializable
    @SerialName("app.bsky.unspecced.defs#threadItemPost")
    data class Visible(
        val hiddenByThreadgate: Boolean,
        val moreParents: Boolean,
        val moreReplies: Int,
        val mutedByViewer: Boolean,
        val opThread: Boolean,
        val post: PostView.Visible
    ) : ThreadV2Post

    @Serializable
    @SerialName("app.bsky.unspecced.defs#threadItemNoUnauthenticated")
    data object UnavailableUnauthenticated : ThreadV2Post

    @Serializable
    @SerialName("app.bsky.unspecced.defs#threadItemNotFound")
    data object NotFound : ThreadV2Post

    @Serializable
    @SerialName("app.bsky.unspecced.defs#threadItemBlocked")
    data class Blocked(
        val author: ProfileInfo.Blocked
    ) : ThreadV2Post
}