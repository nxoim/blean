@file:OptIn(ExperimentalTime::class, ExperimentalDecomposeApi::class)

package com.nxoim.blean.ui.screens.content

import co.touchlab.kermit.Logger
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.childContext
import com.arkivanov.decompose.jetpackcomponentcontext.asJetpackComponentContext
import com.arkivanov.decompose.router.slot.ChildSlot
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.bringToFront
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.pushToFront
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.subscribe
import com.arkivanov.essenty.lifecycle.doOnResume
import com.arkivanov.essenty.statekeeper.ExperimentalStateKeeperApi
import com.nxoim.blean.bskyPrimitives.AtUri
import com.nxoim.blean.client.BleanClient
import com.nxoim.blean.commonThingsDumpster.childCoroutineScope
import com.nxoim.blean.composeVideoPlayer.VideoPlayerCacheConfiguration
import com.nxoim.blean.postRelatedCommons.models.PostContainer
import com.nxoim.blean.postRelatedCommons.models.PostType
import com.nxoim.blean.postRelatedCommons.models.TextAndFacets
import com.nxoim.blean.postRelatedCommons.toFacet
import com.nxoim.blean.shared.navigation.childStack
import com.nxoim.blean.ui.RootNavigator
import com.nxoim.blean.ui.architecture.coroutineScope
import com.nxoim.blean.ui.screens.content.ContentDestinationsInstance.Feed
import com.nxoim.blean.ui.screens.content.ContentDestinationsInstance.Profile
import com.nxoim.blean.ui.screens.content.ContentDestinationsInstance.Search
import com.nxoim.blean.ui.screens.content.ContentDestinationsInstance.Thread
import com.nxoim.blean.ui.screens.content.feed.FeedHostModel
import com.nxoim.blean.ui.screens.content.feed.FeedNavigation
import com.nxoim.blean.ui.screens.content.postCreation.PostWritingModel
import com.nxoim.blean.ui.screens.content.postCreation.PostingSource
import com.nxoim.blean.ui.screens.content.profile.ProfileModel
import com.nxoim.blean.ui.screens.content.profile.ProfileNavigation
import com.nxoim.blean.ui.screens.content.search.SearchModel
import com.nxoim.blean.ui.screens.content.search.SearchNavigation
import com.nxoim.blean.ui.screens.content.sourceImplementations.DraftSourceImpl
import com.nxoim.blean.ui.screens.content.sourceImplementations.FeedPostsSourceImpl
import com.nxoim.blean.ui.screens.content.sourceImplementations.FeedSourceImpl
import com.nxoim.blean.ui.screens.content.sourceImplementations.MediaAwareThingStateImpl
import com.nxoim.blean.ui.screens.content.sourceImplementations.PostWritingHost
import com.nxoim.blean.ui.screens.content.sourceImplementations.ProfileSourceImpl
import com.nxoim.blean.ui.screens.content.sourceImplementations.SearchSourceImpl
import com.nxoim.blean.ui.screens.content.sourceImplementations.ThreadSourceImpl
import com.nxoim.blean.ui.screens.content.thread.ThreadModel
import com.nxoim.blean.ui.screens.content.thread.ThreadNavigation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.jvm.JvmInline
import kotlin.time.ExperimentalTime

class ContentRootScope(
    private val client: StateFlow<BleanClient>,
    private val context: ComponentContext,
    private val rootNavigator: RootNavigator,
    private val onSoftLogout: () -> Unit,
    private val onLogout: () -> Unit,
    private val logger: Logger,
    val playerCacheConfiguration: VideoPlayerCacheConfiguration
) {
    private val _navigator = ContentNavigatorImpl(rootNavigator)

    val slot = mapClientFlowToChildSlot()

    private fun mapClientFlowToChildSlot(): Value<ChildSlot<BleanClient, ContentRootScopeState>> {
        val slotNavigation = SlotNavigation<BleanClient>()

        context.coroutineScope.launch(Dispatchers.Main.immediate) {
            client.collect { slotNavigation.activate(it) }
        }

        // we need a new component context on each change
        return context.childSlot(
            slotNavigation,
            serializer = FakeClientKSerializer(client.value),
            initialConfiguration = { client.value },
            childFactory = { client, componentContext ->
                when (client) {
                    is BleanClient.LoggedIn -> {
                        ContentRootScopeState.Initialized(
                            InitializedContentRootScope(
                                client,
                                componentContext,
                                _navigator,
                                onSoftLogout,
                                onLogout,
                                logger = logger
                            ).apply {
                                componentContext.lifecycle.doOnResume {
                                    // refresh details each time this is resumed
                                    componentContext.coroutineScope.launch {
                                        client.account.refreshAccountDetails()
                                    }
                                }
                            }
                        )
                    }

                    is BleanClient.Loading -> ContentRootScopeState.Uninitialized
                    is BleanClient.SoftLoggedOut -> {
                        ContentRootScopeState.NonInitializable(
                            _navigator
                        )
                    }
                }
            }
        )
    }
}

/**
 * Allows for easy restoration of state of children SOMEHOW.
 * This is a hack
 */
private class FakeClientKSerializer(
    val currentClientValue: BleanClient
) : KSerializer<BleanClient> {
    override val descriptor = SerialDescriptor("FakeClientSerializer", String.serializer().descriptor)

    override fun deserialize(decoder: Decoder) = currentClientValue

    override fun serialize(encoder: Encoder, value: BleanClient) { }
}

sealed interface ContentRootScopeState {
    data object Uninitialized : ContentRootScopeState

    class NonInitializable(val navigator: ContentNavigator) : ContentRootScopeState

    class Initialized(val scope: InitializedContentRootScope) : ContentRootScopeState
}

class InitializedContentRootScope(
    private val client: BleanClient.LoggedIn,
    context: ComponentContext,
    private val _navigator: ContentNavigatorImpl,
    private val onSoftLogout: () -> Unit,
    private val onLogout: () -> Unit,
    private val logger: Logger
) {
    val navigator = _navigator as ContentNavigator
    val userDrafts = client.drafts

    private val postContainerAttentionCache = PostContainerAttentionCache()

    // changing state from initialzed back and forth,
    // like soft log out and then logging back in, crashes with
    // something already being initialized.
    @OptIn(ExperimentalStateKeeperApi::class)
    val stack = context.childStack(
        _navigator,
        initialStack = { listOf(ContentDestinations.Feed) },
        childFactory = { destination, componentContext ->
            when (destination) {
                is ContentDestinations.Feed -> {
                    Feed(
                        FeedHostModel(
                            logger,
                            FeedSourceImpl(client),
                            feedPostsSourceFactory = {
                                FeedPostsSourceImpl(
                                    client,
                                    it,
                                    logger
                                )
                            },
                            navigation = FeedNavigationImpl(),
                            coroutineScope = componentContext.coroutineScope.childCoroutineScope(),
                            lifecycle = componentContext.asJetpackComponentContext().lifecycle
                        )
                    )
                }

                is ContentDestinations.Search -> Search(
                    SearchModel(
                        SearchSourceImpl(client, logger),
                        navigation = SearchNavigationImpl(),
                        coroutineScope = componentContext.coroutineScope.childCoroutineScope()
                    )
                )

                is ContentDestinations.Inbox -> ContentDestinationsInstance.Inbox

                is ContentDestinations.Profile -> Profile(
                    ProfileModel(
                        ProfileSourceImpl(client, logger),
                        ProfileNavigationImpl(),
                        coroutineScope = componentContext.coroutineScope.childCoroutineScope()
                    )
                )

                is ContentDestinations.Thread -> Thread(
                    ThreadModel(
                        ThreadNavigationImpl(),
                        postUri = destination.post,
                        initialVisible = postContainerAttentionCache.get(destination.post),
                        source = ThreadSourceImpl(
                            destination.post,
                            (postContainerAttentionCache.get(destination.post) as? PostContainer.Available<*>)?.value,
                            client,
                            logger
                        ),
                        coroutineScope = componentContext.coroutineScope.childCoroutineScope()
                    )
                )
            }
        }
    )

    val postWritingHost = PostWritingHost(
        context.childContext("PostWritingChild"),
        PostWritingModel(
            draftSource = DraftSourceImpl(userDrafts),
            coroutineScope = context.coroutineScope.childCoroutineScope(),
            posting = object : PostingSource {
                override suspend fun queuePost(
                    id: String,
                    textContent: TextAndFacets,
                    languages: List<String>
                ) {
                    client.posting.enqueue(
                        id,
                        text = textContent.text,
                        facets = textContent.facets?.map { it.toFacet(textContent.text) },
                        languages = languages
                    )
                }
            }
        )
    )

    val mediaAwareThingState = MediaAwareThingStateImpl(
        context.childContext("MediaAwareThingChild")
    ) as MediaAwareThingState

    fun softLogout() { onSoftLogout() }

    fun logout() { onLogout() }

    inner class FeedNavigationImpl : FeedNavigation {
        override fun openPost(post: AtUri) {
            navigator.navigateToThread(post)
        }

        override fun openPost(post: PostContainer<PostType>) {
            postContainerAttentionCache.set(post)
            openPost(post.uri)
        }
    }

    inner class SearchNavigationImpl : SearchNavigation {
        override fun openPost(post: AtUri) {
            navigator.navigateToThread(post)
        }

        override fun openPost(post: PostContainer<PostType>) {
            postContainerAttentionCache.set(post)
            openPost(post.uri)
        }
    }

    inner class ProfileNavigationImpl : ProfileNavigation {
        override fun openPost(post: AtUri) {
            navigator.navigateToThread(post)
        }

        override fun openPost(post: PostContainer<PostType>) {
            postContainerAttentionCache.set(post)
            openPost(post.uri)
        }
    }

    inner class ThreadNavigationImpl : ThreadNavigation {
        override fun back() {
            _navigator.back()
        }

        override fun openPost(post: AtUri) {
            navigator.navigateToThread(post)
        }

        override fun openPost(post: PostContainer<PostType>) {
            postContainerAttentionCache.set(post)
            openPost(post.uri)
        }
    }
}


@Serializable
sealed interface ContentDestinations {
    @Serializable
    data object Feed : ContentDestinations

    @Serializable
    data object Search : ContentDestinations

    @Serializable
    data object Inbox : ContentDestinations

    @Serializable
    data object Profile : ContentDestinations

    @Serializable
    data class Thread(val post: AtUri) : ContentDestinations
}

sealed interface ContentDestinationsInstance {
    @JvmInline
    value class Feed(val model: FeedHostModel) : ContentDestinationsInstance

    @JvmInline
    value class Search(val model: SearchModel) : ContentDestinationsInstance

    data object Inbox : ContentDestinationsInstance

    @JvmInline
    value class Profile(val model: ProfileModel) : ContentDestinationsInstance

    @JvmInline
    value class Thread(val model: ThreadModel) : ContentDestinationsInstance
}

class ContentNavigatorImpl(
    private val rootNavigator: RootNavigator,
    private val navigator: StackNavigation<ContentDestinations> = StackNavigation()
) : ContentNavigator, StackNavigation<ContentDestinations> by navigator {
    override fun navigateToFeed() {
        navigator.bringToFront(ContentDestinations.Feed)
    }

    override fun navigateToSearch() {
        navigator.bringToFront(ContentDestinations.Search)
    }

    override fun navigateToAuthentication() {
        rootNavigator.navigateToAuthenticationHost()
    }

    override fun navigateToNotifications() {
        navigator.bringToFront(ContentDestinations.Inbox)
    }

    override fun navigateToProfile() {
        navigator.bringToFront(ContentDestinations.Profile)
    }

    override fun back() {
        navigator.pop()
    }

    override fun navigateToThread(post: AtUri) {
        navigator.pushToFront(ContentDestinations.Thread(post))
    }
}

interface ContentNavigator {
    fun navigateToFeed()
    fun navigateToSearch()
    fun navigateToNotifications()
    fun navigateToProfile()
    fun navigateToAuthentication()
    fun back()
    fun navigateToThread(post: AtUri)
}

private fun Value<ChildSlot<BleanClient, ContentRootScopeState>>.mapToStateFlow(
    context: ComponentContext
): StateFlow<ContentRootScopeState> {
    val mutableStateFlow = MutableStateFlow(this.value.child!!.instance)

    this.subscribe(context.lifecycle) { mutableStateFlow.value = it.child!!.instance }

    return mutableStateFlow.asStateFlow()
}