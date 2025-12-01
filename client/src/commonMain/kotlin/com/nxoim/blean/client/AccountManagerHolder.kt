@file:OptIn(ExperimentalTime::class)

package com.nxoim.blean.client

import co.touchlab.kermit.Logger
import co.touchlab.stately.concurrency.AtomicReference
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.getOr
import com.github.michaelbull.result.getOrElse
import com.github.michaelbull.result.getOrThrow
import com.github.michaelbull.result.map
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import com.nxoim.blean.api.BleanApi
import com.nxoim.blean.api.BleanKtorClient
import com.nxoim.blean.api.api.OAuthApi
import com.nxoim.blean.api.api.oauthStuff.utils.PDSRequestDPoPAuthenticationContext
import com.nxoim.blean.api.createClient
import com.nxoim.blean.api.utils.AuthenticationContext
import com.nxoim.blean.api.utils.AuthenticationMethod
import com.nxoim.blean.bskyPrimitives.AccountIdentificator
import com.nxoim.blean.commonThingsDumpster.cancelChildren
import com.nxoim.blean.commonThingsDumpster.childCoroutineScope
import com.nxoim.blean.models.LoggedInUserBasicDetails
import com.nxoim.blean.platformCredentialsManagement.OAuthCredentials
import com.nxoim.blean.platformCredentialsManagement.PlatformCredentials
import com.nxoim.blean.platformCredentialsManagement.PlatformCredentialsRepository
import com.nxoim.blean.platformCredentialsManagement.PlatformCredentialsRetrievalError
import com.nxoim.blean.repos.LoggedInUsersRepository
import com.nxoim.blean.repos.LoggedInUsersRoomDatabase
import com.nxoim.blean.repos.OAuthAuthenticationAttemptRepository
import com.nxoim.blean.repos.OAuthConfigSettingsDatabase
import com.nxoim.blean.repos.buildRoomDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.jvm.JvmInline
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

private const val logTag = "AccountManager"

@OptIn(ExperimentalCoroutinesApi::class)
class AccountManagerHolder(
    private val rootDataStorageUri: String,
    private val rootCacheStorageUri: String,
    private val logger: Logger,
    private val encryptionKeyFactory: () -> ByteArray?,
    private val credentialsRepositoryFactory: () -> PlatformCredentialsRepository,
    private val coroutineScope: CoroutineScope,
    private val baseBskyServerEndpoint: String,
    private val baseOauthEndpoint: String
) {
    private val clientId = "$baseOauthEndpoint/client-metadata.json"
    private val _state = MutableStateFlow<AccountManagerState>(AccountManagerState.Idle)
    val state = _state.asStateFlow()

    private val mutex = Mutex()

    suspend fun initializeIfNotInitialized() {
        if (_state.value is AccountManagerState.Initialized) return

        mutex.withLock {
            if (_state.value is AccountManagerState.Initialized) return

            _state.value = AccountManagerState.Initializing

            val result = runCatching {
                val httpClient = createClient(baseServerUrl = baseBskyServerEndpoint)
                val api = BleanApi(httpClient)
                val rootUserRepository = LoggedInUsersRepository(
                    userDao = buildRoomDatabase<LoggedInUsersRoomDatabase>(
                        rootDataStorageUri, "users", encryptionKeyFactory()
                    ).dao(),
                    platformCredentialsRepository = credentialsRepositoryFactory()
                )

                withContext(coroutineScope.coroutineContext) {
                    val session = AccountManager(
                        rootDataStorageUri = "$rootDataStorageUri/data",
                        rootCacheStorageUri = "$rootCacheStorageUri/cache",
                        encryptionKey = encryptionKeyFactory(),
                        logger = logger,
                        scope = coroutineScope.childCoroutineScope(),
                        bleanApi = api,
                        rootUserRepository = rootUserRepository,
                        authenticationManager = AuthenticationManager(
                            rootUserRepository = rootUserRepository,
                            oauthAuthAttemptRepository = OAuthAuthenticationAttemptRepository(
                                buildRoomDatabase<OAuthConfigSettingsDatabase>(
                                    rootDataStorageUri, "oauthAttempt", encryptionKeyFactory()
                                ).dao()
                            ),
                            atprotoOAuthClient = ATProtoOAuthClient(
                                oauthApi = OAuthApi(httpClient),
                                accountApi = api.account,
                                // Assuming clientId is a constant or available in context
                                clientId = clientId
                            ),
                            logger = logger,
                        ),
                        httpClient = httpClient
                    )

                    session.loadInitialAccounts()

                    session
                }
            }

            _state.value = result.fold(
                onSuccess = { AccountManagerState.Initialized(it) },
                onFailure = { AccountManagerState.Error(it) }
            )
        }
    }

    suspend fun deinitialize() {
        mutex.withLock {
            val currentState = _state.value
            if (currentState is AccountManagerState.Initialized) {
                _state.value = AccountManagerState.Idle
                coroutineScope.cancelChildren()
            }
        }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class AccountManager(
    private val rootDataStorageUri: String,
    private val rootCacheStorageUri: String,
    private val encryptionKey: ByteArray?,
    private val logger: Logger,
    private val scope: CoroutineScope,
    private val bleanApi: BleanApi,
    private val rootUserRepository: LoggedInUsersRepository,
    private val authenticationManager: AuthenticationManager,
    private val httpClient: BleanKtorClient
) {
    private val stateUpdateMutex = Mutex()
    private val accounts = mutableMapOf<AccountIdentificator.Did, AccountRecord>()
    private val _accountsState = MutableStateFlow<AccountsState>(AccountsState.Loading)
    val accountsState = _accountsState.asStateFlow()

    internal suspend fun loadInitialAccounts() {
        val users = rootUserRepository.getAllLoggedInUsers().first()

        for (user in users) {
            val recordResult = createAccountRecord(user.did)

            recordResult
                .onSuccess { record ->
                    accounts[user.did] = record
                    initializeAccount(record) // decide soft logout vs init
                }
                .onFailure {
                    val fallbackRecord = createAccountRecordWithFallback(user.did)
                    accounts[user.did] = fallbackRecord
                    initializeAccount(fallbackRecord)
                }
        }

        updatePublicState()
    }

    private suspend fun createAccountRecord(
        did: AccountIdentificator.Did
    ): Result<AccountRecord, MissingUserError> {
        val initial = rootUserRepository.getLoggedInUser(did).first()
            ?: return Err(MissingUserError(did))

        val scope = scope.childCoroutineScope()

        val basicDetailsFlow = rootUserRepository.getLoggedInUser(did)
            .map { it }
            .filterNotNull()
            .stateIn(scope, WhileSubscribed(), initial)

        return Ok(AccountRecord(did, AccountInstance(basicDetailsFlow, logger), scope))
    }

    private fun createAccountRecordWithFallback(
        did: AccountIdentificator.Did
    ): AccountRecord {
        val fallbackDetails = LoggedInUserBasicDetails(
            did = did,
            handle = AccountIdentificator.Handle("@unknown.handle"),
            displayName = null,
            avatarUrl = null
        )

        val newScope = scope.childCoroutineScope()
        val flow = MutableStateFlow(fallbackDetails)

        return AccountRecord(
            did = did,
            instance = AccountInstance(flow, logger),
            scope = newScope
        )
    }

    suspend fun beginOauthAuthorization(
        entry: String
    ): Result<String, ATProtoOAuthClientError> =
        withContext(scope.coroutineContext) {
            authenticationManager.beginOauthAuthorization(entry)
        }

    suspend fun continueOauthAuthorization(
        callbackUrl: String
    ): Result<LoggedInUserBasicDetails, ContinueOAuthAuthorizationError> =
        withContext(scope.coroutineContext) {
            authenticationManager
                .continueOauthAuthorization(callbackUrl)
                .onSuccess { details ->
                    stateUpdateMutex.withLock {
                        val did = details.did
                        val record = accounts[did]
                            ?: createAccountRecordWithFallback(did).also {
                                accounts[did] = it
                            }

                        accounts[did] = record
                        updatePublicState()
                        initializeAccount(record)
                        updatePublicState()

                        logger.i(tag = logTag) {
                            "Initialization after login successful"
                        }
                    }
                }
        }

    private suspend fun buildAuthenticationContextFlow(
        credentialsFlow: StateFlow<Result<PlatformCredentials, PlatformCredentialsRetrievalError>?>,
        record: AccountRecord
    ): Flow<AuthenticationContext?> {
        val pdsContext by lazy {
            PDSRequestDPoPAuthenticationContext(
                onCurrentTimeEpochSeconds = { Clock.System.now().epochSeconds },

                beforeRequestHappens = {
                    record.refreshJob.get()?.await()
                },

                onInvalidAuthToken = {
                    val active = record.refreshJob.get()
                    if (active != null && active.isActive) return@PDSRequestDPoPAuthenticationContext active

                    val newJob = record.scope.async {
                        logger.i(tag = logTag) {
                            "Token is now invalid. WIll refresh now"
                        }

                        record.refreshMutex.withLock {
                            refreshTokensAndHandleErrors(record.did)
                        }
                    }
                    record.refreshJob.set(newJob)
                    newJob
                },

                onRequestAuthMethod = {
                    credentialsFlow.value
                        ?.map {
                            when (it) {
                                is PlatformCredentials.AccessJwt ->
                                    error("Unexpected AccessJwt requested")

                                is PlatformCredentials.OAuth ->
                                    AuthenticationMethod.OAuth(
                                        it.value.accessToken,
                                        it.value.keyPair
                                    )
                            }
                        }
                        ?.getOr(null)
                        ?: error("Credentials missing")
                },

                logger = logger
            )
        }

        return credentialsFlow.mapNotNull { result ->
            result
                ?.onFailure {
                    record.scope.launch { softLogout(record.did) }
                }
                ?.map {
                    when (it) {
                        is PlatformCredentials.AccessJwt ->
                            AuthenticationContext.AccessJwt(it.value, it.pdsUrl)

                        is PlatformCredentials.OAuth ->
                            AuthenticationContext.OAuthContextForPDS(pdsContext)
                    }
                }
                ?.getOrElse {
                    logger.w(tag = logTag) {
                        "Cant create authentication context because $it."
                    }
                    null
                }
        }
    }

    private suspend fun initializeAccount(record: AccountRecord) {
        val credentials = rootUserRepository.getCredentials(record.did).first()

        if (credentials == null || credentials.isErr) {
            logger.w(tag = logTag) {
                "Missing credentials for ${record.did} → soft logout"
            }
            record.instance.markAsNonInitializable()
            return
        }

        val credentialsFlow = rootUserRepository
            .getCredentials(record.did)
            .stateIn(record.scope)

        val flow = buildAuthenticationContextFlow(credentialsFlow, record)
            .stateIn(record.scope)

        record.instance.initialize(
            context = flow,
            bleanApi = bleanApi,
            rootDataPathForUser = "$rootDataStorageUri/${record.did}",
            rootCachePathForUser = "$rootCacheStorageUri/${record.did}",
            encryptionKey = encryptionKey,
            instanceCoroutineScope = record.scope,
            shouldStartActionProcessing = true
        ).onFailure {
            logger.e(tag = logTag, throwable = it) {
                "Initialization failure for ${record.did}"
            }
        }
    }

    private suspend fun refreshTokensAndHandleErrors(
        did: AccountIdentificator.Did
    ): Result<OAuthCredentials, RefreshTokenOAuthError> =
        authenticationManager.refreshOauthClientTokens(did)
            .onSuccess { new ->
                // should we also update label on each basic data update
                val basicUserDetails = rootUserRepository.getLoggedInUser(did).firstOrNull()

                rootUserRepository.saveOrUpdateOAuthCredentials(
                    did = did,
                    accessToken = new.accessToken,
                    refreshToken = new.refreshToken,
                    authorizationServerUrl = new.authorizationServerUrl,
                    clientId = new.clientId,
                    keyPair = new.keyPair,
                    label = basicUserDetails?.displayName
                        ?: basicUserDetails?.handle?.toString()
                        ?: did.toString()
                )
            }
            .onFailure { err ->
                suspend fun logoutAndNotify() {
                    logger.w(tag = logTag) {
                        "Soft logout triggered by refresh error: $err"
                    }
                    softLogoutInternal(did)
                }

                when (err) {
                    RefreshTokenOAuthError.CantObtainAssembledCredentialsFromRepo -> logoutAndNotify()
                    RefreshTokenOAuthError.NecessaryDataMissing -> logoutAndNotify()
                    is RefreshTokenOAuthError.RefreshError -> when (val refreshError = err.value) {
                        is ATProtoOAuthClientError.ConnectionIssues -> {
                            logger.w(tag = logTag) {
                                "Unable to refresh because of error $err. Will not soft log out"
                            }
                        }

                        is ATProtoOAuthClientError.Internal -> logoutAndNotify()
                        is ATProtoOAuthClientError.Other -> logoutAndNotify()
                        is ATProtoOAuthClientError.ServerError -> {
                            if (refreshError.code in 400..499) logoutAndNotify()
                        }

                        is ATProtoOAuthClientError.UnrecoverableTokenError -> logoutAndNotify()
                        is ATProtoOAuthClientError.ValidationError -> logoutAndNotify()
                    }

                    is RefreshTokenOAuthError.Unknown -> logoutAndNotify()
                }
            }

    suspend fun softLogout(did: AccountIdentificator.Did) {
        withContext(scope.coroutineContext) {
            softLogoutInternal(did)
        }
    }

    private suspend fun softLogoutInternal(did: AccountIdentificator.Did) {
        stateUpdateMutex.withLock {
            val record = accounts[did] ?: return
            record.instance.markAsNonInitializable()
            rootUserRepository.removeOAuthCredentials(did)
            updatePublicState()
        }
    }

    suspend fun logout(did: AccountIdentificator.Did) {
        withContext(scope.coroutineContext) {
            stateUpdateMutex.withLock {
                accounts.remove(did)?.let { record ->
                    record.instance.markAsNonInitializableAndNuke()
                    record.scope.cancel()
                    rootUserRepository.removeLoggedInUserAndCredentials(did)
                }
                updatePublicState()
            }
        }
    }

    suspend fun deinitializeFully() {
        scope.coroutineContext[Job]?.cancelAndJoin()

        stateUpdateMutex.withLock {
            accounts.values.forEach { record ->
                record.instance.markAsLoadingAndDeinitialize().getOrThrow()
            }
            accounts.clear()
            _accountsState.value = AccountsState.Loading
        }
        httpClient.close()
    }

    private val accountsMappedToPublic = MutableStateFlow(accounts.mapToPublic())
    private suspend fun updatePublicState() {
        accountsMappedToPublic.value = accounts.mapToPublic()
        _accountsState.value = AccountsState.Initialized(accountsMappedToPublic)
    }

    private fun Map<AccountIdentificator.Did, AccountRecord>.mapToPublic() =
        mapValues { it.value.instance.account }
            .toMap()
}

private data class AccountRecord(
    val did: AccountIdentificator.Did,
    val instance: AccountInstance,
    val scope: CoroutineScope,
    val refreshMutex: Mutex = Mutex(),
    val refreshJob: AtomicReference<Deferred<Result<*, *>>?> =
        AtomicReference(null)
)

@OptIn(ExperimentalCoroutinesApi::class)
sealed interface AccountManagerState {
    data object Idle : AccountManagerState
    data object Initializing : AccountManagerState
    data class Error(val cause: Throwable) : AccountManagerState
    class Initialized(val manager: AccountManager) : AccountManagerState
}

/**
 * Errors can include iat/exp(in the token) errors. This indicates potentially
 * incorrect device time settings.
 */
sealed interface ContinueOAuthAuthorizationError {
    data object DataWasMissing : ContinueOAuthAuthorizationError

    @JvmInline
    value class RequestError(val details: ATProtoOAuthClientError) : ContinueOAuthAuthorizationError

    @JvmInline
    value class Internal(val details: String) : ContinueOAuthAuthorizationError
}

sealed interface RefreshTokenOAuthError {
    data object NecessaryDataMissing : RefreshTokenOAuthError

    @JvmInline
    value class RefreshError(val value: ATProtoOAuthClientError) : RefreshTokenOAuthError

    /**
     * Happens when data was in repo but there was an error retrieving it
     */
    data object CantObtainAssembledCredentialsFromRepo : RefreshTokenOAuthError

    data class Unknown(val message: String) : RefreshTokenOAuthError
}

sealed interface AccountsState {
    data object Loading : AccountsState
    data class Initialized(val accounts: StateFlow<Map<AccountIdentificator.Did, Account>>) :
        AccountsState

    data class InitializationError(val message: String) : AccountsState
}

data class MissingUserError(val did: AccountIdentificator.Did)