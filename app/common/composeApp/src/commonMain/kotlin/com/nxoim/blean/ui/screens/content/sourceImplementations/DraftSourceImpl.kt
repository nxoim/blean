@file:OptIn(ExperimentalTime::class)

package com.nxoim.blean.ui.screens.content.sourceImplementations

import com.nxoim.blean.client.stuff.BleanDrafts
import com.nxoim.blean.models.draft.Draft
import com.nxoim.blean.models.draft.LocalMedia
import com.nxoim.blean.ui.screens.content.postCreation.DraftMediaCacheSource
import com.nxoim.blean.ui.screens.content.postCreation.DraftSource
import com.nxoim.blean.ui.screens.content.postCreation.LocalDraftMedia
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.ExperimentalTime

class DraftSourceImpl(private val userDrafts: BleanDrafts) : DraftSource {
    fun Draft.toWritingSessionDraft() = com.nxoim.blean.ui.screens.content.postCreation.Draft(
        id = this.id,
        text = this.text,
        facets = emptyList(),
        media = this.media.map { storedDraftMedia ->
            when (storedDraftMedia) {
                is LocalMedia.Image -> LocalDraftMedia.Image(storedDraftMedia.key)
                is LocalMedia.Video -> LocalDraftMedia.Video(storedDraftMedia.key)
            }
        }.toSet(),
        creationDate = this.creationDate
    )

    fun com.nxoim.blean.ui.screens.content.postCreation.Draft.toStoredDraft() = Draft(
        id = this.id,
        text = this.text,
        media = this.media.map { writingSessionDraftMedia ->
            when (writingSessionDraftMedia) {
                is LocalDraftMedia.Image -> LocalMedia.Image(writingSessionDraftMedia.key)
                is LocalDraftMedia.Video -> LocalMedia.Video(writingSessionDraftMedia.key)
            }
        }.toSet(),
        creationDate = this.creationDate
    )

    override val mediaSource = object : DraftMediaCacheSource {
        override suspend fun cacheMedia(
            key: String,
            mediaFlow: Flow<ByteArray>
        ) {
            userDrafts.cacheMedia(key, mediaFlow)
        }

        override fun getMediaUri(key: String) = userDrafts.getMediaUri(key)
            ?: error("Attempted to get media uri of a draft and failed")
    }

    override fun getDrafts(amount: IntRange) = userDrafts
        .getDrafts(amount)
        .map { storedDraft -> storedDraft.map { it.toWritingSessionDraft() } }

    override suspend fun deleteDraft(id: String) {
        userDrafts.deleteDraft(id)
    }

    override fun getDraft(id: String): Flow<com.nxoim.blean.ui.screens.content.postCreation.Draft?> =
        userDrafts.getDraft(id).map { it?.toWritingSessionDraft() }

    override suspend fun saveDraft(draft: com.nxoim.blean.ui.screens.content.postCreation.Draft) {
        userDrafts.saveDraft(draft.toStoredDraft())
    }
}