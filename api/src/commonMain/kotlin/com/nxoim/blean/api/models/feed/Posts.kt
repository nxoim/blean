package com.nxoim.blean.api.models.feed

import kotlinx.serialization.Serializable

@Serializable
data class Posts(val posts: List<PostView.Visible>)