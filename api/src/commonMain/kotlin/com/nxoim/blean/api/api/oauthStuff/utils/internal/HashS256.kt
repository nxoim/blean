package com.nxoim.blean.api.api.oauthStuff.utils.internal

import com.nxoim.blean.api.api.oauthStuff.models.S256HashedString
import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.algorithms.SHA256
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

// TODO common?
@OptIn(ExperimentalEncodingApi::class)
suspend fun String.hashS256() = S256HashedString(
    Base64.UrlSafe
        .encode(
            CryptographyProvider.Default
                .get(SHA256)
                .hasher()
                .hash(this.encodeToByteArray())
        )
        .replace("=", "")
)

@OptIn(ExperimentalEncodingApi::class)
fun String.hashS256Blocking() = S256HashedString(
    Base64.UrlSafe
        .encode(
            CryptographyProvider.Default
                .get(SHA256)
                .hasher()
                .hashBlocking(this.encodeToByteArray())
        )
        .replace("=", "")
)