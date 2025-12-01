@file:kotlin.OptIn(InternalCoroutinesApi::class)

package com.nxoim.blean.composeVideoPlayer

import android.content.Context
import androidx.annotation.OptIn
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsManifest
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.exoplayer.trackselection.AdaptiveTrackSelection
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import co.touchlab.kermit.Logger
import co.touchlab.stately.collections.ConcurrentMutableMap
import co.touchlab.stately.collections.ConcurrentMutableSet
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.andThenRecover
import com.github.michaelbull.result.map
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import com.nxoim.blean.composeVideoPlayer.utils.composeVideoPlayerLogTag
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import kotlin.coroutines.resume
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

private var cacheMap = ConcurrentMutableMap<String, CacheHolder>()

class AndroidVideoPlayer(
    override val settings: PlayerSettings,
    private val context: Context,
    private val cacheConfig: VideoPlayerCacheConfiguration,
    private val logger: Logger
) : VideoPlayer {
    private val coroutineScope = MainScope()
    private val mutex = Mutex()
    private val playerInstanceHashCode = this.javaClass.hashCode()
    override var state by mutableStateOf<PlayerState>(
        PlayerState.Loading
    )
        private set

    var lastPlayerLoadConfig: VideoLoadingConfiguration? = null
    var lastPlayerLoadUri: String? = null
    var wasPlayingBeforeDeactivation = false

    @OptIn(UnstableApi::class)
    override fun loadFromUri(
        uri: String,
        configuration: VideoLoadingConfiguration,
        force: Boolean
    ) {
        coroutineScope.launch(Dispatchers.Main.immediate) {
            val load = suspend {
                logger.v(tag = composeVideoPlayerLogTag) { "$androidVideoPlayerSubTag: loadFromUri: Creating ExoPlayer" }
                val exoPlayer = createExoPlayer(
                    context,
                    minToBuffer = configuration.minBuffered,
                    maxToBuffer = configuration.maxBuffered
                )
                launch { exoPlayer.observePlayerSettingsAndApply() }
                lastPlayerLoadConfig = configuration
                lastPlayerLoadUri = uri

                loadByUri(uri, exoPlayer)
            }

            if (state is PlayerState.Initialized && !force) {
                val initializedState = state as PlayerState.Initialized
                val currentMediaId = initializedState.androidPlaybackController.exoPlayer.currentMediaItem?.mediaId
                val targetMediaId = MediaItem.Builder().setUri(uri).build().mediaId

                if (currentMediaId != targetMediaId) {
                    logger.v(tag = composeVideoPlayerLogTag) { "$androidVideoPlayerSubTag: loadFromUri: Media ID mismatch, loading new URI" }
                    load()
                } else {
                    logger.v(tag = composeVideoPlayerLogTag) { "$androidVideoPlayerSubTag: loadFromUri: Media ID match, not loading" }
                }
            } else {
                logger.v(tag = composeVideoPlayerLogTag) { "$androidVideoPlayerSubTag: loadFromUri: Force load or initial load, loading" }
                load()
            }
        }
    }

    @OptIn(UnstableApi::class)
    override fun deactivateAndDispose() {
        coroutineScope.coroutineContext.cancelChildren()
        coroutineScope.launch(NonCancellable) {
            mutex.withLock {
                logger.v(tag = composeVideoPlayerLogTag) { "$androidVideoPlayerSubTag: deactivateAndDispose: Releasing ExoPlayer" }
                (state as? PlayerState.Initialized)
                    ?.androidPlaybackController
                    ?.exoPlayer
                    ?.release()
                state = PlayerState.Uninitialized

                if (cacheConfig is VideoPlayerCacheConfiguration.Enabled) {
                    logger.v(tag = composeVideoPlayerLogTag) { "$androidVideoPlayerSubTag: deactivateAndDispose: Releasing cache" }
                    unsubscribeOrReleaseCache(cacheConfig.path)
                }
            }
        }
    }

    override fun deactivate() {
        coroutineScope.launch {
            mutex.withLock {
                logger.v(tag = composeVideoPlayerLogTag) { "$androidVideoPlayerSubTag: deactivate: Releasing ExoPlayer" }
                (state as? PlayerState.Initialized)
                    ?.androidPlaybackController
                    ?.exoPlayer
                    ?.run {
                        wasPlayingBeforeDeactivation = this.isPlaying
                        this.release()
                    }
            }
        }
    }

    @OptIn(UnstableApi::class)
    override fun reactivate() {
        coroutineScope.launch {
            mutex.withLock {
                logger.v(tag = composeVideoPlayerLogTag) { "$androidVideoPlayerSubTag: asking to reactivate player" }
                (state as? PlayerState.Initialized)
                    ?.let { currentState ->
                        if (lastPlayerLoadUri != null && lastPlayerLoadConfig != null && currentState.androidPlaybackController.exoPlayer.isReleased) {
                            logger.v(tag = composeVideoPlayerLogTag) { "$androidVideoPlayerSubTag: reactivating player" }

                            val exoPlayer = createExoPlayer(
                                context,
                                lastPlayerLoadConfig!!.minBuffered,
                                lastPlayerLoadConfig!!.maxBuffered
                            )
                            launch { exoPlayer.observePlayerSettingsAndApply() }
                            loadByUri(
                                uri = lastPlayerLoadUri!!,
                                exoPlayer = exoPlayer,
                                onStateUpdated = { newState ->
                                    when (newState) {
                                        is PlayerState.Error -> {
                                            logger.v(tag = composeVideoPlayerLogTag) { "$androidVideoPlayerSubTag: reactivate: State updated to Error" }
                                        }
                                        PlayerState.Loading -> {
                                            logger.v(tag = composeVideoPlayerLogTag) { "$androidVideoPlayerSubTag: reactivate: State updated to Loading" }
                                        }
                                        PlayerState.Uninitialized -> {
                                            logger.v(tag = composeVideoPlayerLogTag) { "$androidVideoPlayerSubTag: reactivate: State updated to Uninitialized" }
                                        }

                                        is PlayerState.Initialized -> {
                                            when (val newSettings = newState.settings) {
                                                is VideoSettings.HLS -> {
                                                    if (currentState.settings is VideoSettings.HLS) {
                                                        newSettings.selectedFormatTrack = currentState.settings.selectedFormatTrack
                                                        newSettings.selectedSubtitleTrack = currentState.settings.selectedSubtitleTrack
                                                    }
                                                }
                                                is VideoSettings.Progressive -> {
                                                    logger.v(tag = composeVideoPlayerLogTag) { "$androidVideoPlayerSubTag: reactivate: Progressive settings update todo" }
                                                }
                                            }

                                            newState.controller.elapsed = currentState.controller.elapsed

                                            if (wasPlayingBeforeDeactivation) {
                                                newState.controller.play()
                                                logger.v(tag = composeVideoPlayerLogTag) { "$androidVideoPlayerSubTag: reactivate: Playing after reactivation" }
                                            }

                                            state = newState
                                            logger.v(tag = composeVideoPlayerLogTag) { "$androidVideoPlayerSubTag: reactivate: State updated to Initialized" }
                                        }
                                    }
                                }
                            )

                        }
                    }
            }
        }
    }

    @OptIn(UnstableApi::class)
    private suspend fun loadMedia(
        exoPlayer: ExoPlayer,
        uri: String,
        mediaSourceFactory: MediaSource.Factory? = null
    ): Result<PlayerState.Initialized, Exception> {
        logger.v(tag = composeVideoPlayerLogTag) { "$androidVideoPlayerSubTag: loadMedia: Loading media from URI: $uri" }
        val mediaItem = MediaItem.Builder().setUri(uri).build()
        val setMediaResult = if (mediaSourceFactory != null)
            exoPlayer.setMedia(mediaSourceFactory.createMediaSource(mediaItem))
        else
            exoPlayer.setMedia(mediaItem)

        return setMediaResult.map { videoSettings ->
            PlayerState.Initialized(
                controller = AndroidPlaybackController(exoPlayer),
                settings = videoSettings
            )
        }
    }

    private fun loadByUri(
        uri: String,
        exoPlayer: ExoPlayer
    ) = coroutineScope.launch {
        mutex.withLock {
            loadByUri(
                uri,
                exoPlayer,
                onStateUpdated = { state = it }
            )
        }
    }

    @OptIn(UnstableApi::class)
    private suspend inline fun loadByUri(
        uri: String,
        exoPlayer: ExoPlayer,
        onStateUpdated: (PlayerState) -> Unit,
    ) {
        logger.v(tag = composeVideoPlayerLogTag) { "$androidVideoPlayerSubTag: loadByUri: Loading URI: $uri" }
        (state as? PlayerState.Initialized)
            ?.androidPlaybackController
            ?.exoPlayer
            ?.release()

        onStateUpdated(PlayerState.Loading)

        when (cacheConfig) {
            VideoPlayerCacheConfiguration.Disabled -> {
                logger.v(tag = composeVideoPlayerLogTag) { "$androidVideoPlayerSubTag: loadByUri: Cache disabled, loading without cache" }
                loadMedia(exoPlayer, uri)
                    .onFailure { ex ->
                        val message = buildString {
                            if (ex is PlaybackException)
                                append("${ex.errorCodeName}. ")
                            append(ex.message)
                        }
                        onStateUpdated(PlayerState.Error(message, ex))
                    }
                    .onSuccess(onStateUpdated)
            }

            is VideoPlayerCacheConfiguration.Enabled -> {
                logger.v(tag = composeVideoPlayerLogTag) { "$androidVideoPlayerSubTag: loadByUri: Cache enabled, loading with cache" }
                val holder = cacheMap.getOrPut(cacheConfig.path) {
                    CacheHolder(
                        createCache(
                            cacheConfig.path,
                            context,
                            cacheConfig.maxSize.inWholeMegabytes
                        )
                    )
                }
                holder.subscribe(playerInstanceHashCode)

                val guessedFactory = if (uri.endsWith(".m3u8"))
                    HlsMediaSource.Factory(holder.cache)
                else
                    ProgressiveMediaSource.Factory(holder.cache)

                loadMedia(
                    exoPlayer = exoPlayer,
                    uri = uri,
                    mediaSourceFactory = guessedFactory
                )
                    .andThenRecover {
                        logger.d(tag = composeVideoPlayerLogTag) {
                            "$androidVideoPlayerSubTag: Guessed cache type failed; switching to HLS cache type"
                        }

                        loadMedia(
                            exoPlayer = exoPlayer,
                            uri = uri,
                            mediaSourceFactory = HlsMediaSource.Factory(holder.cache)
                        )
                    }
                    .onFailure { ex ->
                        val message = buildString {
                            if (ex is PlaybackException)
                                append("${ex.errorCodeName}. ")

                            append(ex.message)
                        }

                        onStateUpdated(PlayerState.Error(message, ex))
                    }
                    .onSuccess(onStateUpdated)
            }
        }
    }

    @OptIn(UnstableApi::class)
    private suspend fun unsubscribeOrReleaseCache(cachePath: String) {
        cacheMap[cachePath]?.let { holder ->
            holder.unsubscribe(playerInstanceHashCode)

            if (holder.subscriptionCount == 0) {
                logger.v(tag = composeVideoPlayerLogTag) { "$androidVideoPlayerSubTag: releaseCache: Releasing cache at path: $cachePath" }
                holder.cache.cache?.release()
                cacheMap.remove(cachePath)
            }
        }
    }

    @OptIn(UnstableApi::class)
    private suspend fun ExoPlayer.observePlayerSettingsAndApply() = coroutineScope {
        logger.v(tag = composeVideoPlayerLogTag) { "$androidVideoPlayerSubTag: observePlayerSettingsAndApply: Observing player settings" }
        apply {
            while (isActive && !isReleased) {
                volume = settings.volume
                repeatMode = if (settings.looping)
                    Player.REPEAT_MODE_ALL
                else
                    Player.REPEAT_MODE_OFF
                setPlaybackSpeed(settings.playbackSpeed)
                delay(16)
            }
        }
    }
}

@OptIn(UnstableApi::class)
private class CacheHolder(
    val cache: CacheDataSource.Factory
) {
    private val subscriptions = ConcurrentMutableSet<Int>()
    val subscriptionCount get() = subscriptions.size

    fun subscribe(key: Int) { subscriptions.add(key) }

    fun isSubscribed(key: Int) = subscriptions.contains(key)

    fun unsubscribe(key: Int) { subscriptions.remove(key) }
}

@OptIn(UnstableApi::class)
private fun createCache(
    cachePath: String,
    context: Context,
    maxSizeMb: Long = 512L
) = CacheDataSource.Factory().apply {
    setCache(
        SimpleCache(
            /* cacheDir = */ File(cachePath),
            /* evictor = */ LeastRecentlyUsedCacheEvictor(maxSizeMb * 1024 * 1024L),
            /* databaseProvider = */ StandaloneDatabaseProvider(context)
        )
    )
    setUpstreamDataSourceFactory(DefaultDataSource.Factory(context))
    setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
}


@OptIn(UnstableApi::class)
private fun createExoPlayer(
    context: Context,
    minToBuffer: Duration,
    maxToBuffer: Duration
): ExoPlayer {
    val renderersFactory = DefaultRenderersFactory(context)

    val loadControl = DefaultLoadControl.Builder()
        .setBufferDurationsMs(
            /* minBufferMs= */
            minToBuffer.inWholeMilliseconds.toInt(),
            /* maxBufferMs= */
            maxToBuffer.inWholeMilliseconds.toInt(),
            /* bufferForPlaybackMs= */
            minToBuffer.inWholeMilliseconds.toInt(),
            /* bufferForPlaybackAfterRebufferMs= */
            minToBuffer.inWholeMilliseconds.toInt()
        )
        .setPrioritizeTimeOverSizeThresholds(true)
        .build()

    val trackSelector = DefaultTrackSelector(context, AdaptiveTrackSelection.Factory()).apply {
        parameters = buildUponParameters()
            .setAllowVideoNonSeamlessAdaptiveness(false)
            .setTunnelingEnabled(true)
            .setAllowVideoMixedDecoderSupportAdaptiveness(true)
            .setAllowMultipleAdaptiveSelections(true)
            .build()
    }

    return ExoPlayer.Builder(context)
        .setRenderersFactory(renderersFactory)
        .setLoadControl(loadControl)
        .setUseLazyPreparation(true)
        .setTrackSelector(trackSelector)
        .build()
}

@OptIn(UnstableApi::class)
private suspend fun ExoPlayer.setMedia(
    mediaSource: MediaSource
): Result<VideoSettings, Exception> = withContext(Dispatchers.Main.immediate) {
    clearMediaItems()
    setMediaSource(mediaSource)
    prepare()

    try {
        val readyState = withTimeoutOrNull(20.seconds) { awaitReadyState() }

        readyState
            ?: Err(IllegalStateException("Timeout while waiting for player to initialize"))
    } catch (e: Exception) {
        Err(e)
    }
}

@OptIn(UnstableApi::class)
private suspend fun ExoPlayer.setMedia(
    mediaItem: MediaItem
): Result<VideoSettings, Exception> = withContext(Dispatchers.Main.immediate) {
    clearMediaItems()
    setMediaItem(mediaItem)
    prepare()

    try {
        val readyState = withTimeoutOrNull(20.seconds) { awaitReadyState() }

        readyState
            ?: Err(IllegalStateException("Timeout while waiting for player to initialize"))
    } catch (e: Exception) {
        Err(e)
    }
}

@OptIn(UnstableApi::class)
private suspend fun ExoPlayer.awaitReadyState(): Result<VideoSettings, Exception> {
    fun createVideoSettings(): VideoSettings = if (currentManifest is HlsManifest) {
        VideoSettings.HLS(AndroidHlsController())
    } else {
        VideoSettings.Progressive()
    }

    if (playbackState == Player.STATE_READY) return Ok(createVideoSettings())

    return suspendCancellableCoroutine { continuation ->
        try {
            val listener = object : Player.Listener {
                // only ok when hls controller is up and the first data
                // is loaded and ready to play immediately
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_READY) {
                        continuation.resume(Ok(createVideoSettings()))
                        removeListener(this)
                    }
                }

                override fun onPlayerError(error: PlaybackException) {
                    continuation.resume(Err(error))
                    removeListener(this)
                }
            }

            addListener(listener)
            // invokeOnCancellation is not guaranteed to be
            // called from the main thread unlike the methods
            // in the listener. accessing removeListener here
            // might result in a crash. overridden methods are
            // sufficient for removing the listener
//            continuation.invokeOnCancellation { removeListener(listener) }
        } catch (e: Exception) {
            Err(e)
        }
    }
}

val PlayerState.Initialized.androidPlaybackController get() = this.controller as AndroidPlaybackController