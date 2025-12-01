package com.nxoim.blean.api.models.account

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

@Serializable
enum class ListPurpose {
    @JsonNames("app.bsky.graph.defs#modList")
    Mod,
    @JsonNames("app.bsky.graph.defs#curateList")
    Curate,
    @JsonNames("app.bsky.graph.defs#referenceList")
    Reference
}