package com.nxoim.blean.api.api.oauthStuff.utils

import com.nxoim.blean.api.api.oauthStuff.models.EllipticCurvePublicKeyCoordinates

fun extractXYFromPublicKey(
    publicKeyBytes: ByteArray
): EllipticCurvePublicKeyCoordinates {
    // Skip the X.509 header part (26 bytes),
    // the rest is the body of the public key
    val xStartIndex = 26 + 1
    val yStartIndex = xStartIndex + 32

    // Extract 32 bytes each of X and Y coordinates
    val x = publicKeyBytes.sliceArray(xStartIndex until xStartIndex + 32)
    val y = publicKeyBytes.sliceArray(yStartIndex until yStartIndex + 32)

    return EllipticCurvePublicKeyCoordinates(byteArrayOf(0) + x, byteArrayOf(0) + y)
}