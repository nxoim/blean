package com.nxoim.blean.platformCredentialsManagement

import com.github.michaelbull.result.Result
import com.nxoim.blean.bskyPrimitives.AccountIdentificator
import kotlinx.coroutines.flow.Flow

interface PlatformCredentialsRepository {
    suspend fun saveOrUpdate(credentials: PlatformCredentials, label: String): Result<Unit, PlatformCredentialsSavingError>

    fun get(did: AccountIdentificator.Did): Flow<Result<PlatformCredentials, PlatformCredentialsRetrievalError>?>

    /**
     * If credentials did not exist during removal - it is treated as success
     */
    suspend fun remove(did: AccountIdentificator.Did): Result<Unit, PlatformCredentialsRemovalError>
}
