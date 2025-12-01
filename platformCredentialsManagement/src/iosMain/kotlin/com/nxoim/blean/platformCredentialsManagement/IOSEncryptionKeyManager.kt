package com.nxoim.blean.platformCredentialsManagement

import co.touchlab.kermit.Logger
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.runCatching

interface IOSEncryptionKeyManager {
    fun getOrCreate(
        key: String,
        keyType: KeyType,
        onSuccess: (ByteArray) -> Unit,
        onFailure: (KeychainError) -> Unit
    )

    fun replace(
        key: String,
        keyType: KeyType,
        onSuccess: (ByteArray) -> Unit,
        onFailure: (KeychainError) -> Unit
    )

    fun delete(
        key: String,
        onSuccess: () -> Unit,
        onFailure: (KeychainError) -> Unit
    )
}

fun IOSEncryptionKeyManager.getOrCreate(
    key: String,
    keyType: KeyType
) = with(Logger) {
    getOrCreate(key, keyType)
}

context(logger: Logger)
fun IOSEncryptionKeyManager.getOrCreate(
    key: String,
    keyType: KeyType
): Result<ByteArray, Throwable> = runCatching {
    var data: ByteArray? = null

    this.getOrCreate(
        key,
        keyType,
        onSuccess = {
            logger.v(tag = keychainimplTag) { "Created or received encryption key type $keyType" }
            data = it
        },
        onFailure = {
            logger.e(tag = keychainimplTag) { "Failed to create or receive encryption key. Error: $it" }
            error(it.toString())
        }
    )

    return@runCatching data!!
}

fun IOSEncryptionKeyManager.replace(
    key: String,
    keyType: KeyType
) = with(Logger) {
    replace(key, keyType)
}

context(logger: Logger)
fun IOSEncryptionKeyManager.replace(
    key: String,
    keyType: KeyType
): Result<ByteArray, Throwable> = runCatching {
    var data: ByteArray? = null

    this.replace(
        key,
        keyType,
        onSuccess = {
            logger.v(tag = keychainimplTag) { "Replaced encryption key type $keyType" }
            data = it
        },
        onFailure = {
            logger.e(tag = keychainimplTag) { "Failed to replace encryption key. Error: $it" }
            error(it.toString())
        }
    )

    return@runCatching data!!
}

fun IOSEncryptionKeyManager.delete(key: String) = with(Logger) {
    delete(key)
}

context(logger: Logger)
fun IOSEncryptionKeyManager.delete(key: String): Result<Unit, Throwable> = runCatching {
    this.delete(
        key,
        onSuccess = {
            logger.v(tag = keychainimplTag) { "Deleted encryption key" }
        },
        onFailure = {
            logger.e(tag = keychainimplTag) { "Failed to delete encryption key. Error: $it" }
            error(it.toString())
        }
    )
}


sealed interface KeychainError {
    data object DuplicateEntry : KeychainError
    data object NoEntry : KeychainError
    data class UnexpectedStatus(val status: Int) : KeychainError
    data object Unknown : KeychainError
    data object ConversionError : KeychainError
    data class PlatformError(val message: String?) : KeychainError
}

sealed interface KeyType {
    data class RandomBytes(val length: Int) : KeyType
}

private const val keychainimplTag = "Keychain Impl"