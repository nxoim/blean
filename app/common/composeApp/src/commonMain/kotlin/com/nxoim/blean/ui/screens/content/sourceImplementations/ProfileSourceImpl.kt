package com.nxoim.blean.ui.screens.content.sourceImplementations

import co.touchlab.kermit.Logger
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.nxoim.blean.client.BleanClient
import com.nxoim.blean.client.clientModels.FeedChunk
import com.nxoim.blean.client.stuff.isFailed
import com.nxoim.blean.client.stuff.isSuccess
import com.nxoim.blean.postRelatedCommons.FeedDataAggregator
import com.nxoim.blean.postRelatedCommons.models.ChunkCursor
import com.nxoim.blean.postRelatedCommons.models.PostContainer
import com.nxoim.blean.postRelatedCommons.models.PostIdentification
import com.nxoim.blean.postRelatedCommons.models.PostType
import com.nxoim.blean.ui.screens.content.profile.ProfileSource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class ProfileSourceImpl(
    private val client: BleanClient.LoggedIn,
    private val logger: Logger
) : ProfileSource {
    val expectedChunkItemCount = 20

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getFeedChunkPosts(
        index: Long
    ): Flow<Pair<ChunkCursor, List<PostContainer<PostType.Feed>>>?> =
        client.feed
            .getAuthorFeedChunk(client.usersDid, index)
            .combineWithLocalOutboxLikes()

    override suspend fun loadAndCachePostsForFeed(cursor: String?) =
        client.feed.loadAndCachePostsForAuthorFeed(client.usersDid, cursor, expectedChunkItemCount)

    override suspend fun refreshFeed(): Result<Unit, Throwable> =
        client.feed.refreshAuthorFeed(client.usersDid, expectedChunkItemCount)

    override suspend fun scheduleLike(id: PostIdentification): Result<*, *> {
        client.postInteractionsOutbox.like(id.uri)

        return Ok(Unit)
    }

    override suspend fun scheduleUnlike(
        id: PostIdentification,
    ): Result<*, *> {
        client.postInteractionsOutbox.unlike(id.uri)

        return Ok(Unit)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun Flow<FeedChunk?>.combineWithLocalOutboxLikes(

    ): Flow<Pair<ChunkCursor, List<PostContainer<PostType.Feed>>>?> = flatMapLatest {
        it?.let {
            val likeUris = buildSet {
                addAll(it.feed.map { it.post.uri })
                it.feed.forEach {
                    it.reply?.root?.let { add(it.uri) }
                    it.reply?.parent?.let { add(it.uri) }
                }
            }

            client.postInteractionsOutbox
                .getLikeOutboxStates(likeUris.toList())
                .map { likesList ->
                    val likes = likesList.associateBy { it.uri }
                    with(logger) {
                        val cursor = if (it.feed.size < expectedChunkItemCount && it.cursor == null)
                            ChunkCursor.None
                        else
                            ChunkCursor.Next(it.cursor)

                        cursor to it.feed.map { it ->
                            FeedDataAggregator.buildFeedPost(
                                it.post,
                                it.reply,
                                it.reason,
                                // true if not null, otherwise null so the
                                // value in post contents can be used
                                expectedLikedStatus = likes[it.post.uri]
                                    ?.let { if (!it.status.isFailed || !it.status.isSuccess) it.status.liked else null },
                                expectedReplyRootLikeStatus = likes[it.reply?.root?.uri]
                                    ?.let { if (!it.status.isFailed || !it.status.isSuccess) it.status.liked else null },
                                expectedReplyTargetLikeStatus = likes[it.reply?.parent?.uri]
                                    ?.let { if (!it.status.isFailed || !it.status.isSuccess) it.status.liked else null }
                            )
                        }
                    }
                }
        }
            ?: flowOf(null)
    }

}