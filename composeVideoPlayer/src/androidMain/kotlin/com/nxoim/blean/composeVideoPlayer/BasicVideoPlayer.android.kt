package com.nxoim.blean.composeVideoPlayer

import android.annotation.SuppressLint
import android.view.SurfaceView
import android.view.TextureView
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player.COMMAND_SET_VIDEO_SURFACE
import androidx.media3.common.util.UnstableApi
import kotlin.random.Random

@SuppressLint("RememberReturnType")
@OptIn(UnstableApi::class)
@Composable
actual fun BasicVideoPlayer(
    state: PlayerState.Initialized,
    prioritizePerformance: Boolean,
    isTargetVideoFeedSurface: Boolean,
    modifier: Modifier
) {
    val controller = state.androidPlaybackController
    val exoPlayer = controller.exoPlayer
    val canClearSurface = exoPlayer.availableCommands.containsAny(COMMAND_SET_VIDEO_SURFACE)

    // this looks ridiculous but this helps prevent
    // flickers if switching from one type of viewing to another.
    // the goal is to keep dead views around for a short time
    // because they contain last frames while another view type
    // is selected
    @SuppressLint("UnusedContentLambdaTargetStateParameter")
    AnimatedContent(
        targetState = controller.compositionKey,
        transitionSpec = {
            EnterTransition.None togetherWith fadeOut(tween(0, delayMillis = 200))
        },
    ) { _ ->
        AnimatedContent(
            targetState = prioritizePerformance,
            transitionSpec = {
                EnterTransition.None togetherWith fadeOut(tween(0, delayMillis = 200))
            },
        ) { prioritizePerformance ->
            if (prioritizePerformance) {
                AndroidView(
                    factory = {
                        val view = SurfaceView(it)
                        if (isTargetVideoFeedSurface) {
                            if (canClearSurface) exoPlayer.clearVideoSurface()
                            exoPlayer.setVideoSurfaceView(view)
                        }
                        return@AndroidView view
                    },
                    onRelease = { exoPlayer.clearVideoSurfaceView(it) },
                    modifier = modifier
                )
            } else {
                AndroidView(
                    factory = {
                        val view = TextureView(it)
                        if (isTargetVideoFeedSurface) {
                            if (canClearSurface) exoPlayer.clearVideoSurface()
                            exoPlayer.setVideoTextureView(view)
                        }
                        return@AndroidView view
                    },
                    onRelease = { exoPlayer.clearVideoTextureView(it) },
                    modifier = modifier
                )
            }
        }
    }

    LaunchedEffect(exoPlayer) { // Key on exoPlayer ensures restart if it changes
        controller.observeProgress()
    }

    remember(isTargetVideoFeedSurface) {
        controller.compositionKey = Random.nextInt()
    }
}