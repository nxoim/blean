package com.nxoim.blean.ui.screens.content.sourceImplementations

import co.touchlab.kermit.Logger
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.map
import com.nxoim.blean.client.BleanClient
import com.nxoim.blean.postRelatedCommons.FeedDataAggregator
import com.nxoim.blean.postRelatedCommons.models.ChunkCursor
import com.nxoim.blean.postRelatedCommons.models.PostContainer
import com.nxoim.blean.postRelatedCommons.models.PostType
import com.nxoim.blean.ui.screens.content.search.SearchSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SearchSourceImpl(
    private val client: BleanClient.LoggedIn,
    private val logger: Logger
) : SearchSource {
    val expectedChunkItemCount = 20

    override fun get(
        query: String,
        index: Long
    ): Flow<Pair<ChunkCursor, List<PostContainer<PostType.Search>>>?> =
        client.feed
            .getSearchPosts(query, index)
            .map {
                with(logger) {
                    it?.let {
                        val cursor =
                            if (it.posts.size < expectedChunkItemCount && it.cursor == null)
                                ChunkCursor.None
                            else
                                ChunkCursor.Next(it.cursor)

                        cursor to it.posts.map { FeedDataAggregator.buildSearchPost(it) }
                    }
                }
            }

    override suspend fun clearAndPerformFirstSearch(query: String): Result<Unit, Unit> =
        client.feed.performInitialSearch(query, expectedChunkItemCount)

    override suspend fun loadAndCache(query: String, cursor: String?): Result<Unit, Unit> =
        client.feed.loadAndCachePostsForSearch(query, cursor, expectedChunkItemCount)
            .map { Unit }

    override suspend fun clearAllSearch() =
        client.feed.clearAllSearch()
}