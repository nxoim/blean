package com.nxoim.blean.api.models.repo

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class RecordValidationStatus {
    @SerialName("valid")
    Valid,
    @SerialName("unknown")
    Unknown
}
