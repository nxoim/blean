package com.nxoim.blean.api.api.oauthStuff.models

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@Serializable
@JvmInline
value class OAuthCodeChallenge(val value: S256HashedString) {
    val method get() = "S256"
}
