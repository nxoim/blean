package com.nxoim.blean.api.api

import com.nxoim.blean.api.BleanKtorClient
import com.nxoim.blean.api.api.oauthStuff.models.DPoPProof
import com.nxoim.blean.api.api.oauthStuff.models.OAuthCodeChallenge
import com.nxoim.blean.api.api.oauthStuff.models.OAuthCodeVerifier
import com.nxoim.blean.api.api.oauthStuff.models.OAuthStateToken
import com.nxoim.blean.api.api.oauthStuff.models.ResponseWithDPoPNonce
import com.nxoim.blean.api.models.oauth.AuthorizationServerMetadata
import com.nxoim.blean.api.models.oauth.ClientMetadata
import com.nxoim.blean.api.models.oauth.OAuthProtectedResource
import com.nxoim.blean.api.models.oauth.OAuthTokenData
import com.nxoim.blean.api.models.oauth.PushedAuthorizationRequest
import com.nxoim.blean.api.utils.OAuthRelatedResponseResult
import com.nxoim.blean.api.utils.RequestResult
import com.nxoim.blean.api.utils.appendAcceptApplicationJson
import com.nxoim.blean.api.utils.appendContentTypeApplicationJson
import com.nxoim.blean.api.utils.appendXWWWFormUrlEncoded
import com.nxoim.blean.api.utils.formDataContentFromParameters
import com.nxoim.blean.api.utils.get
import com.nxoim.blean.api.utils.post
import com.nxoim.blean.api.utils.runOauthedRequestCatching
import com.nxoim.blean.api.utils.runOauthedRequestWithDpopNonceCatching
import com.nxoim.blean.api.utils.runRequestCatching
import io.ktor.http.headers
import kotlin.io.encoding.ExperimentalEncodingApi

class OAuthApi(
    private val client: BleanKtorClient
) {
    private val httpClient = client.value
    suspend fun getClientMetadata(fullUrl: String): RequestResult<ClientMetadata> =
        runRequestCatching {
            httpClient.get(
                url = fullUrl,
                headers = headers { appendAcceptApplicationJson() }
            )
        }

    // /push/authorize
    suspend fun getPushedAuthorizationRequest(
        pushedAuthRequestFullUrl: String,
        responseType: String,
        clientId: String,
        redirectUri: String,
        state: OAuthStateToken,
        scopes: List<String>,
        codeChallenge: OAuthCodeChallenge? = null,
        loginHint: String? = null,
        clientAssertion: String? = null,
    ) = runOauthedRequestWithDpopNonceCatching<PushedAuthorizationRequest> {
        if (scopes.none { it == "atproto" }) error("Scopes for PAR must contain atproto")

        httpClient.post(
            pushedAuthRequestFullUrl,
            headers = headers {
                appendAcceptApplicationJson()
                appendXWWWFormUrlEncoded()
            },
            body = formDataContentFromParameters {
                append("client_id", clientId)
                append("redirect_uri", redirectUri)
                append("response_type", responseType)
                append("scope", scopes.joinToString(" "))
                codeChallenge?.let {
                    append("code_challenge", it.value.toString())
                    append("code_challenge_method", it.method)
                }
                loginHint?.let { append("login_hint", it) }
                append("state", state.value)
                clientAssertion?.let { append("client_assertion", it) }
            }
        )
    }

    @OptIn(ExperimentalEncodingApi::class)
    suspend fun getInitialTokens(
        redirectUrl: String,
        codeVerifier: OAuthCodeVerifier,
        code: String,
        tokenEndpointFullUrl: String,
        clientId: String,
        dpopProof: DPoPProof,
    ): OAuthRelatedResponseResult<ResponseWithDPoPNonce<OAuthTokenData>> =
        runOauthedRequestWithDpopNonceCatching<OAuthTokenData> {
            httpClient.post(
                tokenEndpointFullUrl,
                headers = headers {
                    appendAcceptApplicationJson()
                    appendXWWWFormUrlEncoded()
                    append("DPoP", dpopProof.toString())
                },
                body = formDataContentFromParameters {
                    append("grant_type", "authorization_code")
                    append("client_id", clientId)
                    append("redirect_uri", redirectUrl)
                    append("code", code)
                    append("code_verifier", codeVerifier.toString())
                }
            )
        }

    suspend fun getRefreshedToken(
        clientId: String,
        refreshToken: String,
        tokenEndpointFullUrl: String,
        dpopProof: DPoPProof
    ): OAuthRelatedResponseResult<OAuthTokenData> = runOauthedRequestCatching {
        httpClient.post(
            tokenEndpointFullUrl,
            headers = headers {
                appendAcceptApplicationJson()
                appendXWWWFormUrlEncoded()
                append("DPoP", dpopProof.toString())
            },
            body = formDataContentFromParameters {
                append("client_id", clientId)
                append("grant_type", "refresh_token")
                append("refresh_token", refreshToken)
            }
        )
    }

    suspend fun getProtectedResource(
        pdsServerUrl: String
    ): RequestResult<OAuthProtectedResource> = runRequestCatching {
        httpClient.get(
            "$pdsServerUrl/.well-known/oauth-protected-resource",
            headers {
                appendAcceptApplicationJson()
                appendContentTypeApplicationJson()
            }
        )
    }

    suspend fun getAuthorizationServerMetadata(
        entrypoint: String
    ): RequestResult<AuthorizationServerMetadata> =
        runRequestCatching {
            httpClient.get(
                "$entrypoint/.well-known/oauth-authorization-server",
                headers {
                    appendAcceptApplicationJson()
                    appendContentTypeApplicationJson()
                }
            )
        }
}

// todo client assertion

