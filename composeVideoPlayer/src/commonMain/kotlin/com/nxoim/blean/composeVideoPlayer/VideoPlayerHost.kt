package com.nxoim.blean.composeVideoPlayer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import co.touchlab.kermit.Logger
import co.touchlab.stately.collections.ConcurrentMutableMap
import co.touchlab.stately.collections.ConcurrentMutableSet
import com.nxoim.blean.composeVideoPlayer.utils.Cache

class VideoPlayerHost(
    private val logger: Logger,
    private val maxInactiveInstances: Int = 5,
    private val factory: (PlayerSettings) -> VideoPlayer
) {
    private val subscriptions = ConcurrentMutableMap<String, ConcurrentMutableSet<Long>>()

    private val store = Cache<String, VideoPlayer>(
        maxInactiveInstances,
        onEvictionRequest = { key, player, evict ->
            if (subscriptions[key]!!.isEmpty())  {
                evict()
                player.deactivateAndDispose()
                subscriptions.remove(key)
            }
        }
    )

    internal fun getOrCreate(key: String, playerSettings: () -> PlayerSettings, place: Long): VideoPlayer {
        if (subscriptions[key] == null)
            subscriptions[key] = ConcurrentMutableSet<Long>().apply { add(place) }
        else
            subscriptions[key]!!.add(place)

        return store.getOrPut(key) {factory(playerSettings()) }.also {
            it.reactivate()
        }
    }

    @Suppress("MemberExtensionConflict")
    internal fun removeSubscription(
        key: String,
        place: Long,
        onWasLastSubscription: () -> Unit = { }
    ) {
        subscriptions[key]?.remove(place)
        if (subscriptions[key]?.isEmpty() == true) {
            store[key]?.deactivate()
            onWasLastSubscription()
        }
    }
}

val LocalVideoPlayerHost = staticCompositionLocalOf<VideoPlayerHost> {
    error("LocalVideoPlayerStore not initialized")
}

@Composable
expect fun rememberVideoPlayerHost(
    maxInactiveInstances: Int = 5,
    cacheConfig: VideoPlayerCacheConfiguration = VideoPlayerCacheConfiguration.Disabled,
    logger: Logger = Logger
): VideoPlayerHost

