package com.nxoim.blean.composeVideoPlayer

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class VideoLoadingConfiguration(
    val minBuffered: Duration,
    val maxBuffered: Duration,
) {
    companion object {
        val default = VideoLoadingConfiguration(
            minBuffered = 1.seconds,
            maxBuffered = 15.seconds,
        )
    }
}