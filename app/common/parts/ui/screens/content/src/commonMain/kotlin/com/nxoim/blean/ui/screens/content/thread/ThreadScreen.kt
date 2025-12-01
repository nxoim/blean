@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.nxoim.blean.ui.screens.content.thread

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nxoim.blean.postRelatedCommons.models.PostContainer
import com.nxoim.blean.postRelatedCommons.models.PostMediaContent
import com.nxoim.blean.postRelatedCommons.models.PostType
import com.nxoim.blean.ui.composeMaterial3Extensions.ListLoadingIndicator
import com.nxoim.blean.ui.composeMaterial3Extensions.VerticalListRefreshIndication
import com.nxoim.blean.ui.composeUiCommons.CombinedSharedTransitionScope
import com.nxoim.blean.ui.composeUiCommons.Layout
import com.nxoim.blean.ui.composeUiCommons.LocalIsInFocus
import com.nxoim.blean.ui.composeUiCommons.LocalScaffoldPadding
import com.nxoim.blean.ui.composeUiCommons.LocalScrollVisualFactor
import com.nxoim.blean.ui.composeUiCommons.ProvideScaffoldPadding
import com.nxoim.blean.ui.composeUiCommons.detectFocusByPosition
import com.nxoim.blean.ui.composeUiCommons.modifiers.edgeToEdgeSystemBarsAlphaMaskFade
import com.nxoim.blean.ui.composeUiCommons.modifiers.pullToRefresh
import com.nxoim.blean.ui.composeUiCommons.slideUpOnScroll
import com.nxoim.blean.ui.feedViewing.AnimateScrollToTopOnRefreshEffect
import com.nxoim.blean.ui.feedViewing.feedHorizontalContentPadding
import com.nxoim.blean.ui.feedViewing.feedWholeContentPadding
import com.nxoim.blean.ui.feedViewing.localScaffoldPaddingPlusRefreshPullAndMore
import com.nxoim.blean.ui.feedViewing.rememberPullToRefreshStateForM3Indicator
import com.nxoim.blean.ui.postUi.InteractionButtonActions
import com.nxoim.blean.ui.screens.content.thread.componnents.ReplyButtonDivider
import com.nxoim.blean.ui.screens.content.thread.componnents.ThreadAppBar
import com.nxoim.blean.ui.screens.content.thread.componnents.ThreadPost
import com.nxoim.evolpagink.compose.itemsIndexed
import com.nxoim.evolpagink.compose.toState

@Composable
fun ThreadScreen(
    model: ThreadModel,
    mediaViewerSharedTransitionScope: CombinedSharedTransitionScope?,
    onRequestToMaximizeMedia: (
        // TODO media queue provider with pagination
        mediaQueue: List<PostMediaContent>,
        focusOn: PostMediaContent
    ) -> Unit,
    onMediaSharedElementKeyRequest: (of: PostMediaContent) -> String
) {
    val rootScaffoldPadding = LocalScaffoldPadding.current

    val interactionButtonActions = remember {
        InteractionButtonActions(
            onLikeClicked = { model.toggleLike(it) },
            onCommentClicked = {
                model.navigation.openPost(it)
            }
        )
    }

    val refreshState by model.refreshState.collectAsStateWithLifecycle()
    val pullToRefreshState = rememberPullToRefreshStateForM3Indicator(
        refreshState,
        onRefresh = { model.refresh() }
    )

    Scaffold(
        topBar = {
            ThreadAppBar(
                model.navigation,
                Modifier.slideUpOnScroll(LocalScrollVisualFactor.current)
            )
        }
    ) {
        Box {
            ProvideScaffoldPadding(it) {
                val lazyListState = rememberLazyListState()
                val pageableState = model.pageable.toState(
                    lazyListState,
                    key = {
                        when (it) {
                            is PostContainer.Available<PostType.Thread> -> it.value.threadDepth.toString() + it.uri.toString()
                            else -> it.uri.toString()
                        }
                    }
                )
                val isLoadingPrevious by model.pageable.isFetchingPrevious.collectAsStateWithLifecycle()
                val isLoadingNext by model.pageable.isFetchingNext.collectAsStateWithLifecycle()

                AnimateScrollToTopOnRefreshEffect(refreshState, lazyListState)

                LazyColumn(
                    state = lazyListState,
                    contentPadding = localScaffoldPaddingPlusRefreshPullAndMore(pullToRefreshState) + rootScaffoldPadding,
                    modifier = Modifier
                        .pullToRefresh(pullToRefreshState)
                        .edgeToEdgeSystemBarsAlphaMaskFade(),
//                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (pageableState.items.isEmpty()) {
                        val rootPost = model.post.value
                        if (rootPost != null) item(key = rootPost.uri.toString()) {
                            val postContainer by model.post.collectAsStateWithLifecycle()
                            var isItemInFocus by remember { mutableStateOf(false) }


                            postContainer?.let { postContainer ->
                                CompositionLocalProvider(
                                    LocalIsInFocus provides isItemInFocus
                                ) {
                                    Layout(
                                        Modifier.detectFocusByPosition { isItemInFocus = it }
                                    ) {
                                        ThreadPost(
                                            postContainer = postContainer,
                                            mediaViewerSharedTransitionScope,
                                            onRequestToMaximizeMedia = {
                                                if (postContainer is PostContainer.Available<*>) {
                                                    // will be paginated in the future
                                                    val allMedia = postContainer.value.content.media
                                                        ?: error("where did the media come from then")
                                                    onRequestToMaximizeMedia(
                                                        allMedia,
                                                        it
                                                    )
                                                }
                                            },
                                            onMediaSharedElementKeyRequest,
                                            interactionButtonActions,
                                            isMain = true
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        itemsIndexed(pageableState) { index, item ->
                            var isItemInFocus by remember { mutableStateOf(false) }
                            val isFirst = index == 0
                            val isLast = pageableState.items.lastOrNull() == item
                            val isMain = item.uri == model.postUri
                            val canReply = (item as? PostContainer.Available<PostType.Thread>)
                                ?.value
                                ?.replyPermission
                                ?.canUserReply
                                ?: false

                            Column(
                                Modifier
                                    .animateItem()
                                    .detectFocusByPosition { isItemInFocus = it }
                            ) {
                                ListLoadingIndicator(isFirst && isLoadingPrevious)

                                CompositionLocalProvider(LocalIsInFocus provides isItemInFocus) {
                                    ThreadPost(
                                        postContainer = item,
                                        mediaViewerSharedTransitionScope,
                                        onRequestToMaximizeMedia = {
                                            if (item is PostContainer.Available<*>) {
                                                // will be paginated in the future
                                                val allMedia = item.value.content.media
                                                    ?: error("where did the media come from then")
                                                onRequestToMaximizeMedia(
                                                    allMedia,
                                                    it
                                                )
                                            }
                                        },
                                        onMediaSharedElementKeyRequest,
                                        interactionButtonActions,
                                        onPostClicked = {
                                            model.navigation.openPost(it)
                                        },
                                        contentPadding = feedHorizontalContentPadding,
                                        isMain = isMain
                                    )
                                }

                                ListLoadingIndicator(isLoadingNext && isLast)

                                if (isMain && canReply) {
                                    ReplyButtonDivider(Modifier.padding(feedHorizontalContentPadding))
                                } else {
                                    Crossfade(!isLast) { displayDIvider ->
                                        if (displayDIvider) HorizontalDivider(
                                            Modifier.padding(feedWholeContentPadding)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                VerticalListRefreshIndication(pullToRefreshState)
            }
        }
    }
}