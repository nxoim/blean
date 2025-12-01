@file:OptIn(ExperimentalTime::class)

package com.nxoim.blean.repos.media

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext
import okio.FileMetadata
import okio.FileSystem
import okio.HashingSink
import okio.Path
import okio.Path.Companion.toPath
import okio.SYSTEM
import okio.blackholeSink
import okio.buffer
import kotlin.coroutines.CoroutineContext
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

private const val persistent = "0"
private const val temp = "1"

/**
 * A file store implementation that uses Okio for file system operations. This class provides
 * asynchronous file storage capabilities, locks for concurrency control, and
 * the ability to manage temporary files to ensure data integrity during operations like saving.
 *
 * @property pathUri The string URI representing the path where files will be stored.
 * @property fileSystem The [FileSystem] to use for file operations. Defaults to the system file system.
 * @property coroutineContext The [CoroutineContext] in which file operations will be performed. Defaults to [Dispatchers.IO].
 */
class OkioFileStore(
    private val pathUri: String,
    private val fileSystem: FileSystem = FileSystem.SYSTEM,
    private val coroutineContext: CoroutineContext = Dispatchers.IO,
) {
    val baseFilePath = pathUri.toPath().resolve(persistent)
    val tempFilePath = baseFilePath.resolve(temp)

    private val basePathLock = KeyedMutex<String>()
    private val tempFilePathLock = KeyedMutex<String>()

    /**
     * Initializes the file store by creating the base directory and the temporary directory
     * if they don't exist.
     */
    suspend fun createFolderIfMissing() = withContext(coroutineContext){
       if (!fileSystem.exists(pathUri.toPath())) {
           fileSystem.createDirectories(pathUri.toPath())
       }
        if (!fileSystem.exists(baseFilePath)) {
            fileSystem.createDirectories(baseFilePath)
        }
        if (!fileSystem.exists(tempFilePath)) {
            fileSystem.createDirectories(tempFilePath)
        }
    }

    /**
     * Deletes all files in the base directory and reinitializes the file store.
     */
    suspend fun deleteAll() = withContext(coroutineContext) {
        fileSystem.deleteRecursively(baseFilePath)
        createFolderIfMissing()
    }

    /**
     * Saves a file with a key.
     *
     * @param key The key representing the file.
     * @param bytes The [Flow] of [ByteArray] representing the file data.
     * @param onBytesSaved Bytes saved callback
     */
    suspend fun save(
        key: String,
        bytes: Flow<ByteArray>,
        onBytesSaved: ((total: Long) -> Unit)? = null
    ) = tempFilePathLock.withLock(key) {
        try {
            val tempFile = tempFilePath.resolve(key)
            var totalBytesWritten = 0L

            val monitoredBytes = bytes.onEach { chunk ->
                totalBytesWritten += chunk.size
                onBytesSaved?.invoke(totalBytesWritten)
            }

            fileSystem.write(tempFile, monitoredBytes, coroutineContext)
        } catch (throwable: Throwable) {
            fileSystem.delete(tempFilePath.resolve(key))
            throw throwable
        }
        basePathLock.withLock(key) {
            fileSystem.atomicMove(tempFilePath.resolve(key), baseFilePath.resolve(key))
        }
    }


    suspend fun save(key: String, filePath: String, onBytesSaved: ((total: Long) -> Unit)? = null) {
        save(
            key = key,
            bytes = byteArrayFlowFromSource(coroutineContext) { fileSystem.source(filePath.toPath()) },
            onBytesSaved = onBytesSaved
        )
    }

    /**
     * Retrieves the bytes of a file as a [Flow] using a key.
     *
     * @param key The key representing the file.
     * @return A [Flow] of [ByteArray], or `null` if the file is not found.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun getBytes(key: String): Flow<ByteArray?> = flow {
        basePathLock.withLock(key) {
            emit(fileSystem.readByteArrayFlow(baseFilePath.resolve(key), coroutineContext))
        }
    }.flatMapLatest { it }

    /**
     * Retrieves the URI of a file using a key.
     *
     * @param key The key representing the file.
     * @return The URI as a string, or `null` if the file is not found.
     */
    fun getUri(key: String): String? = baseFilePath.resolveIfExists(key)?.toString()


    /**
     * Deletes a file using a key.
     *
     * @param key The key representing the file.
     */
    suspend fun delete(key: String) = withContext(coroutineContext) {
        basePathLock.withLock(key) {
            fileSystem.delete(baseFilePath.resolve(key))
        }
    }

    /**
     * Renames a file using keys for both the old and new filenames.
     *
     * @param oldKey The key for the current file.
     * @param newKey The key for the new file name.
     */
    suspend fun rename(oldKey: String, newKey: String) =
        withContext(coroutineContext) {
            basePathLock.withLock(oldKey) {
                basePathLock.withLock(newKey) {
                    fileSystem.atomicMove(baseFilePath.resolve(oldKey), baseFilePath.resolve(newKey))
                }
            }
        }

    /**
     * Returns a [Flow] of all keys stored in the file store.
     */
    fun getAllKeys(): Flow<String> = flow {
        fileSystem.list(baseFilePath).forEach { if (it != tempFilePath) emit(it.name) }
    }.flowOn(coroutineContext)


    /**
     * Returns a [Flow] of all URIs of files stored in the file store.
     */
    fun getAllUris(): Flow<String> = flow {
        fileSystem.list(baseFilePath).forEach { if (it != tempFilePath) emit(it.toString()) }
    }
        .flowOn(coroutineContext)

    /**
     * Retrieves the size of a file using a key.
     *
     * @param key The key representing the file.
     * @return The size of the file in bytes, or `null` if the file is not found.
     */
    fun getSize(key: String): Long? = baseFilePath.resolveIfExists(key)?.let {
        fileSystem.metadataOrNull(it)?.size
    }

    /**
     * Retrieves the creation time of a file using a key.
     *
     * @param key The key representing the file.
     * @return The creation time as an [Instant], or `null` if the file is not found.
     */
    fun getCreationTime(key: String): Instant? = baseFilePath.resolveIfExists(key)?.let {
        fileSystem.metadataOrNull(it)?.createdAtMillis?.let { Instant.fromEpochMilliseconds(it) }
    }

    /**
     * Retrieves the last modified time of a file using a key.
     *
     * @param key The key representing the file.
     * @return The last modified time as an [Instant], or `null` if the file is not found.
     */
    fun getLastModifiedTime(key: String): Instant? = baseFilePath.resolveIfExists(key)?.let {
        fileSystem.metadataOrNull(it)?.lastModifiedAtMillis?.let { Instant.fromEpochMilliseconds(it) }
    }

    /**
     * Retrieves the total number of files stored in the file store, excluding temporary files.
     *
     * @return The number of files.
     */
    suspend fun getFileCount(): Int = withContext(coroutineContext) {
        fileSystem.list(baseFilePath).count { it != tempFilePath }
    }

    /**
     * Retrieves the total size of all files stored in the file store.
     *
     * @param includeTemp Whether to include the size of temporary files.
     * @return The total size in bytes.
     */
    suspend fun getTotalSizeOfStoreInBytes(): Long = withContext(coroutineContext) {
        (fileSystem.metadataOrNull(tempFilePath)?.size ?: 0) +
                (fileSystem.metadataOrNull(baseFilePath)?.size ?: 0)
    }

    suspend fun getTotalTempFilesSizeInBytes(): Long = withContext(coroutineContext) {
        fileSystem.metadataOrNull(tempFilePath)?.size ?: 0
    }

    suspend fun getTotalNonTempFilesSizeInBytes(): Long = withContext(coroutineContext) {
        fileSystem.metadataOrNull(tempFilePath)?.size ?: 0
    }

    /**
     * Retrieves the SHA-256 hash of a file using a key.
     */
    suspend fun getHash(key: String) = getHash(
        bytes = getBytes(key)
            .onEach { if (it == null) error("Attempted to get sha256 of non-existent file $key") }
            .filterNotNull()
    )

    /**
     * Retrieves the SHA-256 hash of a file
     */
    suspend fun getHash(bytes: Flow<ByteArray>) = withContext(coroutineContext) {
        val sink = HashingSink.sha256(blackholeSink())

        bytes.collect() { sink.buffer().write(it) }
        sink.hash.hex()
    }

    suspend fun getFilesWithMetadata(): List<Pair<Path, FileMetadata>> = withContext(coroutineContext) {
        fileSystem.list(baseFilePath)
            .filter { it.name != temp }
            .mapNotNull { path ->
                fileSystem
                    .metadataOrNull(path)
                    ?.let { path to it }
            }
    }

    private fun Path.resolveIfExists(key: String) =
        if (fileSystem.exists(this.resolve(key)))
            this.resolve(key) else null
}

