package com.nxoim.blean.api.api.oauthStuff.utils.internal

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@OptIn(ExperimentalEncodingApi::class)
fun String.decodeBase64ToString() = Base64.Default
    .withPadding(Base64.PaddingOption.PRESENT_OPTIONAL)
    .decode(this)
    .decodeToString()