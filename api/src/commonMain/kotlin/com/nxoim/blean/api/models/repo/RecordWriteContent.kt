package com.nxoim.blean.api.models.repo

import com.nxoim.blean.api.models.commonParts.Label
import com.nxoim.blean.api.models.commonParts.TimestampISO8601
import com.nxoim.blean.api.models.feed.Facet
import com.nxoim.blean.api.models.feed.PostRecordBlobEmbed
import com.nxoim.blean.api.models.feed.ReplyRecordStrongReferences
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@kotlinx.serialization.Serializable
sealed interface RecordWriteContent {
    @Serializable
    @SerialName("app.bsky.feed.post")
    data class Post(
        val createdAt: TimestampISO8601,
        val langs: List<String>,
        val text: String,
        val facets: List<Facet>? = null,
        val reply: ReplyRecordStrongReferences? = null,
        val embed: PostRecordBlobEmbed? = null,
        val labels: List<Label>? = null,
    ) : RecordWriteContent
}
