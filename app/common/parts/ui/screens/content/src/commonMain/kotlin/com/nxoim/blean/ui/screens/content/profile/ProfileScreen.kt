package com.nxoim.blean.ui.screens.content.profile

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.nxoim.blean.postRelatedCommons.models.PostContainer
import com.nxoim.blean.postRelatedCommons.models.PostMediaContent
import com.nxoim.blean.ui.composeMaterial3Extensions.ListLoadingIndicator
import com.nxoim.blean.ui.composeMaterial3Extensions.VerticalListRefreshIndication
import com.nxoim.blean.ui.composeUiCommons.CombinedSharedTransitionScope
import com.nxoim.blean.ui.composeUiCommons.Layout
import com.nxoim.blean.ui.composeUiCommons.LocalIsInFocus
import com.nxoim.blean.ui.composeUiCommons.detectFocusByPosition
import com.nxoim.blean.ui.composeUiCommons.modifiers.edgeToEdgeSystemBarsAlphaMaskFade
import com.nxoim.blean.ui.composeUiCommons.modifiers.pullToRefresh
import com.nxoim.blean.ui.feedViewing.AnimateScrollToTopOnRefreshEffect
import com.nxoim.blean.ui.feedViewing.feedHorizontalContentPadding
import com.nxoim.blean.ui.feedViewing.feedWholeContentPadding
import com.nxoim.blean.ui.feedViewing.localScaffoldPaddingPlusRefreshPullAndMore
import com.nxoim.blean.ui.feedViewing.rememberPullToRefreshStateForM3Indicator
import com.nxoim.blean.ui.postUi.FeedPost
import com.nxoim.blean.ui.postUi.InteractionButtonActions
import com.nxoim.evolpagink.compose.itemsIndexed
import com.nxoim.evolpagink.compose.toState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ProfileScreen(
    model: ProfileModel,
    mediaViewerSharedTransitionScope: CombinedSharedTransitionScope?,
    onRequestToMaximizeMedia: (
        // TODO media queue provider with pagination
        mediaQueue: List<PostMediaContent>,
        focusOn: PostMediaContent
    ) -> Unit,
    onMediaSharedElementKeyRequest: (of: PostMediaContent) -> String
) {
    // will have tabs
    val isPageInFocus = LocalIsInFocus.current ?: true
    val lazyListState = rememberLazyListState()
    val pageableState = model.pageable.toState(
        lazyListState,
        key = { it.uri.toString() }
    )

    val isLoadingPrevious by model.pageable.isFetchingPrevious.collectAsState()
    val isLoadingNext by model.pageable.isFetchingNext.collectAsState()

    val refreshState by model.refreshState.collectAsState()

    val pullToRefreshState = rememberPullToRefreshStateForM3Indicator(
        refreshState,
        onRefresh = { model.refresh() }
    )

    AnimateScrollToTopOnRefreshEffect(refreshState, lazyListState)

    Box {
        val interactionButtonActions = remember {
            InteractionButtonActions(
                onLikeClicked = {
                    model.toggleLike(targetPost = it)
                },
                onCommentClicked = {
                    model.navigation.openPost(it)
                }
            )
        }

        LazyColumn(
            contentPadding = localScaffoldPaddingPlusRefreshPullAndMore(pullToRefreshState),
            state = lazyListState,
            modifier = Modifier.pullToRefresh(pullToRefreshState).edgeToEdgeSystemBarsAlphaMaskFade()
        ) {
            itemsIndexed(pageableState) { index, item ->
                var isItemInFocus by remember { mutableStateOf(false) }
                val isFirst = index == 0
                val isLast = index == pageableState.items.lastIndex

                // if page is not in focus then no item is in focus
                val focusToProvide = isPageInFocus && isItemInFocus

                ListLoadingIndicator(isFirst && isLoadingPrevious)

                CompositionLocalProvider(LocalIsInFocus provides focusToProvide) {
                    Layout(
                        Modifier.detectFocusByPosition { isItemInFocus = it }
                    ) {
                        FeedPost(
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
                            interactionButtonActions = interactionButtonActions,
                            Modifier
                                .animateItem(),
                            onPostClicked = {
                                model.navigation.openPost(it)
                            },
                            contentPadding = feedHorizontalContentPadding
                        )
                    }
                }

                ListLoadingIndicator(isLoadingNext && isLast)

                Crossfade(isLast) {
                    HorizontalDivider(Modifier.padding(feedWholeContentPadding))
                }
            }
        }

        VerticalListRefreshIndication(pullToRefreshState)
    }
}