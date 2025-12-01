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
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.runCatching
import com.nxoim.blean.api.api.oauthStuff.models.OAuthCodeVerifier
import com.nxoim.blean.api.api.oauthStuff.models.OAuthStateToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// bare minimum needed for persisting necessary data during authentication
private const val KEY_CLIENT_ID = "oauth_client_id"
private const val KEY_AUTH_SERVER = "oauth_auth_server"
private const val KEY_STATE_TOKEN = "oauth_state_token"
private const val KEY_CODE_VERIFIER = "oauth_code_verifier"
private const val KEY_DPOP_NONCE = "oauth_dpop_nonce"

//    val isInProcessOfAuthentication = oauthAuthAttemptRepository
//        .getClientId()
//        .map { it != null }
//        .stateIn(
//            scope = accountManagerCoroutineScope,
//            started = SharingStarted.WhileSubscribed(),
//            initialValue = false
//        )
class OAuthAuthenticationAttemptRepository(
    private val dao: OAuthConfigSettingsDao
) {

    private suspend fun saveSetting(key: String, value: String) {
        dao.saveOrUpdate(RoomOAuthConfigSetting(key, value))
    }

    private fun getSettingValue(key: String): Flow<String?> {
        return dao.getByKey(key).map { it?.value }
    }

    suspend fun saveOrUpdateClientId(to: String): Result<Unit, Throwable> = runCatching {
        saveSetting(KEY_CLIENT_ID, to)
    }
    fun getClientId(): Flow<String?> = getSettingValue(KEY_CLIENT_ID)

    suspend fun saveOrUpdateAuthorizationServer(to: String): Result<Unit, Throwable> = runCatching {
        saveSetting(KEY_AUTH_SERVER, to)
    }
    fun getAuthorizationServer(): Flow<String?> = getSettingValue(KEY_AUTH_SERVER)

    suspend fun saveOrUpdateStateToken(to: OAuthStateToken): Result<Unit, Throwable> = runCatching {
        saveSetting(KEY_STATE_TOKEN, to.value)
    }
    fun getStateToken(): Flow<OAuthStateToken?> {
        return getSettingValue(KEY_STATE_TOKEN).map { it?.let { OAuthStateToken(it) } }
    }

    suspend fun saveOrUpdateCodeVerifier(to: OAuthCodeVerifier): Result<Unit, Throwable> = runCatching {
        saveSetting(KEY_CODE_VERIFIER, to.value)
    }
    fun getCodeVerifier(): Flow<OAuthCodeVerifier?> {
        return getSettingValue(KEY_CODE_VERIFIER).map { it?.let { OAuthCodeVerifier(it) } }
    }

    suspend fun saveOrUpdatePushedAuthorizationRequestDpopNonce(to: String): Result<Unit, Throwable> = runCatching {
        saveSetting(KEY_DPOP_NONCE, to)
    }
    fun getPushedAuthorizationRequestDpopNonce(): Flow<String?> = getSettingValue(KEY_DPOP_NONCE)


    suspend fun clearAll(): Result<Unit, Throwable> = runCatching {
        dao.deleteAll()
    }
}

@Database(entities = [RoomOAuthConfigSetting::class], version = 1)
@ConstructedBy(OAuthConfigSettingsDatabaseConstructor::class)
abstract class OAuthConfigSettingsDatabase : RoomDatabase() {
    abstract fun dao(): OAuthConfigSettingsDao
}

@SuppressNoActualForExpect
expect object OAuthConfigSettingsDatabaseConstructor : RoomDatabaseConstructor<OAuthConfigSettingsDatabase> {
    override fun initialize(): OAuthConfigSettingsDatabase
}

@Dao
interface OAuthConfigSettingsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveOrUpdate(setting: RoomOAuthConfigSetting)

    @Query("SELECT * FROM oauth_config_settings WHERE `key` = :key LIMIT 1")
    fun getByKey(key: String): Flow<RoomOAuthConfigSetting?>

    // @Query("SELECT * FROM oauth_config_settings WHERE `key` IN (:keys)")
    // fun getByKeys(keys: List<String>): Flow<List<RoomOAuthConfigSetting>>

    @Query("DELETE FROM oauth_config_settings WHERE `key` = :key")
    suspend fun deleteByKey(key: String)

    @Query("DELETE FROM oauth_config_settings")
    suspend fun deleteAll()
}

@Entity(tableName = "oauth_config_settings")
data class RoomOAuthConfigSetting(
    @PrimaryKey val key: String,
    val value: String
)
