package com.nxoim.blean.postRelatedCommons.models

import com.nxoim.blean.api.models.commonParts.TimestampISO8601
import com.nxoim.blean.bskyPrimitives.AccountIdentificator
import com.nxoim.blean.bskyPrimitives.AtUri
import kotlinx.serialization.Serializable

@Serializable
sealed interface LabelType {
    @Serializable
    data object NoUnauthenticated : LabelType

    @Serializable
    data object Nudity : LabelType

    @Serializable
    data object Sexual : LabelType

    @Serializable
    data object Porn : LabelType

    @Serializable
    data object Spam : LabelType

    @Serializable
    data class Unsupported(val value: String) : LabelType {
        init {
            require(value.length <= 128) {
                "label value must not be longer than 128 characters"
            }
        }
    }

    companion object {
        // Centralized mapping for string -> LabelType
        // the type needs to be specified explicitly because otherwise the
        // native compiler will say that this is actually <string, any>
        val labelMappings = mapOf<String, LabelType>(
            "!no-unauthenticated" to NoUnauthenticated,
            "nudity" to Nudity,
            "sexual" to Sexual,
            "porn" to Porn,
            "spam" to Spam
        )

        // Resolve function
        fun resolve(value: String): LabelType = labelMappings[value] ?: Unsupported(value)

        // Unresolve function
        fun unresolve(value: LabelType): String = labelMappings.entries
            .find { it.value == value }
            ?.key
            ?: (value as? Unsupported)?.value
            ?: throw IllegalArgumentException("Unknown LabelType")
    }
}

@Serializable
data class LabelData(
    var uriOfContentItAppliesTo: LabelDataUri,
    var value: LabelType,
    var creatorDid: AccountIdentificator.Did,
    var isNegotiationLabel: Boolean?,
    var dateOfCreationISO8601: String,
    val expiresOnISO8601: TimestampISO8601?
    // var signatureByteArray: ByteArray? = null
) {

}

@Serializable
sealed interface LabelDataUri {
    @Serializable
    data class Did(val value: com.nxoim.blean.bskyPrimitives.Did) : LabelDataUri

    @Serializable
    data class Uri(val value: AtUri) : LabelDataUri
}
