package com.nxoim.blean.api.api.oauthStuff.models

import kotlin.jvm.JvmInline

@JvmInline
value class SignedJWTMessage(val value: ByteArray)