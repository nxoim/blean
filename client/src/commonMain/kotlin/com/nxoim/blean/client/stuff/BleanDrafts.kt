package com.nxoim.blean.client.stuff

import co.touchlab.kermit.Logger
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import com.nxoim.blean.draft.DraftsRepository
import com.nxoim.blean.models.draft.Draft
import com.nxoim.blean.repos.media.FileManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext

private const val logTag = "BleanDrafts"

class BleanDrafts(
    private val draftsRepository: DraftsRepository,
    private val fileManager: FileManager,
    private val clientCoroutineScope: CoroutineScope,
    private val logger: Logger
) {
    val scopeContext = clientCoroutineScope.coroutineContext
    suspend fun saveDraft(draft: Draft) = withContext(scopeContext) {
        draftsRepository.saveOrUpdate(draft)
            .onSuccess {
                logger.i(tag = logTag) { "Saved draft" }
            }
            .onFailure {
                logger.e(it, tag = logTag) {  "Unable to save draft" }
            }
    }

    fun getDraft(id: String): Flow<Draft?> = draftsRepository.get(id)
    fun getDrafts(amount: IntRange): Flow<List<Draft>> = draftsRepository.getDrafts(amount)

    suspend fun deleteDraft(id: String) = withContext(scopeContext) {
        getDraft(id).firstOrNull()?.let { draftsRepository.delete(it.id) }
    }

    suspend fun cacheMedia(key: String, path: String) =
        withContext(scopeContext) { fileManager.save(key, path)
        }
    suspend fun cacheMedia(key: String, bytes: Flow<ByteArray>) =
        withContext(scopeContext) { fileManager.save(key, bytes) }

    private suspend fun deleteMedia(key: String) =
        withContext(scopeContext) { fileManager.delete(key) }

    fun getMediaUri(key: String) = fileManager.getUri(key)

    // returns the list of hashed keys
    // note: slow
    suspend fun getUnusedMediaKeys(): List<String> {
        val unusedHashedKeys = mutableListOf<String>()

        repeat(draftsRepository.count().first()) {
            val index = it..(it+1)
            val draft = draftsRepository.getDrafts(index).first().first()

            fileManager.getAllKeys().collect { key ->
                if (draft.media.any { it.key == key }) {
                    unusedHashedKeys.add(key)
                }
            }
        }

        return unusedHashedKeys
    }

    suspend fun deleteUnusedMedia() = withContext(scopeContext) {
        getUnusedMediaKeys().forEach { key -> fileManager.delete(key) }
    }
}