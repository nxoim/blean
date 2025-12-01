package com.nxoim.blean.api.models.feed

import kotlinx.serialization.Serializable

@Serializable
data class FeedGenerators(val feeds: List<FeedDetails>)