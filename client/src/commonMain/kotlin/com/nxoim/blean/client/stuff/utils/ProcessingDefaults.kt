package com.nxoim.blean.client.stuff.utils

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object ProcessingDefaults {
    val NeverPaused = MutableStateFlow(false).asStateFlow()
    val AlwaysPaused = MutableStateFlow(true).asStateFlow()
}
