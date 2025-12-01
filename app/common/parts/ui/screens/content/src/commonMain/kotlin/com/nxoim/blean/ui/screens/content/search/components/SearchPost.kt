package com.nxoim.blean.ui.screens.content.search.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nxoim.blean.postRelatedCommons.models.PostContainer
import com.nxoim.blean.postRelatedCommons.models.PostMediaContent
import com.nxoim.blean.postRelatedCommons.models.PostType
import com.nxoim.blean.ui.composeUiCommons.CombinedSharedTransitionScope
import com.nxoim.blean.ui.postUi.BasePost
import com.nxoim.blean.ui.postUi.InteractionButtonActions
import com.nxoim.blean.ui.postUi.PostLayoutStyle

@Composable
fun SearchPost(
    postContainer: PostContainer<PostType.Search>,
    mediaViewerSharedTransitionScope: CombinedSharedTransitionScope?,
    onRequestToMaximizeMedia: (focusOn: PostMediaContent) -> Unit,
    onMediaSharedElementKeyRequest: (of: PostMediaContent) -> String,
    interactionButtonActions: InteractionButtonActions,
    modifier: Modifier = Modifier,
    onPostClicked: ((post: PostContainer<PostType>) -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp)
) {
    BasePost(
        postContainer = postContainer,
        interactionButtonActions = interactionButtonActions,
        modifier = modifier,
        onPostClicked = onPostClicked,
        contentPadding = contentPadding,
        mediaViewerSharedTransitionScope = mediaViewerSharedTransitionScope,
        onRequestToMaximizeMedia = onRequestToMaximizeMedia,
        onMediaSharedElementKeyRequest = onMediaSharedElementKeyRequest,
        layoutStyle = PostLayoutStyle.Main,
        showReplyLine = false,
        isMainPostInThread = false
    )
}