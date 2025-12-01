@file:OptIn(ExperimentalTime::class)

package com.nxoim.blean.client.stuff

import co.touchlab.kermit.Logger
import com.github.michaelbull.result.getOrElse
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import com.nxoim.blean.api.api.RepoApi
import com.nxoim.blean.api.models.repo.RecordValidationStatus
import com.nxoim.blean.api.models.repo.RecordWrite
import com.nxoim.blean.api.models.repo.RecordWriteContent
import com.nxoim.blean.api.models.repo.RecordWriteResult
import com.nxoim.blean.api.models.repo.RepoCollectionNSID
import com.nxoim.blean.api.utils.AuthenticationContext
import com.nxoim.blean.bskyPrimitives.Did
import com.nxoim.blean.bskyPrimitives.RecordKey
import com.nxoim.blean.commonThingsDumpster.RequestError
import com.nxoim.blean.outbox.OutboxHandlerResult
import com.nxoim.blean.outbox.OutboxRepository
import com.nxoim.blean.outbox.Outboxable
import com.nxoim.blean.outbox.RequestStatus
import com.nxoim.blean.outbox.processContinuously
import com.nxoim.blean.outbox.runCleanup
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

private const val logTag = "PostActionProcessor"

class PostActionProcessor(
    private val userDid: Did,
    private val repoApi: RepoApi,
    private val outboxRepository: OutboxRepository,
    private val onAuthenticationContextRequest: suspend () -> AuthenticationContext,
    private val logger: Logger,
) : ActionProcessor {
    override suspend fun process(paused: StateFlow<Boolean>) {
        outboxRepository.processContinuously<PostAction, PostActionResult>(paused) {
            executePost(it.request)
        }
    }

    override suspend fun recover() {
        logger.i(tag = logTag) {
            "Looking for actions to recover"
        }
        val unfinished = outboxRepository
            .getAll<PostAction, PostActionResult>(
                0L..Long.MAX_VALUE,
                statuses = listOf(RequestStatus.Processing)
            )
            .first()

        logger.w(tag = logTag) {
            "Recovering ${unfinished.size} actions"
        }

        unfinished.forEach { item ->
            val expectedRecordKey = item.request.recordKey
            // check if exists already and early return if it does
            if (expectedRecordKey != null) {
                val existingPostResponse = repoApi.getRecord(
                    onAuthenticationContextRequest(),
                    repo = userDid,
                    collection = RepoCollectionNSID.Post,
                    recordKey = expectedRecordKey
                )

                existingPostResponse
                    .onSuccess {
                        logger.i(tag = logTag) {
                            "While recovering posts in outbox a post was found to be" +
                                    " already in the repository. Marking the outbox item as successful"
                        }
                        outboxRepository.markAsSuccess(item.key, PostActionResult)
                        return@forEach
                    }
                    .onFailure {
                        // NOTE: request error here may contain RecordNotFound in http error 400
                        // that we may rely on in the future
                    }
            }

            outboxRepository.reenqueue(item)
        }
    }

    suspend fun queuePost(
        id: String,
        content: RecordWriteContent.Post
    ) {
        outboxRepository.enqueue(
            PostAction(id, null, content)
        )
        logger.i(tag = logTag) {
            "Queueing post"
        }
    }

    private suspend fun executePost(postAction: PostAction): OutboxHandlerResult<PostActionResult> {
        logger.i(tag = logTag) {
            "Executing post"
        }

        val response = repoApi.applyWrites(
            authenticationContext = onAuthenticationContextRequest(),
            repo = userDid,
            writes = listOf(
                RecordWrite.Create(
                    collection = RepoCollectionNSID.Post,
                    value = postAction.content
                )
            ),
            validate = true
        ).getOrElse {
            logger.e(tag = logTag) {
                "unable to upload post to repository. error: $it"
            }

            return when (it) {
                is RequestError.CantConnectToInternet,
                is RequestError.Internal<*>,
                is RequestError.Timeout,
                is RequestError.Http<*>,
                is RequestError.UnresolvedAddress -> OutboxHandlerResult.FailedRecoverable

                is RequestError.Other<*> -> OutboxHandlerResult.FailedUnrecoverable
            }
        }

        val result = response.results.firstOrNull()
        if (result == null) {
            logger.e(tag = logTag) {
                "Üpon executing post the server returned no results despite successful response"
            }

            return OutboxHandlerResult.FailedUnrecoverable
        }

        if (result is RecordWriteResult.Create) {
            if (result.validationStatus != RecordValidationStatus.Valid) {
                logger.e(tag = logTag) {
                    "Üpon executing post the server returned result with validation status ${result.validationStatus}"
                }

                return OutboxHandlerResult.FailedUnrecoverable
            }
            return OutboxHandlerResult.Success(PostActionResult)
        } else {
            logger.e(tag = logTag) {
                "Üpon executing post the server returned results of unexpected type, ${result::class}"
            }

            return OutboxHandlerResult.FailedUnrecoverable
        }
    }

    override suspend fun autoCleanOutbox(deleteItemsOlderThan: Duration) {
        outboxRepository.runCleanup<PostAction, PostActionResult>(deleteItemsOlderThan)
    }
}

@Serializable
@SerialName("postAction")
data class PostAction(
    val id: String,
    val recordKey: RecordKey.Tid? = null,
    val content: RecordWriteContent.Post
) : Outboxable<PostAction, PostActionResult> {
    override val outboxKey: String = id
}

@Serializable
@SerialName("postActionResult")
data object PostActionResult