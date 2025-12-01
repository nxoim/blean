@file:OptIn(ExperimentalTime::class)

package com.nxoim.blean.client

import co.touchlab.kermit.Logger
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.andThen
import com.github.michaelbull.result.andThenRecover
import com.github.michaelbull.result.map
import com.github.michaelbull.result.mapError
import com.nxoim.blean.api.api.AccountApi
import com.nxoim.blean.api.api.OAuthApi
import com.nxoim.blean.api.api.oauthStuff.models.AuthorizationFromWebviewResult
import com.nxoim.blean.api.api.oauthStuff.models.DPoPProof
import com.nxoim.blean.api.api.oauthStuff.models.ECDSAP256InBase64Keys
import com.nxoim.blean.api.api.oauthStuff.models.OAuthCodeVerifier
import com.nxoim.blean.api.api.oauthStuff.models.OAuthStateToken
import com.nxoim.blean.api.api.oauthStuff.utils.PDSRequestDPoPAuthenticationContext
import com.nxoim.blean.api.api.oauthStuff.utils.buildAuthorizationWebviewUrl
import com.nxoim.blean.api.api.oauthStuff.utils.generate
import com.nxoim.blean.api.api.oauthStuff.utils.generateForTokenRequest
import com.nxoim.blean.api.api.oauthStuff.utils.parseAuthorizationWebviewUrlResult
import com.nxoim.blean.api.api.oauthStuff.utils.signWithECDSAP256InSHA256
import com.nxoim.blean.api.api.oauthStuff.utils.toCodeChallenge
import com.nxoim.blean.api.api.oauthStuff.utils.validateOrThrow
import com.nxoim.blean.api.api.oauthStuff.utils.validateSafetyOrThrow
import com.nxoim.blean.api.api.oauthStuff.utils.validateWebviewAuthResultOrThrow
import com.nxoim.blean.api.api.oauthStuff.utils.verifySafetyOrThrow
import com.nxoim.blean.api.models.oauth.AuthorizationServerMetadata
import com.nxoim.blean.api.models.oauth.ClientMetadata
import com.nxoim.blean.api.models.oauth.OAuthTokenData
import com.nxoim.blean.api.models.oauth.scopes
import com.nxoim.blean.api.utils.AuthenticationContext
import com.nxoim.blean.api.utils.AuthenticationMethod
import com.nxoim.blean.api.utils.OAuthRequestError
import com.nxoim.blean.bskyPrimitives.AccountIdentificator
import com.nxoim.blean.client.ATProtoOAuthClientError.ConnectionIssues
import com.nxoim.blean.client.ATProtoOAuthClientError.Other
import com.nxoim.blean.client.ATProtoOAuthClientError.ServerError
import com.nxoim.blean.commonThingsDumpster.RequestError
import com.nxoim.blean.models.LoggedInUserBasicDetails
import kotlin.jvm.JvmInline
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

private const val logTag = "ATProtoOAuthClient"

class ATProtoOAuthClient(
    private val oauthApi: OAuthApi,
    private val accountApi: AccountApi,
    private val clientId: String,
    private val logger: Logger = Logger
) {
    suspend fun buildWebViewAuthorizationUrl(
        authorizationServerEntrypoint: String,
    ): Result<WebViewAuthorizationData, ATProtoOAuthClientError> =
        getAndValidateClientMetadata()
            .andThen { clientMetadata ->
                getAndValidateAuthorizationServerMetadata(authorizationServerEntrypoint)
                    .andThen { authorizationServerMetadata ->
                        val newStateToken = OAuthStateToken.generate()
                        val newCodeVerifier = OAuthCodeVerifier.generate()

                        oauthApi.getPushedAuthorizationRequest(
                            pushedAuthRequestFullUrl = authorizationServerMetadata.pushedAuthorizationRequestEndpoint,
                            // response types and redirect uris have been validated
                            responseType = clientMetadata.responseTypes.first(),
                            clientId = clientMetadata.clientId,
                            redirectUri = clientMetadata.redirectUris.first(),
                            scopes = clientMetadata.scopes,
                            state = newStateToken,
                            codeChallenge = newCodeVerifier.toCodeChallenge(),
                        )
                            .mapErrorsToConsumableByClientUsers("Pushed authorization request")
                            .map { pushedAuthorizationRequest ->
                                WebViewAuthorizationData(
                                    buildAuthorizationWebviewUrl(
                                        authorizationServerMetadata.authorizationEndpoint,
                                        clientMetadata.clientId,
                                        pushedAuthorizationRequest.response.requestUri
                                    ),
                                    newStateToken,
                                    newCodeVerifier,
                                    clientMetadata.clientId,
                                    authorizationServerMetadata.authorizationEndpoint,
                                    pushedAuthorizationRequest.dpopNonce
                                )
                            }
                    }
            }

    suspend fun performFirstTokenRequest(
        webviewCallbackUrl: String,
        stateToken: OAuthStateToken,
        pushedAuthorizationRequestDpopNonce: String,
        codeVerifier: OAuthCodeVerifier,
        authorizationServerEntrypoint: String
    ): Result<FirstAuthorizationData, ATProtoOAuthClientError> =
        getAndValidateClientMetadata()
            .andThen { clientMetadata ->
                getAndValidateAuthorizationServerMetadata(authorizationServerEntrypoint)
                    .andThen { authorizationServerMetadata ->
                        val webviewResult = try {
                            Ok(
                                validateWebviewAuthResultOrThrow(
                                    parseAuthorizationWebviewUrlResult(webviewCallbackUrl),
                                    authorizationServerMetadata,
                                    stateToken
                                )
                            )
                        } catch (_: IllegalStateException) {
                            Err(ATProtoOAuthClientError.ValidationError("Authorization result"))
                        }

                        webviewResult.andThen {
                            performFirstAuthRequest(
                                clientMetadata,
                                authorizationServerMetadata,
                                pushedAuthorizationRequestDpopNonce,
                                it,
                                codeVerifier
                            )
                        }
                    }
            }

    suspend fun refreshTokens(
        refreshToken: String,
        keyPair: ECDSAP256InBase64Keys,
        pushedAuthorizationRequestDpopNonce: String? = null,
        authorizationServerEntrypoint: String
    ): Result<OAuthTokenData, ATProtoOAuthClientError> =
        getAndValidateClientMetadata()
            .andThen { clientMetadata ->
                getAndValidateAuthorizationServerMetadata(authorizationServerEntrypoint)
                    .andThen { authorizationServerMetadata ->
                        performRefreshRequest(
                            clientMetadata = clientMetadata,
                            authorizationServerMetadata = authorizationServerMetadata,
                            tokenEndpointDpopNonce =
                                pushedAuthorizationRequestDpopNonce ?: "initial",
                            refreshToken = refreshToken,
                            keyPair = keyPair
                        )
                    }
            }

    private suspend inline fun performFirstAuthRequest(
        clientMetadata: ClientMetadata,
        authorizationServerMetadata: AuthorizationServerMetadata,
        pushedAuthorizationRequestDpopNonce: String,
        validatedParsedAuthResult: AuthorizationFromWebviewResult.Success,
        codeVerifier: OAuthCodeVerifier,
    ): Result<FirstAuthorizationData, ATProtoOAuthClientError> {
        val newKeyPair = ECDSAP256InBase64Keys.generate()
        var actualDpopNonceToBeUsedInRequest = pushedAuthorizationRequestDpopNonce

        val request = suspend {
            val dpopProof = DPoPProof.generateForTokenRequest(
                clientId = clientMetadata.clientId,
                tokenEndpoint = authorizationServerMetadata.tokenEndpoint,
                tokenEndpointDpopNonce = actualDpopNonceToBeUsedInRequest,
                publicKey = newKeyPair.extractedXYFromPublicKey,
                timeEpochSeconds = Clock.System.now().epochSeconds,
                sign = { it.signWithECDSAP256InSHA256(newKeyPair.private) }
            )

            oauthApi.getInitialTokens(
                redirectUrl = clientMetadata.redirectUris.first(),
                code = validatedParsedAuthResult.code,
                codeVerifier = codeVerifier,
                clientId = clientMetadata.clientId,
                tokenEndpointFullUrl = authorizationServerMetadata.tokenEndpoint,
                dpopProof = dpopProof
            )
        }

        return request()
            // user is supposed to make it in time until dpop expiration, but uncomment if necessary
//            .andThenRecover {
//                when (it) {
//                    is RequestError.Other -> {
//                        when (val otherError = it.value) {
//                            is OAuthRequestError.UseDpopNonce -> {
//                                val newDpopNonce = otherError.nonce
//
//                                actualDpopNonceToBeUsedInRequest = newDpopNonce
//                                println("Dpop nonce error. Attempting again")
//                                request()
//                            }
//                        }
//                    }
//
//                    // dont recover from others
//                    else -> Err(it)
//                }
//            }
            .mapErrorsToConsumableByClientUsers("First token request")
            .andThen {
                try {
                    Ok(it.response.validateSafetyOrThrow(authorizationServerMetadata.issuer))
                } catch (_: Exception) {
                    Err(ATProtoOAuthClientError.ValidationError("First token request"))
                }
            }
            .andThen {
                try {
                    // get user details to provide for saving which also
                    // will validate the token
                    val authMethod = AuthenticationContext.OAuthContextForPDS(
                        PDSRequestDPoPAuthenticationContext(
                            onCurrentTimeEpochSeconds = { Clock.System.now().epochSeconds },
                            beforeRequestHappens = { /* nothing */ },
                            onInvalidAuthToken = {
                                // TODO this crashes all verifications somehow. idk why token is invalud??
                                // since this is in a try catch block
                                error("Authentication was successful, but verification of the authentication token has failed. Token was invalid upon user data retrieval")
                            },
                            onRequestAuthMethod = {
                                AuthenticationMethod.OAuth(
                                    it.accessToken,
                                    newKeyPair
                                )
                            }
                        )
                    )

                    accountApi
                        .getProfile(authMethod, AccountIdentificator.Did(it.sub))
                        .mapErrorsToConsumableByClientUsers("Authorized request with first tokens")
                        .andThen { userProfile ->
                            Ok(
                                FirstAuthorizationData(
                                    keyPair = newKeyPair,
                                    userBasicDetails = LoggedInUserBasicDetails(
                                        userProfile.did,
                                        userProfile.handle,
                                        userProfile.displayName,
                                        userProfile.avatar
                                    ),
                                    tokenData = it
                                )
                            )
                        }
                } catch (e: IllegalStateException) { // anything else should crash
                    Err(
                        ATProtoOAuthClientError.ValidationError(
                            "Authorized request with first tokens. ${e.message}"
                        )
                    )
                }
            }
    }

    private suspend inline fun performRefreshRequest(
        clientMetadata: ClientMetadata,
        authorizationServerMetadata: AuthorizationServerMetadata,
        tokenEndpointDpopNonce: String,
        refreshToken: String,
        keyPair: ECDSAP256InBase64Keys
    ): Result<OAuthTokenData, ATProtoOAuthClientError> {
        val request: suspend (dpop: String) -> Result<OAuthTokenData, RequestError<OAuthRequestError>> = { dpop ->
            val dpopProof = DPoPProof.generateForTokenRequest(
                clientId = clientMetadata.clientId,
                tokenEndpoint = authorizationServerMetadata.tokenEndpoint,
                tokenEndpointDpopNonce = dpop,
                publicKey = keyPair.extractedXYFromPublicKey,
                timeEpochSeconds = Clock.System.now().epochSeconds,
                sign = { it.signWithECDSAP256InSHA256(keyPair.private) }
            )

            oauthApi.getRefreshedToken(
                clientId = clientMetadata.clientId,
                refreshToken = refreshToken,
                tokenEndpointFullUrl = authorizationServerMetadata.tokenEndpoint,
                dpopProof = dpopProof
            )
        }

        return request(tokenEndpointDpopNonce)
            .andThenRecover {
                when (it) {
                    is RequestError.Other -> {
                        when (val oauthRequestError = it.value) {
                            is OAuthRequestError.UseDPopNonce -> {
                                logger.i(tag = logTag) { "Dpop nonce error. Attempting again" }
                                request(oauthRequestError.nonce)
                            }

                            is OAuthRequestError.InvalidGrant -> return Err(
                                ATProtoOAuthClientError.UnrecoverableTokenError(
                                    "Refreshed token request"
                                )
                            )
                        }
                    }

                    // dont recover from others
                    else -> Err(it)
                }
            }
            .mapErrorsToConsumableByClientUsers(
                "Refreshed token request",
                otherError = OAuthRequestError::toATProtoOAuthClientError
            )
            .andThen {
                try {
                    Ok(it.validateSafetyOrThrow(authorizationServerMetadata.issuer))
                } catch (_: Exception) {
                    Err(ATProtoOAuthClientError.ValidationError("Refreshed token request"))
                }
            }
    }

    private suspend fun getAndValidateClientMetadata() =
        oauthApi.getClientMetadata(clientId)
            .mapErrorsToConsumableByClientUsers("Client metadata")
            .andThen { clientMetadata ->
                try {
                    Ok(clientMetadata.validateOrThrow(clientId))
                } catch (_: Exception) {
                    Err(
                        ATProtoOAuthClientError.ValidationError(
                            "Client metadata"
                        )
                    )
                }
            }

    private suspend fun getAndValidateAuthorizationServerMetadata(entrypoint: String) =
        oauthApi.getAuthorizationServerMetadata(entrypoint)
            .mapErrorsToConsumableByClientUsers("Authorization server metadata")
            .andThen {
                try {
                    Ok(it.verifySafetyOrThrow())
                } catch (_: Exception) {
                    Err(
                        ATProtoOAuthClientError.ValidationError(
                            "Authorization server metadata"
                        )
                    )
                }
            }
}

private fun <T, E> Result<T, RequestError<E>>.mapErrorsToConsumableByClientUsers(
    details: String,
    otherError: (E) -> ATProtoOAuthClientError = { Other(it.toString()) }
): Result<T, ATProtoOAuthClientError> = mapError {
    when (it) {
        is RequestError.Other -> otherError(it.value)

        is RequestError.Http -> ServerError(
            it.code,
            it.description,
            it.responseBody,
            details
        )

        is RequestError.Internal -> ATProtoOAuthClientError.Internal(
            "$details \n${it.exception.stackTraceToString()}"
        )

        is RequestError.UnresolvedAddress,
        is RequestError.CantConnectToInternet,
        is RequestError.Timeout -> ConnectionIssues(details)
    }
}

private fun OAuthRequestError.toATProtoOAuthClientError() =
    ATProtoOAuthClientError.UnrecoverableTokenError(this.toString())

data class WebViewAuthorizationData(
    val link: String,
    val stateToken: OAuthStateToken,
    val codeVerifier: OAuthCodeVerifier,
    val clientId: String,
    val authorizationServer: String,
    val pushedAuthorizationRequestDpopNonce: String
)

sealed interface ATProtoOAuthClientError {
    val details: String

    @JvmInline
    value class ConnectionIssues(override val details: String) : ATProtoOAuthClientError

    data class ServerError(
        val code: Int,
        val description: String,
        val body: String?,
        override val details: String
    ) : ATProtoOAuthClientError

    @JvmInline
    value class Other(override val details: String) : ATProtoOAuthClientError

    @JvmInline
    value class Internal(override val details: String) : ATProtoOAuthClientError

    @JvmInline
    value class ValidationError(override val details: String) : ATProtoOAuthClientError

    @JvmInline
    value class UnrecoverableTokenError(override val details: String) : ATProtoOAuthClientError

    // todo handle  java.io.IOException: Software caused connection abort
    //  as ConnectionIssues
}

data class FirstAuthorizationData(
    val keyPair: ECDSAP256InBase64Keys,
    val userBasicDetails: LoggedInUserBasicDetails,
    val tokenData: OAuthTokenData
)