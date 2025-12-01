package com.nxoim.blean.ui.screens.content.thread

import com.github.michaelbull.result.Result
import com.nxoim.blean.bskyPrimitives.AtUri
import com.nxoim.blean.postRelatedCommons.PageableFeed
import com.nxoim.blean.postRelatedCommons.models.PostContainer
import com.nxoim.blean.postRelatedCommons.models.PostIdentification
import com.nxoim.blean.postRelatedCommons.models.PostType
import com.nxoim.blean.postRelatedCommons.models.optimisticIsLiked
import com.nxoim.blean.postRelatedCommons.models.userInteractionInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ThreadModel(
    val navigation: ThreadNavigation,
    val postUri: AtUri,
    private val initialVisible: PostContainer<PostType>?,
    private val source: ThreadSource,
    private val coroutineScope: CoroutineScope
) {
    val post = source.getPost()
        .stateIn(
            coroutineScope,
            SharingStarted.WhileSubscribed(),
            initialVisible
        )

    private val feed = PageableFeed<PostContainer<PostType.Thread>>(
        scope = coroutineScope,
        fetchChunk = { index -> source.getReplies(index) },
        onRefresh = { source.refresh() }
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

interface ThreadNavigation {
    fun back()
    fun openPost(post: AtUri)
    fun openPost(post: PostContainer<PostType>)
}

interface ThreadSource {
    fun getPost(): Flow<PostContainer<PostType.Thread>?>
    fun getReplies(pageIndex: Int): Flow<List<PostContainer<PostType.Thread>>?>
    suspend fun refresh(): Result<*, *>
    suspend fun loadAndCacheReplies(from: AtUri): Result<*, *>

    suspend fun scheduleLike(id: PostIdentification): Result<*, *>
    suspend fun scheduleUnlike(id: PostIdentification): Result<*, *>
}