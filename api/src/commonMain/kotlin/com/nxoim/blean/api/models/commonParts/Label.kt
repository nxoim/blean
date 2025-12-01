package com.nxoim.blean.api.models.commonParts

import com.nxoim.blean.bskyPrimitives.AccountIdentificator
import com.nxoim.blean.bskyPrimitives.AtUri
import com.nxoim.blean.bskyPrimitives.Cid
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * @param uri Can be [AtUri] or [com.nxoim.blean.bskyPrimitives.Did]
 */
@Serializable
data class Label(
    val ver: Int? = null,
    val src: AccountIdentificator.Did,
    val uri: String, // CAN be a did or at uri
    val cid: Cid? = null,
    @SerialName("val") val _val: String,
    val neg: Boolean? = null,
    val cts: TimestampISO8601,
    val exp: TimestampISO8601? = null,
    val sig: ByteArray? = null
) {
    init {
        require(_val.length <= 128) {
            "Label value must be less than or equal to 128 characters"
        }
    }
}