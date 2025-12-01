@file:OptIn(ExperimentalTime::class)

package com.nxoim.blean.ui.screens.content.postCreation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import co.touchlab.kermit.Logger
import com.nxoim.blean.byteCount.ByteCount
import com.nxoim.blean.byteCount.megabytes
import com.nxoim.blean.postRelatedCommons.models.TextAndFacets
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.FileKitMode
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.openFilePicker
import io.github.vinceglb.filekit.exists
import io.github.vinceglb.filekit.extension
import io.github.vinceglb.filekit.source
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.io.Buffer
import kotlinx.io.readByteArray
import kotlin.random.Random
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

private const val logTag = "PostWritingModel"

class PostWritingModel(
    val maxCharacters: Int = fallbackMaxCharacters,
    private val draftSource: DraftSource,
    private val posting: PostingSource,
    private val logger: Logger = Logger,
    private val coroutineScope: CoroutineScope
) {
    var currentWritingSession by mutableStateOf(WritingSession(maxCharacters))

    val tenLastDrafts = draftSource
        .getDrafts(amount = 0..10)
        .stateIn(
            coroutineScope,
            started = SharingStarted.WhileSubscribed(0),
            initialValue = null
        )

    fun uploadPostAndReset(targetWritingSession: WritingSession) {
        coroutineScope.launch {
            require(targetWritingSession.isNotEmpty()) {
                "Writing session was empty"
            }

            // TODO upload media first
            posting.queuePost(
                id = targetWritingSession.id,
                textContent = targetWritingSession.toContent(),
                languages = targetWritingSession.languages
            )

            draftSource.deleteDraft(targetWritingSession.id)
            createNewWritingSession()
            // might be readded on error by the observer of outbox
        }
    }

    fun openMediaSelector(onResultReceived: () -> Unit = {}) {
        coroutineScope.launch(Dispatchers.IO) {
            FileKit
                .openFilePicker(
                    FileKitType.ImageAndVideo,
                    FileKitMode.Multiple(),
                    title = "MEIDAAAAAA"
                )
                ?.forEach { file ->
                    if (!file.exists()) error("File does not exist")
                    val key = Random.nextBytes(size = 16).decodeToString() // TODO use sha256 instead? but the model is not to be deciding this, so should be done lower

                    draftSource.mediaSource.cacheMedia(key, file.toByteArrayFlow())
                    currentWritingSession.updateMedia(
                        newMedia = (currentWritingSession.mediaUris + file.toLocalMediaType(key)).toSet()
                    )
                    logger.v(tag = logTag) { "file added $key" }
                }

            // this will be used in ui so it needs to be on main becauyse
            // of some stupid bs
            withContext(Dispatchers.Main.immediate) { onResultReceived() }
        }
    }

    fun getImageUri(key: String) = draftSource.mediaSource.getMediaUri(key)

    fun restoreDraft(id: String) {
        coroutineScope.launch {
            val draft = draftSource.getDraft(id).filterNotNull().first()

            currentWritingSession = draft.toDraftWritingSession()
        }
    }

    fun deleteDraft(id: String) {
        coroutineScope.launch { draftSource.deleteDraft(id) }
    }

    fun saveOrUpdateDraft(draft: Draft) {
        coroutineScope.launch { draftSource.saveDraft(draft) }
    }

    fun saveCurrentToDraftAndReset() {
        coroutineScope.launch {
            val doesntExistAlready = tenLastDrafts.value?.none(currentWritingSession::equalsDraft) == true

            if (currentWritingSession.isNotEmpty() && doesntExistAlready) {
                saveOrUpdateDraft(currentWritingSession.toDraft())
            }

            createNewWritingSession()
        }
    }

    private fun createNewWritingSession() {
        currentWritingSession = WritingSession(maxCharacters)
    }
}

sealed interface PostUploadNotifications {
    object Error : PostUploadNotifications
}

private fun WritingSession.toDraft(creationDate: Instant = Clock.System.now()) =
    Draft(id, text, listOf(), mediaUris.toSet(), creationDate)

private fun Draft.toDraftWritingSession() = WritingSession(fallbackMaxCharacters, id).apply {
    updateText(this@toDraftWritingSession.text)
    updateMedia(this@toDraftWritingSession.media)
}

private infix fun WritingSession.equalsDraft(draft: Draft) =
    this.id == draft.id

// im sure this has been done and theres a util somewhere
private fun PlatformFile.toLocalMediaType(key: String) = when (extension) {
    "jpg", "jpeg", "png", "dng", "gif", "gifv",
    "heic", "heif", "webp", "tiff", "tif", "bmp",
    "ico" -> LocalDraftMedia.Image(key)

    "mp4", "mov", "avi", "webm", "mkv" -> LocalDraftMedia.Video(key)

    else -> error("Unsupported extension $extension")
}

fun PlatformFile.toByteArrayFlow(chunkSize: ByteCount = 1.megabytes): Flow<ByteArray> = flow {
    val source = this@toByteArrayFlow.source()!!

    source.use { bufferedSource ->
        val buffer = Buffer()

        while (true) {
            bufferedSource.readAtMostTo(sink = buffer, byteCount = chunkSize.bytes)
            if (buffer.size == 0L) break // End of stream
            emit(buffer.readByteArray())
            buffer.clear()
        }
    }
}

private const val fallbackMaxCharacters = 300

interface DraftSource {
    fun getDrafts(amount: IntRange): Flow<List<Draft>?>
    val mediaSource: DraftMediaCacheSource
    fun getDraft(id: String): Flow<Draft?>
    suspend fun deleteDraft(id: String)
    suspend fun saveDraft(draft: Draft)
}

interface DraftMediaCacheSource {
    suspend fun cacheMedia(key: String, mediaFlow: Flow<ByteArray>)
    fun getMediaUri(key: String): String
}

data class Draft(
    val id: String,
    val text: String,
    val facets: List<DraftTextFacets>,
    val media: Set<LocalDraftMedia>,
    val creationDate: Instant = Clock.System.now()
)

sealed interface LocalDraftMedia {
    val key: String

    data class Image(override val key: String) : LocalDraftMedia
    data class Video(override val key: String) : LocalDraftMedia
}

sealed interface DraftTextFacets {

}

interface PostingSource {
    @OptIn(ExperimentalUuidApi::class)
    suspend fun queuePost(
        id: String = Uuid.random().toHexString(),
        textContent: TextAndFacets,
        languages: List<String>
    )
}