package com.nxoim.blean.client.stuff

import co.touchlab.kermit.Logger
import com.nxoim.blean.api.models.commonParts.Label
import com.nxoim.blean.api.models.feed.Facet
import com.nxoim.blean.api.models.feed.PostRecordBlobEmbed
import com.nxoim.blean.api.models.feed.ReplyRecordStrongReferences
import com.nxoim.blean.api.models.repo.RecordWriteContent
import kotlinx.coroutines.CoroutineScope
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class BleanPosting(
    private val postProcessor: PostActionProcessor,
    private val clientCoroutineScope: CoroutineScope,
    private val logger: Logger
) {
    @OptIn(ExperimentalTime::class, ExperimentalUuidApi::class)
    suspend fun enqueue(
        id: String = Uuid.random().toHexString(),
        text: String? = null,
        facets: List<Facet>? = null,
        languages: List<String>,
        replyTo: ReplyRecordStrongReferences? = null,
        embed: PostRecordBlobEmbed? = null,
        labels: List<Label>? = null,
    ) {
        postProcessor.queuePost(
            id,
            RecordWriteContent.Post(
                createdAt = Clock.System.now().toString(),
                text = text ?: "",
                facets = facets,
                langs = languages,
                reply = replyTo,
                embed = embed,
                labels = labels

            )
        )
    }
}