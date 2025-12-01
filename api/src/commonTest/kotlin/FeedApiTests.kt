import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import com.nxoim.blean.api.api.FeedApi
import com.nxoim.blean.api.api.SessionApi
import com.nxoim.blean.api.models.feed.FeedInteraction
import com.nxoim.blean.api.models.feed.FeedInteractions
import com.nxoim.blean.api.models.feed.Interaction
import com.nxoim.blean.api.utils.LimitUpToHundred
import com.nxoim.blean.bskyPrimitives.AccountIdentificator
import com.nxoim.blean.bskyPrimitives.AtUri
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class FeedApiTests {
    val sessionApi = SessionApi(createTestClient().value)
    val jwtAuthContext = runBlocking { sessionApi.getDefaultApiToken() }
    val feedApi = FeedApi(createTestClient().value)
//    val feedApi = MockFeedApi()

    // broken? "xrpc not supported"
//    @Test
//    fun DescribeFeedGenerator() = runBlocking {
//        val response = feedApi.describeFeedGenerator()
//
//        if (response is DescribeFeedGenerator.Response.Error)
//            error("Could not describe feed generator. Response: $response")
//
//        println(response)
//    }

    @Test
    fun getActorFeeds() = runTest {
        feedApi.getActorFeeds(
            jwtAuthContext,
            AccountIdentificator.Handle("skyfeed.xyz")
        )
            .onSuccess {
                println(it)
                checkSerializationValidity(it)
            }
            .onFailure { error("Could not get actor feeds. Response: $it") }
    }

    @Test
    fun getActorLikes() = runTest {
        feedApi.getActorLikes(jwtAuthContext, ownHandleSecret)
            .onSuccess {
                println(it)
                checkSerializationValidity(it)
            }
            .onFailure { error("Could not get actor likes. Response: $it") }
    }

    @Test
    fun getAuthorFeed() = runTest {
        feedApi.getAuthorFeed(jwtAuthContext, ownHandleSecret)
            .onSuccess {
                println(it)
                checkSerializationValidity(it)
            }
            .onFailure { error("Could not get author feed. Response: $it") }
    }

    @Test
    fun getFeedGenerator() = runTest {
        feedApi.getFeedGenerator(
            jwtAuthContext,
            "at://did:plc:z72i7hdynmk6r22z27h6tvur/app.bsky.feed.generator/thevids"
        )
            .onSuccess {
                println(it)
                checkSerializationValidity(it)
            }
            .onFailure { error("Could not get feed generator. Response: $it") }
    }

    @Test
    fun getFeedGenerators() = runTest {
        feedApi.getFeedGenerators(
            jwtAuthContext,
            listOf("at://did:plc:tenurhgjptubkk5zf5qhi3og/app.bsky.feed.generator/whats-warm")
        )
            .onSuccess {
                println(it)
                checkSerializationValidity(it)
            }
            .onFailure { error("Could not get feed generators. Response: $it") }
    }

    // XRPC NOT SUPPORTED
//    @Test
//    fun getFeedSkeleton() = runTest {
//        val response = feedApi.getFeedSkeleton("at://did:plc:tenurhgjptubkk5zf5qhi3og/app.bsky.feed.generator/whats-warm")
//
//        if (response is get FeedSkeleton.Response.Error)
//            error("Cound not get  author feed. Response: $response")
//
//        println(response)
//    }

    @Test
    fun getFeed() = runTest {
        feedApi.getFeed(
            jwtAuthContext,
//            "at://did:plc:z72i7hdynmk6r22z27h6tvur/app.bsky.feed.generator/thevids",
            "at://did:plc:vpkhqolt662uhesyj6nxm7ys/app.bsky.feed.generator/devfeed",
            limit = LimitUpToHundred(100)
        )
            .onSuccess {
                println(it)
                checkSerializationValidity(it)
            }
            .onFailure { error("Could not get feed. Response: $it") }
    }

    @Test
    fun getLikes() = runTest {
        feedApi.getLikes(
            jwtAuthContext,
            AtUri("at://did:plc:6hit5rjdbmlnscmuk4bbtmuc/app.bsky.feed.post/3l3yqmdxbf52g")
        )
            .onSuccess {
                println(it)
                checkSerializationValidity(it)
            }
            .onFailure { error("Could not get likes. Response: $it") }
    }

    // TODO test
    @Test
    fun getListFeed() = runTest {
        feedApi.getListFeed(
            jwtAuthContext,
            AtUri("at://did:plc:bfjoqzne3tm5yxvpybdfahzo/app.bsky.graph.list/3jzmevbfqkj2u")
        )
            .onSuccess {
                println(it)
                checkSerializationValidity(it)
            }
            .onFailure { error("Could not get list feed. Response: $it") }
    }

    @Test
    fun getPostThread() = runTest {
        feedApi.getFeed(
            jwtAuthContext,
            "at://did:plc:vpkhqolt662uhesyj6nxm7ys/app.bsky.feed.generator/devfeed"
        )
            .onSuccess { feedResponse ->
                val postUris = feedResponse.feed.map {
                    it.post.uri
                }

                postUris.forEach { postUri ->
                    feedApi.getPostThread(jwtAuthContext, postUri)
                        .onSuccess {
                            println(it)
                            checkSerializationValidity(it)
                        }
                        .onFailure { error("Could not get thread. Response: $it") }
                }
            }
            .onFailure { error("Could not get feed. Response: $it") }
    }

    @Test
    fun getPostThreadV2() = runTest {
        feedApi.getFeed(
            jwtAuthContext,
            "at://did:plc:vpkhqolt662uhesyj6nxm7ys/app.bsky.feed.generator/devfeed"
        )
            .onSuccess { feedResponse ->
                val postUris = feedResponse.feed.map {
                    it.post.uri
                }

                postUris.forEach { postUri ->
                    feedApi.getPostThreadV2(
                        jwtAuthContext,
                        postUri,
                        branchingFactor = 1
                    )
                        .onSuccess {
                            println(it)
                            checkSerializationValidity(it)
                        }
                        .onFailure { error("Could not get thread. Response: $it") }
                }
            }
            .onFailure { error("Could not get feed. Response: $it") }
    }

    @Test
    fun getPosts() = runTest {
        feedApi.getPosts(
            jwtAuthContext,
            // https://bsky.app/profile/mary.my.id/post/3lixwz2zzpc45
            listOf(
//                AtUri("at://did:plc:6hit5rjdbmlnscmuk4bbtmuc/app.bsky.feed.post/3l3yqmdxbf52g"),
                AtUri("at://did:plc:ia76kvnndjutgedggx2ibrem/app.bsky.feed.post/3lixwz2zzpc45")
            )
        )
            .onSuccess {
                println(it)
                checkSerializationValidity(it)
            }
            .onFailure { error("Could not get posts. Response: $it") }
    }

    @Test
    fun getQuotes() = runTest {
        feedApi.getQuotes(
            jwtAuthContext,
            AtUri("at://did:plc:6hit5rjdbmlnscmuk4bbtmuc/app.bsky.feed.post/3l3yqmdxbf52g")
        )
            .onSuccess {
                println(it)
                checkSerializationValidity(it)
            }
            .onFailure { error("Could not get quotes. Response: $it") }
    }

    @Test
    fun getRepostedBy() = runTest {
        feedApi.getRepostedBy(
            jwtAuthContext,
            AtUri("at://did:plc:6hit5rjdbmlnscmuk4bbtmuc/app.bsky.feed.post/3l3yqmdxbf52g")
        )
            .onSuccess {
                println(it)
                checkSerializationValidity(it)
            }
            .onFailure { error("Could not get reposted by. Response: $it") }
    }


    // Test technically passes, but will fail because apparently "handle" and
    // "createdAt" are nullable, while the https://docs.bsky.app/docs/api/app-bsky-feed-get-timeline
    // says they're not
    @Test
    fun getTimeline() = runTest {
        feedApi.getTimeline(jwtAuthContext, "following")
            .onSuccess {
                println(it)
                checkSerializationValidity(it)
            }
            .onFailure { error("Could not get timeline. Response: $it") }
    }

    @Test
    fun searchPosts() = runTest {
        feedApi.searchPosts(jwtAuthContext, "taylor")
            .onSuccess {
                println(it)
                checkSerializationValidity(it)
            }
            .onFailure { error("Could not search posts. Response: $it") }
    }

    @Test
    fun sendInteractions() = runTest {
        val targetPost = AtUri("at://did:plc:4fovfpeqomd67hgjhfdnsyrn/app.bsky.feed.post/3les4qcvlr22u")

        feedApi.sendInteractions(
            jwtAuthContext,
            interactions = FeedInteractions(
                listOf(
                FeedInteraction(
                    targetPost,
                    event = Interaction.ClickthroughItem
                ),
                    FeedInteraction(
                        targetPost,
                        event = Interaction.Seen
                    ),
                    FeedInteraction(
                        targetPost,
                        event = Interaction.Like
                    ),
                )
            )
        )
            .onFailure {
                error("Could not send interactions. Response: $it")
            }
    }
}