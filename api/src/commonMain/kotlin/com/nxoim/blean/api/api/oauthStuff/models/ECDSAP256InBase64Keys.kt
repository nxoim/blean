package com.nxoim.blean.api.api.oauthStuff.models

import com.nxoim.blean.api.api.oauthStuff.utils.extractXYFromPublicKey
import kotlinx.serialization.Serializable
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@Serializable
data class ECDSAP256InBase64Keys(val public: String, val private: String) {
    @OptIn(ExperimentalEncodingApi::class)
    val extractedXYFromPublicKey = extractXYFromPublicKey(Base64.decode(this.public))
}

