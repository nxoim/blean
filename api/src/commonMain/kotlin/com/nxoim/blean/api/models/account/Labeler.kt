package com.nxoim.blean.api.models.account

import com.nxoim.blean.bskyPrimitives.Did
import kotlinx.serialization.Serializable

@Serializable
data class Labeler(val did: Did)
