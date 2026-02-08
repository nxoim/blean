package com.nxoim.blean.client

import co.touchlab.kermit.Logger
import com.nxoim.blean.repos.ApiResponseCache
import com.nxoim.blean.repos.ApiResponseRoomDatabase
import com.nxoim.blean.repos.buildRoomDatabase
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.SYSTEM

class ContentRepositories(
    private val baseContentSpecificDbUri: String,
    private val logger: Logger,
    private val apiResponseCacheDb: ApiResponseRoomDatabase,
    private val mediaFileSystem: FileSystem
) {
    constructor(
        baseContentSpecificDbUri: String,
        encryptionKey: ByteArray?,
        logger: Logger
    ) : this(
        baseContentSpecificDbUri,
        logger,
        buildRoomDatabase<ApiResponseRoomDatabase>(
            basePathUri = baseContentSpecificDbUri,
            name = "apiResponseCache",
            encryptionKey = encryptionKey
        ),
        FileSystem.SYSTEM
    )

    val apiResponseCache = ApiResponseCache(
        apiResponseCacheDb.get(),
        logger = logger
    )

    fun initialize() {
//        apiResponseCache.open()
    }

    fun deinitialize() {
        apiResponseCacheDb.close()
    }

    fun deinitializeAndNuke() {
        deinitialize()
        mediaFileSystem.deleteRecursively(baseContentSpecificDbUri.toPath())
    }
}