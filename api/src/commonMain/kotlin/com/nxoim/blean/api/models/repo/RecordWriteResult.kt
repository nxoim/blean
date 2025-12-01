package com.nxoim.blean.api.models.repo

import com.nxoim.blean.bskyPrimitives.AtUri
import com.nxoim.blean.bskyPrimitives.Cid
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface RecordWriteResult {
    @Serializable
    @SerialName("com.atproto.repo.applyWrites#createResult")
    data class Create(
        val cid: Cid,
        val uri: AtUri,
        val validationStatus: RecordValidationStatus? = null
    ) : RecordWriteResult

    @Serializable
    @SerialName("com.atproto.repo.applyWrites#updateResult")
    data class Update(
        val cid: Cid,
        val uri: AtUri,
        val validationStatus: RecordValidationStatus? = null
    ) : RecordWriteResult

    @Serializable
    @SerialName("com.atproto.repo.applyWrites#deleteResult")
    data object Delete : RecordWriteResult
}