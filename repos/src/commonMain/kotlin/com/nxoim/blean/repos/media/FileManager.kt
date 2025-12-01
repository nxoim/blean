@file:OptIn(ExperimentalTime::class)

package com.nxoim.blean.repos.media

import com.nxoim.blean.byteCount.ByteCount
import com.nxoim.blean.byteCount.bytes
import kotlinx.coroutines.flow.Flow
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

class FileManager(
    private val fileStore: OkioFileStore,
    private val config: FileManagerConfig = FileManagerConfig.Default
) {
    constructor(
        path: String,
        config: FileManagerConfig = FileManagerConfig()
    ) : this(
        fileStore = OkioFileStore(path),
        config
    )

    constructor(
        basePathUri: String,
        name: String,
        config: FileManagerConfig = FileManagerConfig.Default
    ) : this("$basePathUri/$name", config)

    suspend fun createFolderIfMissing() { fileStore.createFolderIfMissing() }

    suspend fun save(key: String, bytes: Flow<ByteArray>) = fileStore.save(key, bytes)

    suspend fun save(key: String, filePath: String) = fileStore.save(key, filePath)

    fun getBytes(key: String): Flow<ByteArray?> = fileStore.getBytes(key)

    fun getUri(key: String): String? = fileStore.getUri(key)

    suspend fun delete(key: String) { fileStore.delete(key) }

    suspend fun rename(key: String, newKey: String) = fileStore.rename(key, newKey)

    suspend fun deleteAll() = fileStore.deleteAll()

    fun getAllKeys(): Flow<String> = fileStore.getAllKeys()

    suspend fun deleteOutdatedFiles(clock: Clock = Clock.System) {
        val currentTime = clock.now()

        val allFiles = fileStore.getFilesWithMetadata()
            .sortedBy { it.second.lastModifiedAtMillis ?: 0L }

        val filesToDelete = mutableSetOf<String>()

        config.defaultExpiryPolicies.forEach { policy ->
            when (policy) {
                is ExpiryPolicy.ByAge -> {
                    allFiles.forEach { (path, metadata) ->
                        val lastMod = metadata.lastModifiedAtMillis
                            ?.let { Instant.fromEpochMilliseconds(it) }
                            ?: Instant.DISTANT_PAST

                        if (policy.shouldDelete(currentTime - lastMod)) {
                            filesToDelete.add(path.name)
                        }
                    }
                }
                is ExpiryPolicy.ByFileCount -> {
                    val candidates = allFiles.filterNot { filesToDelete.contains(it.first.name) }

                    if (policy.shouldDelete(candidates.size)) {
                        val deleteCount = candidates.size - policy.max
                        candidates.take(deleteCount).forEach { filesToDelete.add(it.first.name) }
                    }
                }
                is ExpiryPolicy.BySize -> {
                    val candidates = allFiles.filterNot { filesToDelete.contains(it.first.name) }
                    var currentTotalSize = candidates.sumOf { it.second.size ?: 0L }.bytes

                    if (policy.shouldDelete(currentTotalSize)) {
                        for ((path, metadata) in candidates) {
                            if (!policy.shouldDelete(currentTotalSize)) break

                            filesToDelete.add(path.name)
                            currentTotalSize -= (metadata.size ?: 0L).bytes
                        }
                    }
                }
            }
        }

        filesToDelete.forEach { delete(it) }
    }
}

data class FileManagerConfig(
    val defaultExpiryPolicies: Iterable<ExpiryPolicy> = emptySet(),
) {
    companion object {
        val Default = FileManagerConfig()
    }
}

sealed interface ExpiryPolicy {
    data class ByAge(val age: Duration) : ExpiryPolicy {
        fun shouldDelete(current: Duration): Boolean = current > this@ByAge.age
    }

    data class ByFileCount(val max: Int) : ExpiryPolicy {
        fun shouldDelete(current: Int): Boolean = current >= max
    }

    data class BySize(val maxSize: ByteCount) : ExpiryPolicy {
        fun shouldDelete(current: ByteCount): Boolean = current > maxSize
    }
}