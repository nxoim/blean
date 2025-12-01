@file:OptIn(ExperimentalTime::class, ExperimentalSerializationApi::class)

package com.nxoim.blean.outbox

import androidx.room.ColumnInfo
import androidx.room.ConstructedBy
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.RoomRawQuery
import androidx.room.Transaction
import androidx.room.Update
import androidx.sqlite.SQLiteStatement
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.ClassDiscriminatorMode
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import kotlin.jvm.JvmName
import kotlin.time.Clock.System.now
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

class OutboxRepository(
    private val dao: OutboxDao,
    private val json: Json = outboxJson
) {
    /**
     * Adds an item to the outbox with pending status.
     * If an item exists already with this key (from [Outboxable.outboxKey]) - it will be replaced by a new one.
     */
    @OptIn(SensitiveOutboxApi::class)
    suspend inline fun <reified Request : Outboxable<Request, Result>, reified Result : Any> enqueue(
        request: Request,
    ): OutboxItemKey<Request, Result> = saveOrUpdate(
        OutboxItem(
            key = OutboxItemKey(request.outboxKey),
            request = request,
        ),
        strategy = ReplacementStrategy.ReplaceAlways
    )

    /**
     * Adds an item to the outbox with pending status.
     * If an item exists already with this key (from [Outboxable.outboxKey]) - it will be replaced by a new one.
     */
    @OptIn(SensitiveOutboxApi::class)
    suspend inline fun <reified Request : Outboxable<Request, Result>, reified Result : Any> reenqueue(
        item: OutboxItem<Request, Result>
    ): OutboxItemKey<Request, Result> = saveOrUpdate(
        item.copy(
            status = RequestStatus.Pending,
            result = null
        ),
        strategy = ReplacementStrategy.ReplaceAlways
    )

    /**
     * Marks an item as successfully processed.
     * If an item does not exist - throws [IllegalStateException].
     * If already marked as success - returns the key and does nothing else
     */
    @OptIn(SensitiveOutboxApi::class)
    suspend inline fun <reified Request : Outboxable<Request, Result>, reified Result : Any> markAsSuccess(
        key: OutboxItemKey<Request, Result>,
        result: Result
    ): OutboxItemKey<Request, Result> {
        val currentItem = get(key).first()
            ?: error("Item with key $key not found when trying to mark as success")

        if (currentItem.status == RequestStatus.Success) return currentItem.key

        val updatedItem = currentItem.copy(
            status = RequestStatus.Success,
            result = result
        )
        return saveOrUpdate(updatedItem, strategy = ReplacementStrategy.ReplaceAlways)
    }

    /**
     * Marks an item as failed.
     * Increments the failure count.
     * If an item does not exist - throws [IllegalStateException].
     * If item is already marked as failed - returns without incrementing failure count
     */
    @OptIn(SensitiveOutboxApi::class)
    suspend inline fun <reified Request : Outboxable<Request, Result>, reified Result : Any> markAsFailed(
        key: OutboxItemKey<Request, Result>,
        isRecoverable: Boolean
    ): OutboxItemKey<Request, Result> {
        val currentItem = get(key).first()
            ?: error("Item with key $key not found when trying to mark as failed")

        if (currentItem.status == RequestStatus.FailedRecoverable || currentItem.status == RequestStatus.FailedUnrecoverable)
            return currentItem.key

        val newStatus =
            if (isRecoverable) RequestStatus.FailedRecoverable else RequestStatus.FailedUnrecoverable

        val updatedItem = currentItem.copy(
            status = newStatus,
            result = null,
            failureCount = currentItem.failureCount + 1
        )
        return saveOrUpdate(updatedItem, strategy = ReplacementStrategy.ReplaceAlways)
    }

    /**
     * Marks an item as being processed.
     * If an item does not exist or is being processed - throws
     * [IllegalStateException] and restores the original state.
     */
    suspend inline fun <reified Request : Outboxable<Request, Result>, reified Result : Any> markAsProcessing(
        key: OutboxItemKey<Request, Result>
    ) = markAsProcessing(key, serializer(), serializer())

    /**
     * Marks an item as being processed.
     * If an item does not exist or is being processed - throws
     * [IllegalStateException] and restores the original state.
     */
    @OptIn(SensitiveOutboxApi::class, ExperimentalSerializationApi::class)
    suspend fun <Request : Outboxable<Request, Result>, Result : Any> markAsProcessing(
        key: OutboxItemKey<Request, Result>,
        requestSerializer: KSerializer<Request>,
        resultSerializer: KSerializer<Result>
    ): OutboxItemKey<Request, Result> {
        dao.markAsProcessingOrThrowAndReset(
            id = key.value,
            throwIfProcessingAlready = true,
            requestSerialName = requestSerializer.descriptor.serialName,
            resultSerialName = resultSerializer.descriptor.serialName
        )
        return key
    }

    /**
     * Saves or updates the item for it's [OutboxItem.key]
     */
    @SensitiveOutboxApi
    suspend inline fun <reified Request : Outboxable<Request, Result>, reified Result : Any> saveOrUpdate(
        item: OutboxItem<Request, Result>,
        strategy: ReplacementStrategy = ReplacementStrategy.ReplaceAlways,
    ) = saveOrUpdate(item, strategy, serializer(), serializer())

    /**
     * Saves or updates the item for it's [OutboxItem.key]
     */
    @SensitiveOutboxApi
    suspend fun <R : Outboxable<R, Res>, Res : Any> saveOrUpdate(
        item: OutboxItem<R, Res>,
        strategy: ReplacementStrategy = ReplacementStrategy.ReplaceAlways,
        requestSerializer: KSerializer<R>,
        resultSerializer: KSerializer<Res>
    ): OutboxItemKey<R, Res> {
        dao.saveOrUpdate(item.toRoom(json, requestSerializer, resultSerializer), strategy)

        return item.key
    }

    /**
     * Gets an item by the [keyValue] and [Request] [Result] as flow.
     * Creates [OutboxItemKey] internally.
     */
    inline fun <reified Request : Outboxable<Request, Result>, reified Result : Any> get(
        keyValue: String
    ) = get<Request, Result>(OutboxItemKey(keyValue))

    /**
     * Gets an item by the [keyValue] and [Request] [Result] as flow.
     * Creates [OutboxItemKey] internally.
     */
    @JvmName("getWithKeyValues")
    inline fun <reified Request : Outboxable<Request, Result>, reified Result : Any> get(
        keyValues: List<String>
    ) = get<Request, Result>(keys = keyValues.map { OutboxItemKey(it) })

    /**
     * Gets an item by the [key] as flow
     */
    inline fun <reified Request : Outboxable<Request, Result>, reified Result : Any> get(
        key: OutboxItemKey<Request, Result>
    ) = get(key, serializer(), serializer())

    /**
     * Gets an item by the [key] as flow
     */
    @OptIn(ExperimentalSerializationApi::class)
    fun <Request : Outboxable<Request, Result>, Result : Any> get(
        key: OutboxItemKey<Request, Result>,
        requestSerializer: KSerializer<Request>,
        resultSerializer: KSerializer<Result>
    ): Flow<OutboxItem<Request, Result>?> = dao.getById(
        id = key.value,
        requestSerialName = requestSerializer.descriptor.serialName,
        resultSerialName = resultSerializer.descriptor.serialName
    ).map { roomItem ->
        roomItem?.toDomain(json, requestSerializer, resultSerializer)
    }

    /**
     * Gets items by their [keys] as flow
     */
    inline fun <reified Request : Outboxable<Request, Result>, reified Result : Any> get(
        keys: List<OutboxItemKey<Request, Result>>
    ) = get(keys, serializer(), serializer())

    /**
     * Gets items by their [keys] as flow
     */
    @OptIn(ExperimentalSerializationApi::class)
    fun <Request : Outboxable<Request, Result>, Result : Any> get(
        keys: List<OutboxItemKey<Request, Result>>,
        requestSerializer: KSerializer<Request>,
        resultSerializer: KSerializer<Result>
    ): Flow<List<OutboxItem<Request, Result>>> = dao.getByIds(
        ids = keys.map { it.value },
        requestSerialName = requestSerializer.descriptor.serialName,
        resultSerialName = resultSerializer.descriptor.serialName
    ).map { roomItems ->
        roomItems.map { roomItem ->
            roomItem.toDomain(json, requestSerializer, resultSerializer)
        }
    }

    /**
     * Gets items
     *
     * Example
     * ```kotlin
     * outboxRepository.getAll(
     *     fromIndexToIndex = 50..Long.MAX_VALUE, // almost all items, from index 50 to maximum
     *     statuses = listOf(RequestStatus.Success), // only successful ones
     *     sortBy = OutboxSort.Creation.NewerFirst, // sort by creation date
     * )
     * ```
     */

    inline fun <reified Request : Outboxable<Request, Result>, reified Result : Any> getAll(
        fromIndexToIndex: LongRange,
        statuses: List<RequestStatus> = RequestStatus.entries,
        creationTimeRange: ClosedRange<kotlin.time.Instant> = Instant.DISTANT_PAST..Instant.DISTANT_FUTURE,
        failureRange: IntRange = 0..Int.MAX_VALUE,
        sortBy: OutboxSort = OutboxSort.Creation.NewerFirst,
    ) = getAll(
        fromIndexToIndex = fromIndexToIndex,
        statuses = statuses,
        creationTimeRange = creationTimeRange,
        failureRange = failureRange,
        requestSerializer = serializer<Request>(),
        resultSerializer = serializer<Result>(),
        sortBy = sortBy,
    )

    /**
     * Gets items
     *
     * Example
     * ```kotlin
     * outboxRepository.getAll(
     *     fromIndexToIndex = 50..Long.MAX_VALUE, // almost all items, from index 50 to maximum
     *     statuses = listOf(RequestStatus.Success), // only successful ones
     *     sortBy = OutboxSort.Creation.NewerFirst, // sort by creation date
     * )
     * ```
     */
    @OptIn(ExperimentalSerializationApi::class)
    fun <Request : Outboxable<Request, Result>, Result : Any> getAll(
        fromIndexToIndex: LongRange,
        statuses: List<RequestStatus> = RequestStatus.entries,
        creationTimeRange: ClosedRange<kotlin.time.Instant> = Instant.DISTANT_PAST..Instant.DISTANT_FUTURE,
        failureRange: IntRange = 0..Int.MAX_VALUE,
        sortBy: OutboxSort = OutboxSort.Creation.NewerFirst,
        requestSerializer: KSerializer<Request>,
        resultSerializer: KSerializer<Result>,
    ): Flow<List<OutboxItem<Request, Result>>> {
        require(fromIndexToIndex.start >= 0L && fromIndexToIndex.endInclusive <= Long.MAX_VALUE) {
            "fromIndexToIndex must be within 0 and Long.MAX_VALUE, was ${fromIndexToIndex.start}..${fromIndexToIndex.endInclusive}"
        }
        require(fromIndexToIndex.start <= fromIndexToIndex.endInclusive) {
            "fromIndexToIndex must be in ascending order, was ${fromIndexToIndex.start}..${fromIndexToIndex.endInclusive}"
        }

        val limit =
            ((fromIndexToIndex.endInclusive - fromIndexToIndex.start) + 1L).coerceAtMost(Long.MAX_VALUE)

        require(statuses.isNotEmpty()) { "Statuses list cannot be empty for querying." }
        val offset = fromIndexToIndex.start

        val roomRawQuery = OutboxDao.buildFilteredQuery(
            statuses = statuses,
            creationTimeRange = creationTimeRange,
            failureRange = failureRange,
            requestSerialName = requestSerializer.descriptor.serialName,
            resultSerialName = resultSerializer.descriptor.serialName,
            sortBy = sortBy,
            limit = limit,
            offset = offset
        )

        val roomFlow = dao.getFiltered(roomRawQuery)

        return roomFlow.map { list ->
            list.map { it.toDomain(json, requestSerializer, resultSerializer) }
        }
    }

    /**
     * Counts outbox items of give type, with any of the [statuses],
     * created within [creationTimeRange], and within [failureRange]
     */
    @OptIn(ExperimentalSerializationApi::class)
    inline fun <reified Request : Outboxable<Request, *>> count(
        statuses: List<RequestStatus> = RequestStatus.entries,
        creationTimeRange: ClosedRange<kotlin.time.Instant> = Instant.DISTANT_PAST..Instant.DISTANT_FUTURE,
        failureRange: IntRange = 0..Int.MAX_VALUE,
    ) = count(
        statuses,
        creationTimeRange,
        failureRange,
        serialNames = listOf(serializer<Request>().descriptor.serialName)
    )

    @PublishedApi
    internal fun count(
        statuses: List<RequestStatus> = RequestStatus.entries,
        creationTimeRange: ClosedRange<kotlin.time.Instant> = Instant.DISTANT_PAST..Instant.DISTANT_FUTURE,
        failureRange: IntRange = 0..Int.MAX_VALUE,
        serialNames: List<String> = emptyList()
    ): Flow<Long> = dao.count(
        statuses = statuses.map { it.name },
        minTime = creationTimeRange.start.epochSeconds,
        maxTime = creationTimeRange.endInclusive.epochSeconds,
        minErrorCount = failureRange.start,
        maxErrorCount = failureRange.endInclusive,
        requestSerialNames = serialNames
    )

    @SensitiveOutboxApi
    suspend inline fun <reified Request : Outboxable<Request, Result>, reified Result : Any> delete(
        id: OutboxItemKey<Request, Result>
    ) = delete(id, serializer(), serializer())

    @OptIn(ExperimentalSerializationApi::class)
    @SensitiveOutboxApi
    suspend fun <Request : Outboxable<Request, Result>, Result : Any> delete(
        id: OutboxItemKey<Request, Result>,
        requestSerializer: KSerializer<Request>,
        resultSerializer: KSerializer<Result>
    ) {
        dao.deleteById(id.value, requestSerializer.descriptor.serialName, resultSerializer.descriptor.serialName)
    }

    @SensitiveOutboxApi
    suspend inline fun <reified Request : Outboxable<Request, Result>, reified Result : Any> delete(
        ids: List<OutboxItemKey<Request, Result>>
    ) = delete(ids, serializer(), serializer())

    @OptIn(ExperimentalSerializationApi::class)
    @SensitiveOutboxApi
    suspend fun <Request : Outboxable<Request, Result>, Result : Any> delete(
        ids: List<OutboxItemKey<Request, Result>>,
        requestSerializer: KSerializer<Request>,
        resultSerializer: KSerializer<Result>
    ) {
        if (ids.isNotEmpty()) {
            dao.deleteByIds(
                ids.map { it.value },
                requestSerializer.descriptor.serialName,
                resultSerializer.descriptor.serialName
            )
        }
    }

    /**
     * Returns number of rows affected
     */
    @SensitiveOutboxApi
    suspend inline fun <reified Request : Outboxable<Request, Result>, reified Result : Any> deleteIfStatus(
        keys: List<OutboxItemKey<Request, Result>>,
        statuses: List<RequestStatus>
    ) = deleteIfStatus(keys, statuses, serializer(), serializer())

    /**
     * Returns number of rows affected
     */
    @OptIn(ExperimentalSerializationApi::class)
    @SensitiveOutboxApi
    suspend fun <Request : Outboxable<Request, Result>, Result : Any> deleteIfStatus(
        keys: List<OutboxItemKey<Request, Result>>,
        statuses: List<RequestStatus>,
        requestSerializer: KSerializer<Request>,
        resultSerializer: KSerializer<Result>
    ): Int {
        if (keys.isEmpty() || statuses.isEmpty()) return 0

        return dao.deleteIfStatus(
            ids = keys.map { it.value },
            requestSerialName = requestSerializer.descriptor.serialName,
            resultSerialName = resultSerializer.descriptor.serialName,
            statusEnumNames = statuses.map { it.name }
        )
    }
}

@Dao
interface OutboxDao {
    companion object {
        @OptIn(ExperimentalSerializationApi::class)
        internal fun buildFilteredQuery(
            statuses: List<RequestStatus>,
            creationTimeRange: ClosedRange<kotlin.time.Instant>,
            failureRange: IntRange,
            requestSerialName: String,
            resultSerialName: String,
            sortBy: OutboxSort,
            limit: Long,
            offset: Long
        ): RoomRawQuery {
            val params = mutableListOf<Any?>()
            val queryBuilder = StringBuilder("SELECT * FROM outbox_items WHERE ")

            queryBuilder.append("statusEnumValue IN (")
            statuses.forEachIndexed { index, status ->
                queryBuilder.append("?")
                if (index < statuses.size - 1) queryBuilder.append(", ")
                params.add(status.name)
            }
            queryBuilder.append(") ")

            queryBuilder.append("AND createdAtEpochSeconds BETWEEN ? AND ? ")
            params.add(creationTimeRange.start.epochSeconds)
            params.add(creationTimeRange.endInclusive.epochSeconds)

            queryBuilder.append("AND failureCount BETWEEN ? AND ? ")
            params.add(failureRange.start)
            params.add(failureRange.endInclusive)

            queryBuilder.append("AND requestSerialName = ? ")
            params.add(requestSerialName)

            queryBuilder.append("AND resultSerialName = ? ")
            params.add(resultSerialName)

            queryBuilder.append("ORDER BY ${sortBy.columnName} ${sortBy.sqlOrderKeyword} ")

            queryBuilder.append("LIMIT ? OFFSET ?")
            params.add(limit)
            params.add(offset)

            val sqlQueryString = queryBuilder.toString()
            val bindArgs = params.toTypedArray()

            return RoomRawQuery(
                sql = sqlQueryString,
                onBindStatement = createOnBindStatement(bindArgs)
            )
        }
    }

    @Query(
        "SELECT statusEnumValue FROM " +
                "outbox_items WHERE `key` = :id AND " +
                "requestSerialName = :requestSerialName AND " +
                "resultSerialName = :resultSerialName LIMIT 1"
    )
    suspend fun getStatus(
        id: String,
        requestSerialName: String,
        resultSerialName: String
    ): String?


    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertOrAbort(item: RoomOutboxItem): Long

    @Update
    suspend fun update(item: RoomOutboxItem): Int

    @Query(
        """
        UPDATE outbox_items
        SET statusEnumValue = :statusEnumValue,
            requestJson = :requestJson,
            resultJson = :resultJson,
            requestSerialName = :requestSerialName,
            resultSerialName = :resultSerialName,
            failureCount = :failureCount,
            createdAtEpochSeconds = :createdAtEpochSeconds,
            lastAttemptedAtEpochSeconds = :lastAttemptedAtEpochSeconds
        WHERE `key` = :key AND statusEnumValue != :processingName
    """
    )
    suspend fun updateIfNotProcessing(
        key: String,
        statusEnumValue: String,
        requestJson: String,
        resultJson: String?,
        requestSerialName: String,
        resultSerialName: String?,
        failureCount: Int,
        createdAtEpochSeconds: Long,
        lastAttemptedAtEpochSeconds: Long?,
        processingName: String = RequestStatus.Processing.name
    ): Int


    /**
     * Single transactional upsert that handles atomic ReplacementStrategy variants.
     *
     * Returns 1 if a row was inserted or updated, 0 if the operation was ignored.
     *
     * Throws for FailIfExists and FailIfInStates when the condition matches.
     *
     * Note: ReplacementStrategy.Conditional is NOT handled here and will throw.
     */
    @Transaction
    suspend fun saveOrUpdate(item: RoomOutboxItem, strategy: ReplacementStrategy): Int {
        when (strategy) {
            ReplacementStrategy.ReplaceAlways -> {
                val updated = update(item)
                if (updated > 0) return updated
                val insertedRow = insertOrAbort(item)
                return if (insertedRow >= 0) 1 else 0
            }

            ReplacementStrategy.IgnoreIfExists -> {
                val exists = getStatus(item.key, item.requestSerialName, item.resultSerialName)
                if (exists == null) {
                    insertOrAbort(item)
                    return 1
                }
                return 0
            }

            ReplacementStrategy.FailIfExists -> {
                val exists = getStatus(item.key, item.requestSerialName, item.resultSerialName)
                if (exists == null) {
                    insertOrAbort(item)
                    return 1
                }
                throw IllegalStateException("Item ${item.key} already exists")
            }

            is ReplacementStrategy.FailIfInStates -> {
                val exists = getStatus(item.key, item.requestSerialName, item.resultSerialName)
                val blockedNames = strategy.statuses.map { it.name }.toSet()
                if (exists == null) {
                    insertOrAbort(item)
                    return 1
                }
                if (exists in blockedNames) {
                    throw IllegalStateException("Item ${item.key} is in a blocking state: $exists")
                }
                val affected = update(item)
                if (affected == 0) throw IllegalStateException("Expected to update ${item.key} but affected 0 rows")
                return 1
            }

            is ReplacementStrategy.ReplaceIfInStates -> {
                val exists = getStatus(item.key, item.requestSerialName, item.resultSerialName)
                val allowedNames = strategy.statuses.map { it.name }.toSet()
                if (exists == null) {
                    insertOrAbort(item)
                    return 1
                }
                return if (exists in allowedNames) {
                    val affected = update(item)
                    // should we throw
                    if (affected == 0) throw IllegalStateException("Expected to update ${item.key} but affected 0 rows")
                    return 1
                } else {
                    0
                }
            }

            is ReplacementStrategy.IgnoreIfInStates -> {
                val exists = getStatus(item.key, item.requestSerialName, item.resultSerialName)
                val ignoredNames = strategy.statuses.map { it.name }.toSet()
                if (exists == null) {
                    insertOrAbort(item)
                    return 1
                }
                return if (exists in ignoredNames) {
                    0
                } else {
                    val affected = update(item)
                    if (affected == 0) throw IllegalStateException("Expected to update ${item.key} but affected 0 rows")
                    return 1
                }
            }
        }
    }

    @Query(
        """SELECT * FROM outbox_items 
        WHERE `key` = :id 
          AND requestSerialName = :requestSerialName 
          AND resultSerialName = :resultSerialName 
        LIMIT 1"""
    )
    fun getById(
        id: String,
        requestSerialName: String,
        resultSerialName: String
    ): Flow<RoomOutboxItem?>

    @Query(
        """SELECT * FROM outbox_items 
        WHERE `key` IN (:ids) 
          AND requestSerialName = :requestSerialName 
          AND resultSerialName = :resultSerialName"""
    )
    fun getByIds(
        ids: List<String>,
        requestSerialName: String,
        resultSerialName: String
    ): Flow<List<RoomOutboxItem>>

    @RawQuery(observedEntities = [RoomOutboxItem::class])
    fun getFiltered(query: RoomRawQuery): Flow<List<RoomOutboxItem>>

    @Query(
        "SELECT COUNT(*) FROM outbox_items" +
                " WHERE statusEnumValue IN (:statuses) AND" +
                " createdAtEpochSeconds BETWEEN :minTime AND :maxTime AND " +
                "failureCount BETWEEN :minErrorCount AND :maxErrorCount AND " +
                "requestSerialName IN (:requestSerialNames)"
    )
    fun count(
        statuses: List<String>,
        minTime: Long,
        maxTime: Long,
        minErrorCount: Int,
        maxErrorCount: Int,
        requestSerialNames: List<String>
    ): Flow<Long>

    @Query(
        "SELECT COUNT(*) FROM outbox_items" +
                " WHERE statusEnumValue IN (:statuses) AND" +
                " createdAtEpochSeconds BETWEEN :minTime AND :maxTime AND " +
                "failureCount BETWEEN :minErrorCount AND :maxErrorCount"
    )
    fun countWithout(
        statuses: List<String>,
        minTime: Long,
        maxTime: Long,
        minErrorCount: Int,
        maxErrorCount: Int
    ): Flow<Long>

    @Query(
        """DELETE FROM outbox_items 
        WHERE `key` = :id 
          AND requestSerialName = :requestSerialName 
          AND resultSerialName = :resultSerialName"""
    )
    suspend fun deleteById(
        id: String,
        requestSerialName: String,
        resultSerialName: String
    )

    @Query(
        """DELETE FROM outbox_items 
        WHERE `key` IN (:ids) 
          AND requestSerialName = :requestSerialName 
          AND resultSerialName = :resultSerialName"""
    )
    suspend fun deleteByIds(ids: List<String>, requestSerialName: String, resultSerialName: String)

    @Query(
        """DELETE FROM outbox_items 
        WHERE `key` IN (:ids) 
          AND requestSerialName = :requestSerialName 
          AND resultSerialName = :resultSerialName 
          AND statusEnumValue IN (:statusEnumNames)"""
    )
    suspend fun deleteIfStatus(
        ids: List<String>,
        requestSerialName: String,
        resultSerialName: String,
        statusEnumNames: List<String>
    ): Int

    @Query(
        """UPDATE outbox_items 
        SET statusEnumValue = :processingEnumName,
            lastAttemptedAtEpochSeconds = :nowEpoch,
            resultJson = NULL
        WHERE `key` = :id
          AND requestSerialName = :requestSerialName
          AND resultSerialName = :resultSerialName
          AND statusEnumValue != :processingEnumName"""
    )
    suspend fun markAsProcessing(
        id: String,
        requestSerialName: String,
        resultSerialName: String,
        processingEnumName: String = RequestStatus.Processing.name,
        nowEpoch: Long = now().epochSeconds
    ): Int

    @Transaction
    suspend fun markAsProcessingOrThrowAndReset(
        id: String,
        requestSerialName: String,
        resultSerialName: String,
        throwIfProcessingAlready: Boolean = true
    ) {
        val current = getById(id, requestSerialName, resultSerialName).first()
            ?: error("Cant mark as processing for non-existent item")

        if (current.statusEnumValue == RequestStatus.Processing.name && throwIfProcessingAlready) {
            throw IllegalStateException("Item is already being processed")
        }

        val affectedRows = markAsProcessing(id, requestSerialName, resultSerialName)

        if (affectedRows == 0) {
            val affectedRowsAtReset = update(current)
            if (affectedRowsAtReset == 0) {
                error("Error marking item as processing and resetting state")
            }
            if (throwIfProcessingAlready) {
                error("Error marking item as processing")
            }
        }
    }
}

@Database(entities = [RoomOutboxItem::class], version = 1, exportSchema = false)
@ConstructedBy(OutboxRoomDatabaseConstructor::class)
abstract class OutboxRoomDatabase : RoomDatabase() {
    abstract fun dao(): OutboxDao
}

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object OutboxRoomDatabaseConstructor : RoomDatabaseConstructor<OutboxRoomDatabase> {
    override fun initialize(): OutboxRoomDatabase
}

/**
 * Represents the various states an outbox item can be in during its lifecycle.
 */
enum class RequestStatus {
    /**
     * The initial state of an item when it's first added to the outbox.
     * It is awaiting processing.
     */
    Pending,

    /**
     * The item is currently being processed.
     * This status is set when an attempt to process the item begins.
     */
    Processing,

    /**
     * The item has been successfully processed.
     * The associated [OutboxItem.result] should contain the outcome.
     */
    Success,

    /**
     * Processing the item failed, but the failure is deemed recoverable.
     * The system may attempt to reprocess this item.
     */
    FailedRecoverable,

    /**
     * Processing the item failed, and the failure is deemed unrecoverable.
     * The system will not attempt to reprocess this item automatically.
     */
    FailedUnrecoverable
}

/**
 * Represents an individual item within the outbox system.
 * It encapsulates the request to be processed, its current status,
 * any result from processing, and metadata for tracking and management.
 *
 * @param Request The type of the [Outboxable] request.
 * @param Result The type of the expected result upon successful processing.
 * @property key The unique identifier for this outbox item. See [OutboxItemKey].
 * @property request The actual [Outboxable] request data to be processed.
 * @property status The current [RequestStatus] of this item. Defaults to [RequestStatus.Pending].
 * @property result The result of processing the request, if successful. Null otherwise or if not yet processed.
 * @property failureCount The number of times processing has been marked as failed. Incremented by [OutboxRepository.markAsFailed].
 * @property createdAt The [Instant] when this item was created and enqueued.
 * @property lastAttemptedAt The [Instant] when the last processing attempt for this item was made.
 *                           Null if no processing attempt has occurred yet.
 */
data class OutboxItem<Request : Outboxable<Request, Result>, Result : Any>(
    val key: OutboxItemKey<Request, Result>,
    val request: Request,
    val status: RequestStatus = RequestStatus.Pending,
    val result: Result? = null,
    val failureCount: Int = 0,
    val createdAt: kotlin.time.Instant = kotlin.time.Clock.System.now(),
    val lastAttemptedAt: kotlin.time.Instant? = null
)

/**
 * Key holder that exists to enforce type safety. You can not
 * create it at will. Your request must implement [Outboxable] and
 * specify a key, for example:
 * ```kotlin
 * @Serializable
 * @SerialName("whatever") // good practice to explicitly set the serial name
 * data class YourRequest(
 *     val value: YourValue,
 *     override val outboxKey: String
 * ) : Outboxable<YourRequest, YourResult>
 * ```
 * To get the [OutboxItemKey] you need to interact with [OutboxRepository],
 * for example:
 * ```kotlin
 * // is OutboxItemKey<YourRequest, YourResult>
 * val outboxKey = outboxRepository.enqueue(
 *     YourRequest(YourValue(...), "pretendThisIsARandomKey")
 * )
 * ```
 * And then you can use it to update the item in the repository, like
 * ```kotlin
 * outboxRepository.markAsSuccess(outboxKey, YourResult.Success(...))
 * ```
 */
@ConsistentCopyVisibility
data class OutboxItemKey<Request : Any, Result : Any> @PublishedApi internal constructor(
    val value: String
)

/**
 * Represents an item that can be processed through the outbox.
 * Implementations of this interface define the structure of a request
 * and are responsible for providing a unique key for outbox tracking.
 *
 * Example
 * ```kotlin
 * @Serializable
 * @SerialName("likeaction") // good practice to specify serial names
 * sealed interface LikeAction : Outboxable<LikeAction, LikeResult> {
 *     // the same outbox key can be used for the same post id,
 *     // for example, if you want to make sure only one like action
 *     // is scheduled per post id
 *
 *     @Serializable
 *     @SerialName("like)
 *     // lets make post id the the outbox key by default
 *     data class Like(postId: PostId, override val outboxKey: String = postId.toString()) : LikeAction
 *
 *     @Serializable
 *     @SerialName("unlike")
 *     data class Unlike(postId: PostId, override val outboxKey: String = postId.toString()) : LikeAction
 * }
 * ```
 *
 * To know about the processing of outbox items - refer to [processContinuously]
 *
 * @param Request The type of the request itself, typically the implementing class.
 * @param Result The type of the expected result upon successful processing of the request.
 */
interface Outboxable<Request : Any, Result : Any> {
    /**
     * A unique key
     */
    val outboxKey: String
}

@Entity(tableName = "outbox_items")
data class RoomOutboxItem(
    val key: String,
    val statusEnumValue: String,
    val requestJson: String,
    val resultJson: String?,
    val requestSerialName: String,
    val resultSerialName: String,
    @ColumnInfo(failureCountColumn)
    val failureCount: Int,
    @ColumnInfo(createdAtEpochSecondsColumn)
    val createdAtEpochSeconds: Long,
    @ColumnInfo(lastAttemptedAtEpochSecondsColumn)
    val lastAttemptedAtEpochSeconds: Long?,
    @PrimaryKey val actualKey: String = key + resultSerialName + requestSerialName
)

@OptIn(ExperimentalSerializationApi::class)
fun <Request : Outboxable<Request, Result>, Result : Any> OutboxItem<Request, Result>.toRoom(
    json: Json = outboxJson,
    requestSerializer: KSerializer<Request>,
    resultSerializer: KSerializer<Result>
) = RoomOutboxItem(
    key = this.key.value,
    statusEnumValue = this.status.name,
    requestJson = json.encodeToString(requestSerializer, this.request),
    resultJson = this.result?.let { json.encodeToString(resultSerializer, it) },
    requestSerialName = requestSerializer.descriptor.serialName,
    resultSerialName = resultSerializer.descriptor.serialName,
    failureCount = this.failureCount,
    createdAtEpochSeconds = this.createdAt.epochSeconds,
    lastAttemptedAtEpochSeconds = this.lastAttemptedAt?.epochSeconds
)

@OptIn(ExperimentalSerializationApi::class)
fun <Request : Outboxable<Request, Result>, Result : Any> RoomOutboxItem.toDomain(
    json: Json = outboxJson,
    requestSerializer: KSerializer<Request>,
    resultSerializer: KSerializer<Result>
) = OutboxItem<Request, Result>(
    key = OutboxItemKey(this.key),
    status = RequestStatus.valueOf(this.statusEnumValue),
    request = json.decodeFromString(requestSerializer, this.requestJson),
    result = this.resultJson?.let { json.decodeFromString(resultSerializer, it) },
    failureCount = this.failureCount,
    createdAt = Instant.fromEpochSeconds(this.createdAtEpochSeconds),
    lastAttemptedAt = this.lastAttemptedAtEpochSeconds?.let { Instant.fromEpochSeconds(it) }
)

@OptIn(ExperimentalSerializationApi::class)
private val outboxJson = Json {
    classDiscriminator = serialNameKey
    classDiscriminatorMode = ClassDiscriminatorMode.POLYMORPHIC
    ignoreUnknownKeys = true
}

private const val serialNameKey = "type"

sealed interface OutboxSort {
    /**
     * Sort by the time of creation of the item
     */
    sealed interface Creation : OutboxSort {
        data object NewerFirst : Creation
        data object OlderFirst : Creation
    }

    /**
     * Sort by the time of last execution, aka last time an item
     * was marked as [RequestStatus.Processing]
     */
    sealed interface LastExecutionAttempt : OutboxSort {
        data object NewerFirst : LastExecutionAttempt
        data object OlderFirst : LastExecutionAttempt
    }

    /**
     * Sort by failure count
     */
    sealed interface FailureCount : OutboxSort {
        data object LowerFirst : FailureCount
        data object HigherFirst : FailureCount
    }
}

private const val asc = "ASC"
private const val desc = "DESC"

internal val OutboxSort.columnName
    get() = when (this) {
        is OutboxSort.Creation -> createdAtEpochSecondsColumn
        is OutboxSort.LastExecutionAttempt -> lastAttemptedAtEpochSecondsColumn
        is OutboxSort.FailureCount -> failureCountColumn
    }

private val OutboxSort.sqlOrderKeyword
    get() = when (this) {
        OutboxSort.Creation.NewerFirst,
        OutboxSort.LastExecutionAttempt.NewerFirst,
        OutboxSort.FailureCount.HigherFirst -> desc

        OutboxSort.Creation.OlderFirst,
        OutboxSort.LastExecutionAttempt.OlderFirst,
        OutboxSort.FailureCount.LowerFirst -> asc
    }

private fun createOnBindStatement(bindArgs: Array<Any?>): (SQLiteStatement) -> Unit =
    { statement ->
        bindArgs.forEachIndexed { indexBinding, arg ->
            val bindIndex = indexBinding + 1 // SQLite parameters are 1-indexed
            when (arg) {
                null -> statement.bindNull(bindIndex)
                is String -> statement.bindText(bindIndex, arg)
                is Long -> statement.bindLong(bindIndex, arg)
                is Int -> statement.bindInt(bindIndex, arg)
                is Double -> statement.bindDouble(bindIndex, arg)
                is Float -> statement.bindFloat(bindIndex, arg)
                is ByteArray -> statement.bindBlob(bindIndex, arg)
                is Boolean -> statement.bindBoolean(bindIndex, arg)
                else -> throw IllegalArgumentException("Unsupported bind argument type: ${arg.let { it::class.simpleName }}")
            }
        }
    }

@Suppress("ExperimentalAnnotationRetention")
@RequiresOptIn(
    "This api is delicate. Please refer to enqueue, markAsProcessing, markAsSuccess, and markAsFailed methods as they do a little more than just saving the outbox item to the database. For outbox item deletion please refer to outboxCleaner function, deleting items immediately is unnecessary and is potentially an undesirable behavior when working with data in ui that depends on some items in the outbox, particularly when making animated layouts.",
    level = RequiresOptIn.Level.WARNING
)
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
annotation class SensitiveOutboxApi()

sealed interface ReplacementStrategy {
    /**
     * Always replaces existing row or inserts if missing
     */
    data object ReplaceAlways : ReplacementStrategy

    /**
     * Inserts if missing; else ignored
     */
    data object IgnoreIfExists : ReplacementStrategy

    /**
     * Inserts if missing; else throws
     */
    data object FailIfExists : ReplacementStrategy

    /**
     * Throws if existing item is in any of the provided states
     */
    data class FailIfInStates(val statuses: Set<RequestStatus>) : ReplacementStrategy

    /**
     * Replaces if existing item is in any of the provided states
     */
    data class ReplaceIfInStates(val statuses: Set<RequestStatus>) : ReplacementStrategy

    /**
     * Ignore if existing item is in any of the provided states
     */
    data class IgnoreIfInStates(val statuses: Set<RequestStatus>) : ReplacementStrategy
}

internal const val createdAtEpochSecondsColumn = "createdAtEpochSeconds"
internal const val lastAttemptedAtEpochSecondsColumn = "lastAttemptedAtEpochSeconds"
internal const val failureCountColumn = "failureCount"