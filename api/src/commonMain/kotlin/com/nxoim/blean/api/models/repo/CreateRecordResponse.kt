package com.nxoim.blean.api.models.repo

import com.nxoim.blean.bskyPrimitives.AtUri
import com.nxoim.blean.bskyPrimitives.Cid
import kotlinx.serialization.Serializable

@Serializable
data class CreateRecordResponse(
    val uri: AtUri,
    val cid: Cid,
    val commit: RecordCommit,
    val validationStatus: RecordValidationStatus
)

