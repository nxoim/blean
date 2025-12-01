package com.nxoim.blean.api.models.repo

import kotlinx.serialization.Serializable

@Serializable
data class DeleteRecordResponse(
    val commit: RecordCommit? = null
)