package com.nxoim.blean.ui.screens.content.profile

import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.nxoim.blean.bskyPrimitives.AtUri
import com.nxoim.blean.postRelatedCommons.PageableCursoredFeed
import com.nxoim.blean.postRelatedCommons.models.ChunkCursor
import com.nxoim.blean.postRelatedCommons.models.PostContainer
import com.nxoim.blean.postRelatedCommons.models.PostIdentification
import com.nxoim.blean.postRelatedCommons.models.PostType
import com.nxoim.blean.postRelatedCommons.models.optimisticIsLiked
import com.nxoim.blean.postRelatedCommons.models.userInteractionInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class ProfileModel(
    private val source: ProfileSource,
    val navigation: ProfileNavigation,
    private val coroutineScope: CoroutineScope
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
            val userInteractionInfo = targetPost.userInteractionInfo

            if (userInteractionInfo != null) {
                if (userInteractionInfo.likeStatus.optimisticIsLiked) {
                    source.scheduleUnlike(
                        id = targetPost.identification,
                    )
                } else {
                    source.scheduleLike(targetPost.identification)
                }
            }
        }
    }
}

interface ProfileSource {
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


interface OwnControls {

}

interface ProfileNavigation {
    fun openPost(post: AtUri)
    fun openPost(post: PostContainer<PostType>)
}