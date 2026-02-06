package com.nxoim.blean.platformCredentialsManagement

import android.accounts.Account
import android.accounts.AccountManager
import android.accounts.OnAccountsUpdateListener
import android.app.Application
import android.os.Build
import android.os.Bundle
import co.touchlab.kermit.Logger
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.getOr
import com.nxoim.blean.bskyPrimitives.AccountIdentificator
import com.nxoim.blean.bskyPrimitives.Did
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap

private const val logTag = "AndroidCredentialsRepo"

class AndroidCredentialsRepository(
    context: Application,
    private val logger: Logger = Logger
) : PlatformCredentialsRepository {
    private val modificationMutex = Mutex()
    private val accountManager = AccountManager.get(context)

    private val _accountsState = MutableStateFlow(
        emptyMap<AccountIdentificator.Did, Pair<Account, PlatformCredentials>>()
    )

    // we retrieve the accounts from the accounts flow, which is
    // only populated by entries that were retrieved successfully.
    // we cant look up specific accounts in account manager,
    // so we look all of them up, and the ones that have our type
    // and failed decoding will end up in this map
    private val accountRetrievalErrors =
        ConcurrentHashMap<AccountIdentificator.Did, PlatformCredentialsRetrievalError>()

    suspend fun start() {
        accountManager.accountsRemovalAdditionEvents(logger)
            .onStart { emit(Unit) }
            .collect {
                modificationMutex.withLock {
                    val external = accountManager
                        .getAccountsByType(ourAccountType)
                        .toCredentialsMap()

                    _accountsState.update { current ->
                        buildMap {
                            putAll(current)

                            external.forEach { (did, pair) ->
                                if (!current.containsKey(did)) {
                                    put(did, pair)
                                }
                            }
                        }
                    }
                }
            }
    }

    override suspend fun saveOrUpdate(
        credentials: PlatformCredentials,
        label: String
    ): Result<Unit, PlatformCredentialsSavingError> = modificationMutex.withLock {
        logger.v(tag = logTag) { "Saving credentials" }

        val encoded = try {
            Json.encodeToString(credentials)
        } catch (e: SerializationException) {
            logger.e(throwable = e, tag = logTag) { "Failed to encode platform credentials" }
            return Err(
                PlatformCredentialsSavingError.EncodingError("Failed to encode platform credentials: \n${e.stackTraceToString()}")
            )
        }

        val androidAccount = _accountsState.value[credentials.did]
            ?.first
            ?: Account(label, ourAccountType).also {
                val added = accountManager.addAccountExplicitly(
                    it,
                    null,
                    Bundle().apply {
                        putString(
                            didStringForLookupDataKey,
                            credentials.did.toString()
                        )
                    }
                )

                if (!added) return Err(
                    PlatformCredentialsSavingError.UnknownError("AccountManager add failed")
                )
            }

        accountManager.setUserData(androidAccount, dataKey, encoded)

        val verified = try {
            Json.decodeFromString<PlatformCredentials>(
                accountManager.getUserData(androidAccount, dataKey)
            )
        } catch (e: Exception) {
            return Err(
                PlatformCredentialsSavingError.UnknownError("Verification failed")
            )
        }

        if (verified != credentials) return Err(
            PlatformCredentialsSavingError.EncodingError("Verification mismatch")
        )

        _accountsState.update {
            it + (credentials.did to (androidAccount to credentials))
        }

        accountRetrievalErrors.remove(credentials.did)

        logger.v(tag = logTag) { "Credentials saved for ${credentials.did}" }
        Ok(Unit)
    }


    override fun get(
        did: AccountIdentificator.Did
    ): Flow<Result<PlatformCredentials, PlatformCredentialsRetrievalError>> =
        _accountsState
            .also { logger.v(tag = logTag) { "Getting credentials" } }
            .map { map ->
                map[did]?.second
                    ?.let { Ok(it) }
                    ?: Err(
                        accountRetrievalErrors[did]
                            ?: PlatformCredentialsRetrievalError.UnknownError(
                                "Credentials not found for $did"
                            )
                    )
            }


    override suspend fun remove(
        did: AccountIdentificator.Did
    ): Result<Unit, PlatformCredentialsRemovalError> = modificationMutex.withLock {
        logger.v(tag = logTag) { "Removing credentials" }
        val account = _accountsState.firstOrNull()
            ?.get(did)
            ?.first
            ?: return@withLock run {
                accountRetrievalErrors.remove(did)
                Ok(Unit)
            }

        val removal = accountManager.removeAccountExplicitly(account)
        logger.d(tag = logTag) {
            "Removal of account was ${if (removal) "successful" else "unsuccessful"}"
        }
        if (removal) {
            accountRetrievalErrors.remove(did)
            Ok(Unit)
        } else
            Err(PlatformCredentialsRemovalError.UnknownError("Failed to remove account from account manager"))
    }

    private fun Account.getCredentialsFromDataEntry(): Result<PlatformCredentials, PlatformCredentialsRetrievalError>? {
        val savedData = accountManager.getUserData(this, dataKey) ?: return null

        return try {
            if (savedData.isEmpty()) {
                Err(PlatformCredentialsRetrievalError.DecodingError("Empty credentials data?"))
            } else {
                Ok(Json.decodeFromString(savedData))
            }
        } catch (e: SerializationException) {
            val error =
                PlatformCredentialsRetrievalError.DecodingError(e.message ?: "Decoding failed")

            accountManager.getUserData(this, didStringForLookupDataKey)
                ?.let { Did.parse(it).getOr(null) }
                ?.let { did -> accountRetrievalErrors[AccountIdentificator.Did(did)] = error }

            Err(error)
        }
    }


    private fun Array<Account>.toCredentialsMap(): Map<AccountIdentificator.Did, Pair<Account, PlatformCredentials>> =
        mapNotNull { account ->
            account.getCredentialsFromDataEntry()
                ?.getOr(null)
                ?.let { it.did to (account to it) }
        }
            .toMap()
}

@OptIn(InternalCoroutinesApi::class)
fun AccountManager.accountsRemovalAdditionEvents(logger: Logger): Flow<Unit> = callbackFlow {
    logger.v(tag = logTag) { "Initializing accounts flow" }

    val listener = OnAccountsUpdateListener { status ->
        logger.v(tag = logTag) { "emitting accounts" }

        trySendBlocking(Unit)
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

private const val ourAccountType = "com.nxoim.blean"
private const val didStringForLookupDataKey = "did"
private const val dataKey = "data"