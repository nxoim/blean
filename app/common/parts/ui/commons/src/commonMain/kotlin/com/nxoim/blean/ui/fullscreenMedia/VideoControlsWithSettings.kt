package com.nxoim.blean.ui.fullscreenMedia

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nxoim.blean.composeVideoPlayer.PlayerState
import com.nxoim.blean.composeVideoPlayer.VideoPlayer
import com.nxoim.blean.composeVideoPlayer.VideoSettings
import com.nxoim.blean.composeVideoPlayer._FakeHLSController
import com.nxoim.blean.composeVideoPlayer._FakePlaybackController
import com.nxoim.blean.composeVideoPlayer._FakeVideoPlayer
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun VideoControlsWithSettings(
    player: VideoPlayer,
    visible: Boolean,
    controlsState: VideoControlsState,
    modifier: Modifier = Modifier
) {
    AnimatedContent(
        player.state,
        transitionSpec = {
            (fadeIn() + scaleIn(initialScale = 0.97f)) togetherWith
                    scaleOut(targetScale = 0.97f) + fadeOut()
        },
        modifier = modifier
    ) {
        if (it is PlayerState.Initialized) AnimatedVisibility(
            visible,
            enter = fadeIn() + scaleIn(initialScale = 0.97f),
            exit = scaleOut(targetScale = 0.97f) + fadeOut(),
        ) {
            VideoControlsWithSettings(
                player = player,
                playerState = it,
                controlsState = controlsState
            )
        }
    }

}

@Composable
private fun VideoControlsWithSettings(
    player: VideoPlayer,
    playerState: PlayerState.Initialized,
    controlsState: VideoControlsState,
    modifier: Modifier = Modifier,
) {
    VideoControls(
        playbackState = playerState.controller.playbackState,
        onPlayingChange = {
            if (it) playerState.controller.play() else playerState.controller.pause()
        },
        playerState.controller.totalDuration,
        elapsed = playerState.controller.elapsed,
        onNewDurationRequested = {
            playerState.controller.elapsed = it
        },
        volume = player.settings.volume,
        onVolumeChange = {
            player.settings.volume = it
        },
        options = { contentPadding ->
            FullscreenMediaVideoOptions(player, contentPadding)
        },
        state = controlsState,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Preview
@Composable
private fun VideoControlsWithSettingsPreview() {
    val fakePlayerState = PlayerState.Initialized(
        _FakePlaybackController(),
        VideoSettings.HLS(_FakeHLSController())
    )

    MaterialExpressiveTheme {
        Surface {
            VideoControlsWithSettings(
                player = _FakeVideoPlayer(fakePlayerState),
                playerState = fakePlayerState,
                controlsState = rememberVideoControlsState(),
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}