package com.nxoim.blean.api.models.feed

import com.nxoim.blean.bskyPrimitives.AtUri
import kotlinx.serialization.Serializable

@Serializable
data class LabelerViewer(val like: AtUri? = null)
