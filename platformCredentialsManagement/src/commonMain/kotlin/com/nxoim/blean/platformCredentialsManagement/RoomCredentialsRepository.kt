package com.nxoim.blean.platformCredentialsManagement

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
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.nxoim.blean.bskyPrimitives.AccountIdentificator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

private const val logTag = "RoomCredentialsRepository"

class RoomCredentialsRepository(
    private val dao: PlatformCredentialsDao,
    private val logger: Logger,
) : PlatformCredentialsRepository {

    override fun get(
        did: AccountIdentificator.Did
    ): Flow<Result<PlatformCredentials, PlatformCredentialsRetrievalError>?> =
        dao.get(did.toString())
            .onStart {
                logger.v(tag = logTag) { "Fetching credentials for DID: $did" }
            }
            .map { entity ->
                entity?.let {
                    logger.v(tag = logTag) { "Decoding credentials for DID: ${entity.did}" }
                    decodeCredentials(entity)
                }
            }


    override suspend fun saveOrUpdate(
        credentials: PlatformCredentials,
        label: String
    ): Result<Unit, PlatformCredentialsSavingError> {
        logger.v(tag = logTag) {
            "saveOrUpdate called for DID: ${credentials.did}, label: $label"
        }

        return try {
            logger.i(tag = logTag) { "Encoding credentials JSON" }

            val entity = withContext(Dispatchers.Default) {
                val json = Json.encodeToString(
                    PlatformCredentials.serializer(),
                    credentials
                )

                RoomPlatformCredentials(
                    did = credentials.did.toString(),
                    credentialsJson = json,
                )
            }

            logger.v(tag = logTag) { "Saving entity for DID: ${entity.did}" }
            dao.saveOrUpdate(entity)

            Ok(Unit)
        } catch (e: SerializationException) {
            logger.e(e, tag = logTag) { "Error encoding platform credentials" }
            Err(
                PlatformCredentialsSavingError.EncodingError(
                    "Failed to encode platform credentials"
                )
            )
        } catch (e: Exception) {
            logger.e(e, tag = logTag) {
                "Error saving or updating credentials in database"
            }
            Err(
                PlatformCredentialsSavingError.UnknownError(
                    "Failed to save or update credentials in database"
                )
            )
        }
    }

    override suspend fun remove(
        did: AccountIdentificator.Did
    ): Result<Unit, PlatformCredentialsRemovalError> {
        logger.v(tag = logTag) { "Removing credentials for DID: $did" }

        return try {
            dao.delete(did.toString())
            logger.v(tag = logTag) { "Removed credentials for DID: $did" }
            Ok(Unit)
        } catch (e: Exception) {
            logger.e(e, tag = logTag) {
                "Error removing credentials from database"
            }
            Err(
                PlatformCredentialsRemovalError.UnknownError(
                    "Failed to remove credentials from database"
                )
            )
        }
    }

    private fun decodeCredentials(
        entity: RoomPlatformCredentials
    ): Result<PlatformCredentials, PlatformCredentialsRetrievalError> = try {
        val credentials =
            Json.decodeFromString<PlatformCredentials>(entity.credentialsJson)

        logger.v(tag = logTag) {
            "Decoded credentials successfully for DID: ${entity.did}"
        }

        Ok(credentials)
    } catch (e: SerializationException) {
        logger.e(e, tag = logTag) {
            "Error decoding credentials for DID: ${entity.did}"
        }
        Err(
            PlatformCredentialsRetrievalError.DecodingError(
                "Failed to decode credentials"
            )
        )
    } catch (e: Exception) {
        logger.e(e, tag = logTag) {
            "Unknown decoding error for DID: ${entity.did}"
        }
        Err(
            PlatformCredentialsRetrievalError.UnknownError(
                "Unknown error decoding credentials"
            )
        )
    }
}


@Entity(tableName = "platform_credentials")
data class RoomPlatformCredentials(
    @PrimaryKey val did: String,
    val credentialsJson: String,
)

@Dao
interface PlatformCredentialsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveOrUpdate(credentials: RoomPlatformCredentials)

    @Query("SELECT * FROM platform_credentials WHERE did = :did LIMIT 1")
    fun get(did: String): Flow<RoomPlatformCredentials?>

    @Query("SELECT * FROM platform_credentials")
    fun getAll(): Flow<List<RoomPlatformCredentials>>

    @Query("DELETE FROM platform_credentials WHERE did = :did")
    suspend fun delete(did: String)
}

@Database(entities = [RoomPlatformCredentials::class], version = 1, exportSchema = false)
@ConstructedBy(PlatformCredentialsDatabaseConstructor::class)
abstract class PlatformCredentialsDatabase : RoomDatabase() {
    abstract fun dao(): PlatformCredentialsDao
}

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object PlatformCredentialsDatabaseConstructor :
    RoomDatabaseConstructor<PlatformCredentialsDatabase> {
    override fun initialize(): PlatformCredentialsDatabase
}
