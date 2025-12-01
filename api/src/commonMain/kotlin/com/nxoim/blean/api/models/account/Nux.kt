package com.nxoim.blean.api.models.account

import com.nxoim.blean.api.models.commonParts.TimestampISO8601
import kotlinx.serialization.Serializable

@Serializable
data class Nux(
    val id: String,
    val completed: Boolean,
    val data: String? = null, // "Limited to 300 characters." but "Possible values: <= 3000 characters"
    val expiresAt: TimestampISO8601? = null
) {
    init {
        require(id.length <= 100) {
            "id must be less than or equal to 100"
        }

        if (data != null) require(data.length <= 3000) {
            "data must be less than or equal to 3000"
        }
    }
}
