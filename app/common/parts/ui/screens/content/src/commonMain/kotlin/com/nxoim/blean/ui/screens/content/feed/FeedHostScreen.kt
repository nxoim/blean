@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.nxoim.blean.ui.screens.content.feed

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.overscroll
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberOverscrollEffect
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nxoim.blean.postRelatedCommons.models.PostContainer
import com.nxoim.blean.postRelatedCommons.models.PostMediaContent
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
import com.nxoim.blean.ui.postUi.FeedPost
import com.nxoim.blean.ui.postUi.InteractionButtonActions
import com.nxoim.blean.ui.screens.content.feed.models.FeedType
import com.nxoim.evolpagink.compose.itemsIndexed
import com.nxoim.evolpagink.compose.toState
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedHostScreen(
    model: FeedHostModel,
    mediaViewerSharedTransitionScope: CombinedSharedTransitionScope?,
    onRequestToMaximizeMedia: (
        // TODO media queue provider with pagination
        mediaQueue: List<PostMediaContent>,
        focusOn: PostMediaContent
    ) -> Unit,
    onMediaSharedElementKeyRequest: (of: PostMediaContent) -> String
) {
    val rootScaffoldPadding = LocalScaffoldPadding.current
    val feeds by model.feedsModel.feeds.collectAsState()

    if (feeds.isNotEmpty()) Box {
        val scrollVisualFactor = LocalScrollVisualFactor.current
        var selectedFeedIndex by rememberSaveable { mutableIntStateOf(0) }
        val pagerState = rememberPagerState(0 /* TODO restore from storage */) { feeds.size }

        LaunchedEffect(Unit) {
            launch {
                snapshotFlow { pagerState.targetPage }.collectLatest {
                    scrollVisualFactor.reset()
                    selectedFeedIndex = it
                }
            }

            launch {
                snapshotFlow { selectedFeedIndex }.collectLatest {
                    pagerState.animateScrollToPage(it)
                }
            }
        }

        val overscrollEffect = rememberOverscrollEffect()
        Scaffold(
            topBar = {
                PrimaryScrollableTabRow(
                    selectedFeedIndex,
                    edgePadding = 16.dp,
                    modifier = Modifier
                        .slideUpOnScroll(scrollVisualFactor)
                        .background(MaterialTheme.colorScheme.surface)
                        .statusBarsPadding()
                ) {
                    feeds.forEachIndexed { index, feed ->
                        Tab(
                            selected = selectedFeedIndex == index,
                            unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            text = {
                                Text(feed.name)
                            },
                            onClick = {
                                selectedFeedIndex = index
                            }
                        )
                    }
                }
            }
        ) { scaffoldPadding ->
            ProvideScaffoldPadding(scaffoldPadding + rootScaffoldPadding) {
                HorizontalPager(
                    pagerState,
                    key = { index -> feeds!![index].uri },
                    overscrollEffect = overscrollEffect,
                    modifier = Modifier.overscroll(overscrollEffect)
                ) { index ->
                    CompositionLocalProvider(
                        LocalIsInFocus provides (index == pagerState.currentPage)
                    ) {
                        FeedScreen(
                            model.getOrCreateFeedModel(feeds[index]),
                            mediaViewerSharedTransitionScope,
                            onRequestToMaximizeMedia,
                            onMediaSharedElementKeyRequest,
                            Modifier.fillMaxSize(), // for touch detection
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FeedScreen(
    model: FeedPostsModel,
    mediaViewerSharedTransitionScope: CombinedSharedTransitionScope?,
    onRequestToMaximizeMedia: (
        mediaQueue: List<PostMediaContent>,
        focusOn: PostMediaContent
    ) -> Unit,
    onMediaSharedElementKeyRequest: (of: PostMediaContent) -> String,
    modifier: Modifier = Modifier
) {
    val isPageInFocus = LocalIsInFocus.current ?: true
    val feedInfo by model.feedInfo.collectAsState()
    val interactionButtonActions = remember {
        InteractionButtonActions(
            onLikeClicked = {
                model.toggleLike(it)
            },
            onCommentClicked = {
                model.navigation.openPost(it)
            }
        )
    }

    Column(modifier) {
        when (feedInfo.type) {
            FeedType.Unsupported -> Text("Unsupported feed type")
            FeedType.Video -> Text("Bikbok")
            FeedType.NormalPosts -> {
                val lazyListState = rememberLazyListState()
                val pageableState = model.pageable.toState(
                    lazyListState,
                    key = { it.uri.toString() }
                )
                val isLoadingPrevious by model.pageable.isFetchingPrevious.collectAsStateWithLifecycle()
                val isLoadingNext by model.pageable.isFetchingNext.collectAsStateWithLifecycle()

                val refreshState by model.refreshState.collectAsState()

                val pullToRefreshState = rememberPullToRefreshStateForM3Indicator(
                    refreshState,
                    onRefresh = { model.refresh() }
                )

                AnimateScrollToTopOnRefreshEffect(refreshState, lazyListState)

                Box() {
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
                                HorizontalDivider(
                                    Modifier.padding(feedWholeContentPadding)
                                )
                            }
                        }
                    }

                    VerticalListRefreshIndication(pullToRefreshState)
                }
            }
        }
    }
}