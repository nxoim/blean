package com.nxoim.blean.api.models.feed

import com.nxoim.blean.bskyPrimitives.AtUri
import com.nxoim.blean.bskyPrimitives.Cid
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject

@kotlinx.serialization.Serializable(RecordInEmbedInRecordInPost.Serializer::class)
sealed interface RecordInEmbedInRecordInPost {
    @kotlinx.serialization.Serializable
    data class PostStrongReference(
        val cid: Cid,
        val uri: AtUri
    ) : RecordInEmbedInRecordInPost


    @Serializable
    data object Unsupported : RecordInEmbedInRecordInPost

    object Serializer : JsonContentPolymorphicSerializer<RecordInEmbedInRecordInPost>(
        RecordInEmbedInRecordInPost::class) {
        override fun selectDeserializer(element: JsonElement) = when {
            "cid" in element.jsonObject && "uri" in element.jsonObject -> PostStrongReference.serializer()
            else -> Unsupported.serializer()
        }
    }
}
