package com.nxoim.blean.api.models.graph

import kotlinx.serialization.Serializable

// totally not confusing
@Serializable
data class ListFromGraph(
    val cursor: String? = null,
    val list: GraphList,
    val items: List<GraphListItem>
)

