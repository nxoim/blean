package com.nxoim.blean.api.api.oauthStuff.utils

import com.nxoim.blean.api.api.oauthStuff.models.OAuthStateToken
import com.nxoim.blean.api.api.oauthStuff.utils.internal.generateRandomString

fun OAuthStateToken.Companion.generate() = OAuthStateToken(generateRandomString(32))