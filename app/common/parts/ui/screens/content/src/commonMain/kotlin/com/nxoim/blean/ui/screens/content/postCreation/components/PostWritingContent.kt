package com.nxoim.blean.ui.screens.content.postCreation.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.nxoim.blean.ui.composeUiCommons.CombineSharedTransitionAndAnimatedVisibility
import com.nxoim.blean.ui.composeUiCommons.LocalFocusRequestingIsAllowed
import com.nxoim.blean.ui.composeUiCommons.Spacer
import com.nxoim.blean.ui.composeUiCommons.coilImageRequest
import com.nxoim.blean.ui.composeUiCommons.isSoftwareKeyboardOpen
import com.nxoim.blean.ui.screens.content.postCreation.PostWritingModel
import com.nxoim.blean.ui.screens.content.postCreation.WritingSession


@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun PostWritingContent(
    modifier: Modifier,
    component: PostWritingModel,
    textFieldFocusRequester: FocusRequester
) {
    val focusRequestingIsAllowed = LocalFocusRequestingIsAllowed.current
    val drafts by component.tenLastDrafts.collectAsState()
    val keyboardPadding = WindowInsets.ime.asPaddingValues().calculateBottomPadding()
    val navBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val keyboardController = LocalSoftwareKeyboardController.current
    val materialTypography = MaterialTheme.typography
    val keyboardIsOpen by isSoftwareKeyboardOpen()

    AnimatedContent(component.currentWritingSession) { writingSession ->
        val displayDrafts = writingSession.text.isEmpty() && drafts != null && writingSession.mediaUris.isEmpty()

        if (writingSession == component.currentWritingSession && focusRequestingIsAllowed) LaunchedEffect(Unit) {
            textFieldFocusRequester.requestFocus()
            keyboardController?.show()
        }

        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            WritingSection(
                Modifier.padding(horizontal = 16.dp),
                draftsExist = drafts?.isNotEmpty() == true,
                writingSession = writingSession,
                textFieldFocusRequester = textFieldFocusRequester
            )

            MediaPreview(writingSession, component)

            // way more performant to offset the item than remeasure the
            // entire component on padding changes, and its simple enough to do here
            Column(
                Modifier.graphicsLayer {
                    translationY = -maxOf(keyboardPadding, navBarPadding).toPx()
                },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AnimatedVisibility(displayDrafts) {
                    if (drafts != null) DraftsSection(
                        drafts = drafts!!,
                        onRequestMediaUri = component::getImageUri,
                        onRequestDraftRestoration = component::restoreDraft
                    )
                }

                Spacer(16.dp)

//                PostContentOptions(
//                    Modifier.weight(1f, false),
//                    component = component,
//                    onResultReceived = {
//                        if (focusRequestingIsAllowed) {
//                            textFieldFocusRequester.requestFocus()
//                            keyboardController?.show()
//                        }
//                    }
//                )

//                Spacer(16.dp)

//                PostSettings()
            }
        }
    }
}

@Composable
private fun MediaPreview(
    writingSession: WritingSession,
    component: PostWritingModel
) {
    SharedTransitionLayout {
        AnimatedContent(writingSession.mediaUris) { mediaList ->
            CombineSharedTransitionAndAnimatedVisibility(this) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(mediaList, key = { it.key }) { media ->
                        AsyncImage(
                            model = component.getImageUri(media.key)?.let { coilImageRequest(it) },
                            contentDescription = null,
                            placeholder = rememberVectorPainter(Icons.Outlined.Close),
                            modifier = Modifier
                                .sharedBounds(rememberSharedContentState(media.key))
                                .height(160.dp)
                                .clip(RoundedCornerShape(4.dp))
                        )
                    }
                }
            }
        }
    }
}