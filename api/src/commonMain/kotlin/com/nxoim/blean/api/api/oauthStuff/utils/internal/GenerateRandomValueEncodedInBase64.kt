package com.nxoim.blean.api.api.oauthStuff.utils.internal

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.random.Random

@OptIn(ExperimentalEncodingApi::class)
fun generateRandomValueEncodedInBase64(length: Int = 32) = Base64.encode(Random.nextBytes(length))