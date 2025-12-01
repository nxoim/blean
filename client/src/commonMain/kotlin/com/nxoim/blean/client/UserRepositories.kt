package com.nxoim.blean.client

import co.touchlab.kermit.Logger

class UserRepositories(
    private val rootDataPathForUser: String,
    private val rootCachePathForUser: String,
    private val encryptionKey: ByteArray?,
    private val logger: Logger
) {
    val userData = UserDataRepositories("$rootDataPathForUser/user", encryptionKey)
    val contentRepository =
        ContentRepositories("$rootCachePathForUser/content", encryptionKey, logger)

    suspend fun initialize() {
        userData.initialize()
        contentRepository.initialize()
    }

    suspend fun deinitialize() {
        userData.deinitialize()
        contentRepository.deinitialize()
    }

    suspend fun deinitializeAndNuke() {
        userData.deinitializeAndNuke()
        contentRepository.deinitializeAndNuke()
    }
}
