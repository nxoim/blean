package com.nxoim.blean.platformCredentialsManagement

import android.accounts.Account
import android.accounts.AccountManager
import android.accounts.OnAccountsUpdateListener
import android.content.Context
import android.os.Build
import android.os.Bundle
import co.touchlab.kermit.Logger
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.getOr
import com.github.michaelbull.result.getOrThrow
import com.github.michaelbull.result.map
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import com.nxoim.blean.bskyPrimitives.AccountIdentificator
import com.nxoim.blean.bskyPrimitives.Did
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ChannelResult
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap

private const val logTag = "AndroidCredentialsRepository"

class AndroidCredentialsRepository(
    context: Context,
    private val logger: Logger = Logger,
    private val coroutineScope: CoroutineScope
) : PlatformCredentialsRepository {
    private val modificationMutex = Mutex()
    private val scopeContext = coroutineScope.coroutineContext
    private val accountManager = AccountManager.get(context)

    // we retrieve the accounts from the accounts flow, which is
    // only populated by entries that were retrieved succesfully.
    // we cant look up specific accounts in account manager,
    // so we look all of them up, and the ones that have our type
    // and failed decoding end up in this map
    private val accountRetrievalErrors = ConcurrentHashMap<Did, PlatformCredentialsRetrievalError>()

    // because updates to current data should be reflected as updates and not replacements with null in the middle
    private val refreshTrigger = Channel<Unit>(Channel.CONFLATED)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val accountsFlow = accountManager.accountsRemovalAdditionEvents(logger)
        .onStart { emit(Unit) }
        .flatMapLatest {
            refreshTrigger
                .receiveAsFlow()
                .onStart { emit(Unit) }
                .map {
                    accountManager.accounts
                        .filterByOurType()
                        .toCredentialsMap()
                }
        }
        .stateIn(
            scope = coroutineScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyMap()
        )

    override suspend fun saveOrUpdate(
        credentials: PlatformCredentials,
        label: String
    ): Result<Unit, PlatformCredentialsSavingError> = modificationMutex.withLock {
        withContext(scopeContext) {
            logger.v(tag = logTag) { "Saving credentials" }

            val newStringifiedCredentials = try {
                Json.encodeToString(credentials)
            } catch (e: SerializationException) {
                logger.e(throwable = e, tag = logTag) { "Failed to encode platform credentials" }
                return@withContext Err(
                    PlatformCredentialsSavingError.EncodingError("Failed to encode platform credentials: \n${e.stackTraceToString()}")
                )
            }

            val androidAccount = accountsFlow.value[credentials.did]?.first
                ?: run {
                    val new = Account(label, ourAccountType)
                    val addition = accountManager.addAccountExplicitly(
                        new,
                        null,
                        Bundle().apply {
                            putString(
                                didStringForLookupDataKey,
                                credentials.did.toString()
                            )
                        }
                    )

                    // return early if addAccountExplicitly failed
                    if (!addition) {
                        val details =
                            "Failed to add account to account manager. Error related to android's AccountManager"

                        logger.e(tag = logTag) { details }
                        return@withContext Err(PlatformCredentialsSavingError.UnknownError(details))
                    }
                    return@run new
                }

            accountManager.setUserData(
                androidAccount,
                dataKey,
                newStringifiedCredentials
            )

            // verify by getting the data
            val savedCredentialsString = accountManager.getUserData(androidAccount, dataKey)
            val savedCredentials = try {
                Json.decodeFromString<PlatformCredentials>(savedCredentialsString)
            } catch (e: java.lang.NullPointerException) {
                return@withContext Err(PlatformCredentialsSavingError.UnknownError("Unable to verify that credentials were saved due to credentials decoding resulting in NullPointerException"))
            }
            if (savedCredentials != credentials) {
                val details = "Data mismatch during verification of credentials saving"
                logger.e(tag = logTag) { details }
                return@withContext Err(PlatformCredentialsSavingError.EncodingError(details))
            }

            accountRetrievalErrors.remove(credentials.did.value)

            refreshTrigger.send(Unit)
            Ok(Unit)
        }
    }

    override fun get(did: AccountIdentificator.Did): Flow<Result<PlatformCredentials, PlatformCredentialsRetrievalError>> =
        accountsFlow
            .also { logger.v(tag = logTag) { "Getting credentials" } }
            .map {
                val credentials = it[did]?.second

                credentials
                    ?.let(::Ok)
                    ?: Err(
                        accountRetrievalErrors[did.value]
                            ?: PlatformCredentialsRetrievalError.UnknownError("Failed to get credentials from account manager")
                    )
            }
            .onEach {
                it
                    .onSuccess { logger.v(tag = logTag) { "Credentials received successfully" } }
                    .onFailure { logger.v(tag = logTag) { "Could not receive credentials: $it" } }
            }

    override suspend fun remove(
        did: AccountIdentificator.Did
    ): Result<Unit, PlatformCredentialsRemovalError> = modificationMutex.withLock {
        withContext(scopeContext) {
            logger.v(tag = logTag) { "Removing credentials" }
            val account = accountsFlow.firstOrNull()
                ?.get(did)
                ?.first
                ?: return@withContext run {
                    accountRetrievalErrors.remove(did.value)
                    Ok(Unit)
                }

            val removal = accountManager.removeAccountExplicitly(account)
            logger.d(tag = logTag) {
                "Removal of account was ${if (removal) "successful" else "unsuccessful"}"
            }
            if (removal) {
                accountRetrievalErrors.remove(did.value)
                Ok(Unit)
            } else
                Err(PlatformCredentialsRemovalError.UnknownError("Failed to remove account from account manager"))
        }
    }

    private fun Account.getCredentialsFromDataEntry(): Result<PlatformCredentials, PlatformCredentialsRetrievalError>? =
        try {
            val savedData = accountManager.getUserData(this, dataKey)
                .let { if (it.isNullOrEmpty()) null else it }

            if (savedData.isNullOrEmpty()) {
                Err(PlatformCredentialsRetrievalError.DecodingError("Empty credentials data?"))
            } else {
                savedData?.let { Ok(Json.decodeFromString(it)) }
            }

        } catch (e: SerializationException) {
            logger.e(
                throwable = e,
                tag = logTag
            ) { "Failed to decode credentials from Account data" }
            val error =
                PlatformCredentialsRetrievalError.DecodingError("Failed to decode the credentials \n ${e.stackTraceToString()}")

            val did = accountManager
                .getUserData(this, didStringForLookupDataKey)
                .let { if (it == "") null else it }

            did?.let {
                val did = Did.parse(it).getOrThrow()
                accountRetrievalErrors[did] = error
            }
            logger.w(tag = logTag) { "Registered decoding error" }
            Err(error)
        }

    private fun Array<out Account>.toCredentialsMap(): Map<AccountIdentificator.Did, Pair<Account, PlatformCredentials>> =
        this
            .mapNotNull { account ->
                val credentialsStoredData = account
                    .getCredentialsFromDataEntry()
                    ?: return@mapNotNull null

                logger.v(tag = logTag) { "Mapping our android accounts to credentials" }
                return@mapNotNull credentialsStoredData
                    .map { it.did to (account to it) }
                    .getOr(null)
            }
            .toMap()
}

// DOES NOT report changes in the account user data
@OptIn(InternalCoroutinesApi::class)
context(logger: Logger)
fun AccountManager.accountsFlow(): Flow<Array<out Account>?> = callbackFlow {
    logger.v(tag = logTag) { "Initializing accounts flow" }

    val listener = OnAccountsUpdateListener { status ->
        logger.v(tag = logTag) { "emmitting accounts" }

        trySendBlocking(status)
        ChannelResult.success(Unit)
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        addOnAccountsUpdatedListener(
            /* listener = */ listener,
            /* handler = */ null,
            /* updateImmediately = */ true,
            /* accountTypes = */ arrayOf(ourAccountType)
        )
    } else {
        addOnAccountsUpdatedListener(
            /* listener = */ listener,
            /* handler = */ null,
            /* updateImmediately = */ true
        )
    }

    awaitClose {
        logger.v(tag = logTag) { "Closing accounts flow, removing listener" }
        removeOnAccountsUpdatedListener(listener)
    }
}

@OptIn(InternalCoroutinesApi::class)
fun AccountManager.accountsRemovalAdditionEvents(logger: Logger): Flow<Unit> = callbackFlow {
    logger.v(tag = logTag) { "Initializing accounts flow" }

    val listener = OnAccountsUpdateListener { status ->
        logger.v(tag = logTag) { "emmitting accounts" }

        trySendBlocking(Unit)
        ChannelResult.success(Unit)
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        addOnAccountsUpdatedListener(
            /* listener = */ listener,
            /* handler = */ null,
            /* updateImmediately = */ true,
            /* accountTypes = */ arrayOf(ourAccountType)
        )
    } else {
        addOnAccountsUpdatedListener(
            /* listener = */ listener,
            /* handler = */ null,
            /* updateImmediately = */ true
        )
    }

    awaitClose {
        logger.v(tag = logTag) { "Closing accounts flow, removing listener" }
        removeOnAccountsUpdatedListener(listener)
    }
}

private fun Array<out Account>.filterByOurType() =
    filter { it.type.contentEquals(ourAccountType) }.toTypedArray()

private const val ourAccountType = "com.nxoim.blean"
private const val didStringForLookupDataKey = "did"
private const val dataKey = "data"