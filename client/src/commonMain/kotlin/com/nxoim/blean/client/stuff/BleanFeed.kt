package com.nxoim.blean.client.stuff

import co.touchlab.kermit.Logger
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.map
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import com.nxoim.blean.api.api.FeedApi
import com.nxoim.blean.api.models.feed.Feed
import com.nxoim.blean.api.models.feed.PostView
import com.nxoim.blean.api.models.feed.Reply
import com.nxoim.blean.api.models.feed.SearchedPosts
import com.nxoim.blean.api.models.feed.ThreadPost
import com.nxoim.blean.api.models.feed.ThreadSort
import com.nxoim.blean.api.models.feed.ThreadV2
import com.nxoim.blean.api.models.feed.ThreadV2Post
import com.nxoim.blean.api.utils.AuthenticationContext
import com.nxoim.blean.api.utils.LimitUpToHundred
import com.nxoim.blean.bskyPrimitives.AccountIdentificator
import com.nxoim.blean.bskyPrimitives.AtUri
import com.nxoim.blean.client.ContentRepositories
import com.nxoim.blean.client.clientModels.FeedChunk
import com.nxoim.blean.client.clientModels.FeedPost
import com.nxoim.blean.client.clientModels.SearchChunk
import com.nxoim.blean.client.clientModels.toFeedChunk
import com.nxoim.blean.repos.CacheKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

private const val logTag = "BleanFeed"

class BleanFeed(
    private val feedApi: FeedApi,
    private val onAuthenticationContextRequest: suspend () -> AuthenticationContext,
    private val contentRepository: ContentRepositories,
    private val clientCoroutineScope: CoroutineScope,
    private val logger: Logger
) {
    private val scopeContext = clientCoroutineScope.coroutineContext

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getFeedChunk(feedUri: String, index: Long): Flow<FeedChunk?> =
        transformCachedFeedToFeedChunkFlow(
            contentRepository.apiResponseCache.get(feedChunkApiCacheKey(feedUri, index))
        )

    suspend fun loadAndCachePostsForFeed(
        uri: String,
        cursor: String?,
        expectedChunkItemCount: Int = 20
    ): Result<FeedChunk, Unit> {
        logger.v(tag = logTag) { "Loading feed: $uri, cursor: $cursor" }
        return feedApi.getFeed(
            authenticationContext = onAuthenticationContextRequest(),
            feedUri = uri,
            limit = LimitUpToHundred(expectedChunkItemCount),
            cursor = cursor
        )
            .onSuccess { feed ->
                val postUrisForChunk = mutableListOf<AtUri>()
                feed.feed.forEach { feedViewPost ->
                    cacheFeedViewPostDetails(feedViewPost)
                    postUrisForChunk.add(feedViewPost.post.uri)
                }

                val newChunkIndex = contentRepository.apiResponseCache
                    .count(feedChunkApiCacheKey(uri, null))
                    .first()

                contentRepository.apiResponseCache.saveOrUpdate(
                    feedChunkApiCacheKey(uri, newChunkIndex),
                    feed
                )
                logger.v(tag = logTag) { "Saved ${postUrisForChunk.size} post URIs for feed chunk: $uri, cursor: $cursor. Next cursor: ${feed.cursor}" }
            }
            .map { it.toFeedChunk() }
            .mapError {
                logger.e(tag = logTag) { "Failed to load feed: $uri with $it" }
                Unit
            }
    }

    suspend fun refreshFeed(
        uri: String,
        expectedChunkItemCount: Int = 20
    ): Result<Unit, Throwable> = withContext(scopeContext) {
        // todo result errors
        logger.v(tag = logTag) { "Refreshing feed: $uri" }

        feedApi.getFeed(
            onAuthenticationContextRequest(),
            uri,
            LimitUpToHundred(expectedChunkItemCount)
        )
            .map { feed ->
                logger.v(tag = logTag) { "Feed refresh response successful: $uri" }

                feed.feed.forEach { feedViewPost ->
                    cacheFeedViewPostDetails(feedViewPost)
                }

                contentRepository.apiResponseCache.deleteByPrefix(
                    feedChunkApiCacheKey(
                        uri,
                        null
                    )
                )

                contentRepository.apiResponseCache.saveOrUpdate(
                    feedChunkApiCacheKey(uri, 0),
                    feed // Use the renamed 'feed' variable
                )
            }
            .onFailure {
                logger.e(tag = logTag) { "Feed refresh response failed: $uri with $it" }
            }
            .mapError { IllegalStateException(it.toString()) }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getAuthorFeedChunk(author: AccountIdentificator, index: Long): Flow<FeedChunk?> =
        transformCachedFeedToFeedChunkFlow(
            contentRepository.apiResponseCache.get(authorFeedChunkApiCacheKey(author, index))
        )

    suspend fun loadAndCachePostsForAuthorFeed(
        author: AccountIdentificator,
        cursor: String?,
        expectedChunkItemCount: Int = 20
    ): Result<FeedChunk, Unit> {
        logger.v(tag = logTag) { "Loading author feed: ${author}, cursor: $cursor" }
        return feedApi.getAuthorFeed(
            authenticationContext = onAuthenticationContextRequest(),
            actor = author,
            limit = LimitUpToHundred(expectedChunkItemCount),
            cursor = cursor
        )
            .onSuccess { actorFeed ->
                val postUrisForChunk = mutableListOf<AtUri>()
                actorFeed.feed.forEach { feedViewPost ->
                    cacheFeedViewPostDetails(feedViewPost)
                    postUrisForChunk.add(feedViewPost.post.uri)
                }

                val newChunkIndex = contentRepository.apiResponseCache
                    .count(authorFeedChunkApiCacheKey(author, null))
                    .first()

                contentRepository.apiResponseCache.saveOrUpdate(
                    authorFeedChunkApiCacheKey(author, newChunkIndex),
                    actorFeed // Save the whole ActorFeed object
                )
                logger.v(tag = logTag) { "Saved ${postUrisForChunk.size} post URIs for author feed chunk: ${author}, cursor: $cursor. Next cursor: ${actorFeed.cursor}" }
            }
            .map { it.toFeedChunk() } // Assuming ActorFeed.toFeedChunk() exists or will be created
            .mapError {
                logger.e(tag = logTag) { "Failed to load author feed: ${author} with $it" }
                Unit
            }
    }

    suspend fun refreshAuthorFeed(
        author: AccountIdentificator,
        expectedChunkItemCount: Int = 20
    ): Result<Unit, Throwable> = withContext(scopeContext) {
        logger.v(tag = logTag) { "Refreshing author feed: ${author}" }

        feedApi.getAuthorFeed(
            onAuthenticationContextRequest(),
            actor = author,
            limit = LimitUpToHundred(expectedChunkItemCount),
//            filter = FeedFilter.PostsNoReplies
        )
            .map { actorFeed ->
                logger.v(tag = logTag) { "Author feed refresh response successful: ${author}" }

                actorFeed.feed.forEach { feedViewPost ->
                    cacheFeedViewPostDetails(feedViewPost)
                }

                contentRepository.apiResponseCache.deleteByPrefix(
                    authorFeedChunkApiCacheKey(author, null)
                )
                contentRepository.apiResponseCache.saveOrUpdate(
                    authorFeedChunkApiCacheKey(author, 0),
                    actorFeed // Save the whole ActorFeed object
                )
            }
            .onFailure {
                logger.e(tag = logTag) { "Author feed refresh response failed: ${author} with $it" }
            }
            .mapError { IllegalStateException(it.toString()) }
    }


    @OptIn(ExperimentalCoroutinesApi::class)
    fun getSearchPosts(query: String, index: Long): Flow<SearchChunk?> =
        contentRepository.apiResponseCache
            .get(searchChunkApiCacheKey(query, index))
            .map {
                it
                    ?.run {
                        combine(
                            this.posts.map {
                                contentRepository.apiResponseCache.get(postViewCacheKey(it.uri))
                            }
                        ) {
                            SearchChunk(
                                cursor = cursor,
                                posts = it.mapIndexed { index, it ->
                                    it ?: run {
                                        logger.e(tag = logTag) {
                                            "Did not find post in cache during search chunk assembly. " +
                                                    "Falling back to old data in search chunk"
                                        }
                                        this.posts[index]
                                    }
                                },
                                hitsTotal = hitsTotal
                            )
                        }
                    }
                    ?: flowOf(null)
            }
            .flatMapConcat { it }

    suspend fun performInitialSearch(
        query: String,
        expectedChunkItemCount: Int = 20
    ) = withContext(scopeContext) {
        logger.v(tag = logTag) { "Searching for: $query" }

        feedApi.searchPosts(
            onAuthenticationContextRequest(),
            query,
            limit = LimitUpToHundred(expectedChunkItemCount)
        )
            .map {
                logger.v(tag = logTag) { "Search response successful for: $query" }
                contentRepository.apiResponseCache
                    .deleteByPrefix(searchChunkApiCacheKey(query, null))
                contentRepository.apiResponseCache
                    .saveOrUpdate(searchChunkApiCacheKey(query, 0), it)
            }
            .onFailure {
                logger.e(tag = logTag) { "Search response failed for: $query with $it" }
            }
            .mapError { Unit }
    }

    suspend fun clearAllSearch() = withContext(scopeContext) {
        contentRepository.apiResponseCache
            .deleteByPrefix(searchChunkApiCacheKey(null, null))
    }

    suspend fun loadAndCachePostsForSearch(
        query: String,
        cursor: String?,
        expectedChunkItemCount: Int = 20
    ): Result<SearchedPosts, Unit> {
        logger.v(tag = logTag) { "Loading more search results for: $query with cursor: $cursor" }
        return feedApi.searchPosts(
            onAuthenticationContextRequest(),
            query = query,
            cursor = cursor,
            limit = LimitUpToHundred(expectedChunkItemCount)
        )
            .onSuccess {
                contentRepository.apiResponseCache.saveOrUpdate(
                    searchChunkApiCacheKey(
                        query,
                        contentRepository.apiResponseCache.count<SearchedPosts>(
                            searchChunkApiCacheKey(query, null)
                        )
                            .first()
                    ),
                    it
                )
            }
            .onFailure { logger.e(tag = logTag) { "Failed to load more search results for: $query with $it" } }
            .mapError { Unit }
    }

    fun getPost(uri: AtUri): Flow<PostView?> =
        contentRepository.apiResponseCache.get(postViewCacheKey(uri))

    fun getPosts(uris: List<AtUri>): Flow<List<PostView>> =
        contentRepository.apiResponseCache.get(uris.map { postViewCacheKey(it) })

    suspend fun loadAndCachePost(uri: AtUri): Result<PostView?, Unit> =
        loadAndCachePosts(listOf(uri))
            .map { it.firstOrNull() }
            .mapError { Unit }


//    suspend fun loadAndCacheThread(uri: AtUri): Result<PostThread, Unit> {
//        return feedApi.getPostThread(
//            authenticationContext = onAuthenticationContextRequest(),
//            postUri = uri,
//            depth = 2
//        )
//            .onSuccess {
//                saveThreadPostRecursive(it.thread)
//            }
//            .mapError { Unit }
//    }

    suspend fun loadAndCacheThread(uri: AtUri): Result<ThreadV2, Unit> {
        return feedApi.getPostThreadV2(
            authenticationContext = onAuthenticationContextRequest(),
            anchor = uri,
            branchingFactor = 3,
            below = 6,
            sort = ThreadSort.Top
        )
            .onSuccess {
                it.thread.forEach {
                    saveThreadPostRecursive(it.value)
                }
                contentRepository.apiResponseCache.saveOrUpdate(threadCacheKey(uri, false), it)
            }
            .mapError { Unit }
    }


    suspend fun loadAndCachePosts(uris: List<AtUri>): Result<List<PostView>, Unit> {
        return feedApi.getPosts(
            authenticationContext = onAuthenticationContextRequest(),
            postUris = uris
        )
            .onFailure {
                logger.e(tag = logTag) { "Failed to load posts for uris: $uris with $it" }
            }
            .onSuccess { result ->
                result.posts.forEach {
                    contentRepository.apiResponseCache.saveOrUpdate(
                        postViewCacheKey(it.uri),
                        it
                    )
                }
            }
            .map { it.posts }
            .mapError { Unit }
    }


    fun getThreadPosts(from: AtUri, above: Boolean): Flow<ThreadV2?> =
        contentRepository.apiResponseCache
            .get(threadCacheKey(from, above))
//            .map { cached ->
//                // likely mismatch with cached post view
//                // specifically everything but visible posts arent loaded as flows
//                cached?.let {
//                    val thread = it.thread.map { threadItem ->
//                        val updatedValue = when (val value = threadItem.value) {
//                            is ThreadV2Post.Blocked -> flowOf(value)
//                            ThreadV2Post.NotFound -> flowOf(value)
//                            ThreadV2Post.UnavailableUnauthenticated -> flowOf(value)
//                            is ThreadV2Post.Visible -> value.copy(post = )
//                        }
//                        getPost(threadItem.uri).map {
//                            when (it) {
//
//                            }
//                        }
//                    }
//                    it.copy(
//
//                    )
//                }
//            }

    private suspend fun saveThreadPostRecursive(threadPost: ThreadV2Post) {
        coroutineScope {
            when (threadPost) {
                is ThreadV2Post.Visible -> {
                    contentRepository.apiResponseCache.saveOrUpdate(
                        postViewCacheKey(threadPost.post.uri),
                        threadPost.post
                    )
                }

                is ThreadV2Post.NotFound -> {
//                    contentRepository.apiResponseCache.saveOrUpdate(
//                        postViewCacheKey(uri),
//                        PostView.NotFound(uri)
//                    )
                }

                is ThreadV2Post.Blocked -> {
//                    // viewer state is empty. may need to fetch it
//                    // or maybe its not viable to
//                    contentRepository.apiResponseCache.saveOrUpdate(
//                        postViewCacheKey(uri),
//                        PostView.Blocked(uri, blocked = true, author = ProfileInfo.Blocked(threadPost.author.did, ViewerState()))
//                    )
                }

                ThreadV2Post.UnavailableUnauthenticated -> {
//                    logger.w(tag = logTag) { "Thread post unavailable unauthenticated mapped as not found in cache" }
//
//                    contentRepository.apiResponseCache.saveOrUpdate(
//                        postViewCacheKey(uri),
//                        PostView.NotFound(uri)
//                    )
                }
            }
        }
    }

    private suspend fun saveThreadPostRecursive(threadPost: ThreadPost) {
        coroutineScope {
            when (threadPost) {
                is ThreadPost.ThreadPostView -> {
                    contentRepository.apiResponseCache.saveOrUpdate(
                        postViewCacheKey(threadPost.post.uri),
                        threadPost.post
                    )
                    threadPost.parent?.let { saveThreadPostRecursive(it) }
                    threadPost.replies?.forEach { saveThreadPostRecursive(it) }
                }

                is ThreadPost.NotFound -> {
                    contentRepository.apiResponseCache.saveOrUpdate(
                        postViewCacheKey(threadPost.uri),
                        PostView.NotFound(threadPost.uri, threadPost.notFound)
                    )
                }

                is ThreadPost.Blocked -> {
                    contentRepository.apiResponseCache.saveOrUpdate(
                        postViewCacheKey(threadPost.uri),
                        PostView.Blocked(threadPost.uri, threadPost.blocked, threadPost.author)
                    )
                }
            }
        }
    }

    private suspend fun cacheFeedViewPostDetails(feedViewPost: com.nxoim.blean.api.models.feed.FeedPost) {
        contentRepository.apiResponseCache.saveOrUpdate(
            postViewCacheKey(feedViewPost.post.uri),
            feedViewPost.post
        )
        feedViewPost.reply?.root?.let { rootPost ->
            contentRepository.apiResponseCache.saveOrUpdate(
                postViewCacheKey(rootPost.uri),
                rootPost
            )
        }
        feedViewPost.reply?.parent?.let { parentPost ->
            contentRepository.apiResponseCache.saveOrUpdate(
                postViewCacheKey(parentPost.uri),
                parentPost
            )
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun transformCachedFeedToFeedChunkFlow(
        cachedFeedFlow: Flow<Feed?>
    ): Flow<FeedChunk?> =
        cachedFeedFlow.flatMapLatest { cachedFeed ->
            if (cachedFeed == null) {
                flowOf(null)
            } else {
                // Collect all URIs we need to resolve
                val uriToFallback = mutableMapOf<AtUri, PostView>()
                for (fp in cachedFeed.feed) {
                    uriToFallback[fp.post.uri] = fp.post
                    fp.reply?.let { reply ->
                        uriToFallback[reply.parent.uri] = reply.parent
                        uriToFallback[reply.root.uri] = reply.root
                    }
                }

                // Create a map of flows: each URI resolved once
                val uriFlows: Map<AtUri, Flow<PostView>> =
                    uriToFallback.mapValues { (uri, fallback) ->
                        getPost(uri).map {
                            it ?: run {
                                logger.w(tag = logTag) {
                                    "Did not find post $uri in cache during feed chunk assembly. Falling back to old data."
                                }
                                fallback
                            }
                        }
                    }

                // Combine all flows into a single state map
                combine(uriFlows.values) { postsArray ->
                    val resolvedMap = uriFlows.keys.zip(postsArray).toMap()
                    FeedChunk(
                        cursor = cachedFeed.cursor,
                        feed = cachedFeed.feed.map { fp ->
                            val resolvedPost = resolvedMap[fp.post.uri]!!
                            val reply = fp.reply?.let { reply ->
                                Reply(
                                    root = resolvedMap[reply.root.uri]!!,
                                    parent = resolvedMap[reply.parent.uri]!!,
                                    grandparentAuthor = reply.grandparentAuthor
                                )
                            }
                            FeedPost(
                                post = resolvedPost,
                                reply = reply,
                                reason = fp.reason,
                                feedContext = fp.feedContext
                            )
                        }
                    )
                }
            }
        }

//    @OptIn(ExperimentalCoroutinesApi::class)
//    private fun transformCachedFeedToFeedChunkFlow(cachedFeedFlow: Flow<Feed?>): Flow<FeedChunk?> =
//        cachedFeedFlow.flatMapLatest { cachedChunk ->
//            if (cachedChunk == null)
//                flowOf(null)
//            else {
//                val postFlows: List<Flow<FeedPost>> =
//                    cachedChunk.feed.mapIndexed { idx, feedPost ->
//                        getPost(feedPost.post.uri).flatMapConcat { postFromCache ->
//                            if (feedPost.reply != null) {
//                                val replyParentFow =
//                                    feedPost.reply!!.parent.uri.let {
//                                        getPost(it).mapNotNull {
//                                            it ?: run {
//                                                logger.w(tag = logTag) {
//                                                    "Did not find reply parent post in cache during feed chunk assembly. " +
//                                                            "Falling back to old data in feed chunk"
//                                                }
//
//                                                feedPost.reply!!.parent
//                                            }
//                                        }
//                                    }
//                                val replyRootFlow =
//                                    feedPost.reply!!.root.uri.let {
//                                        getPost(it).mapNotNull {
//                                            it ?: run {
//                                                logger.w(tag = logTag) {
//                                                    "Did not find reply root post in cache during feed chunk assembly. " +
//                                                            "Falling back to old data in feed chunk"
//                                                }
//
//                                                feedPost.reply!!.root
//                                            }
//                                        }
//                                    }
//
//                                combine(
//                                    replyRootFlow,
//                                    replyParentFow
//                                ) { replyRoot, replyParent ->
//                                    FeedPost(
//                                        post = postFromCache ?: run {
//                                            logger.w(tag = logTag) {
//                                                "Did not find post in cache during feed chunk assembly. " +
//                                                        "Falling back to old data in feed chunk"
//                                            }
//                                            feedPost.post
//                                        },
//                                        reply = Reply(
//                                            root = replyRoot,
//                                            parent = replyParent,
//                                            grandparentAuthor = feedPost.reply?.grandparentAuthor
//                                        ),
//                                        reason = feedPost.reason,
//                                        feedContext = feedPost.feedContext
//                                    )
//                                }
//                            } else {
//                                flowOf(
//                                    FeedPost(
//                                        post = postFromCache ?: run {
//                                            logger.w(tag = logTag) {
//                                                "Did not find post in cache during feed chunk assembly. " +
//                                                        "Falling back to old data in feed chunk"
//                                            }
//                                            feedPost.post
//                                        },
//                                        reply = feedPost.reply,
//                                        reason = feedPost.reason,
//                                        feedContext = feedPost.feedContext
//                                    )
//                                )
//                            }
//                        }
//                    }
//
//                combine(postFlows) { posts ->
//                    FeedChunk(
//                        cursor = cachedChunk.cursor,
//                        feed = posts.toList()
//                    )
//                }
//            }
//        }
}


private fun feedChunkApiCacheKey(feedUri: String, index: Long?) = CacheKey<Feed>(
    "feedChunk$feedUri".let {
        if (index != null) "$it$index" else it
    }
)

private fun authorFeedChunkApiCacheKey(authorHandle: AccountIdentificator, index: Long?) =
    CacheKey<Feed>(
        "authorFeedChunk${authorHandle}".let {
            if (index != null) "$it$index" else it
        }
    )

private fun searchChunkApiCacheKey(query: String?, index: Long?) = CacheKey<SearchedPosts>(
    "searchChunk"
        .let { if (query != null) "$it$query" else it }
        .let { if (index != null) "$it$index" else it }
)

private fun postViewCacheKey(postUri: AtUri) = CacheKey<PostView>(
    "postView/${postUri}"
)

private fun threadCacheKey(from: AtUri, above: Boolean) = CacheKey<ThreadV2>(
    "threadV2$above$from"
)
