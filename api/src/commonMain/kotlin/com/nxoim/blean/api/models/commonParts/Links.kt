package com.nxoim.blean.api.models.commonParts

import kotlinx.serialization.Serializable

@Serializable
data class Links(
    val privacyPolicy: String,
    val termsOfService: String
)