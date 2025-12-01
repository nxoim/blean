@file:OptIn(ExperimentalTime::class)

package com.nxoim.blean.repos

import androidx.room.ConstructedBy
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import kotlin.jvm.JvmInline
import kotlin.time.ExperimentalTime

private const val logTag = "ApiResponseCache"

class ApiResponseCache(
    private val dao: ApiResponseDao,
    private val logger: Logger
) {
    suspend inline fun <reified T : Any> saveOrUpdate(key: CacheKey<T>, value: T) =
        saveOrUpdate(key, value, serializer())

    suspend fun <T : Any> saveOrUpdate(
        key: CacheKey<T>,
        value: T,
        serializer: KSerializer<T>,
    ) = dao.saveOrUpdate(
        RoomApiResponse(
            id = generateEntryId<T>(key),
            json = jsonConfig.encodeToString(serializer, value),
            className = serializer.descriptor.serialName
        )
    )

    inline fun <reified T : Any> get(key: CacheKey<T>): Flow<T?> =
        get(key, serializer())

    inline fun <reified T : Any> get(keys: List<CacheKey<T>>): Flow<List<T>> =
        get(keys, serializer())

    fun <T : Any> get(
        key: CacheKey<T>,
        serializer: KSerializer<T>
    ): Flow<T?> = dao.get(
        id = generateEntryId<T>(key),
        className = serializer.descriptor.serialName
    ).map { entity ->
        entity?.let {
            try {
                jsonConfig.decodeFromString(serializer, it.json)
            } catch (e: Exception) {
                // TODO: handle corruption, maybe delete the entry?
                logger.e(
                    e,
                    tag = logTag
                ) { "Failed to decode cached data for id ${it.id}: ${e.message}" }
                null
            }
        }
    }

    fun <T : Any> get(
        keys: List<CacheKey<T>>,
        serializer: KSerializer<T>
    ): Flow<List<T>> = dao.get(
        ids = keys.map { generateEntryId<T>(it) },
        className = serializer.descriptor.serialName
    ).map { roomApiResponses ->
        roomApiResponses.mapNotNull {
            try {
                jsonConfig.decodeFromString(serializer, it.json)
            } catch (e: Exception) {
                // TODO: handle corruption, maybe delete the entry?
                logger.e(
                    e,
                    tag = logTag
                ) { "Failed to decode cached data for id ${it.id}: ${e.message}" }
                null
            }
        }
    }

    suspend inline fun <reified T : Any> delete(key: CacheKey<T>) =
        delete(key, serializer())

    suspend fun <T : Any> delete(key: CacheKey<T>, serializer: KSerializer<T>) {
        dao.delete(
            generateEntryId<T>(key),
            serializer.descriptor.serialName
        )
    }

    inline fun <reified T : Any> count(keyPrefix: CacheKey<T>): Flow<Long> =
        count(keyPrefix, serializer())

    fun <T : Any> count(keyPrefix: CacheKey<T>, serializer: KSerializer<T>): Flow<Long> =
        dao.count(
            generateEntryId<T>(keyPrefix),
            serializer.descriptor.serialName
        )

    suspend inline fun <reified T : Any> deleteByPrefix(keyPrefix: CacheKey<T>) =
        deleteByPrefix(keyPrefix, serializer())

    suspend fun <T : Any> deleteByPrefix(keyPrefix: CacheKey<T>, serializer: KSerializer<T>) {
        dao.deleteByPrefix(
            generateEntryId<T>(keyPrefix),
            serializer.descriptor.serialName
        )
    }
}

@Entity(tableName = "api_response_cache")
data class RoomApiResponse(
    @PrimaryKey val id: String,
    val json: String,
    val className: String,
    val creationTimeEpochSeconds: Long = kotlin.time.Clock.System.now().epochSeconds
)

@Dao
interface ApiResponseDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveOrUpdate(response: RoomApiResponse)

    @Query("SELECT * FROM api_response_cache WHERE id = :id AND className = :className LIMIT 1")
    fun get(id: String, className: String): Flow<RoomApiResponse?>

    @Query("SELECT * FROM api_response_cache WHERE id IN (:ids) AND className = :className")
    fun get(ids: List<String>, className: String): Flow<List<RoomApiResponse>>

    @Query("DELETE FROM api_response_cache WHERE id = :id AND className = :className")
    suspend fun delete(id: String, className: String)

    // Uses LIKE with '%' wildcard to simulate BEGINSWITH
    @Query("SELECT COUNT(*) FROM api_response_cache WHERE id LIKE :idPrefix || '%' AND className = :className")
    fun count(idPrefix: String, className: String): Flow<Long>

    // Uses LIKE with '%' wildcard simulate BEGINSWITH
    @Query("DELETE FROM api_response_cache WHERE id LIKE :idPrefix || '%' AND className = :className")
    suspend fun deleteByPrefix(idPrefix: String, className: String)
}

@Database(entities = [RoomApiResponse::class], version = 1, exportSchema = false)
@ConstructedBy(ApiResponseRoomDatabaseConstructor::class)
abstract class ApiResponseRoomDatabase : RoomDatabase() {
    abstract fun get(): ApiResponseDao
}

@SuppressNoActualForExpect
expect object ApiResponseRoomDatabaseConstructor :
    RoomDatabaseConstructor<ApiResponseRoomDatabase> {
    override fun initialize(): ApiResponseRoomDatabase
}

private val jsonConfig = Json {
    ignoreUnknownKeys = true
}

fun <T : Any> generateEntryId(key: CacheKey<T>) = key.value

/**
 * Exists to make it easier to keep track of the type of data
 */
@JvmInline
value class CacheKey<T : Any>(val value: String)