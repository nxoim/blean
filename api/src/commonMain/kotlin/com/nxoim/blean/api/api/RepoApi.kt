package com.nxoim.blean.api.api

import com.nxoim.blean.api.models.repo.ApplyWrites
import com.nxoim.blean.api.models.repo.ApplyWritesResponse
import com.nxoim.blean.api.models.repo.CreateRecord
import com.nxoim.blean.api.models.repo.CreateRecordResponse
import com.nxoim.blean.api.models.repo.DeleteRecord
import com.nxoim.blean.api.models.repo.DeleteRecordResponse
import com.nxoim.blean.api.models.repo.GetRecordResponse
import com.nxoim.blean.api.models.repo.RecordWrite
import com.nxoim.blean.api.models.repo.RepoCollectionNSID
import com.nxoim.blean.api.models.repo.UploadRecord
import com.nxoim.blean.api.utils.AuthenticationContext
import com.nxoim.blean.api.utils.RequestResult
import com.nxoim.blean.api.utils.appendAtprotoAcceptLabelers
import com.nxoim.blean.api.utils.appendContentTypeApplicationJson
import com.nxoim.blean.api.utils.performAuthorizedRequest
import com.nxoim.blean.api.utils.runRequestCatching
import com.nxoim.blean.bskyPrimitives.Cid
import com.nxoim.blean.bskyPrimitives.Did
import com.nxoim.blean.bskyPrimitives.RecordKey
import io.ktor.client.HttpClient
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpMethod

class RepoApi(val httpClient: HttpClient) {
    suspend fun createRecord(
        authenticationContext: AuthenticationContext,
        repo: Did,
        collection: RepoCollectionNSID,
        record: UploadRecord,
        rkey: RecordKey? = null,
        validate: Boolean? = null,
        swapCommit: Cid? = null,
        atprotoAcceptLabelers: List<Did>? = null
    ): RequestResult<CreateRecordResponse> = runRequestCatching {
        httpClient.performAuthorizedRequest(
            authenticationContext,
            httpMethod = HttpMethod.Post,
            endpoint = "/xrpc/com.atproto.repo.createRecord",
            headers = {
                appendContentTypeApplicationJson()
                atprotoAcceptLabelers?.let { appendAtprotoAcceptLabelers(it) }
            },
            body = CreateRecord(
                repo = repo,
                collection = collection,
                record = record,
                rkey = rkey,
                validate = validate,
                swapCommit = swapCommit
            )
        )
    }

    suspend fun deleteRecord(
        authenticationContext: AuthenticationContext,
        repo: Did,
        collection: RepoCollectionNSID,
        rkey: RecordKey,
        swapRecord: Cid? = null,
        swapCommit: Cid? = null,
        atprotoAcceptLabelers: List<Did>? = null
    ): RequestResult<DeleteRecordResponse> = runRequestCatching {
        httpClient.performAuthorizedRequest(
            authenticationContext,
            httpMethod = HttpMethod.Post,
            endpoint = "/xrpc/com.atproto.repo.deleteRecord",
            headers = {
                appendContentTypeApplicationJson()
                atprotoAcceptLabelers?.let { appendAtprotoAcceptLabelers(it) }
            },
            body = DeleteRecord(
                repo = repo,
                collection = collection,
                rkey = rkey,
                swapRecord = swapRecord,
                swapCommit = swapCommit
            )
        )
    }

    suspend fun applyWrites(
        authenticationContext: AuthenticationContext,
        repo: Did,
        writes: List<RecordWrite>,
        validate: Boolean? = null,
        swapCommit: Cid? = null,
        atprotoAcceptLabelers: List<Did>? = null
    ): RequestResult<ApplyWritesResponse> = runRequestCatching {
        httpClient.performAuthorizedRequest(
            authenticationContext = authenticationContext,
            httpMethod = HttpMethod.Post,
            endpoint = "/xrpc/com.atproto.repo.applyWrites",
            headers = {
                appendContentTypeApplicationJson()
                atprotoAcceptLabelers?.let { appendAtprotoAcceptLabelers(it) }
            },
            body = ApplyWrites(
                repo = repo,
                writes = writes,
                validate = validate,
                swapCommit = swapCommit
            )
        )
    }

    // NOTE: could return RecordNotFound in error 400,
    // the return type might be adjusted
    suspend fun getRecord(
        authenticationContext: AuthenticationContext,
        repo: Did,
        collection: RepoCollectionNSID,
        recordKey: RecordKey,
        cid: Cid? = null
    ): RequestResult<GetRecordResponse> = runRequestCatching {
        httpClient.performAuthorizedRequest(
            authenticationContext = authenticationContext,
            httpMethod = HttpMethod.Get,
            endpoint = "/xrpc/com.atproto.repo.getRecord",
            headers = {
                appendContentTypeApplicationJson()
            },
            parameters = {
                append("repo", repo.toString())
                append("collection", collection.id)
                append("rkey", recordKey.toString())
                cid?.let { append("cid", it.toString())}
            }
        ).also {
            println(it.bodyAsText())
        }
    }
}