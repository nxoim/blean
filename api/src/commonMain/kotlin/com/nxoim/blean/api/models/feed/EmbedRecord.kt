package com.nxoim.blean.api.models.feed

import com.nxoim.blean.api.models.account.ListPurpose
import com.nxoim.blean.api.models.commonParts.Label
import com.nxoim.blean.api.models.commonParts.TimestampISO8601
import com.nxoim.blean.api.models.commonParts.UriString
import com.nxoim.blean.api.models.feed.EmbedRecord.Blocked
import com.nxoim.blean.api.models.feed.EmbedRecord.Detached
import com.nxoim.blean.api.models.feed.EmbedRecord.FeedGenerator
import com.nxoim.blean.api.models.feed.EmbedRecord.Labeler
import com.nxoim.blean.api.models.feed.EmbedRecord.Lists
import com.nxoim.blean.api.models.feed.EmbedRecord.NotFound
import com.nxoim.blean.api.models.feed.EmbedRecord.StarterPackBasic
import com.nxoim.blean.api.models.feed.EmbedRecord.View
import com.nxoim.blean.api.models.modelsJsonConfig
import com.nxoim.blean.bskyPrimitives.AtUri
import com.nxoim.blean.bskyPrimitives.Cid
import com.nxoim.blean.bskyPrimitives.Did
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@Serializable(with = EmbedRecord.Serializer::class)
sealed interface EmbedRecord {
    @Serializable
    @SerialName("app.bsky.embed.record#viewRecord")
    data class View(
        val uri: AtUri,
        val cid: Cid,
        val author: ProfileInfo.Basic,
        val value: PostRecord,
        val labels: List<Label>? = null,
        val replyCount: Int? = null,
        val repostCount: Int? = null,
        val likeCount: Int? = null,
        val quoteCount: Int? = null,
        val embeds: List<VisiblePostMediaEmbedAtRootBeforeRecord>? = null,
        val indexedAt: TimestampISO8601
    ) : EmbedRecord

    @Serializable
    @SerialName("app.bsky.embed.record#viewNotFound")
    data class NotFound(
        val uri: AtUri? = null,
        val notFound: Boolean = true
    ) : EmbedRecord

    @Serializable
    @SerialName("app.bsky.embed.record#viewBlocked")
    data class Blocked(
        val uri: AtUri,
        val blocked: Boolean = true,
        val author: ProfileInfo.Blocked
    ) : EmbedRecord

    @Serializable
    @SerialName("app.bsky.embed.record#viewDetached")
    data class Detached(
        val uri: AtUri,
        val detached: Boolean = true
    ) : EmbedRecord

    @Serializable
    @SerialName("app.bsky.feed.defs#generatorView")
    data class FeedGenerator(
        val uri: AtUri,
        val cid: Cid,
        val did: Did,
        val creator: ProfileInfo.Basic,
        val displayName: String,
        val description: String? = null,
        val descriptionFacets: List<Facet>? = null,
        val avatar: UriString? = null,
        val likeCount: Int? = null,
        val acceptsInteractions: Boolean? = null,
        val labels: List<Label>? = null,
        val viewer: GeneratorViewer? = null,
        val indexedAt: TimestampISO8601
    ) : EmbedRecord {
        init {
            if (description != null) require(description.length <= 3000) {
                "description must be less than 3000 characters"
            }
        }
    }

    @Serializable
    @SerialName("app.bsky.graph.defs#listView")
    data class Lists(
        val uri: AtUri,
        val cid: Cid,
        val creator: ProfileInfo.Basic,
        val name: String,
        val purpose: ListPurpose,
        val description: String? = null,
        val descriptionFacets: List<Facet>? = null,
        val avatar: UriString? = null,
        val listItemCount: Int? = null,
        val labels: List<Label>? = null,
        val viewer: ListViewer? = null,
        val indexedAt: TimestampISO8601
    ) : EmbedRecord {
        init {
            require(name.isNotEmpty() && name.length <= 64) {
                "name must be between 1 and 64 characters"
            }

            if (description != null) require(description.length <= 3000) {
                "description must be less than 3000 characters"
            }
        }
    }

    @Serializable
    @SerialName("app.bsky.labeler.defs#labelerView")
    data class Labeler(
        val uri: AtUri,
        val cid: Cid,
        val creator: ProfileInfo.Basic,
        val likeCount: Int? = null,
        val viewer: LabelerViewer? = null,
        val indexedAt: TimestampISO8601,
        val labels: List<Label>? = null
    ) : EmbedRecord

    @Serializable
    @SerialName("app.bsky.graph.defs#starterPackViewBasic")
    data class StarterPackBasic(
        val uri: AtUri,
        val cid: Cid,
        val record: StarterPackRecord,
        val creator: ProfileInfo.Basic,
        val listItemCount: Int? = null,
        val joinedWeekCount: Int? = null,
        val joinedAllTimeCount: Int? = null,
        val labels: List<Label>? = null,
        val indexedAt: TimestampISO8601
    ) : EmbedRecord

    // this exist becase "record": { "record": { ... } } is a thing
    object Serializer : KSerializer<EmbedRecord> {
        override val descriptor: SerialDescriptor = SerialDescriptor("EmbedRecord", JsonElement.serializer().descriptor)

        override fun serialize(encoder: Encoder, value: EmbedRecord) {
            val element = when (value) {
                is View -> modelsJsonConfig.encodeToJsonElement(View.serializer(), value)
                is NotFound -> modelsJsonConfig.encodeToJsonElement(NotFound.serializer(), value)
                is Blocked -> modelsJsonConfig.encodeToJsonElement(Blocked.serializer(), value)
                is Detached -> modelsJsonConfig.encodeToJsonElement(Detached.serializer(), value)
                is FeedGenerator -> modelsJsonConfig.encodeToJsonElement(FeedGenerator.serializer(), value)
                is Lists -> modelsJsonConfig.encodeToJsonElement(Lists.serializer(), value)
                is Labeler -> modelsJsonConfig.encodeToJsonElement(Labeler.serializer(), value)
                is StarterPackBasic -> modelsJsonConfig.encodeToJsonElement(StarterPackBasic.serializer(), value)
            }

            val typeDiscriminator = when (value) {
                is Blocked -> Blocked.serializer().descriptor.serialName
                is Detached -> Detached.serializer().descriptor.serialName
                is FeedGenerator -> FeedGenerator.serializer().descriptor.serialName
                is Labeler -> Labeler.serializer().descriptor.serialName
                is Lists -> Lists.serializer().descriptor.serialName
                is NotFound -> NotFound.serializer().descriptor.serialName
                is StarterPackBasic -> StarterPackBasic.serializer().descriptor.serialName
                is View -> View.serializer().descriptor.serialName
            }

            val elementWithTypeDiscriminator = element
                .jsonObject
                .toMutableMap()
                .apply { put("\$type", JsonPrimitive(typeDiscriminator)) }
                .let { JsonObject(it) }

            encoder.encodeSerializableValue(JsonElement.serializer(), elementWithTypeDiscriminator)
        }

        override fun deserialize(decoder: Decoder): EmbedRecord {
            val obj = decoder.decodeSerializableValue(JsonElement.serializer()) as? JsonObject

            return when (val type = (obj?.get("\$type") as? JsonPrimitive)?.contentOrNull) {
                null -> {
                    val packedRecord = obj?.get("record") as? JsonObject

                    // confirmed "record": { "record": { ... } } crime
                    if (packedRecord != null) {
                        val recordType = packedRecord.jsonObject["\$type"]?.jsonPrimitive?.contentOrNull

                        if (recordType?.startsWith("app.bsky.embed.record") == true)
                            parseEmbedRecord(packedRecord)
                                ?: throw SerializationException("Cannot unpack EmbedRecord. $packedRecord")
                        else
                            throw SerializationException("Unknown record type found when unpacking: $recordType")
                    } else {
                        throw SerializationException("Cannot deserialize EmbedRecord. $obj")
                    }
                }

                else -> parseEmbedRecord(obj)
                    // is not supposed to be null
                    ?: throw SerializationException("Cannot deserialize EmbedRecord with unsupported type $type")
            }
        }
    }
}

private fun parseEmbedRecord(obj: JsonObject) =
    when ((obj["\$type"] as? JsonPrimitive)?.contentOrNull) {
        "app.bsky.embed.record#viewRecord" ->
            modelsJsonConfig.decodeFromJsonElement<View>(obj)
        "app.bsky.embed.record#viewNotFound" ->
            modelsJsonConfig.decodeFromJsonElement<NotFound>(obj)
        "app.bsky.embed.record#viewBlocked" ->
            modelsJsonConfig.decodeFromJsonElement<Blocked>(obj)
        "app.bsky.embed.record#viewDetached" ->
            modelsJsonConfig.decodeFromJsonElement<Detached>(obj)
        "app.bsky.feed.defs#generatorView" ->
            modelsJsonConfig.decodeFromJsonElement<FeedGenerator>(obj)
        "app.bsky.graph.defs#listView" ->
            modelsJsonConfig.decodeFromJsonElement<Lists>(obj)
        "app.bsky.labeler.defs#labelerView" ->
            modelsJsonConfig.decodeFromJsonElement<Labeler>(obj)
        "app.bsky.graph.defs#starterPackViewBasic" ->
            modelsJsonConfig.decodeFromJsonElement<StarterPackBasic>(obj)
        else -> null
    }
