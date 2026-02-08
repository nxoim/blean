package com.nxoim.blean.api.api.mock

import com.nxoim.blean.api.api.RepoApi
import com.nxoim.blean.api.models.repo.ApplyWritesResponse
import com.nxoim.blean.api.models.repo.CreateRecordResponse
import com.nxoim.blean.api.models.repo.DeleteRecordResponse
import com.nxoim.blean.api.models.repo.GetRecordResponse
import com.nxoim.blean.api.models.repo.RecordWrite
import com.nxoim.blean.api.models.repo.RepoCollectionNSID
import com.nxoim.blean.api.models.repo.UploadRecord
import com.nxoim.blean.api.utils.AuthenticationContext
import com.nxoim.blean.api.utils.RequestResult
import com.nxoim.blean.bskyPrimitives.Cid
import com.nxoim.blean.bskyPrimitives.Did
import com.nxoim.blean.bskyPrimitives.RecordKey

open class _MockRepoApi : RepoApi {
    override suspend fun createRecord(
        authenticationContext: AuthenticationContext,
        repo: Did,
        collection: RepoCollectionNSID,
        record: UploadRecord,
        rkey: RecordKey?,
        validate: Boolean?,
        swapCommit: Cid?,
        atprotoAcceptLabelers: List<Did>?
    ): RequestResult<CreateRecordResponse> {
        TODO("Not yet implemented")
    }

    override suspend fun deleteRecord(
        authenticationContext: AuthenticationContext,
        repo: Did,
        collection: RepoCollectionNSID,
        rkey: RecordKey,
        swapRecord: Cid?,
        swapCommit: Cid?,
        atprotoAcceptLabelers: List<Did>?
    ): RequestResult<DeleteRecordResponse> {
        TODO("Not yet implemented")
    }

    override suspend fun applyWrites(
        authenticationContext: AuthenticationContext,
        repo: Did,
        writes: List<RecordWrite>,
        validate: Boolean?,
        swapCommit: Cid?,
        atprotoAcceptLabelers: List<Did>?
    ): RequestResult<ApplyWritesResponse> {
        TODO("Not yet implemented")
    }

    override suspend fun getRecord(
        authenticationContext: AuthenticationContext,
        repo: Did,
        collection: RepoCollectionNSID,
        recordKey: RecordKey,
        cid: Cid?
    ): RequestResult<GetRecordResponse> {
        TODO("Not yet implemented")
    }
}