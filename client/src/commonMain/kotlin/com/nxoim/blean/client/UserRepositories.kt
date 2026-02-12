package com.nxoim.blean.client

import co.touchlab.kermit.Logger
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.SYSTEM

// TODO safe abstraction of file management starting here
class UserRepositories(
    val userData: UserDataRepositories,
    val contentRepository: ContentRepositories,
    private val rootDataUserPath: String,
    private val rootCacheUserPath: String,
    private val mediaFileSystem: FileSystem
)  {
    constructor(
        rootDataPathForUser: String,
        rootCachePathForUser: String,
        encryptionKey: ByteArray?,
        logger: Logger,
        mediaFileSystem: FileSystem = FileSystem.SYSTEM
    ) : this(
        userData = UserDataRepositories(
            baseContentSpecificDbUri = "$rootDataPathForUser/user",
            encryptionKey = encryptionKey,
            mediaFileSystem = mediaFileSystem
        ),
        contentRepository = ContentRepositories(
            baseContentSpecificDbUri = "$rootCachePathForUser/content",
            encryptionKey = encryptionKey,
            logger = logger,
            mediaFileSystem = mediaFileSystem
        ),
        rootDataUserPath = rootDataPathForUser,
        rootCacheUserPath = rootCachePathForUser,
        mediaFileSystem = mediaFileSystem
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
        deinitialize()
        mediaFileSystem.deleteRecursively(rootDataUserPath.toPath())
        mediaFileSystem.deleteRecursively(rootCacheUserPath.toPath())
    }
}
