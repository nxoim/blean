package com.nxoim.blean.composeVideoPlayer

import androidx.annotation.OptIn
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.microseconds
import kotlin.time.Duration.Companion.milliseconds

@OptIn(UnstableApi::class)
class AndroidPlaybackController(
    val exoPlayer: ExoPlayer,
) : PlaybackController {
    override var playbackState by mutableStateOf<PlaybackState>(PlaybackState.Stopped.Buffering)
        private set
    override var totalDuration by mutableStateOf(
        if (exoPlayer.isCurrentMediaItemLive)
            exoPlayer.getLiveDuration()
        else
            exoPlayer.contentDuration.milliseconds
    )

    private var _currentPosition by mutableStateOf(exoPlayer.currentPosition.milliseconds)
    override var elapsed: Duration
        get() = _currentPosition
        set(value) { seekToAndHandleExceptions(value) }

    override var bufferedFraction by mutableFloatStateOf(exoPlayer.bufferedPercentage * 0.01f)
        private set

    /**
     * Only one view at a time can receive the video feed.
     * This allows us to forcefully update the target view,
     * avoiding having the video feed frozen
     */
    var compositionKey by mutableIntStateOf(0)
    private val exoplayerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            updateState(playbackState)
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            // we dont know what will stop the video here
            if (isPlaying) playbackState = PlaybackState.Playing
        }

        override fun onPlayerError(error: PlaybackException) {
            updateState(error)
        }
    }

    init {
        exoPlayer.addListener(exoplayerListener)
    }

    override fun play() {
        exoPlayer.play()
    }

    override fun pause() {
        exoPlayer.pause()
        playbackState = PlaybackState.Stopped.ByUser
    }

    private var observingJob: Job? = null
    suspend fun observeProgress() {
        observingJob?.cancel()
        observingJob = coroutineScope {
            launch(Dispatchers.Main.immediate) {
//        val timelineWindow = exoPlayer.currentTimeline.getWindow(exoPlayer.currentWindowIndex, Timeline.Window())
                while (!exoPlayer.isReleased && isActive) {
                    withFrameMillis { }
                    bufferedFraction = exoPlayer.bufferedPercentage * 0.01f

                   totalDuration = exoPlayer.getLiveDuration()
                    _currentPosition =
//                    if (exoPlayer.isCurrentMediaItemLive) {
//                    contentLength = exoPlayer.getLiveDuration()
//
//                    if (timelineWindow.windowStartTimeMs != C.TIME_UNSET) {
//                        (timelineWindow.windowStartTimeMs - exoPlayer.currentPosition).milliseconds
//                    } else {
//                        exoPlayer.currentPosition.milliseconds
//                    }
//                } else {
                        exoPlayer.contentPosition.milliseconds
//                }
                }
                observingJob = null
            }
        }
    }

    private fun seekToAndHandleExceptions(to: Duration) {
        if (exoPlayer.playerError?.errorCode == PlaybackException.ERROR_CODE_IO_UNSPECIFIED) {
            exoPlayer.seekToDefaultPosition()
            exoPlayer.prepare()
        }

        exoPlayer.seekTo(to.inWholeMilliseconds.coerceIn(0, totalDuration.inWholeMilliseconds))
    }

    private fun updateState(playbackState: Int) {
        when (playbackState) {
            Player.STATE_ENDED -> {
                this@AndroidPlaybackController.playbackState = if (exoPlayer.isCurrentWindowLive) {
                    // live streams might not actually "end"
                    PlaybackState.Stopped.ByUser
                } else {
                    PlaybackState.Stopped.ByCompletion
                }
            }

            Player.STATE_BUFFERING -> {
                this@AndroidPlaybackController.playbackState = PlaybackState.Stopped.Buffering
            }

            Player.STATE_READY -> {
                this@AndroidPlaybackController.playbackState = if (exoPlayer.isPlaying)
                    PlaybackState.Playing
                else
                    PlaybackState.Stopped.ByUser
            }

            Player.STATE_IDLE -> {

            }
        }
    }

    private fun updateState(error: PlaybackException) {
        when (error.errorCode) {
            PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW -> {
                playbackState = PlaybackState.Stopped.Buffering
                exoPlayer.seekToDefaultPosition()
                exoPlayer.prepare()
                exoPlayer.play()
            }

            // can occur due to caching i think? idfk
            PlaybackException.ERROR_CODE_IO_UNSPECIFIED -> {
                exoPlayer.prepare()
            }


            else -> {
                playbackState = PlaybackState.Stopped.ByError(
                    message = error.errorCodeName,
                    exception = error
                )
            }
        }
    }
}

@OptIn(UnstableApi::class)
private fun ExoPlayer.getLiveDuration() = currentTimeline
    .getWindow(currentMediaItemIndex, Timeline.Window())
    .durationUs
    .microseconds