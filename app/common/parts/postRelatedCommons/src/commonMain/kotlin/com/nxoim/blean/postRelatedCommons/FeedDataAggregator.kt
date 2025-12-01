package com.nxoim.blean.postRelatedCommons

import co.touchlab.kermit.Logger
import com.github.michaelbull.result.getOrThrow
import com.nxoim.blean.api.models.account.ListPurpose
import com.nxoim.blean.api.models.commonParts.Label
import com.nxoim.blean.api.models.feed.EmbedRecord
import com.nxoim.blean.api.models.feed.Facet
import com.nxoim.blean.api.models.feed.FacetFeature
import com.nxoim.blean.api.models.feed.FacetIndex
import com.nxoim.blean.api.models.feed.PostReason
import com.nxoim.blean.api.models.feed.PostRecord
import com.nxoim.blean.api.models.feed.PostView
import com.nxoim.blean.api.models.feed.ProfileInfo
import com.nxoim.blean.api.models.feed.Reply
import com.nxoim.blean.api.models.feed.Threadgate
import com.nxoim.blean.api.models.feed.ThreadgateRule
import com.nxoim.blean.api.models.feed.VisiblePostMediaEmbedAtRootBeforeRecord
import com.nxoim.blean.bskyPrimitives.AccountIdentificator
import com.nxoim.blean.bskyPrimitives.AtUri
import com.nxoim.blean.bskyPrimitives.Did
import com.nxoim.blean.postRelatedCommons.models.BlockReason
import com.nxoim.blean.postRelatedCommons.models.ContentPreview
import com.nxoim.blean.postRelatedCommons.models.FeedPostReason
import com.nxoim.blean.postRelatedCommons.models.LabelData
import com.nxoim.blean.postRelatedCommons.models.LabelDataUri
import com.nxoim.blean.postRelatedCommons.models.LabelType
import com.nxoim.blean.postRelatedCommons.models.LikeStatus
import com.nxoim.blean.postRelatedCommons.models.PostContainer
import com.nxoim.blean.postRelatedCommons.models.PostContent
import com.nxoim.blean.postRelatedCommons.models.PostIdentification
import com.nxoim.blean.postRelatedCommons.models.PostMediaContent
import com.nxoim.blean.postRelatedCommons.models.PostType
import com.nxoim.blean.postRelatedCommons.models.PostVisibilityWarning
import com.nxoim.blean.postRelatedCommons.models.ReplyPermission
import com.nxoim.blean.postRelatedCommons.models.RepostStatus
import com.nxoim.blean.postRelatedCommons.models.TextAndFacets
import com.nxoim.blean.postRelatedCommons.models.TextFacet
import com.nxoim.blean.postRelatedCommons.models.UserInteractionInfo
import com.nxoim.blean.postRelatedCommons.models.VideoCaptions
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

private const val logTag = "FeedDataAggregator"

object FeedDataAggregator {
    fun buildThreadPost( // todo use
        logger: Logger,
        post: PostView,
        threadDepth: Int,
        expectedLikedStatus: Boolean? = null,
        viewerDid: AccountIdentificator.Did?,
        authorDid: AccountIdentificator.Did,
        viewerIsMentioned: Boolean,
        authorFollowsViewer: Boolean,
        viewerFollowsAuthor: Boolean,
        viewerListMemberships: Set<AtUri>,
        isGloballyBlocked: Boolean
    ): PostContainer<PostType.Thread> = with(logger) {
        when (post) {
            is PostView.Blocked ->
                PostContainer.Unavailable.Blocked(post.uri, post.getBlockReason())

            is PostView.NotFound ->
                PostContainer.Unavailable.NotFound(post.uri)

            is PostView.Visible -> {
                val content = processPostContent(post, expectedLikedStatus)

                val permission = post.threadgate.toReplyPermission(
                    viewerDid = viewerDid.toString(),
                    authorDid = authorDid.toString(),
                    viewerIsMentioned = viewerIsMentioned,
                    authorFollowsViewer = authorFollowsViewer,
                    viewerFollowsAuthor = viewerFollowsAuthor,
                    viewerListMemberships = viewerListMemberships,
                    isGloballyBlocked = isGloballyBlocked
                )

                return PostContainer.Available(
                    PostType.Thread(
                        content = content,
                        visibilityWarnings = buildSet {
                            extractSpoilerReasons(content.labels)?.let(::add)
                        },
                        userInteractionInfo = buildFullUserRelatedPostInfo(
                            post,
                            expectedLikedStatus
                        ),
                        threadDepth = threadDepth,
                        replyPermission = permission,
                        identification = post.extractIdentification()
                    )
                )
            }
        }
    }

    fun buildThreadPost(
        logger: Logger,
        post: PostView,
        threadDepth: Int,
        expectedLikedStatus: Boolean? = null
    ): PostContainer<PostType.Thread> = with(logger) {
        when (post) {
            is PostView.Blocked ->
                PostContainer.Unavailable.Blocked(post.uri, post.getBlockReason())

            is PostView.NotFound ->
                PostContainer.Unavailable.NotFound(post.uri)

            is PostView.Visible -> {
                val content = processPostContent(post, expectedLikedStatus)

                return PostContainer.Available(
                    PostType.Thread(
                        content = content,
                        visibilityWarnings = buildSet {
                            extractSpoilerReasons(content.labels)?.let {
                                add(it)
                            }
                        },
                        userInteractionInfo = buildFullUserRelatedPostInfo(
                            post,
                            expectedLikedStatus
                        ),
                        threadDepth = threadDepth,
                        replyPermission = post.threadgate.toReplyPermission(),
                        identification = post.extractIdentification()
                    )
                )
            }
        }
    }

    context(logger: Logger)
    fun buildFeedPost(
        post: PostView,
        reply: Reply? = null,
        reason: PostReason? = null,
        expectedLikedStatus: Boolean? = null,
        expectedReplyRootLikeStatus: Boolean? = null,
        expectedReplyTargetLikeStatus: Boolean? = null
    ): PostContainer<PostType.Feed> {
        return when (post) {
            is PostView.Blocked ->
                PostContainer.Unavailable.Blocked(post.uri, post.getBlockReason())

            is PostView.NotFound ->
                PostContainer.Unavailable.NotFound(post.uri)

            is PostView.Visible -> {
                val content = processPostContent(post, expectedLikedStatus)

                return PostContainer.Available(
                    PostType.Feed(
                        content = content,
                        replyRoot = reply?.root?.let {
                            buildReplyRootPost(
                                it,
                                expectedReplyRootLikeStatus
                            )
                        },
                        replyTarget = reply?.parent?.let {
                            buildReplyTargetPost(
                                it,
                                expectedReplyTargetLikeStatus
                            )
                        },
                        reasonForAppearingInFeed = reason?.let { processFeedPostReason(it) },
                        visibilityWarnings = buildSet {
                            extractSpoilerReasons(content.labels)?.let {
                                add(it)
                            }
                        },
                        userInteractionInfo = buildFullUserRelatedPostInfo(
                            post,
                            expectedLikedStatus
                        ),
                        identification = post.extractIdentification()
                    )
                )
            }
        }
    }

    context(logger: Logger)
    fun buildSearchPost(
        post: PostView,
        expectedLikedStatus: Boolean? = null
    ): PostContainer<PostType.Search> {
        return when (post) {
            is PostView.Blocked ->
                PostContainer.Unavailable.Blocked(post.uri, post.getBlockReason())

            is PostView.NotFound ->
                PostContainer.Unavailable.NotFound(post.uri)

            is PostView.Visible -> {
                val content = processPostContent(post, expectedLikedStatus)
                val visibilityWarnings = buildSet {
                    extractSpoilerReasons(content.labels)?.let {
                        add(it)
                    }
                }

                PostContainer.Available(
                    PostType.Search(
                        identification = post.extractIdentification(),
                        content = content,
                        visibilityWarnings = visibilityWarnings,
                        userInteractionInfo = buildFullUserRelatedPostInfo(
                            post,
                            expectedLikedStatus
                        )
                    )
                )
            }
        }
    }
}

context(logger: Logger)
private fun buildReplyRootPost(post: PostView, expectedLikedStatus: Boolean?) = when (post) {
    is PostView.Blocked -> PostContainer.Unavailable.Blocked(post.uri, post.getBlockReason())
    is PostView.NotFound -> null
    is PostView.Visible -> {
        val content = processPostContent(post, expectedLikedStatus)

        PostContainer.Available<PostType.ReplyRoot>(
            PostType.ReplyRoot(
                identification = post.extractIdentification(),
                content = content,
                visibilityWarnings = buildSet {
                    extractSpoilerReasons(content.labels)?.let {
                        add(it)
                    }
                },
                userInteractionInfo = buildFullUserRelatedPostInfo(
                    post,
                    expectedLikedStatus
                )
            )
        )
    }
}

context(logger: Logger)
private fun buildReplyTargetPost(post: PostView, expectedLikedStatus: Boolean?) = when (post) {
    is PostView.Blocked -> {
        PostContainer.Unavailable.Blocked(post.uri, post.getBlockReason())
    }

    is PostView.NotFound -> null
    is PostView.Visible -> {
        val content = processPostContent(post, expectedLikedStatus)

        PostContainer.Available(
            value = PostType.ReplyTarget(
                identification = post.extractIdentification(),
                content = content,
                visibilityWarnings = buildSet {
                    extractSpoilerReasons(content.labels)?.let {
                        add(it)
                    }
                },
                userInteractionInfo = buildFullUserRelatedPostInfo(
                    post,
                    expectedLikedStatus
                )
            )
        )
    }
}

private fun processFeedPostReason(reason: PostReason) = when (reason) {
    is PostReason.Pin -> FeedPostReason.Pinned
    is PostReason.Repost -> FeedPostReason.Repost
}

private fun extractSpoilerReasons(labelData: List<LabelData>?): PostVisibilityWarning.HiddenBySpoiler? {
    val spoilerReasons = labelData?.run { mutableListOf<LabelType>() }
    labelData?.forEach { label ->
        when (val labelType = label.value) {
            LabelType.NoUnauthenticated,
            is LabelType.Unsupported -> {
            }

            else -> spoilerReasons?.add(labelType)
        }
    }

    return if (!spoilerReasons.isNullOrEmpty()) {
        PostVisibilityWarning.HiddenBySpoiler(spoilerReasons)
    } else null
}

private fun buildFullUserRelatedPostInfo(
    post: PostView.Visible,
    expectedLikedStatus: Boolean?
): UserInteractionInfo {
    var likeReference: AtUri? = null
    var repostReference: AtUri? = null

    post.viewer?.let { viewer ->
        likeReference = viewer.like
        // can get thread muted
        repostReference = viewer.repost
//
//            if (viewer.muted == true)
//                visibilityWarnings.add(PostVisibilityWarning.Muted.AuthorOnItsOwn)

        // TODO is this author blocked?
    }

    return UserInteractionInfo(
        likeStatus = when {
            expectedLikedStatus == true -> LikeStatus.Liked.Pending
            expectedLikedStatus == false -> LikeStatus.NotLiked.Pending
            likeReference != null -> LikeStatus.Liked.NotPending(likeReference)

            else -> LikeStatus.NotLiked.NotPending
        },
        repostStatus = when {
            repostReference != null -> RepostStatus.Reposted.NotPending(repostReference)
            else -> RepostStatus.NotReposted.NotPending
        },
    )
}

context(logger: Logger)
private fun processPostContent(post: PostView.Visible, expectedLikedStatus: Boolean?): PostContent {
    val profilePreviewData = createProfilePreviewData(post.author)
    val labelData = post.labels?.let { buildLabels(it) }

    val text = post.record.toUsableTextAndFacets()

    val (media, contentPreview, quoteContent) = processEmbed(post.embed)
    val likeNumberOffset = when {
//        expectedLikedStatus == true && post.viewer != null && post.viewer!!.like != null -> 0
//        expectedLikedStatus == false && post.viewer != null && post.viewer!!.like == null -> 0
        expectedLikedStatus == true && post.viewer != null && post.viewer!!.like == null -> 1
        expectedLikedStatus == false && post.viewer != null && post.viewer!!.like != null -> -1
        else -> 0
    }

    @OptIn(ExperimentalTime::class)
    return PostContent(
        creationDate = Instant.parse(post.indexedAt),
        likeCount = post.likeCount?.let { it + likeNumberOffset } ?: -1,
        replyCount = post.replyCount ?: -1,
        repostCount = post.repostCount ?: -1,
        quoteCount = post.quoteCount ?: -1,
        author = profilePreviewData,
        labels = labelData,
        text = text,
        media = media,
        contentPreview = contentPreview,
        quote = quoteContent
    )
}

private fun createProfilePreviewData(author: ProfileInfo.Basic) = ProfilePreviewData(
    did = author.did,
    handle = author.handle,
    displayName = author.displayName,
    avatarUrl = author.avatar,
    labels = author.labels?.let { buildLabels(it) },
    isFollowedByThisUser = author.associated?.viewer?.followedBy != null,
    isMutedByUser = author.viewer?.muted == true,
    isBlockedByUser = author.viewer?.blockedBy == true
)

context(logger: Logger)
private fun processEmbed(
    embed: VisiblePostMediaEmbedAtRootBeforeRecord?
): Triple<List<PostMediaContent>, ContentPreview?, PostContainer<PostType.Quote>?> {
    val media = mutableListOf<PostMediaContent>()
    var contentPreview: ContentPreview? = null
    // TODO var unavailableContent:
    var quoteContent: PostContainer<PostType.Quote>? = null
    val unprocessedContentList = mutableListOf<String>()

    when (embed) {
        is VisiblePostMediaEmbedAtRootBeforeRecord.External -> {
            contentPreview = ContentPreview.Link(
                url = embed.external.uri,
                description = embed.external.description,
                title = embed.external.title,
                thumbnailCdnUrl = embed.external.thumb,
            )
        }

        is VisiblePostMediaEmbedAtRootBeforeRecord.Images -> {
            media.addAll(
                embed.images.map { image ->
                    PostMediaContent.ImageWithCDNLinks(
                        thumb = image.thumb,
                        fullsize = image.fullsize,
                        altText = image.alt,
                        widthPx = image.aspectRatio?.width,
                        heightPx = image.aspectRatio?.height
                    )
                }
            )
        }

        is VisiblePostMediaEmbedAtRootBeforeRecord.RecordView -> {
            processEmbedRecord(embed.record).run {
                first?.let { contentPreview = it }
                second?.let { quoteContent = it }
            }
        }

        is VisiblePostMediaEmbedAtRootBeforeRecord.RecordWithMediaView -> {
            processEmbedRecord(embed.record).run {
                first?.let { contentPreview = it }
                second?.let { quoteContent = it }
            }

            processEmbed(embed.media).run {
                first.let { media.addAll(it) }
                second?.let { contentPreview = it }
                third?.let { quoteContent = it }
            }
        }

        is VisiblePostMediaEmbedAtRootBeforeRecord.Unsupported -> {
            unprocessedContentList.add(embed.toString())
            logger.i(tag = logTag) { "Unsupported embed: $embed" }
        }

        is VisiblePostMediaEmbedAtRootBeforeRecord.VideoView -> {
            media.add(
                PostMediaContent.Video(
                    cid = embed.cid.toString(),
                    playlistUri = embed.playlist,
                    thumbnail = embed.thumbnail,
                    altText = embed.alt,
                    widthPx = embed.aspectRatio?.width,
                    heightPx = embed.aspectRatio?.height,
                    captions = embed.captions?.mapNotNull {
                        it.ref.link?.let { url ->
                            VideoCaptions(url)
                        }
                    }
                )
            )
        }

        null -> {

        }
    }

    if (unprocessedContentList.isNotEmpty())
        logger.i(tag = logTag) { "unprocessed content ${unprocessedContentList.joinToString("\n-")}" }
    return Triple(media, contentPreview, quoteContent)
}

context(logger: Logger)
private fun processEmbedRecord(record: EmbedRecord): Pair<ContentPreview?, PostContainer<PostType.Quote>?> {
    var contentPreview: ContentPreview? = null
    var quoteContent: PostContainer<PostType.Quote>? = null

    when (record) {
        is EmbedRecord.Blocked -> quoteContent = PostContainer.Unavailable.Blocked(
            uri = record.uri,
            blockReason = record.getBlockedReason()
        )

        is EmbedRecord.Detached -> quoteContent = PostContainer.Unavailable.Detached(
            uri = record.uri
        )

        is EmbedRecord.FeedGenerator -> contentPreview = ContentPreview.FeedGenerator(
            uri = record.uri,
            title = record.displayName,
            description = record.description?.let { description ->
                TextAndFacets(
                    text = description,
                    facets = record.descriptionFacets?.flatMap { it.toUsable(description) }
                )
            },
            creator = createProfilePreviewData(record.creator),
            avatarUrl = record.avatar,
            likeCount = record.likeCount ?: 0
        )

        is EmbedRecord.Labeler -> contentPreview = ContentPreview.Labeler(
            uri = record.uri,
            title = record.creator.toString(),
            description = TextAndFacets(
                text = "null description",
                facets = null
            ),
            creator = createProfilePreviewData(record.creator)
        )

        // TODO use list purposes. mod/ curation/reference/etc
        is EmbedRecord.Lists -> contentPreview = ContentPreview.List(
            uri = record.uri,
            title = record.name,
            description = record.description?.let { description ->
                TextAndFacets(
                    text = description,
                    facets = record.descriptionFacets?.flatMap { it.toUsable(description) }
                )
            },
            creator = createProfilePreviewData(record.creator),
            purpose = when (val it = record.purpose) {
                ListPurpose.Mod -> ContentPreview.List.Purpose.Mod
                ListPurpose.Curate -> ContentPreview.List.Purpose.Curate
                ListPurpose.Reference -> ContentPreview.List.Purpose.Reference
            },
            avatarUrl = record.avatar,
            entryCount = record.listItemCount
        )

        is EmbedRecord.NotFound -> {
            // do not display not found things
        }

        is EmbedRecord.StarterPackBasic -> contentPreview = ContentPreview.StarterPack(
            uri = record.uri,
            title = record.record.name,
            description = record.record.description?.let { description ->
                TextAndFacets(
                    text = description,
                    facets = record.record.descriptionFacets?.flatMap { it.toUsable(description) }
                )
            },
            creator = createProfilePreviewData(record.creator)
        )
//            is EmbedRecord.Unsupported -> contentPreview = ContentPreview.Unsupported(record.uri)
        is EmbedRecord.View -> {
            // QUOTED STUFF
            val quoteEmbeds = record.embeds?.map {
                processEmbed(it)
            }
            if (quoteEmbeds != null && quoteEmbeds.size > 1) {
                logger.i(tag = logTag) { "Quote has more than one embed" }
            }
            val quoteLabelData = record.labels?.let { buildLabels(it) }
            val quoteVisibilityWarnings = buildSet {
                extractSpoilerReasons(quoteLabelData)?.let {
                    add(it)
                }
            }
            @OptIn(ExperimentalTime::class)
            quoteContent = PostContainer.Available(
                PostType.Quote(
                    identification = record.extractIdentification(),
                    content = PostContent(
                        creationDate = Instant.parse(record.indexedAt),
                        likeCount = record.likeCount ?: -1,
                        replyCount = record.replyCount ?: -1,
                        repostCount = record.repostCount ?: -1,
                        quoteCount = record.quoteCount ?: -1,
                        author = createProfilePreviewData(record.author),
                        labels = quoteLabelData,
                        text = record.value.toUsableTextAndFacets(),
                        media = quoteEmbeds?.firstOrNull()?.first,
                        contentPreview = quoteEmbeds?.firstOrNull()?.second,
                        quote = null // no quotes inside quotes. should i make more models for cases like this?
                    ),
                    visibilityWarnings = quoteVisibilityWarnings
                )
            )
        }
    }

    return Pair(contentPreview, quoteContent)
}

private fun PostRecord.toUsableTextAndFacets() =
    TextAndFacets(
        text = this.text,
        facets = this.facets?.flatMap { it.toUsable(this.text) }
    )

fun String.byteToCharIndex(byteIndex: Int): Int {
    val bytes = this.encodeToByteArray()
    if (byteIndex <= 0) return 0
    if (byteIndex >= bytes.size) return this.length
    return bytes.decodeToString(endIndex = byteIndex).length
}

fun String.charToByteIndex(charIndex: Int): Int {
    if (charIndex <= 0) return 0
    if (charIndex >= this.length) return this.encodeToByteArray().size
    return this.substring(0, charIndex).encodeToByteArray().size
}

fun TextFacet.toFacet(text: String): Facet {
    val byteStart: Int
    val byteEnd: Int

    when (this) {
        is TextFacet.Did -> {
            byteStart = text.charToByteIndex(firstCharIndex)
            byteEnd = text.charToByteIndex(lastCharIndex)
            return Facet(
                index = FacetIndex(byteStart, byteEnd),
                features = listOf(FacetFeature.Mention(did))
            )
        }

        is TextFacet.Link -> {
            byteStart = text.charToByteIndex(firstCharIndex)
            byteEnd = text.charToByteIndex(lastCharIndex)
            return Facet(
                index = FacetIndex(byteStart, byteEnd),
                features = listOf(FacetFeature.Link(url))
            )
        }

        is TextFacet.Tag -> {
            byteStart = text.charToByteIndex(firstCharIndex)
            byteEnd = text.charToByteIndex(lastCharIndex)
            return Facet(
                index = FacetIndex(byteStart, byteEnd),
                features = listOf(FacetFeature.Tag(tag))
            )
        }
    }
}

private fun Facet.toUsable(text: String): List<TextFacet> {
    val usableFacets = mutableListOf<TextFacet>()
    this.features.forEach { feature ->
        val startChar = text.byteToCharIndex(index.byteStart)
        val endChar = text.byteToCharIndex(index.byteEnd)
        when (feature) {
            is FacetFeature.Mention -> usableFacets.add(
                TextFacet.Did(feature.did, startChar, endChar)
            )
            is FacetFeature.Link -> usableFacets.add(
                TextFacet.Link(feature.uri, startChar, endChar)
            )
            is FacetFeature.Tag -> usableFacets.add(
                TextFacet.Tag(feature.tag, startChar, endChar)
            )

            else -> {}
        }
    }
    return usableFacets
}

private fun buildLabels(labels: List<Label>): List<LabelData> = labels
    .asSequence()
    .map {
        LabelData(
            uriOfContentItAppliesTo = if (it.uri.startsWith("did"))
                LabelDataUri.Did(
                    Did.parse(it.uri).getOrThrow()
                )
            else // will throw if is not an at uri and a second check
            // is not really needed
                LabelDataUri.Uri(AtUri(it.uri)),
            value = LabelType.Companion.resolve(it._val),
            creatorDid = it.src,
            isNegotiationLabel = it.neg,
            dateOfCreationISO8601 = it.cts,
            expiresOnISO8601 = it.exp,
        )
    }
    .toList()

// i wish the "viewer" thing was properly typed
private fun PostView.Blocked.getBlockReason() = when {
    // check the contents of the list if one exists
    author.viewer.blockingByList != null -> BlockReason.AuthorByTheUserFromModList(
        uri = author.viewer.blockingByList!!.uri,
        title = author.viewer.blockingByList!!.name
    )

    author.viewer.isBlockingByList -> BlockReason.AuthorByTheUserFromModList(
        uri = author.viewer.blocking!!,
        title = null
    )

    author.viewer.isBlockingManually -> BlockReason.AuthorByTheUserManual

    author.viewer.blockedBy == true -> BlockReason.UserByTheAuthor

    else -> BlockReason.Unknown
}


private fun EmbedRecord.Blocked.getBlockedReason() = when {
    blocked -> BlockReason.AuthorByTheUserManual

    author.viewer.blockingByList != null -> BlockReason.AuthorByTheUserFromModList(
        uri = author.viewer.blockingByList!!.uri,
        title = author.viewer.blockingByList!!.name
    )

    author.viewer.isBlockingByList -> BlockReason.AuthorByTheUserFromModList(
        uri = author.viewer.blocking!!,
        title = null
    )

    author.viewer.isBlockingManually -> BlockReason.AuthorByTheUserManual

    author.viewer.blockedBy == true -> BlockReason.UserByTheAuthor

    else -> BlockReason.Unknown
}

private fun PostView.Visible.extractIdentification() = PostIdentification(
    uri = this.uri,
    cid = this.cid
)

private fun EmbedRecord.View.extractIdentification() = PostIdentification(
    uri = this.uri,
    cid = this.cid
)

private fun Threadgate?.toReplyPermission(): ReplyPermission {
    val record = this?.record ?: return ReplyPermission.Everyone()

    val rules = record.allow ?: return ReplyPermission.Everyone()

    if (rules.isEmpty()) return ReplyPermission.NoOne(false)

    val activeRules = rules.map { rule ->
        when (rule) {
            is ThreadgateRule.OnlyMentionedUsers -> ReplyPermission.Restricted.Rule.Mentioned
            is ThreadgateRule.OnlyFollowing -> ReplyPermission.Restricted.Rule.CreatorsFollows
            is ThreadgateRule.OnlyFollowers -> ReplyPermission.Restricted.Rule.CreatorsFollowers
            is ThreadgateRule.OnlyListMembers -> ReplyPermission.Restricted.Rule.List(rule.list)
        }
    }

    return ReplyPermission.Restricted(activeRules.toSet(), true)
}

fun Threadgate?.toReplyPermission(
    viewerDid: String?, // null = anonymous
    authorDid: String,
    viewerIsMentioned: Boolean,
    authorFollowsViewer: Boolean,
    viewerFollowsAuthor: Boolean,
    viewerListMemberships: Set<AtUri>,
    isGloballyBlocked: Boolean // blocked/muted/banned by creator
): ReplyPermission {
    val record = this?.record
    val allowRules = record?.allow

    if (record == null || allowRules == null) {
        val canReply = when {
            viewerDid == authorDid -> true
            isGloballyBlocked -> false
            else -> viewerDid != null
        }
        return ReplyPermission.Everyone(canUserReply = canReply)
    }

    if (allowRules.isEmpty()) {
        val canReply = (viewerDid == authorDid) // only author can reply to themselves
        return ReplyPermission.NoOne(canUserReply = canReply)
    }

    val translatedRules = allowRules.map { rule ->
        when (rule) {
            is ThreadgateRule.OnlyMentionedUsers ->
                ReplyPermission.Restricted.Rule.Mentioned

            is ThreadgateRule.OnlyFollowing ->
                ReplyPermission.Restricted.Rule.CreatorsFollows

            is ThreadgateRule.OnlyFollowers ->
                ReplyPermission.Restricted.Rule.CreatorsFollowers

            is ThreadgateRule.OnlyListMembers ->
                ReplyPermission.Restricted.Rule.List(rule.list)
        }
    }.toSet()

    val canReply = when {
        viewerDid == authorDid -> true // creator always replies
        isGloballyBlocked -> false
        else -> translatedRules.any { r ->
            when (r) {
                ReplyPermission.Restricted.Rule.Mentioned ->
                    viewerIsMentioned

                ReplyPermission.Restricted.Rule.CreatorsFollows ->
                    authorFollowsViewer

                ReplyPermission.Restricted.Rule.CreatorsFollowers ->
                    viewerFollowsAuthor

                is ReplyPermission.Restricted.Rule.List ->
                    viewerListMemberships.contains(r.listUri)
            }
        }
    }

    return ReplyPermission.Restricted(
        rules = translatedRules,
        canUserReply = canReply
    )
}
