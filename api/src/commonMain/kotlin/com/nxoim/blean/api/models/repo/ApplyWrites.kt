package com.nxoim.blean.api.models.repo

import com.nxoim.blean.bskyPrimitives.Cid
import com.nxoim.blean.bskyPrimitives.Did
import kotlinx.serialization.Serializable

@Serializable
data class ApplyWrites(
    val repo: Did,
    val writes: List<RecordWrite>,
    val validate: Boolean? = null,
    val swapCommit: Cid? = null
)