package com.nxoim.blean.api.api.oauthStuff.utils

fun getEntrypointFromAuthorizationServer(
    authorizationServer: String
) = authorizationServer.removeSuffix("/oauth/authorize")