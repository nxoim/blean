package com.nxoim.blean.api.api.oauthStuff.utils.internal

import kotlin.io.encoding.ExperimentalEncodingApi

@OptIn(ExperimentalEncodingApi::class)
fun generateRandomString(length: Int = 32): String {
    val lists = ('a'..'z') + ('0'..'9')
    return (1..length)
        .map { lists.random() }
        .joinToString("")
}