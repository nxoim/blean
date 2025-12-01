package com.nxoim.blean.api.models.feed

import com.nxoim.blean.api.models.commonParts.LanguageString
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("app.bsky.feed.post")
data class PostRecord(
    val text: String,
    val createdAt: String,
    val langs: List<LanguageString>? = null,
    val reply: ReplyRecordStrongReferences? = null,
    val embed: PostRecordBlobEmbed? = null,
    val facets: List<Facet>? = null
)

