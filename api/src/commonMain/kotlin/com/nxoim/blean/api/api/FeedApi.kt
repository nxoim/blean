package com.nxoim.blean.api.api

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
import com.nxoim.blean.api.utils.appendAcceptApplicationJson
import com.nxoim.blean.api.utils.appendAtprotoAcceptLabelers
import com.nxoim.blean.api.utils.appendAtprotoProxy
import com.nxoim.blean.api.utils.appendContentTypeApplicationJson
import com.nxoim.blean.api.utils.bskyAppAtprotoProxy
import com.nxoim.blean.api.utils.performAuthorizedRequest
import com.nxoim.blean.api.utils.runRequestCatching
import com.nxoim.blean.bskyPrimitives.AccountIdentificator
import com.nxoim.blean.bskyPrimitives.AtUri
import com.nxoim.blean.bskyPrimitives.Cid
import com.nxoim.blean.bskyPrimitives.Did
import io.ktor.client.HttpClient
import io.ktor.http.HttpMethod
import kotlinx.serialization.json.Json

class FeedApi(private val httpClient: HttpClient) {
    suspend fun describeFeedGenerator(
        authenticationContext: AuthenticationContext
    ): RequestResult<DescribedFeedGenerator> = runRequestCatching {
        httpClient.performAuthorizedRequest(
            authenticationContext = authenticationContext,
            httpMethod = HttpMethod.Get,
            endpoint = "/xrpc/app.bsky.feed.describeFeedGenerator",
            headers = {
                appendAcceptApplicationJson()
                appendContentTypeApplicationJson()
            }
        )
    }

    suspend fun getActorFeeds(
        authenticationContext: AuthenticationContext,
        actor: AccountIdentificator.Handle,
        limit: LimitUpToHundred? = LimitUpToHundred(50),
        cursor: String? = null
    ): RequestResult<ActorFeeds> = runRequestCatching {
        httpClient.performAuthorizedRequest(
            authenticationContext = authenticationContext,
            httpMethod = HttpMethod.Get,
            endpoint = "/xrpc/app.bsky.feed.getActorFeeds",
            headers = {
                appendContentTypeApplicationJson()
            },
            parameters = {
                append("actor", actor.toString())
                if (limit != null) append("limit", limit.value.toString())
                if (cursor != null) append("cursor", cursor)
            }
        )
    }

    suspend fun getActorLikes(
        authenticationContext: AuthenticationContext,
        actor: AccountIdentificator.Handle,
        limit: LimitUpToHundred? = LimitUpToHundred(50),
        cursor: String? = null
    ): RequestResult<ActorLikes> = runRequestCatching {
        httpClient.performAuthorizedRequest(
            authenticationContext = authenticationContext,
            httpMethod = HttpMethod.Get,
            endpoint = "/xrpc/app.bsky.feed.getActorLikes",
            headers = {
                appendAcceptApplicationJson()
            },
            parameters = {
                append("actor", actor.toString())
                if (limit != null) append("limit", limit.value.toString())
                if (cursor != null) append("cursor", cursor)
            }
        )
    }

    suspend fun getAuthorFeed(
        authenticationContext: AuthenticationContext,
        actor: AccountIdentificator,
        limit: LimitUpToHundred? = LimitUpToHundred(50),
        cursor: String? = null,
        filter: FeedFilter? = FeedFilter.PostsWithReplies
    ): RequestResult<Feed> = runRequestCatching {
        httpClient.performAuthorizedRequest(
            authenticationContext = authenticationContext,
            httpMethod = HttpMethod.Get,
            endpoint = "/xrpc/app.bsky.feed.getAuthorFeed",
            headers = {
                appendAcceptApplicationJson()
            },
            parameters = {
                append("actor", actor.toString())
                if (limit != null) append("limit", limit.value.toString())
                if (cursor != null) append("cursor", cursor)
                if (filter != null) append("filter", filter.requestParameterBody)
            }
        )
    }

    suspend fun getFeedGenerator(
        authenticationContext: AuthenticationContext,
        feedUri: String // is string because can be equal to "following"
    ): RequestResult<FeedGenerator> = runRequestCatching {
        httpClient.performAuthorizedRequest(
            authenticationContext = authenticationContext,
            httpMethod = HttpMethod.Get,
            endpoint = "/xrpc/app.bsky.feed.getFeedGenerator",
            headers = {
                appendAcceptApplicationJson()
            },
            parameters = { append("feed", feedUri.toString()) }
        )
    }

    suspend fun getFeedGenerators(
        authenticationContext: AuthenticationContext,
        feedUris: List<String>
    ): RequestResult<FeedGenerators> = runRequestCatching {
        httpClient.performAuthorizedRequest(
            authenticationContext = authenticationContext,
            httpMethod = HttpMethod.Get,
            endpoint = "/xrpc/app.bsky.feed.getFeedGenerators",
            headers = {
                appendAcceptApplicationJson()
            },
            parameters = { appendAll("feeds", feedUris) }
        )
    }

    suspend fun getFeedSkeleton(
        authenticationContext: AuthenticationContext,
        feedUri: String,
        limit: LimitUpToHundred? = LimitUpToHundred(50),
        cursor: String? = null
    ): RequestResult<FeedSkeleton> = runRequestCatching {
        httpClient.performAuthorizedRequest(
            authenticationContext = authenticationContext,
            httpMethod = HttpMethod.Get,
            endpoint = "/xrpc/app.bsky.feed.getFeedSkeleton",
            headers = {
                appendAcceptApplicationJson()
            },
            parameters = {
                append("feed", feedUri.toString())
                if (limit != null) append("limit", limit.value.toString())
                if (cursor != null) append("cursor", cursor)
            }
        )
    }

    suspend fun getFeed(
        authenticationContext: AuthenticationContext,
        feedUri: String,
        limit: LimitUpToHundred? = LimitUpToHundred(50),
        cursor: String? = null
    ): RequestResult<Feed> = runRequestCatching {
        httpClient.performAuthorizedRequest(
            authenticationContext = authenticationContext,
            httpMethod = HttpMethod.Get,
            endpoint = "/xrpc/app.bsky.feed.getFeed",
            headers = {
                appendAcceptApplicationJson()
            },
            parameters = {
                append("feed", feedUri.toString())
                if (limit != null) append("limit", limit.value.toString())
                if (cursor != null) append("cursor", cursor)
            }
        )
    }

    suspend fun getLikes(
        authenticationContext: AuthenticationContext,
        postUri: AtUri,
        cid: Cid? = null,
        limit: LimitUpToHundred? = LimitUpToHundred(50),
        cursor: String? = null
    ): RequestResult<Likes> = runRequestCatching {
        httpClient.performAuthorizedRequest(
            authenticationContext = authenticationContext,
            httpMethod = HttpMethod.Get,
            endpoint = "/xrpc/app.bsky.feed.getLikes",
            headers = {
                appendAcceptApplicationJson()
            },
            parameters = {
                append("uri", postUri.toString())
                if (cid != null) append("cid", cid.toString())
                if (limit != null) append("limit", limit.value.toString())
                if (cursor != null) append("cursor", cursor)
            }
        )
    }

    suspend fun getListFeed(
        authenticationContext: AuthenticationContext,
        listUri: AtUri,
        limit: LimitUpToHundred? = LimitUpToHundred(50),
        cursor: String? = null
    ): RequestResult<ListFeed> = runRequestCatching {
        httpClient.performAuthorizedRequest(
            authenticationContext = authenticationContext,
            httpMethod = HttpMethod.Get,
            endpoint = "/xrpc/app.bsky.feed.getListFeed",
            headers = {
                appendAcceptApplicationJson()
            },
            parameters = {
                append("list", listUri.toString())
                if (limit != null) append("limit", limit.value.toString())
                if (cursor != null) append("cursor", cursor)
            }
        )
    }

    suspend fun getPostThreadV2(
        authenticationContext: AuthenticationContext,
        anchor: AtUri,
        above: Boolean = false,
        below: Int = 6,
        branchingFactor: Int = 10,
        prioritizeFollowedUsers: Boolean = true,
        sort: ThreadSort = ThreadSort.Oldest
    ): RequestResult<ThreadV2> = runRequestCatching {
        require(below in threadV2PostAmountRange)
        require(branchingFactor in threadV2PostBranchingRange)

        httpClient.performAuthorizedRequest(
            authenticationContext,
            HttpMethod.Get,
            "/xrpc/app.bsky.unspecced.getPostThreadV2",
            headers = {
                appendAcceptApplicationJson()
            },
            parameters = {
                append("anchor", anchor.toString())
                append("above", above.toString())
                append("below", below.toString())
                append("branchingFactor", branchingFactor.toString())
                append("prioritizeFollowedUsers", prioritizeFollowedUsers.toString())
                append("sort", sort.value)
            }
        )
    }

    suspend fun getPostThread(
        authenticationContext: AuthenticationContext,
        postUri: AtUri,
        depth: Int? = 6,
        parentHeight: Int? = 80
    ): RequestResult<PostThread> = runRequestCatching {
        if (depth != null) require(depth <= 1000) {
            "depth in getPostThread can not be more than 1000"
        }

        if (parentHeight != null) require(parentHeight <= 1000) {
            "parentHeight in getPostThread can not be more than 1000"
        }

        httpClient.performAuthorizedRequest(
            authenticationContext = authenticationContext,
            httpMethod = HttpMethod.Get,
            endpoint = "/xrpc/app.bsky.feed.getPostThread",
            headers = {
                appendAcceptApplicationJson()
            },
            parameters = {
                append("uri", postUri.toString())
                if (depth != null) append("depth", depth.toString())
                if (parentHeight != null) append("parentheight", parentHeight.toString())
            }
        )
    }

    suspend fun getPosts(
        authenticationContext: AuthenticationContext,
        postUris: List<AtUri>
    ): RequestResult<Posts> = runRequestCatching {
        require(postUris.size <= 25) {
            "uris in GetPosts request can not be more than 25"
        }

        httpClient.performAuthorizedRequest(
            authenticationContext = authenticationContext,
            httpMethod = HttpMethod.Get,
            endpoint = "/xrpc/app.bsky.feed.getPosts",
            headers = {
                appendAcceptApplicationJson()
            },
            parameters = { appendAll("uris", postUris.map { it.toString() }) }
        )
    }

    suspend fun getQuotes(
        authenticationContext: AuthenticationContext,
        postUri: AtUri,
        cid: Cid? = null,
        limit: LimitUpToHundred? = LimitUpToHundred(50),
        cursor: String? = null
    ): RequestResult<Quotes> = runRequestCatching {
        httpClient.performAuthorizedRequest(
            authenticationContext = authenticationContext,
            httpMethod = HttpMethod.Get,
            endpoint = "/xrpc/app.bsky.feed.getQuotes",
            headers = {
                appendAcceptApplicationJson()
            },
            parameters = {
                append("uri", postUri.toString())
                if (cid != null) append("cid", cid.toString())
                if (limit != null) append("limit", limit.value.toString())
                if (cursor != null) append("cursor", cursor)
            }
        )
    }

    suspend fun getRepostedBy(
        authenticationContext: AuthenticationContext,
        postUri: AtUri,
        cid: Cid? = null,
        limit: LimitUpToHundred? = LimitUpToHundred(50),
        cursor: String? = null
    ): RequestResult<RepostedBy> = runRequestCatching {
        httpClient.performAuthorizedRequest(
            authenticationContext = authenticationContext,
            httpMethod = HttpMethod.Get,
            endpoint = "/xrpc/app.bsky.feed.getRepostedBy",
            headers = {
                appendAcceptApplicationJson()
            },
            parameters = {
                append("uri", postUri.toString())
                if (cid != null) append("cid", cid.toString())
                if (limit != null) append("limit", limit.value.toString())
                if (cursor != null) append("cursor", cursor)
            }
        )
    }

    suspend fun getTimeline(
        authenticationContext: AuthenticationContext,
        algorithm: String, // todo algorithm as enum or something?
        limit: LimitUpToHundred? = LimitUpToHundred(50),
        cursor: String? = null
    ): RequestResult<Timeline> = runRequestCatching {
        httpClient.performAuthorizedRequest(
            authenticationContext = authenticationContext,
            httpMethod = HttpMethod.Get,
            endpoint = "/xrpc/app.bsky.feed.getTimeline",
            headers = {
                appendAcceptApplicationJson()
            },
            parameters = {
                append("algorithm", algorithm)
                if (limit != null) append("limit", limit.value.toString())
                if (cursor != null) append("cursor", cursor)
            }
        )
    }

    suspend fun searchPosts(
        authenticationContext: AuthenticationContext,
        query: String,
        sort: SearchSort? = SearchSort.Latest,
        since: String? = null,
        until: String? = null,
        mentions: AccountIdentificator.Handle? = null,
        author: AccountIdentificator.Handle? = null,
        lang: LanguageString? = null,
        domain: String? = null,
        url: UriString? = null,
        tag: List<String>? = null,
        limit: LimitUpToHundred? = LimitUpToHundred(50),
        cursor: String? = null
    ): RequestResult<SearchedPosts> = runRequestCatching {
        if (tag != null) require(tag.any { it.length <= 640 }) {
            "tag must not contain more than 640 characters"
        }

        httpClient.performAuthorizedRequest(
            authenticationContext = authenticationContext,
            httpMethod = HttpMethod.Get,
            endpoint = "/xrpc/app.bsky.feed.searchPosts",
            headers = {
                appendAcceptApplicationJson()
            },
            parameters = {
                append("q", query)
                if (sort != null) append("sort", sort.requestParameterBody)
                if (since != null) append("since", since)
                if (until != null) append("until", until)
                if (mentions != null) append("mentions", mentions.toString())
                if (author != null) append("author", author.toString())
                if (lang != null) append("lang", lang)
                if (domain != null) append("domain", domain)
                if (url != null) append("url", url.toString())
                if (tag != null) append("tag", tag.joinToString(","))
                if (limit != null) append("limit", limit.value.toString())
                if (cursor != null) append("cursor", cursor)
            }
        )
    }

    suspend fun sendInteractions(
        authenticationContext: AuthenticationContext,
        interactions: FeedInteractions,
        acceptLabelers: List<Did> = emptyList(),
        atprotoProxy: String = bskyAppAtprotoProxy,
    ): RequestResult<UnknownInteractions> = runRequestCatching {
        httpClient.performAuthorizedRequest(
            authenticationContext = authenticationContext,
            httpMethod = HttpMethod.Post,
            endpoint = "/xrpc/app.bsky.feed.sendInteractions",
            headers = {
                appendAcceptApplicationJson()
                appendContentTypeApplicationJson()
                appendAtprotoProxy(atprotoProxy)
                if (acceptLabelers.isNotEmpty()) appendAtprotoAcceptLabelers(acceptLabelers)
            },
            body = interactions.also {
                println(Json.encodeToString(interactions))
            }
        )
    }
}

private val threadV2PostAmountRange = 0..20
private val threadV2PostBranchingRange = 0..100