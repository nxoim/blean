package com.nxoim.blean.ui.screens.content.postCreation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.nxoim.blean.postRelatedCommons.models.TextAndFacets
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

// todo change history
@OptIn(ExperimentalUuidApi::class)
class WritingSession(
    private val maxCharacters: Int,
    val id: String = Uuid.random().toHexString(),
    private val initialLanguages: List<String> = listOf()
) {
    var text by mutableStateOf("")
        private set
    var mediaUris by mutableStateOf(listOf<LocalDraftMedia>())
        private set
    val languages = initialLanguages.toMutableList()
//    val facets
    // add hashtags and all

    fun updateText(newText: String, ignoreMaxCharacters: Boolean = false) {
        text = if (ignoreMaxCharacters) newText else newText.take(maxCharacters)
    }

    fun updateMedia(newMedia: Set<LocalDraftMedia>) {
        mediaUris = newMedia.toList()
        // TODO request compression of media
        //  along with metadata removal. upload only on posting
    }

    fun toContent() = TextAndFacets(
        text,
        facets = null
    )

    fun isNotEmpty() = text.isNotEmpty() ||  mediaUris.isNotEmpty()
}