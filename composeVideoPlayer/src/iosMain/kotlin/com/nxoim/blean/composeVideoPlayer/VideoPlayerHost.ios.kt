package com.nxoim.blean.composeVideoPlayer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import co.touchlab.kermit.Logger

@Composable
actual fun rememberVideoPlayerHost(
    maxInactiveInstances: Int,
    cacheConfig: VideoPlayerCacheConfiguration,
    logger: Logger
): VideoPlayerHost = remember {
        VideoPlayerHost(logger, maxInactiveInstances, { VideoPlayer.Notlmplemented })
    }

