package com.nxoim.blean.composeVideoPlayer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import co.touchlab.kermit.Logger

@Composable
actual fun rememberVideoPlayerHost(
    maxInactiveInstances: Int,
    cacheConfig: VideoPlayerCacheConfiguration,
    logger: Logger
): VideoPlayerHost {
    val context = LocalContext.current.applicationContext

    return remember {
        VideoPlayerHost(
            logger,
            maxInactiveInstances,
            factory = { playerSettings -> AndroidVideoPlayer(playerSettings, context, cacheConfig, logger) }
        )
    }
}