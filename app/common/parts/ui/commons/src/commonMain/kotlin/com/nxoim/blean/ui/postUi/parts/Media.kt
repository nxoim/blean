package com.nxoim.blean.ui.postUi.parts

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.nxoim.blean.composeVideoPlayer.BasicVideoPlayer
import com.nxoim.blean.composeVideoPlayer.PlaybackState
import com.nxoim.blean.composeVideoPlayer.PlayerSettings
import com.nxoim.blean.composeVideoPlayer.PlayerState
import com.nxoim.blean.composeVideoPlayer.VideoPlayer
import com.nxoim.blean.composeVideoPlayer.rememberVideoPlayer
import com.nxoim.blean.postRelatedCommons.models.PostMediaContent
import com.nxoim.blean.ui.composeMaterial3Extensions.GenericMediaError
import com.nxoim.blean.ui.composeMaterial3Extensions.GenericMediaErrorDetailsContainer
import com.nxoim.blean.ui.composeUiCommons.LocalIsInFocus
import com.nxoim.blean.ui.composeUiCommons.coilImageRequest
import com.nxoim.blean.ui.composeUiCommons.modifiers.conditionalThen
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun Media(
    mediaContent: PostMediaContent,
    modifier: Modifier = Modifier,
    forceImageAspectRatio: Boolean = false,
    maxHeight: Dp = 340.dp,
    videoAutoplay: Boolean = true,
    initialVideoPlayerSettings: () -> PlayerSettings =  { PlayerSettings(volume = 0f, looping = true) },
    isFocused: Boolean = LocalIsInFocus.current ?: true,
    onClick: (() -> Unit)? = null
) {
    when (mediaContent) {
        is PostMediaContent.ImageWithCDNLinks -> ImageMedia(
            modifier = modifier,
            forceImageAspectRatio = forceImageAspectRatio,
            mediaContent = mediaContent,
            maxHeight = maxHeight
        )

        is PostMediaContent.Video -> {
            val player = rememberVideoPlayer(
                key = mediaContent.playlistUri,
                initialPlayerSettings = initialVideoPlayerSettings
            )

            LaunchedEffect(isFocused) {
                // we dont need to initialize right away
                delay(300)
                if (isActive && isFocused) {
                    player.loadFromUri(mediaContent.playlistUri)
                }
            }

            LaunchedEffect(player.state, isFocused) {
                val playerState = player.state

                if (playerState is PlayerState.Initialized) {
                    if (isFocused)
                        if (videoAutoplay) playerState.controller.play()
                        else
                            playerState.controller.pause()
                }
            }

            VideoMedia(
                modifier = modifier.let {
                    if (onClick != null)
                        it.clickable {
                            onClick()
                            player.settings.volume = 1f
                        }
                    else
                        it
                },
                forceImageAspectRatio = forceImageAspectRatio,
                player = player,
                mediaContent = mediaContent,
                maxHeight = maxHeight,
            )
        }

        PostMediaContent.Unsupported -> UnsupportedMedia(modifier = modifier)
    }
}

@Composable
fun Media(
    mediaContent: PostMediaContent,
    modifier: Modifier = Modifier,
    forceImageAspectRatio: Boolean = false,
    maxHeight: Dp = 340.dp,
    onPlayerRequested: () -> VideoPlayer
) {
    when (mediaContent) {
        is PostMediaContent.ImageWithCDNLinks -> ImageMedia(
            modifier = modifier,
            forceImageAspectRatio = forceImageAspectRatio,
            mediaContent = mediaContent,
            maxHeight = maxHeight
        )

        is PostMediaContent.Video -> VideoMedia(
            modifier = modifier,
            forceImageAspectRatio = forceImageAspectRatio,
            player = onPlayerRequested(),
            mediaContent = mediaContent,
            maxHeight = maxHeight,
        )

        PostMediaContent.Unsupported -> UnsupportedMedia(modifier = modifier)
    }
}

@Composable
fun ImageMedia(
    modifier: Modifier = Modifier,
    forceImageAspectRatio: Boolean = false,
    mediaContent: PostMediaContent.ImageWithCDNLinks,
    maxHeight: Dp = 340.dp
) {
    val aspectRatio = mediaContent.widthPx?.let { width ->
        mediaContent.heightPx?.let { height ->
            width.toFloat() / height
        }
    } ?: 1f

    Box(
        modifier
            .conditionalThen(
                forceImageAspectRatio,
                onTrue = {
                    it
                        .heightIn(max = maxHeight)
                        .aspectRatio(aspectRatio, matchHeightConstraintsFirst = true)
                },
                onFalse = { it.height(maxHeight) }
            )
    ) {
        AsyncImage(
            coilImageRequest(mediaContent.thumb),
            contentDescription = mediaContent.altText,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        //        SubcomposeAsyncImage(
//            coilImageRequest(mediaContent.thumb),
//            contentDescription = mediaContent.altText,
//            contentScale = ContentScale.Crop,
//            modifier = Modifier.fillMaxSize(),
//            error = {
//                GenericMediaError(
//                    onClick = this.painter::restart
//                )
//            }
//        )
    }
}

@Composable
fun VideoMedia(
    mediaContent: PostMediaContent.Video,
    modifier: Modifier = Modifier,
    player: VideoPlayer,
    forceImageAspectRatio: Boolean = false,
    maxHeight: Dp = 340.dp,
) {
    val aspectRatio =
        (mediaContent.widthPx?.toFloat() ?: 1f) / (mediaContent.heightPx?.toFloat() ?: 1f)

    Crossfade(
        player.state,
        modifier = modifier
            .conditionalThen(
                forceImageAspectRatio,
                onTrue = {
                    it
                        .heightIn(max = maxHeight)
                        .aspectRatio(aspectRatio, matchHeightConstraintsFirst = true)
                },
                onFalse = { it.height(maxHeight) }
            )
    ) { playerState ->
        when (playerState) {
            is PlayerState.Error -> VideoError(
                playerState,
                onRetry = {
                    player.loadFromUri(mediaContent.playlistUri, force = true)
                }
            )

            is PlayerState.Initialized -> {
                InitializedVideoPlayer(
                    player.settings,
                    playerState,
                    onReload = { player.loadFromUri(mediaContent.playlistUri, force = true) }
                )
            }

            PlayerState.Loading -> AsyncImage(
                mediaContent.thumbnail?.let { coilImageRequest(it) },
                contentDescription = null,
                modifier = Modifier.fillMaxSize()
            )

            PlayerState.Uninitialized -> UninitializedVideoPlayer(
                mediaContent = mediaContent,
                videoPlayer = player
            )
        }
    }
}

@Composable
private fun InitializedVideoPlayer(
    playerSettings: PlayerSettings,
    playerState: PlayerState.Initialized,
    onReload: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        BasicVideoPlayer(playerState, modifier = Modifier.fillMaxSize())

        ProgressBarOverlay(playerState = playerState)

        Crossfade(playerState.controller.playbackState) { videoState ->
            when (videoState) {
                PlaybackState.Playing -> {}
                PlaybackState.Stopped.Buffering -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                PlaybackState.Stopped.ByCompletion -> {
                    // this should not happen because the video
                    // is supposed to loop
                }

                is PlaybackState.Stopped.ByError -> VideoError(
                    error = PlayerState.Error(videoState.message, videoState.exception),
                    onRetry = onReload
                )

                PlaybackState.Stopped.ByUser -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    // do not display any controls since this is not required by the ui in the context of feed
                    FilledTonalIconButton(onClick = { playerState.controller.play() }) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = null
                        )
                    }
                }
            }
        }

        MuteButton(
            playerSettings.volume,
            onValueChange = { playerSettings.volume = it },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(8.dp)
        )
    }
}

@Composable
private fun VideoError(
    error: PlayerState.Error,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    GenericMediaError(
        onClick = onRetry,
        text = {
            Text("Could not load the video")
        },
        details = {
            GenericMediaErrorDetailsContainer {
                Text(error.message)
            }
        },
        modifier = modifier.fillMaxSize()
    )
}

@Composable
private fun ProgressBarOverlay(playerState: PlayerState.Initialized) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .alpha(0.85f),
        contentAlignment = Alignment.BottomStart
    ) {
        val progress =
            remember(playerState.controller.elapsed, playerState.controller.totalDuration) {
                if (playerState.controller.totalDuration.isFinite() && playerState.controller.totalDuration.inWholeMilliseconds > 0) {
                    playerState.controller.elapsed.inWholeMilliseconds.toFloat() / playerState.controller.totalDuration.inWholeMilliseconds
                } else {
                    0f
                }
            }

        val bufferedFraction = remember(playerState.controller.bufferedFraction) {
            playerState.controller.bufferedFraction.coerceIn(0f, 1f)
        }

        HorizontalDivider(
            Modifier.fillMaxWidth(progress),
            thickness = 4.dp,
        )
        HorizontalDivider(
            Modifier.fillMaxWidth(bufferedFraction).alpha(0.4f),
            thickness = 4.dp,
        )
    }
}

@Composable
private fun MuteButton(
    current: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val isMuted = current == 0f

    Surface(
        modifier = modifier
            .alpha(0.7f)
            .clip(CircleShape)
            .clickable(onClick = { onValueChange(if (isMuted) 1f else 0f) }),
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        AnimatedContent(isMuted) {
            if (it) {
                Icon(
                    Icons.AutoMirrored.Filled.VolumeOff,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(4.dp)
                        .size(16.dp)
                )
            } else {
                Icon(
                    Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(4.dp)
                        .size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun UninitializedVideoPlayer(
    mediaContent: PostMediaContent.Video,
    videoPlayer: VideoPlayer
) {
    Box(contentAlignment = Alignment.Center) {
        AsyncImage(
            mediaContent.thumbnail?.let { coilImageRequest(it) },
            contentDescription = null,
            modifier = Modifier.fillMaxSize()
        )
        IconButton(onClick = { videoPlayer.loadFromUri(mediaContent.playlistUri) }) {
            Icon(
                Icons.Default.PlayArrow,
                contentDescription = null
            )
        }
    }
}


@Composable
fun UnsupportedMedia(modifier: Modifier) {
    Text(
        "Unsupported",
        modifier = modifier.size(50.dp)
            .background(Color.Gray)
    )
}