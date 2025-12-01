package com.nxoim.blean.api.api.oauthStuff.models

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

/**
 * The request made using this token will return a callback url that will
 * contain this token, kind of like an id of the request
 */

@Serializable
@JvmInline
value class OAuthStateToken(val value: String) {
    override fun toString() = value
}