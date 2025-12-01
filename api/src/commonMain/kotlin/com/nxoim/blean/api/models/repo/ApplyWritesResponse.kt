package com.nxoim.blean.api.models.repo

import kotlinx.serialization.Serializable

@Serializable
data class ApplyWritesResponse(
    val commit: RecordCommit,
    val results: List<RecordWriteResult>
)