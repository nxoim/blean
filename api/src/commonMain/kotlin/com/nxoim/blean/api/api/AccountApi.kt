package com.nxoim.blean.api.api

import com.nxoim.blean.api.models.account.DidDocument
import com.nxoim.blean.api.models.account.Preferences
import com.nxoim.blean.api.models.account.Profile
import com.nxoim.blean.api.models.account.Profiles
import com.nxoim.blean.api.models.account.Suggestions
import com.nxoim.blean.api.utils.AuthenticationContext
import com.nxoim.blean.api.utils.LimitUpToHundred
import com.nxoim.blean.api.utils.RequestResult
import com.nxoim.blean.api.utils.appendAcceptApplicationJson
import com.nxoim.blean.api.utils.appendContentTypeApplicationJson
import com.nxoim.blean.api.utils.get
import com.nxoim.blean.api.utils.performAuthorizedRequest
import com.nxoim.blean.api.utils.runRequestCatching
import com.nxoim.blean.bskyPrimitives.AccountIdentificator
import com.nxoim.blean.bskyPrimitives.Did
import io.ktor.client.HttpClient
import io.ktor.http.HttpMethod
import io.ktor.http.headers
import io.ktor.http.parameters


class AccountApi(
    private val httpClient: HttpClient
) {
    suspend fun getProfile(
        authenticationContext: AuthenticationContext,
        targetHandleOrDid: AccountIdentificator
    ): RequestResult<Profile> = runRequestCatching {
        httpClient.performAuthorizedRequest(
            authenticationContext = authenticationContext,
            httpMethod = HttpMethod.Get,
            endpoint = "/xrpc/app.bsky.actor.getProfile",
            parameters = {
                append("actor", targetHandleOrDid.toString())
            }
        )
    }

    suspend fun getProfileUnauthorized(
        targetHandleOrDid: AccountIdentificator
    ): RequestResult<Profile> = runRequestCatching {
        httpClient.get(
            "/https://public.api.bsky.app/xrpc/app.bsky.actor.getProfile",
            headers { appendContentTypeApplicationJson() },
            parameters { append("actor", targetHandleOrDid.toString()) },
        )
    }

    suspend fun getProfiles(
        authenticationContext: AuthenticationContext,
        targetHandlesOrDids: List<AccountIdentificator>
    ): RequestResult<Profiles> = runRequestCatching {
        require(targetHandlesOrDids.size <= 25) {
            "profileHandlesOrDids must not contain more than 25 elements"
        }

        httpClient.performAuthorizedRequest(
            authenticationContext = authenticationContext,
            httpMethod = HttpMethod.Get,
            endpoint = "/xrpc/app.bsky.actor.getProfiles",
            parameters = {
                appendAll("actors", targetHandlesOrDids.map { it.toString() })
            }
        )
    }

    suspend fun getSuggestions(
        authenticationContext: AuthenticationContext,
        limit: LimitUpToHundred? = LimitUpToHundred(50),
        cursor: String? = null
    ): RequestResult<Suggestions> = runRequestCatching {
        httpClient.performAuthorizedRequest(
            authenticationContext = authenticationContext,
            httpMethod = HttpMethod.Get,
            endpoint = "/xrpc/app.bsky.actor.getSuggestions",
            headers = {
                appendContentTypeApplicationJson()
                appendAcceptApplicationJson()
            },
            parameters = {
                if (limit != null) append("limit", limit.value.toString())
                if (cursor != null) append("cursor", cursor)
            }
        )
    }

    suspend fun getPreferences(
        authenticationContext: AuthenticationContext
    ): RequestResult<Preferences> =
        runRequestCatching {
            httpClient.performAuthorizedRequest(
                authenticationContext = authenticationContext,
                httpMethod = HttpMethod.Get,
                endpoint = "/xrpc/app.bsky.actor.getPreferences",
                headers = {
                    appendContentTypeApplicationJson()
                }
            )
        }

    // TODO shouldnt be here i think
    suspend fun getDidDocumentFromPlcDirectory(did: Did): RequestResult<DidDocument> =
        runRequestCatching {
            httpClient.get(
                "https://plc.directory/$did",
                headers { appendAcceptApplicationJson() }
            )
        }

    // TODO
    suspend fun putPreferences(): Nothing = TODO()
    suspend fun searchActorsTypeahead(): Nothing = TODO()
    suspend fun searchActors(): Nothing = TODO()
}