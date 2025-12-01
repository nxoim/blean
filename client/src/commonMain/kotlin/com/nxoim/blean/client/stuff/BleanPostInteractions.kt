package com.nxoim.blean.client.stuff

import co.touchlab.kermit.Logger
import com.nxoim.blean.bskyPrimitives.AtUri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

private const val logTag = "BleanPostInteractions"

class BleanPostInteractions(
    private val feed: BleanFeed,
    private val likeProcessor: LikeActionProcessor,
    private val clientCoroutineScope: CoroutineScope,
    private val logger: Logger
) {
    private val clientCoroutineContext = clientCoroutineScope.coroutineContext

    suspend fun like(
        postUri: AtUri,
    ) = withContext(clientCoroutineContext) {
        logger.v(tag = logTag) { "enqueuing like for $postUri" }
        likeProcessor.enqueueLike(postUri)
    }

    suspend fun unlike(
        postUri: AtUri,
    ) = withContext(clientCoroutineContext) {
        logger.v(tag = logTag) { "enqueuing unlike for $postUri" }
        likeProcessor.enqueueUnlike(postUri)
    }

    fun getLikeOutboxState(postUri: AtUri): Flow<LikeOutboxState?> =
        likeProcessor.getLikeOutboxState(postUri)

    fun getLikeOutboxStates(postUris: List<AtUri>): Flow<List<LikeOutboxState>> =
        likeProcessor.getLikeOutboxStates(postUris)
}

