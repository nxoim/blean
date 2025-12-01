package com.nxoim.blean.api.api.oauthStuff.utils

import com.nxoim.blean.api.models.oauth.OAuthTokenData

fun OAuthTokenData.validateSafetyOrThrow(
    authorizationServerIssuer: String
): OAuthTokenData {
    require(this.decodeAuthToken().iss == authorizationServerIssuer) {
        "OAuth token issuer does not match the authorization server one"
    }

    require(this.scope.contains("atproto")) {
        "OAuth token scopes must contain 'atproto'"
    }

    return this
}