package com.nxoim.blean.api.api.oauthStuff.utils

import com.nxoim.blean.api.api.oauthStuff.models.SignedJWTMessage
import com.nxoim.blean.api.api.oauthStuff.models.UnsignedJWTMessage
import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.algorithms.EC
import dev.whyoleg.cryptography.algorithms.ECDSA
import dev.whyoleg.cryptography.algorithms.SHA256
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@OptIn(ExperimentalEncodingApi::class)
fun UnsignedJWTMessage.signWithECDSAP256InSHA256(key: String): SignedJWTMessage {
    val privateKey = CryptographyProvider.Default.get(ECDSA)
        .privateKeyDecoder(EC.Curve.P256)
        .decodeFromByteArrayBlocking(
            EC.PrivateKey.Format.DER,
            Base64.decode(key)
        )

    return SignedJWTMessage(
        privateKey
            .signatureGenerator(SHA256, ECDSA.SignatureFormat.RAW)
            .generateSignatureBlocking(this.value.encodeToByteArray())
    )
}