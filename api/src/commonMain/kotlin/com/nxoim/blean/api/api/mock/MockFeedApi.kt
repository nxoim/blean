package com.nxoim.blean.api.api.mock

import com.nxoim.blean.api.api.FeedApi
import com.nxoim.blean.api.models.commonParts.LanguageString
import com.nxoim.blean.api.models.commonParts.UriString
import com.nxoim.blean.api.models.feed.ActorFeeds
import com.nxoim.blean.api.models.feed.ActorLikes
import com.nxoim.blean.api.models.feed.DescribedFeedGenerator
import com.nxoim.blean.api.models.feed.Feed
import com.nxoim.blean.api.models.feed.FeedFilter
import com.nxoim.blean.api.models.feed.FeedGenerator
import com.nxoim.blean.api.models.feed.FeedGenerators
import com.nxoim.blean.api.models.feed.FeedInteractions
import com.nxoim.blean.api.models.feed.FeedSkeleton
import com.nxoim.blean.api.models.feed.Likes
import com.nxoim.blean.api.models.feed.ListFeed
import com.nxoim.blean.api.models.feed.PostThread
import com.nxoim.blean.api.models.feed.Posts
import com.nxoim.blean.api.models.feed.Quotes
import com.nxoim.blean.api.models.feed.RepostedBy
import com.nxoim.blean.api.models.feed.SearchSort
import com.nxoim.blean.api.models.feed.SearchedPosts
import com.nxoim.blean.api.models.feed.ThreadSort
import com.nxoim.blean.api.models.feed.ThreadV2
import com.nxoim.blean.api.models.feed.Timeline
import com.nxoim.blean.api.models.feed.UnknownInteractions
import com.nxoim.blean.api.utils.AuthenticationContext
import com.nxoim.blean.api.utils.LimitUpToHundred
import com.nxoim.blean.api.utils.RequestResult
import com.nxoim.blean.bskyPrimitives.AccountIdentificator
import com.nxoim.blean.bskyPrimitives.AtUri
import com.nxoim.blean.bskyPrimitives.Cid
import com.nxoim.blean.bskyPrimitives.Did

open class _MockFeedApi : FeedApi {
    override suspend fun describeFeedGenerator(authenticationContext: AuthenticationContext): RequestResult<DescribedFeedGenerator> {
        TODO("Not yet implemented")
    }

    override suspend fun getActorFeeds(
        authenticationContext: AuthenticationContext,
        actor: AccountIdentificator.Handle,
        limit: LimitUpToHundred?,
        cursor: String?
    ): RequestResult<ActorFeeds> {
        TODO("Not yet implemented")
    }

    override suspend fun getActorLikes(
        authenticationContext: AuthenticationContext,
        actor: AccountIdentificator.Handle,
        limit: LimitUpToHundred?,
        cursor: String?
    ): RequestResult<ActorLikes> {
        TODO("Not yet implemented")
    }

    override suspend fun getAuthorFeed(
        authenticationContext: AuthenticationContext,
        actor: AccountIdentificator,
        limit: LimitUpToHundred?,
        cursor: String?,
        filter: FeedFilter?
    ): RequestResult<Feed> {
        TODO("Not yet implemented")
    }

    override suspend fun getFeedGenerator(
        authenticationContext: AuthenticationContext,
        feedUri: String
    ): RequestResult<FeedGenerator> {
        TODO("Not yet implemented")
    }

    override suspend fun getFeedGenerators(
        authenticationContext: AuthenticationContext,
        feedUris: List<String>
    ): RequestResult<FeedGenerators> {
        TODO("Not yet implemented")
    }

    override suspend fun getFeedSkeleton(
        authenticationContext: AuthenticationContext,
        feedUri: String,
        limit: LimitUpToHundred?,
        cursor: String?
    ): RequestResult<FeedSkeleton> {
        TODO("Not yet implemented")
    }

    override suspend fun getFeed(
        authenticationContext: AuthenticationContext,
        feedUri: String,
        limit: LimitUpToHundred?,
        cursor: String?
    ): RequestResult<Feed> {
        TODO("Not yet implemented")
    }

    override suspend fun getLikes(
        authenticationContext: AuthenticationContext,
        postUri: AtUri,
        cid: Cid?,
        limit: LimitUpToHundred?,
        cursor: String?
    ): RequestResult<Likes> {
        TODO("Not yet implemented")
    }

    override suspend fun getListFeed(
        authenticationContext: AuthenticationContext,
        listUri: AtUri,
        limit: LimitUpToHundred?,
        cursor: String?
    ): RequestResult<ListFeed> {
        TODO("Not yet implemented")
    }

    override suspend fun getPostThreadV2(
        authenticationContext: AuthenticationContext,
        anchor: AtUri,
        above: Boolean,
        below: Int,
        branchingFactor: Int,
        prioritizeFollowedUsers: Boolean,
        sort: ThreadSort
    ): RequestResult<ThreadV2> {
        TODO("Not yet implemented")
    }

    override suspend fun getPostThread(
        authenticationContext: AuthenticationContext,
        postUri: AtUri,
        depth: Int?,
        parentHeight: Int?
    ): RequestResult<PostThread> {
        TODO("Not yet implemented")
    }

    override suspend fun getPosts(
        authenticationContext: AuthenticationContext,
        postUris: List<AtUri>
    ): RequestResult<Posts> {
        TODO("Not yet implemented")
    }

    override suspend fun getQuotes(
        authenticationContext: AuthenticationContext,
        postUri: AtUri,
        cid: Cid?,
        limit: LimitUpToHundred?,
        cursor: String?
    ): RequestResult<Quotes> {
        TODO("Not yet implemented")
    }

    override suspend fun getRepostedBy(
        authenticationContext: AuthenticationContext,
        postUri: AtUri,
        cid: Cid?,
        limit: LimitUpToHundred?,
        cursor: String?
    ): RequestResult<RepostedBy> {
        TODO("Not yet implemented")
    }

    override suspend fun getTimeline(
        authenticationContext: AuthenticationContext,
        algorithm: String,
        limit: LimitUpToHundred?,
        cursor: String?
    ): RequestResult<Timeline> {
        TODO("Not yet implemented")
    }

    override suspend fun searchPosts(
        authenticationContext: AuthenticationContext,
        query: String,
        sort: SearchSort?,
        since: String?,
        until: String?,
        mentions: AccountIdentificator.Handle?,
        author: AccountIdentificator.Handle?,
        lang: LanguageString?,
        domain: String?,
        url: UriString?,
        tag: List<String>?,
        limit: LimitUpToHundred?,
        cursor: String?
    ): RequestResult<SearchedPosts> {
        TODO("Not yet implemented")
    }

    override suspend fun sendInteractions(
        authenticationContext: AuthenticationContext,
        interactions: FeedInteractions,
        acceptLabelers: List<Did>,
        atprotoProxy: String
    ): RequestResult<UnknownInteractions> {
        TODO("Not yet implemented")
    }
}