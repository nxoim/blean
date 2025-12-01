package com.nxoim.blean.ui.screens.content.postCreation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.nxoim.blean.ui.composeUiCommons.LocalFocusRequestingIsAllowed
import com.nxoim.blean.ui.composeUiCommons.ScrollVisualFactorRootImpl
import com.nxoim.blean.ui.screens.content.postCreation.components.NewPostButton
import com.nxoim.blean.ui.screens.content.postCreation.components.PostWritingScaffold

@Composable
fun PostWritingOverlay(
    currentModel: PostWritingModel?,
    onOpen: () -> Unit,
    onHide: () -> Unit,
    newPostButtonVisible: Boolean,
    scrollVisualFactorRoot: ScrollVisualFactorRootImpl,
    latestScaffoldPadding: PaddingValues
) {
    SharedTransitionLayout {
        val sharedContentState = rememberSharedContentState(0)

        AnimatedContent(
            currentModel,
            transitionSpec = {
                (fadeIn() + scaleIn()) togetherWith (fadeOut() + scaleOut())
            }
        ) { model ->
            if (model == null) {
                NewPostButton(
                    scrollVisualFactorRoot = scrollVisualFactorRoot,
                    visible = newPostButtonVisible,
                    onClick = onOpen,
                    latestScaffoldPadding = latestScaffoldPadding,
                    sharedContentState = sharedContentState
                )
            } else {
                val isOpen = currentModel != null
                CompositionLocalProvider(
                    LocalFocusRequestingIsAllowed provides isOpen
                ) {
                    PostWritingScaffold(
                        model,
                        sharedContentState,
                        onHide = onHide
                    )
                }
            }
        }
    }
}