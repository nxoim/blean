package com.nxoim.blean.api.utils

import kotlin.jvm.JvmInline

// We REALLY need new typealias features for checks like this
@JvmInline
value class LimitUpToHundred(val value: Int) {
    init {
        require(value in 1..100) { "LimitUpToHundred must be between 1 and 100. Was $value" }
    }
}

