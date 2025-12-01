package com.nxoim.blean.composeVideoPlayer

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun BasicVideoPlayer(
    state: PlayerState.Initialized,
    prioritizePerformance: Boolean = true,
    isTargetVideoFeedSurface: Boolean = true,
    modifier: Modifier = Modifier
)