package com.nxoim.blean.ui.screens.content.feed

import androidx.lifecycle.Lifecycle
import co.touchlab.kermit.Logger
import com.github.michaelbull.result.Ok
import com.nxoim.blean.bskyPrimitives.AtUri
import com.nxoim.blean.commonThingsDumpster.childCoroutineScope
import com.nxoim.blean.postRelatedCommons.PageableCursoredFeed
import com.nxoim.blean.postRelatedCommons.models.ChunkCursor
import com.nxoim.blean.postRelatedCommons.models.PostContainer
import com.nxoim.blean.postRelatedCommons.models.PostType
import com.nxoim.blean.postRelatedCommons.models.optimisticIsLiked
import com.nxoim.blean.postRelatedCommons.models.userInteractionInfo
import com.nxoim.blean.ui.screens.content.feed.models.SavedFeed
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FeedHostModel(
    private val logger: Logger,
    private val feedSource: FeedSource,
    private val feedPostsSourceFactory: (feedUri: String) -> FeedPostsSource,
    private val navigation: FeedNavigation,
    private val coroutineScope: CoroutineScope,
    private val lifecycle: androidx.lifecycle.Lifecycle
) {
    val feedsModel = FeedsModel(coroutineScope, feedSource)
    private val feedPostsModelCache = mutableMapOf<String, FeedPostsModel>()

    fun getOrCreateFeedModel(
        savedFeed: SavedFeed
    ) = feedPostsModelCache.getOrPut(savedFeed.uri) {
        val coroutineScope = coroutineScope.childCoroutineScope()
        val source = feedPostsSourceFactory(savedFeed.uri)
        // creating a separate flow for feed info so ir's
        // completely unrelated to pagination where feed info's could
        // be thrown away from memory and added back etc
        val feedInfo = feedSource
            .getSavedFeed(savedFeed.uri)
            .filterNotNull() // use the last known value
            .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), savedFeed)

        FeedPostsModel(
            feedInfo,
            coroutineScope,
            source,
            navigation
        )
    }
}

class FeedPostsModel(
    val feedInfo: StateFlow<SavedFeed>,
    private val coroutineScope: CoroutineScope,
    private val source: FeedPostsSource,
    val navigation: FeedNavigation
) {
    private val feed = PageableCursoredFeed(
        scope = coroutineScope,
        fetchChunk = { index -> source.getFeedChunkPosts(index) },
        onFirstPageEmpty = { source.refreshFeed() },
        onNextPageEmpty = { cursor ->
            when (cursor) {
                is ChunkCursor.Next -> source.loadAndCachePostsForFeed(cursor.value)
                is ChunkCursor.None -> Ok(Unit)
            }
        }
    )

    val pageable = feed.pageable
    val refreshState = feed.refreshState
    fun refresh() = feed.refresh()

    fun toggleLike(targetPost: PostType) {
        coroutineScope.launch {
            println("$logTag: Toggling like for post: ${targetPost.identification}")
            val userInteractionInfo = targetPost.userInteractionInfo

            if (userInteractionInfo != null) {
                if (userInteractionInfo.likeStatus.optimisticIsLiked) {
                    println("$logTag: Scheduling unlike for post: ${targetPost.identification}")
                    source.scheduleUnlike(
                        id = targetPost.identification,
                    )
                } else {
                    println("$logTag: Scheduling like for post: ${targetPost.identification}")
                    source.scheduleLike(targetPost.identification)
                }
            } else {
                println("$logTag: Cannot toggle like for post ${targetPost.identification} interaction info is null.")
            }
        }
    }
}

class FeedsModel(
    private val coroutineScope: CoroutineScope,
    private val feedSource: FeedSource
) {
    val feeds = feedSource.getFeeds().filterNotNull().stateIn(
        coroutineScope,
        SharingStarted.WhileSubscribed(),
        emptyList()
    )
}

private const val logTag = "FeedHostModel"

interface FeedNavigation {
    fun openPost(post: AtUri)
    fun openPost(post: PostContainer<PostType>)
}