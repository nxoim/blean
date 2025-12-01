package com.nxoim.blean.api.api.oauthStuff.utils

import com.nxoim.blean.api.models.oauth.AuthorizationServerMetadata

fun AuthorizationServerMetadata.verifySafetyOrThrow(): AuthorizationServerMetadata {
    if (!tokenEndpoint.startsWith("https"))
        error("Authorization server's token endpoint is unsafe")

    return this
}
