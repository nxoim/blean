package com.nxoim.blean.api.models.repo

import com.nxoim.blean.api.models.commonParts.TimestampISO8601
import com.nxoim.blean.bskyPrimitives.AtUri
import com.nxoim.blean.bskyPrimitives.Cid
import com.nxoim.blean.bskyPrimitives.Did
import com.nxoim.blean.bskyPrimitives.RecordKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@ConsistentCopyVisibility
@Serializable
data class CreateRecord private constructor(
    val repo: Did,
    val collection: RepoCollectionNSID,
    val record: UploadRecord,
    val rkey: String? = null,
    val validate: Boolean? = null,
    val swapCommit: Cid? = null
) {
    constructor(
        repo: Did,
        collection: RepoCollectionNSID,
        record: UploadRecord,
        rkey: RecordKey? = null,
        validate: Boolean? = null,
        swapCommit: Cid? = null
    ) : this(
        repo,
        collection,
        record,
        rkey?.toString(),
        validate,
        swapCommit
    )
}



@Serializable
sealed interface UploadRecord {
    @Serializable
    @SerialName("app.bsky.feed.like")
    data class Like(
        val subject: UploadRecordSubject,
        val createdAt: TimestampISO8601,
    ) : UploadRecord
}

@Serializable
data class UploadRecordSubject(
    val uri: AtUri,
    val cid: Cid
)