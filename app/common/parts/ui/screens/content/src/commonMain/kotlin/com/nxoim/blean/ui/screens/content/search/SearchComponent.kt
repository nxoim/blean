package com.nxoim.blean.ui.screens.content.search

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastRoundToInt
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nxoim.blean.postRelatedCommons.models.PostContainer
import com.nxoim.blean.postRelatedCommons.models.PostMediaContent
import com.nxoim.blean.ui.composeMaterial3Extensions.ListLoadingIndicator
import com.nxoim.blean.ui.composeMaterial3Extensions.scaleInWithFade
import com.nxoim.blean.ui.composeMaterial3Extensions.scaleOutWithFade
import com.nxoim.blean.ui.composeUiCommons.CombinedSharedTransitionScope
import com.nxoim.blean.ui.composeUiCommons.LocalIsInFocus
import com.nxoim.blean.ui.composeUiCommons.LocalScaffoldPadding
import com.nxoim.blean.ui.composeUiCommons.LocalScrollVisualFactor
import com.nxoim.blean.ui.composeUiCommons.ProvideScaffoldPadding
import com.nxoim.blean.ui.composeUiCommons.detectFocusByPosition
import com.nxoim.blean.ui.composeUiCommons.modifiers.edgeToEdgeSystemBarsAlphaMaskFade
import com.nxoim.blean.ui.feedViewing.feedHorizontalContentPadding
import com.nxoim.blean.ui.feedViewing.feedWholeContentPadding
import com.nxoim.blean.ui.postUi.InteractionButtonActions
import com.nxoim.blean.ui.screens.content.search.components.SearchPost
import com.nxoim.evolpagink.compose.itemsIndexed
import com.nxoim.evolpagink.compose.toState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchComponent(
    model: SearchModel,
    mediaViewerSharedTransitionScope: CombinedSharedTransitionScope?,
    onRequestToMaximizeMedia: (
        // TODO media queue provider with pagination
        mediaQueue: List<PostMediaContent>,
        focusOn: PostMediaContent
    ) -> Unit,
    onMediaSharedElementKeyRequest: (of: PostMediaContent) -> String
) {
    val interactionButtonActions = remember {
        InteractionButtonActions(
            onLikeClicked = { /* TODO */ },
            onCommentClicked = {
                model.navigation.openPost(it)
            }
        )
    }

    Scaffold(
        bottomBar = {
            val rootScaffoldPadding = LocalScaffoldPadding.current
            val scrollVisualFactor = LocalScrollVisualFactor.current
            val navBarInsets = WindowInsets.navigationBars.asPaddingValues()
            val surroundingPadding = PaddingValues(16.dp)
            // not using the m3 searchbar component because
            // of its fuckass inset padding
            Box(
                Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier
                        .padding(surroundingPadding)
                        .offset {
                            val pixelsFromBottom = (
                                    rootScaffoldPadding.calculateBottomPadding() -
                                            navBarInsets.calculateBottomPadding() +
                                            surroundingPadding.calculateBottomPadding()
                                    ).toPx()

                            IntOffset(
                                0,
                                (pixelsFromBottom * scrollVisualFactor.fraction.value).fastRoundToInt()
                            )
                        }
                        .padding(bottom = LocalScaffoldPadding.current.calculateBottomPadding()),
                ) {
                    SearchBarDefaults.InputField(
                        query = model.query,
                        onQueryChange = model::search,
                        onSearch = model::search,
                        expanded = false,
                        onExpandedChange = { },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = null
                            )
                        },
                        trailingIcon = {
                            AnimatedVisibility(
                                !model.query.isEmpty(),
                                enter = scaleInWithFade(),
                                exit = scaleOutWithFade()
                            ) {
                                IconButton(onClick = { model.search("") }) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = null
                                    )
                                }
                            }
                        },
                        placeholder = {
                            Text("What would you like to search?")
                        }
                    )
                }
            }

        }
    ) { scaffoldPadding ->
        ProvideScaffoldPadding(scaffoldPadding) {
            val lazyListState = rememberLazyListState()
            val pageableState = model.pageable.toState(
                lazyListState,
                key = { it.uri.toString() }
            )
            val isLoadingPrevious by model.pageable.isFetchingPrevious.collectAsStateWithLifecycle()
            val isLoadingNext by model.pageable.isFetchingNext.collectAsStateWithLifecycle()

            Box {
                AnimatedContent(
                    model.searchState,
                    modifier = Modifier.systemBarsPadding()
                ) { searchState ->
                    when (searchState) {
                        SearchState.Done -> {}
                        SearchState.Idle -> {}
                        is SearchState.Failed -> {
                            Text("Failed to search. ${searchState.message}")
                        }

                        SearchState.Loading -> {
                            Box(modifier = Modifier.fillMaxWidth()) {
                                LinearProgressIndicator(Modifier.align(Alignment.Center))
                            }
                        }
                    }
                }
                Crossfade(model.query.isEmpty()) { queryIsEmpty ->
                    if (queryIsEmpty) {
                        SearchHomeContent()
                    } else {
                        LazyColumn(
                            contentPadding = LocalScaffoldPadding.current,
                            state = lazyListState,
                            modifier = Modifier.edgeToEdgeSystemBarsAlphaMaskFade()
                        ) {
                            itemsIndexed(pageableState) { index, item ->
                                var isItemInFocus by remember { mutableStateOf(false) }
                                val isFirst = index == 0
                                val isLast = index == pageableState.items.lastIndex

                                Column(
                                    Modifier
                                        .animateItem()
                                        .detectFocusByPosition { isItemInFocus = it }
                                ) {
                                    ListLoadingIndicator(isFirst && isLoadingPrevious)

                                    CompositionLocalProvider(LocalIsInFocus provides isItemInFocus) {
                                        SearchPost(
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
                                            onPostClicked = {
                                                model.navigation.openPost(it)
                                            },
                                            contentPadding = feedHorizontalContentPadding
                                        )
                                    }

                                    ListLoadingIndicator(isLoadingNext && isLast)

                                    Crossfade(isLast) { last ->
                                        if (!last) HorizontalDivider(
                                            Modifier.padding(feedWholeContentPadding)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SearchHomeContent() {
    Text("TODO suggestions and stuff")
}