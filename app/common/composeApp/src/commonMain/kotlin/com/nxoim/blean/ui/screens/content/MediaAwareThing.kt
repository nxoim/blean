package com.nxoim.blean.ui.screens.content

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import com.nxoim.blean.ui.composeMaterial3Extensions.MediaSharedBoundsTransition
import com.nxoim.blean.ui.composeUiCommons.CombinedSharedTransitionScope
import com.nxoim.blean.ui.fullscreenMedia.FullScreenMedia
import com.nxoim.blean.ui.fullscreenMedia.FullScreenMediaViewer

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun MediaAwareThing(
    mediaAwareThingState: MediaAwareThingState,
    onMediaSharedElementKeyRequest: (of: FullScreenMedia) -> String,
    content: @Composable CombinedSharedTransitionScope.() -> Unit
) {
    val saveableStateHolder = rememberSaveableStateHolder()

    SharedTransitionLayout {
        AnimatedContent(
            mediaAwareThingState.state,
            transitionSpec = MediaSharedBoundsTransition.containerTransitionSpec
        ) { state ->
            val mediaTransitionScope =
                remember { CombinedSharedTransitionScope(this@SharedTransitionLayout, this) }

            saveableStateHolder.SaveableStateProvider(state.toString()) {
                when (state) {
                    MediaAwareThingState.State.None -> content(mediaTransitionScope)

                    is MediaAwareThingState.State.Selected -> {
                        Surface(color = MaterialTheme.colorScheme.surfaceContainerLowest) {
                            FullScreenMediaViewer(
                                mediaContent = state.all,
                                firstSelected = state.firstToView,
                                onDismissed = mediaAwareThingState.handler::dismiss,
                                mediaSharedElementTransitionScope = mediaTransitionScope,
                                onMediaSharedElementKeyRequest = onMediaSharedElementKeyRequest,
                                // we want fancy fade animations between different ui layers,
                                // therefore were making the ui itself unaffected
                                // by the animation of AnimatedContent via overlaying,
                                // but let the animation state get through for shared elements
                                modifier = Modifier.renderInSharedTransitionScopeOverlay()
                            )
                        }
                    }
                }
            }
        }
    }
}

interface MediaAwareThingState {
    val state: State
    val handler: MediaSelectionHandler

    sealed interface State {
        data object None : State
        class Selected(
            val firstToView: FullScreenMedia,
            val all: List<FullScreenMedia>
        ) : State
    }

}

interface MediaSelectionHandler {
    fun selectMedia(allMedia: List<FullScreenMedia>, focusOn: FullScreenMedia)
    fun dismiss()
}