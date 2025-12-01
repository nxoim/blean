@file:OptIn(ExperimentalTime::class)

package com.nxoim.blean.client.stuff

import co.touchlab.kermit.Logger
import com.github.michaelbull.result.getOr
import com.github.michaelbull.result.getOrElse
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import com.nxoim.blean.api.api.RepoApi
import com.nxoim.blean.api.models.feed.PostView
import com.nxoim.blean.api.models.repo.RepoCollectionNSID
import com.nxoim.blean.api.models.repo.UploadRecord
import com.nxoim.blean.api.models.repo.UploadRecordSubject
import com.nxoim.blean.api.utils.AuthenticationContext
import com.nxoim.blean.bskyPrimitives.AtUri
import com.nxoim.blean.bskyPrimitives.Did
import com.nxoim.blean.bskyPrimitives.ParsedAtUri
import com.nxoim.blean.bskyPrimitives.RecordKey
import com.nxoim.blean.bskyPrimitives.parse
import com.nxoim.blean.commonThingsDumpster.InstantSerializer
import com.nxoim.blean.outbox.OutboxHandlerResult
import com.nxoim.blean.outbox.OutboxItem
import com.nxoim.blean.outbox.OutboxRepository
import com.nxoim.blean.outbox.Outboxable
import com.nxoim.blean.outbox.RequestStatus
import com.nxoim.blean.outbox.processContinuously
import com.nxoim.blean.outbox.runCleanup
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

private const val logTag = "LikeActionProcessor"

class LikeActionProcessor(
    private val userDid: Did,
    private val repoApi: RepoApi,
    private val outboxRepository: OutboxRepository,
    private val feed: BleanFeed,
    private val onAuthenticationContextRequest: suspend () -> AuthenticationContext,
    private val logger: Logger,
    private val refreshPostsAtLast: Boolean = true,
    private val refreshPostsPrior: Boolean = false,
) : ActionProcessor {
    suspend fun enqueueLike(postUri: AtUri) {
        outboxRepository.enqueue(LikeAction.Like(postUri))
    }

    suspend fun enqueueUnlike(postUri: AtUri) {
        outboxRepository.enqueue(LikeAction.Unlike(postUri))
    }

    override suspend fun process(
        paused: StateFlow<Boolean>
    ) {
        outboxRepository.processContinuously<LikeAction, LikeActionResult>(
            paused,
            concurrentProcessLimit = 2
        ) { outboxItem ->
            val cachedPost = feed.getPost(outboxItem.request.postUri).first()

            val post = if (!refreshPostsPrior && cachedPost != null)
                cachedPost
            else {
                feed.loadAndCachePost(outboxItem.request.postUri)
                    .onFailure {
                        logger.w(tag = logTag) { "processLikes: refresh failed $it" }
                    }
                    .getOr(null)
                    ?: return@processContinuously OutboxHandlerResult.FailedRecoverable
            }

            val result = when (outboxItem.request) {
                is LikeAction.Like -> executeLike(post)
                is LikeAction.Unlike -> executeUnlike(post)
            }

            if (refreshPostsAtLast) feed.loadAndCachePost(outboxItem.request.postUri)

            result
        }
    }

    private suspend fun executeLike(post: PostView): OutboxHandlerResult<LikeActionResult.Like> {
        val result = when (post) {
            is PostView.Visible -> {
                val isAlreadyLiked = post.viewer?.like != null

                // should refetch and double check on if already liked
                if (isAlreadyLiked) {
                    OutboxHandlerResult.Success(LikeActionResult.Like(post.viewer?.like!!))
                } else {
                    val responseValue = repoApi.createRecord(
                        authenticationContext = onAuthenticationContextRequest(),
                        repo = userDid,
                        collection = RepoCollectionNSID.Like,
                        record = UploadRecord.Like(
                            UploadRecordSubject(uri = post.uri, cid = post.cid),
                            createdAt = kotlin.time.Clock.System.now().toString()
                        )
                    ).getOrElse {
                        logger.w(tag = logTag) { "executeLike: failed to createRecord for postUri=${post.uri}, error=$it" }
                        return OutboxHandlerResult.FailedUnrecoverable
                    }

                    OutboxHandlerResult.Success(LikeActionResult.Like(responseValue.uri))
                }
            }

            else -> {
                logger.w(tag = logTag) { "executeLike: post unavailable postUri=${post.uri}, postState=$post" }
                OutboxHandlerResult.FailedUnrecoverable
            }
        }
        return result
    }

    private suspend fun executeUnlike(post: PostView): OutboxHandlerResult<LikeActionResult.Unlike> {
        val result = when (post) {
            is PostView.Visible -> {
                val isAlreadyUnliked = post.viewer?.like == null

                if (isAlreadyUnliked) {
                    OutboxHandlerResult.Success(LikeActionResult.Unlike)
                } else {
                    val deletionKey = post.viewer?.like?.parse().let {
                        if (it is ParsedAtUri.Like) {
                            RecordKey.Any(it.recordKey)
                        } else {
                            logger.w(tag = logTag) { "executeUnlike: parse failed for likeUri=${post.viewer?.like}, error=$it" }
                            return OutboxHandlerResult.FailedUnrecoverable
                        }
                    }

                    repoApi.deleteRecord(
                        authenticationContext = onAuthenticationContextRequest(),
                        repo = userDid,
                        collection = RepoCollectionNSID.Like,
                        rkey = deletionKey
                    ).getOrElse {
                        logger.w(tag = logTag) { "executeUnlike: deleteRecord failed for likeUri=${post.viewer?.like}, error=$it" }
                        return OutboxHandlerResult.FailedUnrecoverable
                    }

                    OutboxHandlerResult.Success(LikeActionResult.Unlike)
                }
            }

            else -> {
                logger.w(tag = logTag) { "executeUnlike: post unavailable postUri=${post.uri}, postState=$post" }
                OutboxHandlerResult.FailedUnrecoverable
            }
        }
        return result
    }

    fun getLikeOutboxState(postUri: AtUri): Flow<LikeOutboxState?> = outboxRepository
        .get<LikeAction, LikeActionResult>(postUri.toString())
        .map { rawOutboxItem -> rawOutboxItem?.toLikeOutboxState() }

    fun getLikeOutboxStates(postUris: List<AtUri>): Flow<List<LikeOutboxState>> = outboxRepository
        .get<LikeAction, LikeActionResult>(postUris.map { it.toString() })
        .map { it.map { rawOutboxItem -> rawOutboxItem.toLikeOutboxState() } }

    override suspend fun autoCleanOutbox(deleteItemsOlderThan: Duration) {
        outboxRepository.runCleanup<LikeAction, LikeActionResult>(
            olderThan = deleteItemsOlderThan
        )
    }

    override suspend fun recover() {
        val items = outboxRepository
            .getAll<LikeAction, LikeActionResult>(
                0..Long.MAX_VALUE,
                statuses = listOf(RequestStatus.Processing, RequestStatus.Pending)
            )
            .first()

        items.forEach { outboxItem ->
            feed.loadAndCachePost(outboxItem.request.postUri)
                .onSuccess { post ->
                    when (post) {
                        null,
                        is PostView.Blocked,
                        is PostView.NotFound -> {
                            logger.w(tag = logTag) {
                                "recoverLikeActions: post ${outboxItem.request.postUri} unavailable, marking as failed unrecoverable. Post state: $post"
                            }
                            outboxRepository.markAsFailed(outboxItem.key, isRecoverable = false)
                        }

                        is PostView.Visible -> {
                            when (outboxItem.request) {
                                is LikeAction.Like -> {
                                    val isntAlreadyLiked =
                                        post.viewer != null && post.viewer!!.like == null

                                    if (isntAlreadyLiked) {
                                        outboxRepository.enqueue(outboxItem.request)
                                    } else {
                                        outboxRepository.markAsSuccess(
                                            outboxItem.key,
                                            LikeActionResult.Like(post.viewer!!.like!!)
                                        )
                                    }
                                }

                                is LikeAction.Unlike -> {
                                    val isntAlreadyUnliked =
                                        post.viewer != null && post.viewer!!.like != null

                                    if (isntAlreadyUnliked) {
                                        outboxRepository.enqueue(outboxItem.request)
                                    } else {
                                        outboxRepository.markAsSuccess(
                                            outboxItem.key,
                                            LikeActionResult.Unlike
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                .onFailure {
                    logger.w(tag = logTag) { "recoverLikeActions: failed to load post ${outboxItem.request.postUri} during recovery. Error: $it" }
                }
        }
    }
}

@Serializable
@SerialName("likeAction")
sealed interface LikeAction : Outboxable<LikeAction, LikeActionResult> {
    val postUri: AtUri
    override val outboxKey: String get() = postUri.toString()

    @Serializable
    @SerialName("like")
    data class Like(
        override val postUri: AtUri,
        val timeOfRequest: @Serializable(InstantSerializer::class) Instant = kotlin.time.Clock.System.now()
    ) : LikeAction

    @Serializable
    @SerialName("unlike")
    data class Unlike(
        override val postUri: AtUri,
    ) : LikeAction
}

@Serializable
sealed interface LikeActionResult {
    @Serializable
    data class Like(val likeRepoReference: AtUri) : LikeActionResult

    @Serializable
    data object Unlike : LikeActionResult
}

data class LikeOutboxState(val uri: AtUri, val status: LikeOutboxStatus)

sealed interface LikeOutboxStatus {
    val liked: Boolean

    data object PendingLike : LikeOutboxStatus {
        override val liked = true
    }

    data object PendingUnlike : LikeOutboxStatus {
        override val liked = false
    }

    data object SuccessLike : LikeOutboxStatus {
        override val liked = true
    }

    data object SuccessUnlike : LikeOutboxStatus {
        override val liked = false
    }

    data object FailedLike : LikeOutboxStatus {
        override val liked = true
    }

    data object FailedUnlike : LikeOutboxStatus {
        override val liked = false
    }
}

val LikeOutboxStatus.isPending get() = this is LikeOutboxStatus.PendingLike || this is LikeOutboxStatus.PendingUnlike
val LikeOutboxStatus.isSuccess get() = this is LikeOutboxStatus.SuccessLike || this is LikeOutboxStatus.SuccessUnlike
val LikeOutboxStatus.isFailed get() = this is LikeOutboxStatus.FailedLike || this is LikeOutboxStatus.FailedUnlike

fun OutboxItem<LikeAction, LikeActionResult>.toLikeOutboxState() = LikeOutboxState(
    uri = this.request.postUri,
    status = when (status) {
        RequestStatus.Pending,
        RequestStatus.Processing -> if (request is LikeAction.Like)
            LikeOutboxStatus.PendingLike
        else
            LikeOutboxStatus.PendingUnlike

        RequestStatus.Success -> if (request is LikeAction.Like)
            LikeOutboxStatus.SuccessLike
        else
            LikeOutboxStatus.SuccessUnlike

        RequestStatus.FailedRecoverable,
        RequestStatus.FailedUnrecoverable -> if (request is LikeAction.Like)
            LikeOutboxStatus.FailedLike
        else
            LikeOutboxStatus.FailedUnlike
    }
)

