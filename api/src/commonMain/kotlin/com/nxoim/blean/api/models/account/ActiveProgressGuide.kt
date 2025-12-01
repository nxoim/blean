package com.nxoim.blean.api.models.account

import kotlinx.serialization.Serializable

@Serializable
data class ActiveProgressGuide(val guide: String) {
    init {
        require(guide.length <= 100) {
            "guide must be less than or equal to 100"
        }
    }
}