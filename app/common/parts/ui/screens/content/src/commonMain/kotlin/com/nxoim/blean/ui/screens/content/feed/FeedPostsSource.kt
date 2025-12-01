package com.nxoim.blean.ui.screens.content.feed

import com.github.michaelbull.result.Result
import com.nxoim.blean.postRelatedCommons.models.ChunkCursor
import com.nxoim.blean.postRelatedCommons.models.PostContainer
import com.nxoim.blean.postRelatedCommons.models.PostIdentification
import com.nxoim.blean.postRelatedCommons.models.PostType
import kotlinx.coroutines.flow.Flow

interface FeedPostsSource {
    fun getFeedChunkPosts(
        index: Long
    ): Flow<Pair<ChunkCursor, List<PostContainer<PostType.Feed>>>?>

    suspend fun loadAndCachePostsForFeed(
        cursor: String?
    ): Result<*, *>

    suspend fun refreshFeed(): Result<*, *>


    suspend fun scheduleLike(id: PostIdentification): com.github.michaelbull.result.Result<*, *>
    suspend fun scheduleUnlike(id: PostIdentification): Result<*, *>
}
