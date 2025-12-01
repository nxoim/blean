package com.nxoim.blean.api.api.oauthStuff.utils

import com.nxoim.blean.api.api.oauthStuff.models.OAuthCodeVerifier
import com.nxoim.blean.api.api.oauthStuff.utils.internal.generateRandomString

fun OAuthCodeVerifier.Companion.generate() = OAuthCodeVerifier(generateRandomString(43))