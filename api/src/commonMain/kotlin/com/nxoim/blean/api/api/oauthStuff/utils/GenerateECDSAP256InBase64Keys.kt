package com.nxoim.blean.api.api.oauthStuff.utils

import com.nxoim.blean.api.api.oauthStuff.models.ECDSAP256InBase64Keys
import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.algorithms.EC
import dev.whyoleg.cryptography.algorithms.ECDSA
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@OptIn(ExperimentalEncodingApi::class)
fun ECDSAP256InBase64Keys.Companion.generate() = CryptographyProvider.Default
    .get(ECDSA)
    .keyPairGenerator(EC.Curve.P256)
    .generateKeyBlocking()
    .run {
        ECDSAP256InBase64Keys(
            public = Base64.encode(this.publicKey.encodeToByteArrayBlocking(EC.PublicKey.Format.DER)),
            private = Base64.encode(this.privateKey.encodeToByteArrayBlocking(EC.PrivateKey.Format.DER))
        )
    }