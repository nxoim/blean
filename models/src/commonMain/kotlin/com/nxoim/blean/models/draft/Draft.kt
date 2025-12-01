@file:OptIn(ExperimentalTime::class)

package com.nxoim.blean.models.draft

import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

data class Draft(
    val id: String,
    val text: String,
    val media: Set<LocalMedia>,
    val creationDate: Instant = Clock.System.now()
)

sealed class LocalMedia(val key: String) {
    data class Image(private val _key: String) : LocalMedia(_key)
    data class Video(private val _key: String) : LocalMedia(_key)
}
