package com.nxoim.blean.client

import co.touchlab.kermit.Logger

class UserRepositories(
    val userData: UserDataRepositories,
    val contentRepository: ContentRepositories
)  {
    constructor(
        rootDataPathForUser: String,
        rootCachePathForUser: String,
        encryptionKey: ByteArray?,
        logger: Logger
    ) : this(
        UserDataRepositories("$rootDataPathForUser/user", encryptionKey),
        ContentRepositories("$rootCachePathForUser/content", encryptionKey, logger)
    )

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
