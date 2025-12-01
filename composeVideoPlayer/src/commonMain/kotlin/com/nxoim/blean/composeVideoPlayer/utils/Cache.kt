package com.nxoim.blean.composeVideoPlayer.utils

import co.touchlab.stately.collections.ConcurrentMutableMap


class Cache<K, V>(
    private val maxSize: Int,
    private val onEvictionRequest: (key: K, value: V, evict: () -> Unit) -> Unit = { _, _, evict -> evict() }
) {
    private val cache = ConcurrentMutableMap<K, V>()
    val size get() = cache.size

    fun put(key: K, value: V) {
        cache[key] = value
        evictIfNeeded()
    }

    operator fun get(key: K): V? = cache[key]

    fun getOrPut(key: K, factory: () -> V): V = cache.getOrPut(key) {
        evictIfNeeded()
        factory()
    }

    fun invalidateForcefully(key: K) = cache.remove(key)

    fun invalidateEverythingForcefully() = cache.clear()

    private fun evictIfNeeded() {
        if (maxSize == 0) {
            invalidateEverythingForcefully()
            return
        }

        if (cache.size > maxSize && maxSize > 0) {
            val keyToEvict = cache.keys.first()
            val valueToEvict = cache[keyToEvict]

            if (valueToEvict != null) {
                val evict: () -> Unit = { invalidateForcefully(keyToEvict) }

                onEvictionRequest(keyToEvict, valueToEvict, evict)
            } else {
                cache.remove(cache.keys.first())
            }
        }
    }
}