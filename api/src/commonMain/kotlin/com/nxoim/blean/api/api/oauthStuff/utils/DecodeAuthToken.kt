package com.nxoim.blean.api.api.oauthStuff.utils

import com.nxoim.blean.api.api.oauthStuff.models.DecodedOAuthToken
import com.nxoim.blean.api.api.oauthStuff.utils.internal.decodeBase64ToString
import com.nxoim.blean.api.models.modelsJsonConfig
import com.nxoim.blean.api.models.oauth.OAuthTokenData

fun decodeAuthToken(token: String) = token
    .split(".")
    .let {
        it.getOrNull(1)
            ?: error("Unexpected format of authentication token encountered during decoding")
    }
    .decodeBase64ToString()
    .let { modelsJsonConfig.decodeFromString(DecodedOAuthToken.serializer(), it) }

fun OAuthTokenData.decodeAuthToken() = this
    .accessToken
    .split(".")
    .get(1)
    .decodeBase64ToString()
    .let { modelsJsonConfig.decodeFromString(DecodedOAuthToken.serializer(), it) }