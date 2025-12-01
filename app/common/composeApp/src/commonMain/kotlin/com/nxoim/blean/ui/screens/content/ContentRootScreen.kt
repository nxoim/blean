@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)

package com.nxoim.blean.ui.screens.content

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Dp
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.nxoim.blean.composeVideoPlayer.LocalVideoPlayerHost
import com.nxoim.blean.composeVideoPlayer.rememberVideoPlayerHost
import com.nxoim.blean.postRelatedCommons.models.PostMediaContent
import com.nxoim.blean.ui.composeUiCommons.LocalScaffoldPadding
import com.nxoim.blean.ui.composeUiCommons.LocalScrollVisualFactor
import com.nxoim.blean.ui.composeUiCommons.ProvideScaffoldPadding
import com.nxoim.blean.ui.composeUiCommons.ScrollVisualFactorRoot
import com.nxoim.blean.ui.composeUiCommons.ScrollVisualFactorRootImpl
import com.nxoim.blean.ui.composeUiCommons.animateFloatAsState
import com.nxoim.blean.ui.composeUiCommons.copy
import com.nxoim.blean.ui.composeUiCommons.modifiers.layoutAsIfMeasuredZero
import com.nxoim.blean.ui.composeUiCommons.rememberScrollVisualFactorRoot
import com.nxoim.blean.ui.composeUiCommons.slideDownOnScroll
import com.nxoim.blean.ui.fullscreenMedia.FullScreenMedia
import com.nxoim.blean.ui.screens.content.feed.FeedHostScreen
import com.nxoim.blean.ui.screens.content.postCreation.PostWritingOverlay
import com.nxoim.blean.ui.screens.content.profile.ProfileScreen
import com.nxoim.blean.ui.screens.content.search.SearchComponent
import com.nxoim.blean.ui.screens.content.thread.ThreadScreen

@Composable
fun ContentRootScreen(scope: ContentRootScope) {
    CompositionLocalProvider(
        LocalVideoPlayerHost provides rememberVideoPlayerHost(
            cacheConfig = scope.playerCacheConfiguration
        )
    ) {
        AnimatedContent(scope.slot.subscribeAsState().value.child) { child ->
            val state = child!!.instance

            when (state) {
                is ContentRootScopeState.Initialized ->
                    InitializedHostOfContent(state.scope)

                ContentRootScopeState.Uninitialized -> {
                    Text("Client not initialized")
                }

                is ContentRootScopeState.NonInitializable -> Column(Modifier.statusBarsPadding()) {
                    Text("Must login again")
                    Button(onClick = { state.navigator.navigateToAuthentication() }) {
                        Text("Back to login")
                    }
                }
            }
        }
    }
}

@Composable
private fun InitializedHostOfContent(scope: InitializedContentRootScope) {
    MediaAwareThing(
        scope.mediaAwareThingState,
        onMediaSharedElementKeyRequest = FullScreenMedia::sharedElementKey
    ) {
        Box(contentAlignment = Alignment.BottomEnd) {
            val stack by scope.stack.subscribeAsState()
            val currentInstance = stack.active.instance

            val scrollVisualFactorRoot = rememberScrollVisualFactorRoot(
                animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec()
            )
            var latestScaffoldPadding by remember { mutableStateOf(PaddingValues.Zero) }

            Scaffold(
                modifier = Modifier.nestedScroll(scrollVisualFactorRoot),
                topBar = { DebugAccessThing(scope) },
                bottomBar = {
                    ContentRootNavigationBar(currentInstance, scrollVisualFactorRoot, scope)
                }
            ) { scaffoldPadding ->
                latestScaffoldPadding = scaffoldPadding
                val scaffoldPaddingNoTop = scaffoldPadding.copy(top = Dp.Hairline)
                val mediaSharedElementTransitionScope = this@MediaAwareThing

                Children(scope.stack) { (_, instance) ->
                    val systembarsPadding = WindowInsets.systemBars.asPaddingValues()
                    val paddingForThisScreen = remember {
                        when (instance) {
                            is ContentDestinationsInstance.Feed -> scaffoldPaddingNoTop
                            is ContentDestinationsInstance.Search -> scaffoldPaddingNoTop
                            is ContentDestinationsInstance.Inbox -> scaffoldPaddingNoTop
                            is ContentDestinationsInstance.Profile -> scaffoldPaddingNoTop
                            else -> systembarsPadding
                        }
                    }
                    // reset on enter and exit of any
                    ResetScrollVisualOnDisplayAndDisposalEffect(
                        scrollVisualFactorRoot
                    )

                    CompositionLocalProvider(
                        LocalScaffoldPadding provides paddingForThisScreen,
                        LocalScrollVisualFactor provides scrollVisualFactorRoot
                    ) {
                        when (instance) {
                            is ContentDestinationsInstance.Feed -> FeedHostScreen(
                                instance.model,
                                mediaSharedElementTransitionScope,
                                onRequestToMaximizeMedia = { mediaQueue, focusOn ->
                                    val convertedMediaQueue =
                                        mediaQueue.mapNotNull { it.toFullScreenMedia() }
                                    val convertedFocusOn =
                                        focusOn.toFullScreenMedia()

                                    convertedFocusOn?.let {
                                        scope.mediaAwareThingState.handler.selectMedia(
                                            convertedMediaQueue,
                                            it
                                        )
                                    }
                                },
                                onMediaSharedElementKeyRequest = PostMediaContent::sharedElementKey
                            )

                            is ContentDestinationsInstance.Search -> SearchComponent(
                                instance.model,
                                mediaSharedElementTransitionScope,
                                onRequestToMaximizeMedia = { mediaQueue, focusOn ->
                                    val convertedMediaQueue =
                                        mediaQueue.mapNotNull { it.toFullScreenMedia() }
                                    val convertedFocusOn =
                                        focusOn.toFullScreenMedia()

                                    convertedFocusOn?.let {
                                        scope.mediaAwareThingState.handler.selectMedia(
                                            convertedMediaQueue,
                                            it
                                        )
                                    }
                                },
                                onMediaSharedElementKeyRequest = PostMediaContent::sharedElementKey
                            )

                            is ContentDestinationsInstance.Inbox -> Text("Not implemented")
                            is ContentDestinationsInstance.Profile -> ProfileScreen(
                                instance.model,
                                mediaSharedElementTransitionScope,
                                onRequestToMaximizeMedia = { mediaQueue, focusOn ->
                                    val convertedMediaQueue =
                                        mediaQueue.mapNotNull { it.toFullScreenMedia() }
                                    val convertedFocusOn =
                                        focusOn.toFullScreenMedia()

                                    convertedFocusOn?.let {
                                        scope.mediaAwareThingState.handler.selectMedia(
                                            convertedMediaQueue,
                                            it
                                        )
                                    }
                                },
                                onMediaSharedElementKeyRequest = PostMediaContent::sharedElementKey
                            )

                            is ContentDestinationsInstance.Thread -> ProvideScaffoldPadding(
                                PaddingValues.Zero
                            ) {
                                ThreadScreen(
                                    instance.model,
                                    mediaSharedElementTransitionScope,
                                    onRequestToMaximizeMedia = { mediaQueue, focusOn ->
                                        val convertedMediaQueue =
                                            mediaQueue.mapNotNull { it.toFullScreenMedia() }
                                        val convertedFocusOn =
                                            focusOn.toFullScreenMedia()

                                        convertedFocusOn?.let {
                                            scope.mediaAwareThingState.handler.selectMedia(
                                                convertedMediaQueue,
                                                it
                                            )
                                        }
                                    },
                                    onMediaSharedElementKeyRequest = PostMediaContent::sharedElementKey
                                )
                            }
                        }
                    }
                }
            }

            val newPostButtonVisible = when (currentInstance) {
                is ContentDestinationsInstance.Feed,
                is ContentDestinationsInstance.Profile -> true

                else -> false
            }

            PostWritingOverlay(
                onOpen = { scope.postWritingHost.open() },
                onHide = { scope.postWritingHost.hide() },
                currentModel = scope.postWritingHost.model,
                newPostButtonVisible = newPostButtonVisible,
                scrollVisualFactorRoot = scrollVisualFactorRoot,
                latestScaffoldPadding = latestScaffoldPadding
            )
        }
    }
}

@Composable
private fun DebugAccessThing(scope: InitializedContentRootScope) {
    var expanded by remember { mutableStateOf(false) }

    Box(Modifier.layoutAsIfMeasuredZero()) {
        IconButton(
            onClick = { expanded = true },
            modifier = Modifier.statusBarsPadding()
        ) {
            Icon(Icons.Default.Build, null)
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("Soft logout") },
                onClick = {
                    scope.softLogout()
                    expanded = false
                }
            )

            DropdownMenuItem(
                text = { Text("Logout") },
                onClick = {
                    scope.logout()
                    expanded = false
                }
            )
        }
    }
}

@Composable
private fun ContentRootNavigationBar(
    currentInstance: ContentDestinationsInstance,
    scrollVisualFactorRoot: ScrollVisualFactorRootImpl,
    scope: InitializedContentRootScope
) {
    val visibilityAlpha by animateFloatAsState(
        when (currentInstance) {
            is ContentDestinationsInstance.Feed,
            is ContentDestinationsInstance.Inbox,
            is ContentDestinationsInstance.Profile,
            is ContentDestinationsInstance.Search -> 1f

            else -> 0f
        },
        animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec()
    )
    Column(
        Modifier.slideDownOnScroll(scrollVisualFactorRoot)
    ) {

        NavigationBar(
            Modifier
                .graphicsLayer() { this.alpha = visibilityAlpha }
        ) {
            NavigationBarItem(
                selected = currentInstance is ContentDestinationsInstance.Feed,
                onClick = { scope.navigator.navigateToFeed() },
                label = {
                    Text("Feed")
                },
                icon = {
                    Icon(Icons.Filled.Menu, contentDescription = null)
                }
            )

            NavigationBarItem(
                selected = currentInstance is ContentDestinationsInstance.Search,
                onClick = { scope.navigator.navigateToSearch() },
                label = {
                    Text("Search")
                },
                icon = {
                    Icon(
                        Icons.Outlined.Search,
                        contentDescription = null
                    )
                }
            )

            NavigationBarItem(
                selected = currentInstance is ContentDestinationsInstance.Inbox,
                onClick = { scope.navigator.navigateToNotifications() },
                label = {
                    Text("Inbox")
                },
                icon = {
                    Icon(
                        Icons.Outlined.Inbox,
                        contentDescription = null
                    )
                }
            )

            NavigationBarItem(
                selected = currentInstance is ContentDestinationsInstance.Profile,
                onClick = { scope.navigator.navigateToProfile() },
                label = {
                    Text("Profile")
                },
                icon = {
                    Icon(
                        Icons.Outlined.Person,
                        contentDescription = null
                    )
                }
            )
        }
    }
}

@Composable
private fun ResetScrollVisualOnDisplayAndDisposalEffect(
    scrollVisualFactorRoot: ScrollVisualFactorRoot
) = DisposableEffect(scrollVisualFactorRoot) {
    scrollVisualFactorRoot.reset()
    onDispose {
        scrollVisualFactorRoot.reset()
    }
}

private fun PostMediaContent.sharedElementKey() = when (this) {
    is PostMediaContent.ImageWithCDNLinks -> this.fullsize
    PostMediaContent.Unsupported -> ""
    is PostMediaContent.Video -> this.playlistUri
}

private fun FullScreenMedia.sharedElementKey() = when (this) {
    is FullScreenMedia.Image -> this.fullsize
    is FullScreenMedia.Video -> this.playlistUri
}

private fun PostMediaContent.toFullScreenMedia() = when (this) {
    is PostMediaContent.ImageWithCDNLinks -> FullScreenMedia.Image(
        thumb = thumb,
        fullsize = fullsize,
        altText = altText,
        widthPx = widthPx,
        heightPx = heightPx
    )

    PostMediaContent.Unsupported -> null

    is PostMediaContent.Video -> FullScreenMedia.Video(
        playlistUri = playlistUri,
        thumbnail = thumbnail,
        altText = altText,
        widthPx = widthPx,
        heightPx = heightPx,
        captionUris = captions?.map { it.url }
    )
}