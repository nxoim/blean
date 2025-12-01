package com.nxoim.blean.api.models.repo

import com.nxoim.blean.bskyPrimitives.Cid
import com.nxoim.blean.bskyPrimitives.RecordKey
import kotlinx.serialization.Serializable

@Serializable
data class RecordCommit(
    val cid: Cid,
    val rev: RecordKey.Tid
)