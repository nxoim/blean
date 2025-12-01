package com.nxoim.blean.api.models.feed

import com.nxoim.blean.api.models.commonParts.Label
import com.nxoim.blean.api.models.commonParts.TimestampISO8601
import com.nxoim.blean.api.models.commonParts.UriString
import com.nxoim.blean.api.models.commonParts.ViewerState
import com.nxoim.blean.api.utils.throwPolymorphicSerializerError
import com.nxoim.blean.bskyPrimitives.AccountIdentificator
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject


@Serializable(ProfileInfo.Serializer::class)
sealed interface ProfileInfo {
    // possibly is the same as actor
    @Serializable
    @SerialName("app.bsky.actor.defs#profileViewBasic")
    data class Basic(
        val did: AccountIdentificator.Did,
        val handle: AccountIdentificator.Handle,
        val displayName: String? = null,
        val description: String? = null,
        val avatar: UriString? = null,
        val associated: ProfileAssociated? = null,
        val viewer: ViewerState? = null,
        val labels: List<Label>? = null,
        val createdAt: TimestampISO8601? = null
    ) : ProfileInfo {
        init {
            if (displayName != null) require(displayName.length <= 640) {
                "displayName must be less than 640 characters"
            }
        }
    }

    @Serializable
    @SerialName("app.bsky.actor.defs#profileView")
    data class Basic2(
        val did: AccountIdentificator.Did,
        val handle: AccountIdentificator.Handle,
        val displayName: String? = null,
        val description: String? = null,
        val avatar: UriString? = null,
        val associated: ProfileAssociated? = null,
        val indexedAt: TimestampISO8601? = null,
        val createdAt: TimestampISO8601? = null,
        val viewer: ViewerState? = null,
        val labels: List<Label>? = null,
    ) : ProfileInfo {
        init {
            if (displayName != null) require(displayName.length <= 640) {
                "displayName must be less than 640 characters"
            }
        }
    }

    @Serializable
    @SerialName("app.bsky.actor.defs#profileViewDetailed")
    data class Detailed(
        val did: AccountIdentificator.Did,
        val handle: AccountIdentificator.Handle,
        val displayName: String? = null,
        val description: String? = null,
        val avatar: UriString? = null,
        val banner: UriString? = null,
        val followersCount: Int? = null,
        val followsCount: Int? = null,
        val postsCount: Int? = null,
        val associated: ProfileAssociated? = null,
        val joinedViaStarterPack: EmbedRecord.StarterPackBasic? = null,
        val indexedAt: TimestampISO8601? = null,
        val createdAt: TimestampISO8601? = null,
        val viewer: ViewerState? = null,
        val labels: List<Label>? = null,
        val pinnedPost: RepoStrongReference? = null
    ) : ProfileInfo {
        init {
            if (displayName != null) require(displayName.length <= 640) {
                "displayName must be less than 640 characters"
            }
        }
    }

    @Serializable
    data class Blocked(
        val did: AccountIdentificator.Did,
        val viewer: ViewerState
    ) : ProfileInfo

    object Serializer : JsonContentPolymorphicSerializer<ProfileInfo>(ProfileInfo::class) {
        override fun selectDeserializer(element: JsonElement) = when {
            "createdAt" in element.jsonObject -> Basic.serializer()
            "handle" in element.jsonObject -> Basic2.serializer()
            "did" in element.jsonObject && "viewer" in element.jsonObject -> Blocked.serializer()
            else -> throwPolymorphicSerializerError(element.jsonObject)
        }
    }
}
