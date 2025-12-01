package com.nxoim.blean.postRelatedCommons.models

import com.nxoim.blean.bskyPrimitives.AtUri
import com.nxoim.blean.bskyPrimitives.Cid
import com.nxoim.blean.postRelatedCommons.ProfilePreviewData
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

sealed interface PostType {
    val identification: PostIdentification
    val content: PostContent
    val visibilityWarnings: Set<PostVisibilityWarning>

    data class Feed(
        override val identification: PostIdentification,
        override val content: PostContent,
        override val visibilityWarnings: Set<PostVisibilityWarning>,
        val replyTarget: PostContainer<ReplyTarget>?,
        val replyRoot: PostContainer<ReplyRoot>?,
        val reasonForAppearingInFeed: FeedPostReason?,
        val userInteractionInfo: UserInteractionInfo
    ) : PostType

    data class ReplyRoot(
        override val identification: PostIdentification,
        override val content: PostContent,
        override val visibilityWarnings: Set<PostVisibilityWarning>,
        val userInteractionInfo: UserInteractionInfo
    ) : PostType

    data class ReplyTarget(
        override val identification: PostIdentification,
        override val content: PostContent,
        override val visibilityWarnings: Set<PostVisibilityWarning>,
        val userInteractionInfo: UserInteractionInfo
    ) : PostType

    data class Quote(
        override val identification: PostIdentification,
        override val content: PostContent,
        override val visibilityWarnings: Set<PostVisibilityWarning>
    ) : PostType

    data class Search(
        override val identification: PostIdentification,
        override val content: PostContent,
        override val visibilityWarnings: Set<PostVisibilityWarning>,
        val userInteractionInfo: UserInteractionInfo
    ) : PostType

    data class Thread(
        override val identification: PostIdentification,
        override val content: PostContent,
        override val visibilityWarnings: Set<PostVisibilityWarning>,
        val threadDepth: Int,
        val userInteractionInfo: UserInteractionInfo,
        val replyPermission: ReplyPermission
    ) : PostType
}

sealed interface FeedPostReason {
    data object Repost : FeedPostReason
    data object Pinned : FeedPostReason
}

@OptIn(ExperimentalTime::class)
data class PostContent(
    val creationDate: Instant,
    val likeCount: Int,
    val replyCount: Int,
    val quoteCount: Int,
    val repostCount: Int,
    val author: ProfilePreviewData,
    val labels: List<LabelData>?,
    val text: TextAndFacets?,
    val media: List<PostMediaContent>?,
    val contentPreview: ContentPreview?,
    val quote: PostContainer<PostType.Quote>?,
)

data class UserInteractionInfo(
    val likeStatus: LikeStatus,
    val repostStatus: RepostStatus
)

sealed interface PostVisibilityWarning {
    data class HiddenBySpoiler(val reasons: List<LabelType>) : PostVisibilityWarning

    sealed interface Muted : PostVisibilityWarning {
        data object Keyword : Muted

        data object AuthorOnItsOwn : Muted

        data class AuthorByList(val listUri: AtUri) : Muted
    }
}

val PostType.uri get() = this.identification.uri

data class PostIdentification(
    val uri: AtUri,
    val cid: Cid
)

val PostType.userInteractionInfo get() = when(this) {
    is PostType.Feed -> this.userInteractionInfo
    is PostType.ReplyRoot -> this.userInteractionInfo
    is PostType.ReplyTarget -> this.userInteractionInfo
    is PostType.Search -> this.userInteractionInfo
    is PostType.Thread -> this.userInteractionInfo
    is PostType.Quote -> null
}

sealed interface ReplyPermission {
    val canUserReply: Boolean

    data class Everyone(override val canUserReply: Boolean = true) : ReplyPermission
    data class NoOne(override val canUserReply: Boolean) : ReplyPermission

    data class Restricted(val rules: Set<Rule>, override val canUserReply: Boolean) : ReplyPermission {
        sealed interface Rule {
            data object Mentioned : Rule
            data object CreatorsFollows : Rule
            data object CreatorsFollowers : Rule
            data class List(val listUri: AtUri) : Rule
        }
    }
}