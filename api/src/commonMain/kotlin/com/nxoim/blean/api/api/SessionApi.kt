package com.nxoim.blean.api.api

import com.nxoim.blean.api.models.session.CreatedSession
import com.nxoim.blean.api.models.session.Session
import com.nxoim.blean.api.utils.RequestResult
import com.nxoim.blean.api.utils.appendBearerToken
import com.nxoim.blean.api.utils.appendContentTypeApplicationJson
import com.nxoim.blean.api.utils.get
import com.nxoim.blean.api.utils.post
import com.nxoim.blean.api.utils.runRequestCatching
import io.ktor.client.HttpClient
import io.ktor.http.headers

fun SessionApi(httpClient: HttpClient): SessionApi = KtorSessionApi(httpClient)

interface SessionApi {
    suspend fun createNewSessionWithLoginPassword(
        identifier: String,
        password: String,
        authFactorToken: String? = null
    ): RequestResult<CreatedSession>

    /**
     * @param accessJwt access/bearer token
     */
    suspend fun getSession(accessJwt: String): RequestResult<Session>
}

////////////////////////////////////////////////////////////////////////////////////////////

private class KtorSessionApi(
    private val httpClient: HttpClient
) : SessionApi {
    override suspend fun createNewSessionWithLoginPassword(
        identifier: String,
        password: String,
        authFactorToken: String?
    ): RequestResult<CreatedSession> = runRequestCatching {
        httpClient.post(
            url = "/xrpc/com.atproto.server.createSession",
            headers = headers {
                appendContentTypeApplicationJson()
            },
            body = buildMap {
                put("identifier", identifier)
                put("password", password)
                if (authFactorToken != null) put("authFactorToken", authFactorToken)
            }
        )
    }

    override suspend fun getSession(
        accessJwt: String
    ): RequestResult<Session> = runRequestCatching {
        httpClient.get(
            "/xrpc/com.atproto.server.getSession",
            headers {
                appendContentTypeApplicationJson()
                appendBearerToken(accessJwt)
            }
        )
    }
}