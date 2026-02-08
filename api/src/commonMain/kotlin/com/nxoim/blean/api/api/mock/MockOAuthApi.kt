package com.nxoim.blean.api.api.mock

import com.nxoim.blean.api.api.OAuthApi
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

open class _MockOAuthApi : OAuthApi {
    override suspend fun getClientMetadata(fullUrl: String): RequestResult<ClientMetadata> {
        TODO("Not yet implemented")
    }

    override suspend fun getPushedAuthorizationRequest(
        pushedAuthRequestFullUrl: String,
        responseType: String,
        clientId: String,
        redirectUri: String,
        state: OAuthStateToken,
        scopes: List<String>,
        codeChallenge: OAuthCodeChallenge?,
        loginHint: String?,
        clientAssertion: String?
    ): OAuthRelatedResponseResult<ResponseWithDPoPNonce<PushedAuthorizationRequest>> {
        TODO("Not yet implemented")
    }

    override suspend fun getInitialTokens(
        redirectUrl: String,
        codeVerifier: OAuthCodeVerifier,
        code: String,
        tokenEndpointFullUrl: String,
        clientId: String,
        dpopProof: DPoPProof
    ): OAuthRelatedResponseResult<ResponseWithDPoPNonce<OAuthTokenData>> {
        TODO("Not yet implemented")
    }

    override suspend fun getRefreshedToken(
        clientId: String,
        refreshToken: String,
        tokenEndpointFullUrl: String,
        dpopProof: DPoPProof
    ): OAuthRelatedResponseResult<OAuthTokenData> {
        TODO("Not yet implemented")
    }

    override suspend fun getProtectedResource(pdsServerUrl: String): RequestResult<OAuthProtectedResource> {
        TODO("Not yet implemented")
    }

    override suspend fun getAuthorizationServerMetadata(entrypoint: String): RequestResult<AuthorizationServerMetadata> {
        TODO("Not yet implemented")
    }
}