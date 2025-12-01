package com.nxoim.blean.client

import co.touchlab.kermit.Logger
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.map
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import com.nxoim.blean.api.BleanApi
import com.nxoim.blean.api.utils.AuthenticationContext
import com.nxoim.blean.client.stuff.ActionProcessor
import com.nxoim.blean.client.stuff.BleanAccount
import com.nxoim.blean.client.stuff.BleanDrafts
import com.nxoim.blean.client.stuff.BleanFeed
import com.nxoim.blean.client.stuff.BleanPostInteractions
import com.nxoim.blean.client.stuff.BleanPosting
import com.nxoim.blean.client.stuff.LikeActionProcessor
import com.nxoim.blean.client.stuff.PostActionProcessor
import com.nxoim.blean.models.LoggedInUserBasicDetails
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

private const val logTag = "AccountInstance"

class AccountInstance(
    val basicDetails: StateFlow<LoggedInUserBasicDetails>,
    private val logger: Logger
) {
    private val isActionProcessingPaused = MutableStateFlow(false)
    private val mutex = Mutex()
    private val client = MutableStateFlow<BleanClient>(
        BleanClient.Loading(basicDetails.value.did)
    )
    val account = Account(
        basicDetails = basicDetails,
        client = client.asStateFlow()
    )

    private var clientInstanceCached: ClientInstance? = null

    internal suspend fun initialize(
        context: Flow<AuthenticationContext?>,
        bleanApi: BleanApi,
        rootDataPathForUser: String,
        rootCachePathForUser: String,
        encryptionKey: ByteArray?,
        instanceCoroutineScope: CoroutineScope,
        shouldStartActionProcessing: Boolean
    ): Result<Unit, Throwable> {
        val isInitializedAlready = mutex.withLock { client.value is BleanClient.LoggedIn }

        if (isInitializedAlready)
            return Ok(Unit)
        else {
            val result = mutex.withLock {
                isActionProcessingPaused.value != shouldStartActionProcessing

                try {
                    val clientInstance = with(logger) {
                        createClientInstance(
                            basicDetails = basicDetails,
                            onAuthenticationContextRequest = {
                                context
                                    .onEach {
                                        if (it == null) client.update { BleanClient.SoftLoggedOut(basicDetails.value.did) }
                                    }
                                    .filterNotNull()
                                    .first()
                            },
                            bleanApi = bleanApi,
                            rootDataPathForUser = rootDataPathForUser,
                            rootCachePathForUser = rootCachePathForUser,
                            encryptionKey = encryptionKey,
                            instanceCoroutineScope = instanceCoroutineScope,
                        )
                    }

                    clientInstanceCached = clientInstance

                    client.update { BleanClient.LoggedIn(clientInstance) }
                    Ok(clientInstance)
                } catch (e: Exception) {
                    if (e is CancellationException)
                        throw e
                    else {
                        logger.e(tag = logTag, throwable = e) {
                            "Unable to initialize instance"
                        }
                        Err(e)
                    }
                }
            }

            result
                .onFailure {
                    clientInstanceCached?.apply {
                        instanceCoroutineScope
                            .coroutineContext[Job]
                            ?.cancelAndJoin()
                    }
                    clientInstanceCached = null
                }
                .onSuccess {
                    instanceCoroutineScope.launch {
                        val actionProcessors = it.all

                        actionProcessors.recoverEachInParallel().joinAll()
                        actionProcessors.processEachInParallel(isActionProcessingPaused.asStateFlow())
                        actionProcessors.autoCleanEachInParallel(deleteItemsOlderThan = 10.seconds)
                    }
                }

            return result.map { Unit }
        }
    }

    internal suspend fun markAsLoadingAndDeinitialize(): Result<Unit, Throwable> = mutex.withLock {
        return try {
            client.update { BleanClient.Loading(basicDetails.value.did) }
            clientInstanceCached?.deinitialize(nuke = false)
            clientInstanceCached = null
            Ok(Unit)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            clientInstanceCached = null
            Err(e)
        }
    }

    internal suspend fun markAsNonInitializable(): Result<Unit, Throwable> = mutex.withLock {
        return try {
            client.update { BleanClient.SoftLoggedOut(basicDetails.value.did) }
            clientInstanceCached?.deinitialize(nuke = false)
            clientInstanceCached = null
            Ok(Unit)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            clientInstanceCached = null
            Err(e)
        }
    }

    internal suspend fun markAsNonInitializableAndNuke(): Result<Unit, Throwable> = mutex.withLock {
        return try {
            client.update { BleanClient.SoftLoggedOut(basicDetails.value.did) }
            clientInstanceCached?.deinitialize(nuke = true)
            clientInstanceCached = null
            Ok(Unit)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            clientInstanceCached = null
            Err(e)
        }
    }

    fun startActionProcessing() { isActionProcessingPaused.value = false }
    fun pauseActionProcessing() { isActionProcessingPaused.value = true }
}

context(logger: Logger)
fun createClientInstance(
    basicDetails: StateFlow<LoggedInUserBasicDetails>,
    onAuthenticationContextRequest: suspend () -> AuthenticationContext,
    bleanApi: BleanApi,
    rootDataPathForUser: String,
    rootCachePathForUser: String,
    encryptionKey: ByteArray?,
    instanceCoroutineScope: CoroutineScope
) = object : ClientInstance {
    override val usersDid = basicDetails.value.did
    override val instanceCoroutineScope = instanceCoroutineScope

    override val userRepositories = UserRepositories(
        rootDataPathForUser = rootDataPathForUser,
        rootCachePathForUser = rootCachePathForUser,
        encryptionKey = encryptionKey,
        logger = logger
    ).apply {
        instanceCoroutineScope.launch { initialize() } // include storage inits into the try catch
    }

    override val account = BleanAccount(
        accountApi = bleanApi.account,
        feedApi = bleanApi.feed,
        userDataRepository = userRepositories.userData,
        onAuthenticationContextRequest = onAuthenticationContextRequest,
        clientCoroutineScope = instanceCoroutineScope,
        logger = logger
    )

    override val drafts = BleanDrafts(
        draftsRepository = userRepositories.userData.drafts,
        fileManager = userRepositories.userData.draftMediaStorage,
        clientCoroutineScope = instanceCoroutineScope,
        logger = logger
    )

    override val feed = BleanFeed(
        bleanApi.feed,
        onAuthenticationContextRequest,
        contentRepository = userRepositories.contentRepository,
        clientCoroutineScope = instanceCoroutineScope,
        logger = logger
    )

    override val likeActionProcessor = LikeActionProcessor(
        userDid = usersDid.value,
        repoApi = bleanApi.repo,
        feed = feed,
        outboxRepository = userRepositories.userData.postInteractionsOutbox,
        onAuthenticationContextRequest = onAuthenticationContextRequest,
        logger = logger
    )

    override val postActionProcessor = PostActionProcessor(
        userDid = usersDid.value,
        repoApi = bleanApi.repo,
        outboxRepository = userRepositories.userData.postInteractionsOutbox,
        onAuthenticationContextRequest = onAuthenticationContextRequest,
        logger = logger
    )

    override val posting = BleanPosting(
        postProcessor = postActionProcessor,
        clientCoroutineScope = instanceCoroutineScope,
        logger = logger
    )

    override val postInteractionsOutbox = BleanPostInteractions(
        feed = feed,
        likeProcessor = likeActionProcessor,
        clientCoroutineScope = instanceCoroutineScope,
        logger = logger
    )
}

interface ClientInstance : OAuthBleanClient, ActionProcessors {
    val userRepositories: UserRepositories
    val instanceCoroutineScope: CoroutineScope

    // Wait for jobs to finish and then close repos
    suspend fun deinitialize(nuke: Boolean = false) {
        instanceCoroutineScope
            .coroutineContext[Job]
            ?.cancelAndJoin()

        if (nuke)
            userRepositories.deinitializeAndNuke()
        else
            userRepositories.deinitialize()
    }
}

interface ActionProcessors {
    val likeActionProcessor: LikeActionProcessor
    val postActionProcessor: PostActionProcessor
    val all get() = buildSet {
        add(likeActionProcessor)
        add(postActionProcessor)
    }
}

context(scope: CoroutineScope)
suspend fun Iterable<ActionProcessor>.recoverEachInParallel() = with(scope) {
    map { launch { it.recover() } }
}

context(scope: CoroutineScope)
suspend fun Iterable<ActionProcessor>.processEachInParallel(
    isPaused: StateFlow<Boolean>
) = with(scope) {
    map { launch { it.process(isPaused)} }
}

context(scope: CoroutineScope)
suspend fun Iterable<ActionProcessor>.autoCleanEachInParallel(deleteItemsOlderThan: Duration) = with(scope) {
    map { launch { it.autoCleanOutbox(deleteItemsOlderThan) } }
}