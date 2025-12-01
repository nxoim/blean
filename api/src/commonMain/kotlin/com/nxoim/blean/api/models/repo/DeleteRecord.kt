package com.nxoim.blean.api.models.repo

import com.nxoim.blean.bskyPrimitives.Cid
import com.nxoim.blean.bskyPrimitives.Did
import com.nxoim.blean.bskyPrimitives.RecordKey
import kotlinx.serialization.Serializable

@ConsistentCopyVisibility
@Serializable
data class DeleteRecord private constructor(
    val repo: Did,
    val collection: RepoCollectionNSID,
    val rkey: String,
    val swapRecord: Cid? = null,
    val swapCommit: Cid? = null
) {
    constructor(
        repo: Did,
        collection: RepoCollectionNSID,
        rkey: RecordKey,
        swapRecord: Cid? = null,
        swapCommit: Cid? = null
    ) : this(
        repo,
        collection,
        rkey.toString(),
        swapCommit
    )
}