package com.nxoim.blean.ui.screens.content.sourceImplementations

import co.touchlab.kermit.Logger
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.nxoim.blean.client.BleanClient
import com.nxoim.blean.client.stuff.isFailed
import com.nxoim.blean.client.stuff.isSuccess
import com.nxoim.blean.postRelatedCommons.FeedDataAggregator
import com.nxoim.blean.postRelatedCommons.models.ChunkCursor
import com.nxoim.blean.postRelatedCommons.models.PostContainer
import com.nxoim.blean.postRelatedCommons.models.PostIdentification
import com.nxoim.blean.postRelatedCommons.models.PostType
import com.nxoim.blean.ui.screens.content.feed.FeedPostsSource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class FeedPostsSourceImpl(
    private val client: BleanClient.LoggedIn,
    private val feedUri: String,
    private val logger: Logger
) : FeedPostsSource {
    val expectedChunkItemCount = 20

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getFeedChunkPosts(
        index: Long
    ): Flow<Pair<ChunkCursor, List<PostContainer<PostType.Feed>>>?> =
        client.feed
            .getFeedChunk(feedUri, index)
            .flatMapLatest { feedChunk ->
                with(logger) {
                    feedChunk
                        ?.run {
                            client.postInteractionsOutbox
                                .getLikeOutboxStates(this.feed.map { it.post.uri })
                                .map { likesList ->
                                    val likes = likesList.associateBy { it.uri }

                                    val cursor =
                                        if (feed.size < expectedChunkItemCount && cursor == null)
                                            ChunkCursor.None
                                        else
                                            ChunkCursor.Next(cursor)

                                    val postsBoundToStatesInLocalOutbox = feed.map { it ->
                                        val expectedLikedStatus = likes[it.post.uri]?.let {
                                            if (!it.status.isFailed || !it.status.isSuccess) it.status.liked else null
                                        }

                                        FeedDataAggregator.buildFeedPost(
                                            it.post,
                                            it.reply,
                                            it.reason,
                                            expectedLikedStatus
                                        )
                                    }

                                    cursor to postsBoundToStatesInLocalOutbox
                                }
                        }
                        ?: flowOf(null)
                }
            }

    override suspend fun loadAndCachePostsForFeed(cursor: String?) =
        client.feed.loadAndCachePostsForFeed(feedUri, cursor, expectedChunkItemCount)

    override suspend fun refreshFeed(): Result<Unit, Throwable> =
        client.feed.refreshFeed(feedUri, expectedChunkItemCount)

    override suspend fun scheduleLike(id: PostIdentification): Result<*, *> {
        client.postInteractionsOutbox.like(id.uri)
        client.feed.loadAndCachePost(id.uri)
//            ?.onSuccess {
//                // refresh post data
//                client.feed.loadPost(id.uri)
//            }

        return Ok(Unit)
    }

    override suspend fun scheduleUnlike(
        id: PostIdentification,
    ): Result<*, *> {
        client.postInteractionsOutbox.unlike(id.uri)
        client.feed.loadAndCachePost(id.uri)

//            ?.onSuccess {
//                // refresh post data
//                client.feed.loadPost(id.uri)
//            }
        return Ok(Unit)
    }
}