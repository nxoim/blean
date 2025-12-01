package com.nxoim.blean.client

import co.touchlab.kermit.Logger
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.andThen
import com.github.michaelbull.result.map
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import com.nxoim.blean.api.api.oauthStuff.utils.getEntrypointFromAuthorizationServer
import com.nxoim.blean.bskyPrimitives.AccountIdentificator
import com.nxoim.blean.models.LoggedInUserBasicDetails
import com.nxoim.blean.platformCredentialsManagement.OAuthCredentials
import com.nxoim.blean.platformCredentialsManagement.PlatformCredentials
import com.nxoim.blean.repos.LoggedInUsersRepository
import com.nxoim.blean.repos.OAuthAuthenticationAttemptRepository
import kotlinx.coroutines.flow.firstOrNull

private const val logTag = "AuthenticationManager"

class AuthenticationManager(
    private val rootUserRepository: LoggedInUsersRepository,
    private val oauthAuthAttemptRepository: OAuthAuthenticationAttemptRepository,
    private val atprotoOAuthClient: ATProtoOAuthClient,
    private val logger: Logger,
) {
    suspend fun beginOauthAuthorization(
        authorizationServerEntrypoint: String
    ): Result<String, ATProtoOAuthClientError> {
        oauthAuthAttemptRepository.clearAll()

        return atprotoOAuthClient.buildWebViewAuthorizationUrl(authorizationServerEntrypoint)
            .onSuccess {
                oauthAuthAttemptRepository.saveOrUpdateStateToken(it.stateToken)
                oauthAuthAttemptRepository.saveOrUpdateClientId(it.clientId)
                oauthAuthAttemptRepository.saveOrUpdateAuthorizationServer(it.authorizationServer)
                oauthAuthAttemptRepository.saveOrUpdateCodeVerifier(it.codeVerifier)
                oauthAuthAttemptRepository.saveOrUpdatePushedAuthorizationRequestDpopNonce(it.pushedAuthorizationRequestDpopNonce)
            }
            .map { it.link }
    }

    suspend fun continueOauthAuthorization(
        webviewCallbackUrl: String,
    ): Result<LoggedInUserBasicDetails, ContinueOAuthAuthorizationError> {
        val missing = Err(ContinueOAuthAuthorizationError.DataWasMissing)
        val stateToken = oauthAuthAttemptRepository.getStateToken()
            .firstOrNull()
            ?: return missing
        val dpopNonce = oauthAuthAttemptRepository.getPushedAuthorizationRequestDpopNonce()
            .firstOrNull()
            ?: return missing
        val codeVerifier = oauthAuthAttemptRepository.getCodeVerifier()
            .firstOrNull()
            ?: return missing
        val authorizationServer = oauthAuthAttemptRepository.getAuthorizationServer()
            .firstOrNull()
            ?: return missing
        val clientId = oauthAuthAttemptRepository.getClientId()
            .firstOrNull()
            ?: return missing

        return atprotoOAuthClient.performFirstTokenRequest(
            webviewCallbackUrl,
            stateToken,
            dpopNonce,
            codeVerifier,
            getEntrypointFromAuthorizationServer(authorizationServer)
        )
            .mapError { ContinueOAuthAuthorizationError.RequestError(it) }
            .onSuccess { firstAuthorizationData ->
                rootUserRepository.saveOrUpdateOAuthCredentials(
                    did = firstAuthorizationData.userBasicDetails.did,
                    accessToken = firstAuthorizationData.tokenData.accessToken,
                    refreshToken = firstAuthorizationData.tokenData.refreshToken,
                    authorizationServerUrl = authorizationServer,
                    clientId = clientId,
                    keyPair = firstAuthorizationData.keyPair,
                    label = firstAuthorizationData.userBasicDetails.displayName
                        ?: firstAuthorizationData.userBasicDetails.handle.toString()
                )

                rootUserRepository.saveOrUpdateLoggedInUser(firstAuthorizationData.userBasicDetails)
            }
            .map { it.userBasicDetails }
            .also { oauthAuthAttemptRepository.clearAll() }
    }

    suspend fun refreshOauthClientTokens(
        did: AccountIdentificator.Did,
    ): Result<OAuthCredentials, RefreshTokenOAuthError> {
        logger.v(tag = logTag) { "refreshing tokens for $did" }
        val storedCredentials = rootUserRepository
            .getCredentials(did)
            .firstOrNull()
            ?: return Err(RefreshTokenOAuthError.NecessaryDataMissing)

        return storedCredentials
            .mapError {
                RefreshTokenOAuthError.CantObtainAssembledCredentialsFromRepo
            }
            // return early because unrecoverable
            .onFailure { return Err(it) }
            .andThen {
                when (it) {
                    is PlatformCredentials.AccessJwt -> Err(RefreshTokenOAuthError.Unknown("Why would credentials be access jwt? Oauth ones were expected"))
                    is PlatformCredentials.OAuth -> Ok(it.value)
                }
            }
            .andThen { storedCredentials ->
                atprotoOAuthClient.refreshTokens(
                    storedCredentials.refreshToken,
                    storedCredentials.keyPair,
                    authorizationServerEntrypoint =
                        getEntrypointFromAuthorizationServer(storedCredentials.authorizationServerUrl)
                )
                    .mapError { RefreshTokenOAuthError.RefreshError(it) as RefreshTokenOAuthError }
                    .onSuccess { logger.v(tag = logTag) { "refreshed successfully" } }
                    .onFailure { logger.e(tag = logTag) { "failed to refresh tokens. $it" } }
                    .map { newTokens ->
                        OAuthCredentials(
                            did,
                            newTokens.accessToken,
                            newTokens.refreshToken ?: storedCredentials.refreshToken,
                            storedCredentials.authorizationServerUrl,
                            storedCredentials.clientId,
                            storedCredentials.keyPair
                        )
                    }
            }
    }
}