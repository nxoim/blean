package com.nxoim.blean.api.models.feed

import com.nxoim.blean.api.models.account.Chat
import com.nxoim.blean.api.models.commonParts.Label
import com.nxoim.blean.api.models.commonParts.ViewerState
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("app.bsky.actor.defs#profileAssociated")
data class ProfileAssociated(
    val lists: Int? = null,
    val feedGens: Int? = null,
    val starterPacks: Int? = null,
    val viewer: ViewerState? = null,
    val labels: List<Label>? = null,
    val chat: Chat? = null
)