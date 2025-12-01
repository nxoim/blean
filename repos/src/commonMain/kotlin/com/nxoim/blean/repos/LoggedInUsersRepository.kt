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
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.getOrThrow
import com.github.michaelbull.result.onSuccess
import com.github.michaelbull.result.runCatching
import com.nxoim.blean.api.api.oauthStuff.models.ECDSAP256InBase64Keys
import com.nxoim.blean.bskyPrimitives.AccountIdentificator
import com.nxoim.blean.bskyPrimitives.Did
import com.nxoim.blean.models.LoggedInUserBasicDetails
import com.nxoim.blean.platformCredentialsManagement.OAuthCredentials
import com.nxoim.blean.platformCredentialsManagement.PlatformCredentials
import com.nxoim.blean.platformCredentialsManagement.PlatformCredentialsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class LoggedInUsersRepository(
    private val userDao: LoggedInUserBasicDetailsDao,
    private val platformCredentialsRepository: PlatformCredentialsRepository
) {
    suspend fun saveOrUpdateOAuthCredentials(
        did: AccountIdentificator.Did,
        accessToken: String,
        refreshToken: String?,
        authorizationServerUrl: String,
        clientId: String,
        keyPair: ECDSAP256InBase64Keys,
        label: String = did.toString()
    ): Result<Unit, Throwable> {
        val currentCreds = platformCredentialsRepository.get(did).firstOrNull()
        val actualRefreshToken =
            refreshToken ?: (currentCreds as? PlatformCredentials.OAuth)?.value?.refreshToken

        if (actualRefreshToken == null && refreshToken == null) {
            return Err(IllegalStateException("Refresh token is required but not provided and couldn't be found."))
        }

        platformCredentialsRepository.saveOrUpdate(
            PlatformCredentials.OAuth(
                OAuthCredentials(
                    did,
                    accessToken,
                    actualRefreshToken!!,
                    authorizationServerUrl,
                    clientId,
                    keyPair
                )
            ),
            label = label
        )

        return Ok(Unit)
    }

    fun getCredentials(did: AccountIdentificator.Did) =
        platformCredentialsRepository.get(did)

    suspend fun removeOAuthCredentials(did: AccountIdentificator.Did) =
        platformCredentialsRepository.remove(did)

    suspend fun saveOrUpdateLoggedInUser(
        basicDetails: LoggedInUserBasicDetails
    ): Result<Unit, Throwable> = runCatching {
        val existingUser = userDao.getByDid(basicDetails.did.toString()).firstOrNull()
        userDao.saveOrUpdate(basicDetails.toRoom(lastUsed = existingUser?.lastUsed))
    }

    suspend fun saveOrUpdateLoggedInUser(
        did: AccountIdentificator.Did,
        handle: String,
        displayName: String?,
        avatarUrl: String?
    ): Result<Unit, Throwable> = runCatching {
        val details = LoggedInUserBasicDetails(
            did,
            AccountIdentificator.Handle(handle),
            displayName,
            avatarUrl
        )
        val existingUser = userDao.getByDid(did.toString()).firstOrNull()
        userDao.saveOrUpdate(details.toRoom(lastUsed = existingUser?.lastUsed))
    }


    fun getLoggedInUser(did: AccountIdentificator.Did): Flow<LoggedInUserBasicDetails?> =
        userDao.getByDid(did.toString()).map { it?.toDomain() }


    suspend fun updateLoggedInUserLastUsed(did: AccountIdentificator.Did): Result<Unit, Throwable> =
        runCatching {
            userDao.updateLastUsed(did.toString(), Clock.System.now().epochSeconds)
        }

    suspend fun removeLoggedInUserAndCredentials(
        did: AccountIdentificator.Did
    ): Result<Unit, *> = platformCredentialsRepository
        .remove(did)
        .onSuccess { userDao.deleteByDid(did.toString()) }

    fun getAllLoggedInUsers(): Flow<List<LoggedInUserBasicDetails>> = userDao
        .getAllSortedByLastUsed()
        .map { list -> list.map { it.toDomain() } }

    suspend fun clear(): Result<Unit, Throwable> = runCatching {
//        platformCredentialsRepository.clearAll()
        userDao.deleteAll()
//         credClearResult.getOrThrow()
    }
}

@Database(entities = [RoomLoggedInUserBasicDetails::class], version = 1, exportSchema = false)
@ConstructedBy(LoggedInUsersRoomDatabaseConstructor::class)
abstract class LoggedInUsersRoomDatabase : RoomDatabase() {
    abstract fun dao(): LoggedInUserBasicDetailsDao
}

@SuppressNoActualForExpect
expect object LoggedInUsersRoomDatabaseConstructor :
    RoomDatabaseConstructor<LoggedInUsersRoomDatabase> {
    override fun initialize(): LoggedInUsersRoomDatabase
}

@Dao
interface LoggedInUserBasicDetailsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveOrUpdate(user: RoomLoggedInUserBasicDetails)

    @Query("SELECT * FROM logged_in_users WHERE did = :did LIMIT 1")
    fun getByDid(did: String): Flow<RoomLoggedInUserBasicDetails?>

    @Query("UPDATE logged_in_users SET lastUsed = :lastUsedTimestamp WHERE did = :did")
    suspend fun updateLastUsed(did: String, lastUsedTimestamp: Long)

    @Query("DELETE FROM logged_in_users WHERE did = :did")
    suspend fun deleteByDid(did: String)

    @Query("SELECT * FROM logged_in_users ORDER BY lastUsed ASC")
    fun getAllSortedByLastUsed(): Flow<List<RoomLoggedInUserBasicDetails>>

    @Query("DELETE FROM logged_in_users")
    suspend fun deleteAll()
}

@Entity(tableName = "logged_in_users")
data class RoomLoggedInUserBasicDetails(
    @PrimaryKey val did: String,
    val handle: String,
    val displayName: String?,
    val avatarUrl: String?,
    val lastUsed: Long
)

fun LoggedInUserBasicDetails.toRoom(lastUsed: Long? = null): RoomLoggedInUserBasicDetails {
    return RoomLoggedInUserBasicDetails(
        did = this.did.toString(),
        handle = this.handle.toString(),
        displayName = this.displayName,
        avatarUrl = this.avatarUrl,
        lastUsed = lastUsed ?: Clock.System.now().epochSeconds
    )
}

fun RoomLoggedInUserBasicDetails.toDomain(): LoggedInUserBasicDetails {
    return LoggedInUserBasicDetails(
        did = AccountIdentificator.Did(Did.parse(this.did).getOrThrow()),
        handle = AccountIdentificator.Handle(this.handle),
        displayName = this.displayName,
        avatarUrl = this.avatarUrl
    )
}
