package com.nxoim.blean.api.models.repo;

import com.nxoim.blean.bskyPrimitives.RecordKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface RecordWrite {
    @ConsistentCopyVisibility
    @Serializable
    @SerialName("com.atproto.repo.applyWrites#create")
    data class Create private constructor(
        val collection: RepoCollectionNSID,
        val value: RecordWriteContent,
        val rkey: String? = null,
    ) : RecordWrite {
        constructor(
            collection: RepoCollectionNSID,
            value: RecordWriteContent,
            rkey: RecordKey? = null,
        ) : this(
            collection = collection,
            value = value,
            rkey = rkey?.toString(),
        )
    }

    @ConsistentCopyVisibility
    @Serializable
    @SerialName("com.atproto.repo.applyWrites#update")
    data class Update private constructor(
        val collection: RepoCollectionNSID,
        val rkey: String,
        val value: RecordWriteContent,
    ) : RecordWrite {
        constructor(
            collection: RepoCollectionNSID,
            rkey: RecordKey,
            value: RecordWriteContent,
        ) : this(
            collection = collection,
            rkey = rkey.toString(),
            value = value,
        )
    }

    @ConsistentCopyVisibility
    @Serializable
    @SerialName("com.atproto.repo.applyWrites#delete")
    data class Delete private constructor (
        val collection: RepoCollectionNSID,
        val rkey: String,
    ) : RecordWrite {
        constructor(
            collection: RepoCollectionNSID,
            rkey: RecordKey,
        ) : this(
            collection = collection,
            rkey = rkey.toString(),
        )
    }
}