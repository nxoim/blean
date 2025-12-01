@file:OptIn(ExperimentalTime::class)

import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import com.nxoim.blean.api.api.FeedApi
import com.nxoim.blean.api.api.RepoApi
import com.nxoim.blean.api.api.SessionApi
import com.nxoim.blean.api.models.feed.ThreadPost
import com.nxoim.blean.api.models.repo.RecordWrite
import com.nxoim.blean.api.models.repo.RecordWriteContent
import com.nxoim.blean.api.models.repo.RecordWriteResult
import com.nxoim.blean.api.models.repo.RepoCollectionNSID
import com.nxoim.blean.api.models.repo.UploadRecord
import com.nxoim.blean.api.models.repo.UploadRecordSubject
import com.nxoim.blean.api.models.session.CreatedSession
import com.nxoim.blean.bskyPrimitives.AtUri
import com.nxoim.blean.bskyPrimitives.ParsedAtUri
import com.nxoim.blean.bskyPrimitives.RecordKey
import com.nxoim.blean.bskyPrimitives.parse
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class RepoApiTets {
    val client = createTestClient()
    val sessionApi = SessionApi(client.value)
    lateinit var accountSession: CreatedSession
    val jwtAuthContext =
        runBlocking { sessionApi.getDefaultApiToken(onCreatedSession = { accountSession = it }) }
    val feedApi = FeedApi(client.value)
    val repoApi = RepoApi(client.value)

    @Test
    fun createAndDeleteRecord() = runTest {
        val postUri =
            AtUri("at://did:plc:4fovfpeqomd67hgjhfdnsyrn/app.bsky.feed.post/3ldohpd724k2f")
        feedApi.getPostThread(jwtAuthContext, postUri)
            .onSuccess {
                println(it)
                checkSerializationValidity(it)
                val targetRepo = accountSession.did

                when (val thread = it.thread) {
                    is ThreadPost.ThreadPostView -> {
                        val post = thread.post

                        repoApi.createRecord(
                            authenticationContext = jwtAuthContext,
                            repo = targetRepo,
                            collection = RepoCollectionNSID.Like,
                            record = UploadRecord.Like(
                                subject = UploadRecordSubject(
                                    uri = post.uri,
                                    cid = post.cid
                                ),
                                createdAt = Clock.System.now().toString()
                            )
                        )
                            .onSuccess {
                                checkSerializationValidity(it)

                                val recordKeyToDelete = RecordKey.Any(
                                    (it.uri.parse() as ParsedAtUri.Like).recordKey
                                )

                                repoApi.deleteRecord(
                                    authenticationContext = jwtAuthContext,
                                    repo = targetRepo,
                                    collection = RepoCollectionNSID.Like,
                                    rkey = recordKeyToDelete
                                )
                                    .onSuccess {
                                        checkSerializationValidity(it)
                                    }
                                    .onFailure {
                                        error("Could not delete record. Error: $it")
                                    }
                            }
                            .onFailure { error("Could not create record. Error: $it") }
                    }

                    else -> error("can not create record for the test because was unable to get the post to create a record for")
                }
            }
            .onFailure { error("Could not get thread. Response: $it") }
    }

    @Test
    fun applyWritesCreateThenGetThenDeletePost() = runTest {
        val targetRepo = accountSession.did

        repoApi.applyWrites(
            authenticationContext = jwtAuthContext,
            repo = targetRepo,
            writes = listOf(
                RecordWrite.Create(
                    collection = RepoCollectionNSID.Post,
                    value = RecordWriteContent.Post(
                        createdAt = Clock.System.now().toString(),
                        langs = listOf("en"),
                        text = "Hello world from applyWrites! This post will be deleted?? unless something goes wrong that is.",
                        reply = null
                    )
                )
            ),
            validate = true
        )
            .onFailure { error("failed to create a post $it") }
            .onSuccess { response ->
                println("created a post successfully $response")
                checkSerializationValidity(response)

                response.results.forEach { result ->
                    when (result) {
                        is RecordWriteResult.Create -> {
                            val uri = result.uri
                                .parse() as? ParsedAtUri.Post ?: error(
                                "why would parsed uri be anything than a post one if we only created 1 post?"
                            )

                            repoApi.getRecord(
                                jwtAuthContext,
                                targetRepo,
                                collection = RepoCollectionNSID.Post,
                                RecordKey.Any(uri.recordKey)
                            )
                                .onFailure {
                                    error("failed to get the created post $it")
                                }
                                .onSuccess {
                                    println("was able to get record")

                                    repoApi.deleteRecord(
                                        authenticationContext = jwtAuthContext,
                                        repo = targetRepo,
                                        collection = RepoCollectionNSID.Post,
                                        rkey = RecordKey.Any(uri.recordKey)
                                    )
                                        .onSuccess { response ->
                                            println("deletion of just created post was successful $response")

                                            require(response.commit != null) {
                                                "⚠️returned commit of deletion was null. Are we sure the operation was successful?"
                                            }

                                            checkSerializationValidity(response)
                                        }
                                        .onFailure { error ->
                                            error("failed to delete the created post $error")
                                        }
                                }
                        }

                        else -> error("why would result be anything other than create if the only write was create?")
                    }
                }
            }
    }

    @Test
    fun applyWritesWithCustomRkey() = runTest {
        val targetRepo = accountSession.did
        val customRkey = RecordKey.Tid.next()

        repoApi.applyWrites(
            authenticationContext = jwtAuthContext,
            repo = targetRepo,
            writes = listOf(
                RecordWrite.Create(
                    collection = RepoCollectionNSID.Post,
                    rkey = customRkey,
                    value = RecordWriteContent.Post(
                        createdAt = Clock.System.now().toString(),
                        langs = listOf("en"),
                        text = "Testing custom rkey creation: $customRkey",
                        reply = null
                    )
                )
            ),
            validate = true
        )
            .onFailure { error("failed to create post with custom rkey $it") }
            .onSuccess { response ->
                println("created post with custom rkey: $response")
                checkSerializationValidity(response)

                val result = response.results.first()
                if (result !is RecordWriteResult.Create) error("Result was not a Create")

                val uri = result.uri.parse() as? ParsedAtUri.Post
                    ?: error("Failed to parse URI")

                require(uri.recordKey == customRkey.toString()) {
                    "FAILURE: Server ignored custom rkey. Expected '$customRkey', got '${uri.recordKey}'"
                }

                repoApi.deleteRecord(
                    authenticationContext = jwtAuthContext,
                    repo = targetRepo,
                    collection = RepoCollectionNSID.Post,
                    rkey = customRkey
                )
                    .onSuccess { deleteResponse ->
                        println("delete with custom rkey successful: $deleteResponse")
                        require(deleteResponse.commit != null) { "Delete commit was null" }
                        checkSerializationValidity(deleteResponse)
                    }
                    .onFailure { error("failed to delete the custom rkey post $it") }
            }
    }
}
