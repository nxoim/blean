@file:OptIn(ExperimentalTime::class, SensitiveOutboxApi::class)

package com.nxoim.blean.outbox

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.conflate
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

/**
 * Runs a cleanup process for outbox items in a coroutine scope.
 *
 * The cleanup process involves:
 * 1. Identifying items that are older than the [olderThan]
 *    and have a status of [RequestStatus.Success] or [RequestStatus.FailedUnrecoverable].
 * 2. Deleting these identified items from the [OutboxRepository].
 * 3. Emitting status updates via the [onStatusUpdate] callback.
 *
 * @param Request The specific type of [Outboxable] item this cleanup will target.
 * @param Result The specific type of [Any] associated with the [Request]'s result.
 * @param olderThan The duration after which items are considered outdated from the moment of creation (inclusive).
 * @param maxBatchSize The maximum number of items to process in a single batch. Must be at least 1.
 * @param onStatusUpdate A callback function that receives [CleanupStatus] updates,
 *                       indicating the current state of the cleanup process (e.g., running, completed, no items).
 */
suspend inline fun <reified Request : Outboxable<Request, Result>, reified Result : Any> OutboxRepository.runCleanup(
    olderThan: Duration = Duration.ZERO,
    maxBatchSize: Int = 50,
    crossinline onStatusUpdate: (status: CleanupStatus) -> Unit = { }
) = coroutineScope {
    require(maxBatchSize > 0) {
        "maxBatchSize must be at least 1"
    }

    getAll<Request, Result>(
        fromIndexToIndex = 0..(maxBatchSize - 1).toLong(),
        statuses = irrelevantStatuses,
        sortBy = OutboxSort.Creation.OlderFirst
    )
        .conflate()
        .collect { items ->
            onStatusUpdate(CleanupStatus.Running)

            if (items.isNotEmpty()) {
                val irrelevantItems = items.mapNotNull { item ->
                    val outdated = (item.createdAt + olderThan) <= kotlin.time.Clock.System.now()

                    if (outdated) {
                        item.key
                    } else {
                        null
                    }
                }

                if (irrelevantItems.isNotEmpty()) {
                    val deletedCount = deleteIfStatus<Request, Result>(
                        irrelevantItems,
                        statuses = irrelevantStatuses
                    )

                    onStatusUpdate(CleanupStatus.Completed(deletedCount))
                } else {
                    onStatusUpdate(CleanupStatus.NoItemsToProcess)
                }
            } else {
                onStatusUpdate(CleanupStatus.NoItemsToProcess)
            }
        }
}

@PublishedApi
internal val irrelevantStatuses = listOf(
    RequestStatus.Success,
    RequestStatus.FailedUnrecoverable,
)

sealed interface CleanupStatus {
    data object Running : CleanupStatus
    data class Completed(val itemsProcessedCount: Int) : CleanupStatus
    object NoItemsToProcess : CleanupStatus
}
