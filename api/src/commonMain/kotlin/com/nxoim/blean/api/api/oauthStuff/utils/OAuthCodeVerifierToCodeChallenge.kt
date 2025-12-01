package com.nxoim.blean.api.api.oauthStuff.utils

import com.nxoim.blean.api.api.oauthStuff.models.OAuthCodeChallenge
import com.nxoim.blean.api.api.oauthStuff.models.OAuthCodeVerifier

/**
 * Generate using [OAuthCodeVerifier.toCodeChallenge]
 */
fun OAuthCodeVerifier.toCodeChallenge() = OAuthCodeChallenge(this.hashS256Blocking())