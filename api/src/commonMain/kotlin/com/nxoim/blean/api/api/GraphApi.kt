package com.nxoim.blean.api.api

import com.nxoim.blean.api.models.graph.ListFromGraph
import com.nxoim.blean.api.utils.AuthenticationContext
import com.nxoim.blean.api.utils.LimitUpToHundred
import com.nxoim.blean.api.utils.RequestResult
import com.nxoim.blean.api.utils.appendAcceptApplicationJson
import com.nxoim.blean.api.utils.performAuthorizedRequest
import com.nxoim.blean.api.utils.runRequestCatching
import com.nxoim.blean.bskyPrimitives.AtUri
import io.ktor.client.HttpClient
import io.ktor.http.HttpMethod

class GraphApi(
    private val httpClient: HttpClient
) {
    suspend fun getList(
        authenticationContext: AuthenticationContext,
        list: AtUri,
        limit: LimitUpToHundred? = LimitUpToHundred(50),
        cursor: String? = null
    ): RequestResult<ListFromGraph> = runRequestCatching {
        httpClient.performAuthorizedRequest(
            authenticationContext,
            HttpMethod.Get,
            "/xrpc/app.bsky.graph.getList",
            headers = {
                appendAcceptApplicationJson()
            },
            parameters = {
                append("list", list.toString())
                if (limit != null) append("limit", limit.value.toString())
                if (cursor != null) append("cursor", cursor)
            }
        )
    }
}