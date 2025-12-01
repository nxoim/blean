package com.nxoim.blean.ui.fullscreenMedia

import androidx.compose.foundation.layout.Arrangement.Absolute.spacedBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ShortText
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.nxoim.blean.composeVideoPlayer.PlayerState
import com.nxoim.blean.composeVideoPlayer.VideoPlayer
import com.nxoim.blean.composeVideoPlayer.VideoSettings
import com.nxoim.blean.composeVideoPlayer._FakeHLSController
import com.nxoim.blean.composeVideoPlayer._FakePlaybackController
import com.nxoim.blean.composeVideoPlayer._FakeVideoPlayer
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun FullscreenMediaVideoOptions(
    player: VideoPlayer,
    contentPadding: PaddingValues,
) {
    VideoOptionsContainer(
        containerColor = Color.Transparent,
        contentPadding = contentPadding,
    ) {
        SpeedSelector(
            currentSpeedMultiplier = player.settings.playbackSpeed,
            onNewSpeedMultiplier = { player.settings.playbackSpeed = it },
        )

//                                            HorizontalDivider()

        Column(
            Modifier.fillMaxWidth().width(IntrinsicSize.Max),
            verticalArrangement = spacedBy(4.dp)
        ) {
            Setting(
                title = { Text("Subtitles") },
                subtitle = { Text("English") },
                leadingIcon = {
                    Icon(
                        Icons.AutoMirrored.Outlined.ShortText,
                        contentDescription = null
                    )
                },
                onClick = { },
                modifier = Modifier
            )
        }
    }
}
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Preview
@Composable
private fun Preview() {
    val fakePlayerState = PlayerState.Initialized(
        _FakePlaybackController(),
        VideoSettings.HLS(_FakeHLSController())
    )

    MaterialExpressiveTheme {
        Surface {
            FullscreenMediaVideoOptions(
                _FakeVideoPlayer(fakePlayerState),
                PaddingValues(16.dp)
            )
        }
    }
}