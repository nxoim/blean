@file:OptIn(ExperimentalTime::class)

package com.nxoim.blean.client

import co.touchlab.kermit.Logger
import co.touchlab.stately.concurrency.AtomicReference
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.andThen
import com.github.michaelbull.result.get
import com.github.michaelbull.result.getOrElse
import com.github.michaelbull.result.map
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import com.nxoim.blean.api.BleanApi
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
import kotlinx.coroutines.awaitAll
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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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

                val session = AccountManager(
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
                            clientId = clientId
                        ),
                        logger = logger,
                    ),
                    onDestroy = {
                        httpClient.close()
                    },
                    userRepositoriesFactory = {
                        UserRepositories(
                            rootDataPathForUser = "$rootDataStorageUri/data/$it",
                            rootCachePathForUser = "$rootCacheStorageUri/cache/$it",
                            encryptionKey = encryptionKeyFactory(),
                            logger = logger
                        )
                    }
                )

                session.loadInitialAccounts()

                session
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

class AccountManager(
    private val logger: Logger,
    private val scope: CoroutineScope,
    private val bleanApi: BleanApi,
    private val rootUserRepository: LoggedInUsersRepository,
    private val authenticationManager: AuthenticationManager,
    private val onDestroy: suspend () -> Unit = { },
    private val userRepositoriesFactory: (AccountIdentificator.Did) -> UserRepositories
) {
    private val stateUpdateMutex = Mutex()
    private val _records = MutableStateFlow(
        emptyMap<AccountIdentificator.Did, AccountRecord>()
    )
    private val _accounts = MutableStateFlow(
        emptyMap<AccountIdentificator.Did, Account>()
    )
    private val _accountsState = MutableStateFlow<AccountsState>(
        AccountsState.Loading
    )
    val accountsState = _accountsState.asStateFlow()

    internal suspend fun loadInitialAccounts() {
        val users = rootUserRepository.getAllLoggedInUsers().first()

        val initialRecords = users.associateBy(
            keySelector = { it.did },
            valueTransform = { createAccountRecord(initialDetails = it) }
        )

        stateUpdateMutex.withLock {
            _records.value = initialRecords
            _accounts.value = initialRecords.asAccounts
            _accountsState.value = AccountsState.Initialized(_accounts.asStateFlow())
        }

        initialRecords.values
            .map { record ->
                record.scope.async {
                    record.initialize().onFailure {
                        logger.w(tag = logTag) {
                            "Initialization skipped for ${record.did}: $it"
                        }
                    }
                }
            }
            .awaitAll()
    }

    suspend fun beginOauthAuthorization(
        entry: String
    ): Result<String, ATProtoOAuthClientError> =
        authenticationManager.beginOauthAuthorization(entry)

    suspend fun continueOauthAuthorization(
        callbackUrl: String
    ): Result<LoggedInUserBasicDetails, ContinueOAuthAuthorizationError> =
        authenticationManager
            .continueOauthAuthorization(callbackUrl)
            .andThen { details ->
                val record = stateUpdateMutex.withLock {
                    val existing = _records.value[details.did]
                    val resolved = existing ?: createAccountRecord(details)

                    _records.update { it + (details.did to resolved) }
                    _accounts.value = _records.value.asAccounts
                    resolved
                }

                record.initialize()
                    .map { details }
                    .mapError {
                        ContinueOAuthAuthorizationError.Internal(
                            "Post-login initialization failed:\n$it"
                        )
                    }
            }

    internal suspend fun forceRefreshOrAwait(did: AccountIdentificator.Did): Result<*, Throwable> {
        return _records.value[did]
            ?.forceRefreshToken()
            ?.mapError { IllegalStateException(it.toString()) }
            ?: Err(IllegalStateException("Unable to force refresh tokens of account that is not recorded in the client"))
    }

    suspend fun softLogout(did: AccountIdentificator.Did): Result<*, Throwable> =
        stateUpdateMutex.withLock {
            _records.value[did]?.softLogout()
                ?.onFailure {
                    logger.e(tag = logTag) {
                        "Failed to soft log out:\n${it.stackTraceToString()}"
                    }
                }
                ?: Err(IllegalStateException("Unable to soft log out of account that is not recorded in the client"))
        }

    suspend fun logout(did: AccountIdentificator.Did): Result<*, Throwable> =
        stateUpdateMutex.withLock {
            _records.value[did]?.logout()
                ?.onSuccess {
                    _records.update { it - did }
                    _accounts.value = _records.value.asAccounts
                }
                ?.onFailure {
                    logger.e(tag = logTag) {
                        "Failed to log out:\n${it.stackTraceToString()}"
                    }
                }
                ?: Err(IllegalStateException("Unable to log out of account that is not recorded in the client"))
        }


    suspend fun deinitializeFully(): Result<*, Throwable> {
        scope.coroutineContext[Job]?.cancelAndJoin()

        stateUpdateMutex.withLock {
            _records.value.values.forEach { record ->
                record.deinitialize().onFailure {
                    return Err(IllegalStateException("An error occurred while fully deinitializing AccountManager:\n${it.stackTraceToString()}"))
                }
            }
            _records.value = emptyMap()
            _accounts.value = emptyMap()
            _accountsState.value = AccountsState.Loading
            onDestroy()
        }

        return Ok(Unit)
    }

    private fun createAccountRecord(
        initialDetails: LoggedInUserBasicDetails
    ): AccountRecord {
        val scope = scope.childCoroutineScope()

        val basicDetailsFlow = rootUserRepository.getLoggedInUser(initialDetails.did)
            .filterNotNull()
            .stateIn(scope, WhileSubscribed(), initialDetails)

        return AccountRecord(
            did = initialDetails.did,
            instance = AccountInstance(basicDetailsFlow, logger),
            scope = scope,
            userRepositoriesFactory = { userRepositoriesFactory(initialDetails.did) }
        )
    }

    private val Map<AccountIdentificator.Did, AccountRecord>.asAccounts
        get() = mapValues { it.value.instance.account }

    ////////////////////////////////////////////////////////////////////////////////////////

    private inner class AccountRecord(
        val did: AccountIdentificator.Did,
        val instance: AccountInstance,
        val scope: CoroutineScope,
        private val userRepositoriesFactory: () -> UserRepositories
    ) {
        val refreshMutex = Mutex()
        val refreshJob = AtomicReference<Deferred<Result<*, *>>?>(null)

        suspend fun initialize(): Result<Unit, InitializationError> {
            val initialCredentials = rootUserRepository.getCredentials(did)
                .firstOrNull()
                ?: run {
                    instance.markAsNonInitializable()
                    return Err(InitializationError.MissingCredentials)
                }

            initialCredentials.onFailure {
                instance.markAsNonInitializable()
                return Err(InitializationError.CredentialsRetrieval(IllegalStateException(it.toString())))
            }

            val credentialsFlow = rootUserRepository
                .getCredentials(did)
                .stateIn(scope)

            val authContextFlow = buildAuthenticationContextFlow(credentialsFlow)
                .stateIn(scope)

            return instance.initialize(
                context = authContextFlow,
                bleanApi = bleanApi,
                userRepositoriesFactory = userRepositoriesFactory,
                instanceCoroutineScope = scope,
                shouldStartActionProcessing = true,
            )
                .mapError { InitializationError.InstanceInitialization(it) }
        }

        private suspend fun buildAuthenticationContextFlow(
            credentialsFlow: StateFlow<Result<PlatformCredentials, PlatformCredentialsRetrievalError>?>
        ): Flow<AuthenticationContext?> {
            val pdsContext by lazy {
                PDSRequestDPoPAuthenticationContext(
                    onCurrentTimeEpochSeconds = { Clock.System.now().epochSeconds },

                    beforeRequestHappens = { refreshJob.get()?.await() },

                    onInvalidAuthToken = {
                        val active = refreshJob.get()
                        if (active != null && active.isActive)
                            return@PDSRequestDPoPAuthenticationContext active

                        val newJob = scope.async {
                            logger.i(tag = logTag) {
                                "Token is now invalid. Will refresh now for $did"
                            }
                            refreshMutex.withLock {
                                refreshTokensAndHandleErrors()
                            }
                        }
                        refreshJob.set(newJob)
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
                            ?.get()
                            ?: error("Credentials missing")
                    },

                    logger = logger
                )
            }

            return credentialsFlow.map { result ->
                result
                    ?.onFailure { scope.launch { softLogout() } }
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
                            "Cannot create auth context: $it"
                        }
                        null
                    }
            }
        }

        private suspend fun refreshTokensAndHandleErrors(
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
                    val shouldLogout = when (err) {
                        RefreshTokenOAuthError.CantObtainAssembledCredentialsFromRepo,
                        RefreshTokenOAuthError.NecessaryDataMissing,
                        is RefreshTokenOAuthError.Unknown -> true

                        is RefreshTokenOAuthError.RefreshError -> when (val e = err.value) {
                            is ATProtoOAuthClientError.ConnectionIssues -> false
                            is ATProtoOAuthClientError.ServerError -> e.code in 400..499
                            else -> true
                        }
                    }

                    if (shouldLogout) {
                        logger.w(tag = logTag) {
                            "Soft logout due to refresh error: $err"
                        }
                        softLogout()
                    }
                }

        suspend fun forceRefreshToken(): Result<*, *> {
            val existingJob = refreshJob.get()
            if (existingJob != null && existingJob.isActive) {
                logger.i(tag = logTag) {
                    "Refresh already in progress for $did, waiting for it to complete"
                }
                return existingJob.await().map { Unit }
            }

            val newJob = scope.async {
                logger.i(tag = logTag) {
                    "Force refreshing tokens for $did"
                }
                refreshMutex.withLock {
                    refreshTokensAndHandleErrors().map { Unit }
                }
            }

            refreshJob.set(newJob)
            return newJob.await()
        }

        suspend fun softLogout(): Result<*, Throwable> =
            rootUserRepository
                .removeOAuthCredentials(did)
                .onFailure {
                    logger.e(tag = logTag) { "Failed to remove credentials on soft logout. $it" }
                }
                .andThen { instance.markAsNonInitializable() }
                .mapError { IllegalStateException("Failed to soft logout. $it") }

        suspend fun logout(): Result<*, Throwable> = rootUserRepository
            .removeLoggedInUserAndCredentials(did)
            .andThen {
                instance.markAsNonInitializableAndNuke()
                    .also { scope.cancel() }
            }
            .mapError { IllegalStateException("Failed to log out. $it") }

        suspend fun deinitialize(): Result<*, Throwable> =
            instance.markAsLoadingAndDeinitialize()
    }
}

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

private sealed interface InitializationError {
    data object MissingCredentials : InitializationError
    data class CredentialsRetrieval(val cause: Throwable) : InitializationError
    data class InstanceInitialization(val cause: Throwable) : InitializationError
}
