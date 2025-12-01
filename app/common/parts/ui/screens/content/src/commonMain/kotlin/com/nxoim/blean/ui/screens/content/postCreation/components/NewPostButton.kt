package com.nxoim.blean.ui.screens.content.postCreation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nxoim.blean.ui.composeMaterial3Extensions.ExpressiveBoundsTransform
import com.nxoim.blean.ui.composeMaterial3Extensions.scaleInWithFade
import com.nxoim.blean.ui.composeMaterial3Extensions.scaleOutWithFade
import com.nxoim.blean.ui.composeUiCommons.ScrollVisualFactorRoot
import com.nxoim.blean.ui.composeUiCommons.Spacer

@Composable
context(animatedVisibilityScope: AnimatedVisibilityScope, transitionScope: SharedTransitionScope)
fun NewPostButton(
    scrollVisualFactorRoot: ScrollVisualFactorRoot,
    visible: Boolean,
    onClick: () -> Unit,
    latestScaffoldPadding: PaddingValues,
    sharedContentState: SharedTransitionScope.SharedContentState
) {
    with(transitionScope) {
        AnimatedVisibility(
            visible && scrollVisualFactorRoot.fraction.value <= 0.1f,
            enter = scaleInWithFade(),
            exit = scaleOutWithFade()
        ) {
            ExtendedFloatingActionButton(
                onClick = onClick,
                content = {
                    Icon(
                        Icons.Default.Create,
                        contentDescription = null
                    )

                    Spacer(8.dp)

                    Text("New Post", modifier = Modifier.skipToLookaheadSize())
                },
                modifier = Modifier
                    .padding(16.dp)
                    .padding(latestScaffoldPadding)
                    .sharedBounds(
                        sharedContentState,
                        animatedVisibilityScope,
                        resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds,
                        boundsTransform = ExpressiveBoundsTransform
                    )
            )
        }
    }
}