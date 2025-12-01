package com.nxoim.blean.api.api.oauthStuff.models

import kotlinx.serialization.Serializable

/**
 * The two 32-byte coordinates that make up a point on an elliptic curve,
 * which corresponds to an ECC public key.
 */
@Serializable
class EllipticCurvePublicKeyCoordinates(val x: ByteArray, val y: ByteArray)
