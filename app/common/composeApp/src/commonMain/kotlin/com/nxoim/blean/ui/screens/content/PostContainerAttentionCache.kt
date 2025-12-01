package com.nxoim.blean.ui.screens.content

import androidx.collection.LruCache
import com.nxoim.blean.bskyPrimitives.AtUri
import com.nxoim.blean.postRelatedCommons.models.PostContainer

class PostContainerAttentionCache(
    private val maxCapacity: Int = 5
) {
    private val cache = LruCache<AtUri, PostContainer<*>>(maxCapacity)

    fun get(key: AtUri): PostContainer<*>? = cache[key]

    fun set(value: PostContainer<*>) {
        cache.put(value.uri, value)
    }

    // todo receive on low memory signal
    fun clear() {

    }
}