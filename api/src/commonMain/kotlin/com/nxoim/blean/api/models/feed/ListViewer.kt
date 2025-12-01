package com.nxoim.blean.api.models.feed

import com.nxoim.blean.bskyPrimitives.AtUri
import kotlinx.serialization.Serializable

@Serializable
data class ListViewer(val muted: Boolean? = null, val blocked: AtUri? = null)