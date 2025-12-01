package com.nxoim.blean.ui.screens.content.sourceImplementations

import co.touchlab.kermit.Logger
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.onSuccess
import com.nxoim.blean.api.models.feed.ThreadV2
import com.nxoim.blean.bskyPrimitives.AtUri
import com.nxoim.blean.client.BleanClient
import com.nxoim.blean.client.stuff.isFailed
import com.nxoim.blean.client.stuff.isSuccess
import com.nxoim.blean.postRelatedCommons.FeedDataAggregator
import com.nxoim.blean.postRelatedCommons.models.PostContainer
import com.nxoim.blean.postRelatedCommons.models.PostIdentification
import com.nxoim.blean.postRelatedCommons.models.PostType
import com.nxoim.blean.ui.screens.content.thread.ThreadSource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class ThreadSourceImpl(
    private val postUri: AtUri,
    private val unconvertedThreadReference: PostType?,
    private val client: BleanClient.LoggedIn,
    private val logger: Logger
) : ThreadSource {
    private val threadCache = MutableStateFlow<ThreadV2?>(null)
    val expectedChunkItemCount = 5

    override fun getPost(): Flow<PostContainer<PostType.Thread>?> =
        if (unconvertedThreadReference == null) {
            getPost(postUri)
        } else {
            val postFlow = client.feed.getPost(postUri).let {
                val likeStatusFlow = client.postInteractionsOutbox.getLikeOutboxState(postUri)
                it.combine(likeStatusFlow) { post, likeStatus ->
                    post to likeStatus
                }
            }

            postFlow.map { postLikePair ->
                val post = postLikePair.first
                val postLike = postLikePair.second

                if (post != null) {
                    FeedDataAggregator.buildThreadPost(
                        logger,
                        post,
                        threadDepth = 0,
                        expectedLikedStatus = postLike?.takeIf { !it.status.isFailed && !it.status.isSuccess }?.status?.liked,
                    )
                } else null.also {
                    client.feed.loadAndCachePost(postUri)
                }
            }
        }
            .distinctUntilChanged()

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getPost(uri: AtUri): Flow<PostContainer<PostType.Thread>?> = client.feed.getPost(uri).flatMapLatest { post ->
        client.postInteractionsOutbox.getLikeOutboxState(uri).map { likeStatus ->
            if (post != null) {
                FeedDataAggregator.buildThreadPost(
                    logger,
                    post,
                    threadDepth = 0,
                    expectedLikedStatus = likeStatus?.takeIf { !it.status.isFailed && !it.status.isSuccess }?.status?.liked
                )
            } else null
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getReplies(
        pageIndex: Int
    ): Flow<List<PostContainer<PostType.Thread>>?> =
        threadCache.flatMapLatest { cachedThread ->
            cachedThread?.thread
                ?.drop(pageIndex * expectedChunkItemCount)
                ?.take(expectedChunkItemCount)
                ?.map { it.uri }
                ?.let { postUris ->
                    client.postInteractionsOutbox.getLikeOutboxStates(postUris)
                        .flatMapLatest { likesList ->
                            val likes = likesList.associateBy { it.uri }

                            client.feed.getPosts(postUris).map { rawPosts ->
                                rawPosts.map { post ->
                                    FeedDataAggregator.buildThreadPost(
                                        logger,
                                        post,
                                        threadDepth = 0,
                                        expectedLikedStatus = likes[post.uri]?.takeIf { !it.status.isFailed && !it.status.isSuccess }?.status?.liked
                                    )
                                }
                            }
                        }
                }
                ?: flowOf(null)
        }

    override suspend fun refresh(): Result<*, *> {
        return client.feed.loadAndCacheThread(postUri).onSuccess {
            threadCache.value = it
        }
    }

    override suspend fun loadAndCacheReplies(from: AtUri): Result<*, *> {
        // no op for now because bruh we load all at once
        return Ok(Unit)
    }

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