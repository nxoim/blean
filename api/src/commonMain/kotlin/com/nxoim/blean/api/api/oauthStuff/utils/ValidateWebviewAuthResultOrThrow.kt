package com.nxoim.blean.api.api.oauthStuff.utils

import com.nxoim.blean.api.api.oauthStuff.models.AuthorizationFromWebviewResult
import com.nxoim.blean.api.api.oauthStuff.models.OAuthStateToken
import com.nxoim.blean.api.models.oauth.AuthorizationServerMetadata

/**
 * @throws IllegalStateException
 */
fun validateWebviewAuthResultOrThrow(
    parsedAuthResult: AuthorizationFromWebviewResult,
    authServer: AuthorizationServerMetadata,
    oauthStateToken: OAuthStateToken
): AuthorizationFromWebviewResult.Success {
    when (parsedAuthResult) {
        is AuthorizationFromWebviewResult.Error -> error("Authorization from webview, or parsing it's result, failed. Cannot proceed validation, \n$parsedAuthResult")

        is AuthorizationFromWebviewResult.Success -> {
            val isStateValid = parsedAuthResult.state == oauthStateToken.toString()
            val isIssValid = parsedAuthResult.iss == authServer.issuer

            if (!isIssValid && !isStateValid) error("iss(issuer) received in the authentication result does not match the issuer of the Authorization Server and state received in the authentication result does not match the locally generated state")
            if (!isStateValid) error("State received in the authentication result does not match the locally generated state")
            if (!isIssValid) error("iss(issuer) received in the authentication result does not match the issuer of the Authorization Server")
        }
    }

    return parsedAuthResult
}