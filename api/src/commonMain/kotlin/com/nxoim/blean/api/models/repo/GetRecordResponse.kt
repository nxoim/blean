package com.nxoim.blean.api.models.repo

import com.nxoim.blean.bskyPrimitives.AtUri
import com.nxoim.blean.bskyPrimitives.Cid
import kotlinx.serialization.Serializable

@Serializable
data class GetRecordResponse(
    val uri: AtUri,
    val value: RecordWriteContent,
    val cid: Cid? = null
)

