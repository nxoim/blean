package com.nxoim.blean.composeVideoPlayer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.currentCompositeKeyHashCode
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toLong
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.nxoim.blean.byteCount.ByteCount
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds


interface VideoPlayer {
    val state: PlayerState
    val settings: PlayerSettings
    fun loadFromUri(
        uri: String,
        configuration: VideoLoadingConfiguration = VideoLoadingConfiguration.default,
        force: Boolean = false,
    )
    fun deactivateAndDispose()
    fun deactivate()
    fun reactivate()

    companion object {
        val Notlmplemented = object : VideoPlayer {
            override val state = PlayerState.Error("Not implemented", null)
            override val settings = PlayerSettings()

            override fun loadFromUri(
                uri: String,
                configuration: VideoLoadingConfiguration,
                force: Boolean
            ) {
            }

            override fun deactivateAndDispose() {
            }

            override fun deactivate() {
            }

            override fun reactivate() {
            }
        }
    }
}

@Suppress("ClassName")
class _FakeVideoPlayer(
    override val state: PlayerState,
    override val settings: PlayerSettings = PlayerSettings()
) : VideoPlayer {
    override fun loadFromUri(
        uri: String,
        configuration: VideoLoadingConfiguration,
        force: Boolean
    ) {
    }

    override fun deactivateAndDispose() {}

    override fun deactivate() {}

    override fun reactivate() {}
}

sealed interface PlayerState {
    data class Initialized(
        val controller: PlaybackController,
        val settings: VideoSettings
    ) : PlayerState

    data object Loading : PlayerState
    data object Uninitialized : PlayerState
    data class Error(val message: String, val exception: Exception?) :
        PlayerState
}

interface PlaybackController {
    val playbackState: PlaybackState
    val totalDuration: Duration
    var elapsed: Duration
    val bufferedFraction: Float

    fun play()
    fun pause()
}

@Suppress("ClassName")
class _FakePlaybackController : PlaybackController {
    override val playbackState = PlaybackState.Playing
    override val totalDuration = 2.minutes
    override var elapsed: Duration
        get() = 30.seconds
        set(value) {}
    override val bufferedFraction = 0.4f

    override fun play() {}
    override fun pause() {}
}

sealed interface VideoPlayerCacheConfiguration {
    data class Enabled(
        val path: String,
        val maxSize: ByteCount
    ) : VideoPlayerCacheConfiguration

    data object Disabled : VideoPlayerCacheConfiguration
}

// This interface provides flexibility for managing player settings.
// It allows for a global instance that child instances can access.
// For example, if volume is global, a child instance can reference the
// global instance's volume. Any leak caused by an implementation
// of the use case described above is a sign of massive skill issues.
// DO NOT hold a direct reference to the global instance if you know
// it could change
interface PlayerSettings {
    // every user should be able to modify settings
    var subtitlesEnabled: Boolean
    var volume: Float
    var looping: Boolean
    var playbackSpeed: Float
    // todo last subtitle locale?
}

fun PlayerSettings(
    subtitlesEnabled: Boolean = false,
    volume: Float = 1f,
    looping: Boolean = false,
    playbackSpeed: Float = 1f
): PlayerSettings = object : PlayerSettings {
    override var subtitlesEnabled by mutableStateOf(subtitlesEnabled)
    override var volume by mutableFloatStateOf(volume)
    override var looping: Boolean by mutableStateOf(looping)
    override var playbackSpeed by mutableFloatStateOf(playbackSpeed)
}

sealed interface VideoSettings {
    //TODO implement progressive video and mb dash? idk
    class Progressive() : VideoSettings

    class HLS(
        private val implementation: HLSController
    ) : VideoSettings, HLSController by implementation
}

sealed interface PlaybackState {
    data object Playing : PlaybackState

    sealed interface Stopped : PlaybackState {
        data object ByUser : Stopped
        data object ByCompletion : Stopped
        data object Buffering : Stopped
        data class ByError(val message: String, val exception: Exception?) : Stopped
    }
}

@Composable
fun rememberVideoPlayer(
    key: String,
    // NOTE somehow force retention if required?
    initialPlayerSettings: () -> PlayerSettings = { PlayerSettings() }
): VideoPlayer = key(key) {
    val host = LocalVideoPlayerHost.current
    val place = currentCompositeKeyHashCode.toLong()
    val holder = remember(place, host, key) {
        object : PlayerHolder {
            override val player = host.getOrCreate(key, initialPlayerSettings, place)
            override fun onForgotten() {
                host.removeSubscription(key, place)
            }

            override fun onAbandoned() { }
            override fun onRemembered() { }
        }
    }

    ManagePlayerLifecycle(holder.player)

    return holder.player
}

@Composable
@NonRestartableComposable
private fun ManagePlayerLifecycle(player: VideoPlayer) {
    var wasPlayingBeforeLifecycleStopEvent by remember {
        mutableStateOf(false)
    }

    // have to deactivate the player for ux consistency
    // across platforms at least for now
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
        (player.state as? PlayerState.Initialized)?.run {
            wasPlayingBeforeLifecycleStopEvent = controller.playbackState == PlaybackState.Playing
        }

        player.deactivate()
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        player.reactivate()

        if (wasPlayingBeforeLifecycleStopEvent) {
            (player.state as? PlayerState.Initialized)?.run {
                controller.play()
            }
        }
    }
}

private interface PlayerHolder : RememberObserver {
    val player: VideoPlayer
}
