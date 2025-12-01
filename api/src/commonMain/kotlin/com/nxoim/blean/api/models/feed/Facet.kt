package com.nxoim.blean.api.models.feed

import kotlinx.serialization.Serializable

@Serializable
data class Facet(
    val index: FacetIndex,
    val features: List<FacetFeature>
)