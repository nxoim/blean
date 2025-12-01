package com.nxoim.blean.api.models.feed

import com.nxoim.blean.bskyPrimitives.AtUri
import com.nxoim.blean.bskyPrimitives.Cid
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Reply(
    val root: PostView,
    val parent: PostView,
    val grandparentAuthor: ProfileInfo? = null
)

@Serializable
data class ReplyRecordStrongReferences(
    val root: RepoStrongReference,
    val parent: RepoStrongReference,
//    val grandparentAuthor: Author? = null
)

// TODO move to more common place or to repo models
@Serializable
@SerialName("com.atproto.repo.strongRef")
data class RepoStrongReference(
    val cid: Cid,
    val uri: AtUri
)