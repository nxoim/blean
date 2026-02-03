package com.nxoim.blean.ui.fullscreenMedia

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.Crossfade
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animate
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.compose.ui.zIndex
import coil3.compose.AsyncImage
import com.nxoim.blean.composeVideoPlayer.BasicVideoPlayer
import com.nxoim.blean.composeVideoPlayer.PlaybackState
import com.nxoim.blean.composeVideoPlayer.PlayerState
import com.nxoim.blean.composeVideoPlayer.VideoPlayer
import com.nxoim.blean.composeVideoPlayer.rememberVideoPlayer
import com.nxoim.blean.ui.composeMaterial3Extensions.ExpressiveBoundsTransform
import com.nxoim.blean.ui.composeMaterial3Extensions.GenericMediaError
import com.nxoim.blean.ui.composeMaterial3Extensions.GenericMediaErrorDetailsContainer
import com.nxoim.blean.ui.composeMaterial3Extensions.MediaSharedBoundsTransition
import com.nxoim.blean.ui.composeMaterial3Extensions.scaleInWithFade
import com.nxoim.blean.ui.composeMaterial3Extensions.scaleOutWithFade
import com.nxoim.blean.ui.composeUiCommons.CombinedSharedTransitionScope
import com.nxoim.blean.ui.composeUiCommons.coilImageRequest
import com.nxoim.blean.ui.composeUiCommons.modifiers.offsetWithMotionFrameOfReference
import com.nxoim.blean.ui.composeUiCommons.modifiers.swipeable.SwipeConstraint
import com.nxoim.blean.ui.composeUiCommons.modifiers.swipeable.swipeable
import kotlinx.coroutines.launch

// TODO custom zoom
@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FullScreenMediaViewer(
    mediaContent: List<FullScreenMedia>,
    firstSelected: FullScreenMedia,
    onDismissed: () -> Unit,
    mediaSharedElementTransitionScope: CombinedSharedTransitionScope?,
    onMediaSharedElementKeyRequest: (of: FullScreenMedia) -> String,
    modifier: Modifier = Modifier,
    playbackStateTransitionSpec: AnimatedContentTransitionScope<*>.() -> ContentTransform = run {
        val enter = scaleInWithFade()
        val exit = scaleOutWithFade()

        return@run { enter togetherWith exit }
    }
) {
    val pagerState = rememberPagerState(mediaContent.indexOf(firstSelected)) { mediaContent.size }
    val videoPlayers = rememberVideoPlayers(mediaContent)
    val initialPlayerSettings by rememberInitialPlayerSettings(videoPlayers)

    with(mediaSharedElementTransitionScope) {
        PlayerSettingsRestorationEffect(
            videoPlayers = videoPlayers,
            initialPlayerSettings = initialPlayerSettings,
        )

        FullScreenMediaViewerContent(
            mediaContent = mediaContent,
            pagerState = pagerState,
            videoPlayers = videoPlayers,
            onDismissed = onDismissed,
            onMediaSharedElementKeyRequest = onMediaSharedElementKeyRequest,
            modifier = modifier,
            playbackStateTransitionSpec = playbackStateTransitionSpec
        )
    }
}

@Composable
private fun rememberVideoPlayers(
    mediaContent: List<FullScreenMedia>
) = rememberUpdatedState(
    newValue = mediaContent
        .mapNotNull {
            if (it is FullScreenMedia.Video)
                it to rememberVideoPlayer(it.playlistUri)
            else
                null
        }
        .toMap()
).value

@Composable
private fun rememberInitialPlayerSettings(
    videoPlayers: Map<FullScreenMedia.Video, VideoPlayer?>
) = produceState(mapOf(), videoPlayers) {
    val settingsMap = videoPlayers
        .mapNotNull {
            val settingsBackup = it.value?.let { player ->
                PlayerSettingsBackup(
                    volume = player.settings.volume,
                    playing = (player.state as? PlayerState.Initialized)?.controller?.playbackState is PlaybackState.Playing
                )
            }
            it.key to settingsBackup
        }
        .toMap<FullScreenMedia, PlayerSettingsBackup?>()

    val isInitial = settingsMap.isEmpty()

    if (isInitial) {
        videoPlayers.values.forEach {
            it?.settings?.volume = 1f
        }
    }
    value = settingsMap
}

@Composable
context(sharedElementScope: CombinedSharedTransitionScope?)
private fun PlayerSettingsRestorationEffect(
    videoPlayers: Map<FullScreenMedia.Video, VideoPlayer?>,
    initialPlayerSettings: Map<FullScreenMedia, PlayerSettingsBackup?>
) {
    val restorePlayerSettings = remember {
        {
            videoPlayers.forEach { (content, player) ->
                val settingsBackup = initialPlayerSettings[content]
                if (settingsBackup != null && player != null) {
                    player.settings.volume = settingsBackup.volume
                    if (!settingsBackup.playing) {
                        (player.state as? PlayerState.Initialized)?.controller?.pause()
                    }
                }
            }
        }
    }

    val sharedElementTarget = sharedElementScope?.visibility?.transition?.targetState

    if (sharedElementTarget != null) {
        LaunchedEffect(sharedElementTarget) {
            if (sharedElementTarget != EnterExitState.Visible) restorePlayerSettings()
        }
    } else {
        DisposableEffect(Unit) {
            onDispose { restorePlayerSettings() }
        }
    }
}

@Composable
context(sharedElementScope: CombinedSharedTransitionScope?)
private fun FullScreenMediaViewerContent(
    mediaContent: List<FullScreenMedia>,
    pagerState: PagerState,
    videoPlayers: Map<FullScreenMedia.Video, VideoPlayer?>,
    onDismissed: () -> Unit,
    onMediaSharedElementKeyRequest: (of: FullScreenMedia) -> String,
    playbackStateTransitionSpec: AnimatedContentTransitionScope<*>.() -> ContentTransform,
    modifier: Modifier = Modifier,
    gesturesEnabled: Boolean = sharedElementScope
        ?.let { it.visibility.transition.targetState != EnterExitState.PostExit }
        ?: true
) {
    Box {
        DraggablePager(
            pagerState = pagerState,
            mediaContent = mediaContent,
            videoPlayers = videoPlayers,
            onDismissed = onDismissed,
            onMediaSharedElementKeyRequest = onMediaSharedElementKeyRequest,
            modifier = modifier,
            playbackStateTransitionSpec = playbackStateTransitionSpec,
            gesturesEnabled = gesturesEnabled
        )
        DismissButton(onDismissed)
    }
}

@Composable
context(sharedElementScope: CombinedSharedTransitionScope?)
private fun DraggablePager(
    pagerState: PagerState,
    mediaContent: List<FullScreenMedia>,
    videoPlayers: Map<FullScreenMedia.Video, VideoPlayer?>,
    onDismissed: () -> Unit,
    onMediaSharedElementKeyRequest: (of: FullScreenMedia) -> String,
    modifier: Modifier = Modifier,
    playbackStateTransitionSpec: AnimatedContentTransitionScope<*>.() -> ContentTransform,
    gesturesEnabled: Boolean
) {
    val coroutineScope = rememberCoroutineScope()
    var offset by remember { mutableStateOf(Offset.Zero) }
    var swipeDismissVelocityForConsumers by remember {
        mutableStateOf(Velocity.Zero)
    }

    HorizontalPager(
        state = pagerState,
        userScrollEnabled = gesturesEnabled,
        modifier = modifier
            .swipeable(
                detectionConstraint = SwipeConstraint.vertical(),
                isEnabled = { gesturesEnabled },
                onStart = { },
                onProgress = { delta, _, _ ->
                    offset += delta
                },
                onCancel = { velocity ->
                    coroutineScope.launch {
                        animate(
                            typeConverter = Offset.VectorConverter,
                            initialValue = offset,
                            targetValue = Offset.Zero,
                            initialVelocity = Offset(0f, velocity.y)
                        ) { value, _ ->
                            offset = value
                        }
                    }
                },
                onConfirm = { velocity, _ ->
                    swipeDismissVelocityForConsumers = velocity
                    onDismissed()
                }
            )
            .fillMaxSize(),
        pageSpacing = 16.dp
    ) { pageIndex ->
        val content = mediaContent[pageIndex]
        val isFocused = pagerState.currentPage == pageIndex
        MediaPage(
            content = content,
            videoPlayers = videoPlayers,
            isFocused = isFocused,
            onMediaSharedElementKeyRequest = onMediaSharedElementKeyRequest,
            offset = offset,
            playbackStateTransitionSpec = playbackStateTransitionSpec,
            swipeDismissVelocity = swipeDismissVelocityForConsumers
        )
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
context(sharedElementScope: CombinedSharedTransitionScope?)
private fun MediaPage(
    content: FullScreenMedia,
    videoPlayers: Map<FullScreenMedia.Video, VideoPlayer?>,
    isFocused: Boolean,
    onMediaSharedElementKeyRequest: (of: FullScreenMedia) -> String,
    offset: Offset,
    playbackStateTransitionSpec: AnimatedContentTransitionScope<*>.() -> ContentTransform,
    swipeDismissVelocity: Velocity
) {
    val sharedTransitionModifier = Modifier
        .offsetWithMotionFrameOfReference { offset.round() }
        .let {
            if (sharedElementScope == null) {
                it
            } else {
                with(sharedElementScope) {
                    val sharedContentState = rememberSharedContentState(
                        onMediaSharedElementKeyRequest(content)
                    ).apply {
                        // always apply velocity when we know for sure
                        // the state was attached to the modifier,
                        // which is guaranteed when this condition is
                        // true. otherwise theres a crash
                        remember(swipeDismissVelocity) {
                            if (isMatchFound) prepareTransitionWithInitialVelocity(swipeDismissVelocity)
                        }
                    }

                    it.sharedBounds(
                        sharedContentState,
                        enter = MediaSharedBoundsTransition.Overlay.enter,
                        exit = MediaSharedBoundsTransition.Overlay.exit,
                        resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds(
                            ContentScale.Crop
                        ),
                        boundsTransform = ExpressiveBoundsTransform
                    )
                }
            }
        }

    when (content) {
        is FullScreenMedia.Image -> {
            ImagePage(content, sharedTransitionModifier)
        }

        is FullScreenMedia.Video -> VideoPage(
            content,
            videoPlayers[content]!!,
            isFocused,
            sharedTransitionModifier,
            playbackStateTransitionSpec
        )
    }
}

@Composable
private fun DismissButton(onDismissed: () -> Unit) {
    Row(Modifier.statusBarsPadding().padding(horizontal = 8.dp).zIndex(200f)) {
        FilledTonalIconButton(onDismissed) {
            Icon(
                Icons.Outlined.Close,
                contentDescription = null
            )
        }
    }
}

@Composable
private fun ImagePage(content: FullScreenMedia.Image, modifier: Modifier = Modifier) {
    val aspectRatio =
        (content.widthPx?.toFloat() ?: 1f) / (content.heightPx?.toFloat() ?: 1f)

    AsyncImage(
        coilImageRequest(content.fullsize, placeholder = content.thumb),
        contentDescription = null,
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(aspectRatio)
    )
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
context(sharedElementScope: CombinedSharedTransitionScope?)
private fun VideoPage(
    content: FullScreenMedia.Video,
    player: VideoPlayer,
    isFocused: Boolean,
    modifier: Modifier = Modifier,
    playbackStateTransitionSpec: AnimatedContentTransitionScope<*>.() -> ContentTransform
) {
    val controlsState = rememberVideoControlsState()
    val userIdlingDetectionState = rememberUserIdlingDetectionState(
        enabled = { !controlsState.optionsVisible }
    )

    Box(
        Modifier
            .userIdleDetection(userIdlingDetectionState)
            .fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        val aspectRatio =
            (content.widthPx?.toFloat() ?: 1f) / (content.heightPx?.toFloat() ?: 1f)

        LaunchedEffect(Unit) {
            player.loadFromUri(content.playlistUri)
            player.settings.volume = 1f
        }

        LaunchedEffect(isFocused, player.state) {
            (player.state as? PlayerState.Initialized)?.let { state ->
                if (isFocused) {
                    state.controller.play()
                } else {
                    state.controller.pause()
                }
            }
        }

        Crossfade(
            player.state,
            modifier = modifier
                .aspectRatio(aspectRatio)
        ) { state ->
            VideoPlayerContent(
                state,
                player,
                content,
                isTargetVideoSurface = sharedElementScope == null
                        || sharedElementScope.visibility.transition.targetState == EnterExitState.Visible,
                playbackStateTransitionSpec = playbackStateTransitionSpec
            )
        }

        VideoControlsWithSettings(
            player,
            userIdlingDetectionState,
            controlsState,
            Modifier
                .align(BiasAlignment(0f, 0.85f))
                .widthIn(max = 420.dp)
                .padding(horizontal = 16.dp)
                .systemBarsPadding()

        )
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
context(sharedElementScope: CombinedSharedTransitionScope?)
private fun VideoControlsWithSettings(
    player: VideoPlayer,
    userIdlingDetectionState: UserIdlingDetectionState,
    controlsState: VideoControlsState,
    modifier: Modifier = Modifier
) {
    val visible = (player.state as? PlayerState.Initialized)
        ?.let { initializedPlayerState ->
            val state = initializedPlayerState.controller.playbackState
            val isPaused = state is PlaybackState.Stopped.ByUser

            val playbackHasIssues = (player.state as? PlayerState.Initialized)
                ?.let { it.controller.playbackState is PlaybackState.Stopped.ByError }
                ?: false

            val isIdle = userIdlingDetectionState.isIdle
            // of composable, not the video
            val firstFrameRendered by produceState(false) {
                withFrameNanos { }
                value = true
            }
            val isExiting = sharedElementScope
                ?.let { it.transition.targetState == EnterExitState.PostExit }
                ?: false

            (isPaused && !isExiting && firstFrameRendered) || (!playbackHasIssues && !isIdle && firstFrameRendered && !isExiting)
        }
        ?: false

    VideoControlsWithSettings(
        player = player,
        visible = visible,
        controlsState = controlsState,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun VideoPlayerContent(
    playerState: PlayerState,
    player: VideoPlayer,
    content: FullScreenMedia.Video,
    isTargetVideoSurface: Boolean = true,
    modifier: Modifier = Modifier,
    playbackStateTransitionSpec: AnimatedContentTransitionScope<*>.() -> ContentTransform
) {
    when (playerState) {
        is PlayerState.Error -> Box(modifier, contentAlignment = Alignment.Center) {
            GenericMediaError(
                onClick = {},
                text = {
                    Text("Could not load the video")
                },
                details = {
                    GenericMediaErrorDetailsContainer {
                        Text(playerState.message)
                    }
                }
            )
        }

        is PlayerState.Initialized -> Box(
            modifier,
            contentAlignment = Alignment.Center
        ) {
            BasicVideoPlayer(
                playerState,
                isTargetVideoFeedSurface = isTargetVideoSurface,
                modifier = Modifier.fillMaxWidth()
            )

            AnimatedContent(
                playerState.controller.playbackState,
                transitionSpec = playbackStateTransitionSpec
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    when (it) {
                        PlaybackState.Playing -> {}

                        PlaybackState.Stopped.Buffering -> {
                            CircularWavyProgressIndicator(
                                progress = {
                                    playerState.controller.bufferedFraction
                                }
                            )
                        }

                        PlaybackState.Stopped.ByCompletion -> {}

                        is PlaybackState.Stopped.ByError -> {
                            GenericPlaybackError(
                                onReloadRequest = {
                                    player.loadFromUri(
                                        content.playlistUri,
                                        force = true
                                    )
                                }
                            )
                        }

                        PlaybackState.Stopped.ByUser -> {}
                    }
                }
            }
        }

        PlayerState.Loading -> Box(modifier, contentAlignment = Alignment.Center) {
            AsyncImage(
                content.thumbnail?.let { coilImageRequest(it) },
                contentDescription = null,
                modifier = Modifier.fillMaxWidth()
            )
            CircularProgressIndicator()
        }

        PlayerState.Uninitialized -> {
            AsyncImage(
                content.thumbnail?.let { coilImageRequest(it) },
                contentDescription = null,
                modifier = modifier.fillMaxWidth()
            )
        }
    }
}

private data class PlayerSettingsBackup(
    val volume: Float,
    val playing: Boolean
)