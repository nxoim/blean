@file:OptIn(SensitiveOutboxApi::class, ExperimentalTime::class)

package com.nxoim.blean.outbox

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.withContext
import kotlin.time.ExperimentalTime

/**
 * Collects pending and recoverable items <[Request],[Result]> (sorted by created first),
 * and gives them out for processing in a continuous, long-running manner.
 * This function utilizes a `supervisorScope`, meaning an unhandled exception during
 * the processing of a single item (within its launched coroutine) will not terminate the
 * entire `processContinuously` operation, allowing other items to still be processed.
 *
 * Example:
 * ```kotlin
 * outbox.processContinuously<YourRequest, YourResult> { item ->
 *     val result: YourApiResultType = yourApi.doSomething(item.request.requestParameter)
 *
 *     return@processContinuously when(result) {
 *         is YourApiResultType.Success -> OutboxHandlerResult.Success(result.value)
 *         else -> OutboxHandlerResult.FailedUnrecoverable // or OutboxHandlerResult.FailedRecoverable
 *     }
 * }
 * ```
 *
 * Upon exceptions thrown by the [handler], the internal `outboxItemProcessor`
 * attempts to reset the item to its state before processing began, and then rethrows the exception.
 *
 * Concurrency is limited by [concurrentProcessLimit], which dictates the maximum number of
 * items processed in parallel using an internal [Semaphore]. Items are fetched one-by-one;
 * if an item is updated (e.g., its data changes) while it's queued or about to be processed,
 * an internal mechanism (`mostRelevantOutboxItem` map) ensures the most recent version of the item
 * is handled.
 *
 * Items will be collected for processing as long as their processing
 * was attempted less than [maxRetries] (i.e., `errorRetryCount` < `maxRetries`).
 * The `errorRetryCount` is incremented by the underlying `OutboxRepository.markAsProcessing` call.
 *
 * @param Request The specific type of [Outboxable] item this instance will process.
 * @param Result The specific type of [Any] that represents a successful processing outcome.
 * @param isPaused When true new items will not start processing.
 * @param maxRetries The maximum number of times an item's processing will be retried if it fails
 *                   recoverably. Items with an `errorRetryCount` up to this value (exclusive) are eligible.
 * @param concurrentProcessLimit The maximum number of items that can be processed concurrently.
 * @param sort The [OutboxSort] to use when fetching items for processing.
 * @param handler A suspend lambda that receives an [OutboxItem] and must return an
 *                [OutboxHandlerResult] indicating the outcome of the processing attempt.
 */
@OptIn(ExperimentalCoroutinesApi::class)
suspend inline fun <reified Request : Outboxable<Request, Result>, reified Result : Any> OutboxRepository.processContinuously(
    isPaused: StateFlow<Boolean>,
    maxRetries: Int = 3,
    concurrentProcessLimit: Int = 16,
    sort: OutboxSort = OutboxSort.Creation.OlderFirst,
    crossinline handler: suspend (OutboxItem<Request, Result>) -> OutboxHandlerResult<Result>
) = supervisorScope {
    val processingSemaphore = Semaphore(concurrentProcessLimit)

    // as one gets sent - another comes in.
    // we avoid scheduling irrelevant items by collecting
    // items individually rather than chunks.

    isPaused
        .flatMapLatest { paused ->
            if (paused)
                emptyFlow()
            else
                getAll<Request, Result>(
                fromIndexToIndex = 0..0L,
                statuses = listOf(
                    RequestStatus.Pending,
                    RequestStatus.FailedRecoverable
                ),
                failureRange = 0..maxRetries,
                sortBy = sort
            )
                // avoid collecting the same item twice because
                // the same item may be emitted twice because of
                // any change happening in the database
                .distinctUntilChanged()
                // only collect the most relevant when semaphore permits
                .conflate()
        }
        .collect { items ->
            // 'acquire' outside of launch block to block emissions
            // until semaphore permits this 'acquire' (because of .conflate() above)
            processingSemaphore.acquire()
            val item = items.firstOrNull()

            if (item == null) {
                processingSemaphore.release()
            } else launch {
                try {
                    outboxItemProcessor<Request, Result>(
                        item = item,
                        handler = handler
                    )

                } finally {
                    processingSemaphore.release()
                }
            }
        }
}

@PublishedApi
@OptIn(ExperimentalTime::class)
internal suspend inline fun <reified Request : Outboxable<Request, Result>, reified Result : Any> OutboxRepository.outboxItemProcessor(
    item: OutboxItem<Request, Result>,
    handler: suspend (OutboxItem<Request, Result>) -> OutboxHandlerResult<Result>,
) {
    markAsProcessing(item.key)

    try {
        val result = handler(item)

        withContext(NonCancellable) {
            val current = get(item.key).first()
            val requestIsStillRelevant = current != null && current.request == item.request

            if (requestIsStillRelevant) {
                when (result) {
                    OutboxHandlerResult.FailedRecoverable ->
                        markAsFailed(item.key, isRecoverable = true)

                    OutboxHandlerResult.FailedUnrecoverable ->
                        markAsFailed(item.key, isRecoverable = false)

                    is OutboxHandlerResult.Success<Result> ->
                        markAsSuccess(item.key, result.value)
                }
            } else {
                //  do not act on result for stale request
            }
        }
    } catch (e: Exception) {
        // avoids overwriting a successful update that happened before the exception
        withContext(NonCancellable) {
            val current = get(item.key).first()

            require(current != null) {
                "Item was absent in the outbox repository right after it's " +
                        "processing failed. Expected item to stay in repository " +
                        "with processing status. Can not reset the item back to pending."
            }

            if (current.status !in statusesToAvoidOverwriting) saveOrUpdate(item)
        }

        throw e
    }
}

@PublishedApi
internal val statusesToAvoidOverwriting = setOf(
    RequestStatus.FailedUnrecoverable,
    RequestStatus.FailedRecoverable,
    RequestStatus.Success
)

sealed interface OutboxHandlerResult<out T : Any> {
    data class Success<T : Any>(val value: T) : OutboxHandlerResult<T>

    // could add information about timeout, like timestamp
    // for next earliest retry into recoverable
    data object FailedRecoverable : OutboxHandlerResult<Nothing>
    data object FailedUnrecoverable : OutboxHandlerResult<Nothing>
}