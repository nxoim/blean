package com.nxoim.blean.ui.screens.content.thread.componnents

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
import com.nxoim.blean.ui.postUi.parts.ContentDetached
import com.nxoim.blean.ui.postUi.parts.ContentFromBlockedAccount
import com.nxoim.blean.ui.postUi.parts.ContentNotFound
import com.nxoim.blean.ui.postUi.parts.ContentUnsupported

@Composable
fun ThreadPost(
    postContainer: PostContainer<PostType>,
    mediaViewerSharedTransitionScope: CombinedSharedTransitionScope?,
    onRequestToMaximizeMedia: (focusOn: PostMediaContent) -> Unit,
    onMediaSharedElementKeyRequest: (of: PostMediaContent) -> String,
    interactionButtonActions: InteractionButtonActions,
    modifier: Modifier = Modifier,
    onPostClicked: ((post: PostContainer<PostType>) -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp),
    isMain: Boolean = false
) {
    when (postContainer) {
        is PostContainer.Unavailable.Unsupported -> ContentUnsupported()
        is PostContainer.Unavailable.Blocked -> ContentFromBlockedAccount(postContainer.blockReason)
        is PostContainer.Unavailable.Detached -> ContentDetached()
        is PostContainer.Unavailable.NotFound -> ContentNotFound()

        is PostContainer.Available -> {
            val post = postContainer.value

            if (post is PostType.Thread && post.threadDepth > 1) {
                BasePost(
                    postContainer = postContainer,
                    interactionButtonActions = interactionButtonActions,
                    modifier = modifier,
                    onPostClicked = onPostClicked,
                    contentPadding = contentPadding,
                    layoutStyle = PostLayoutStyle.ReferredTo,
                    showReplyLine = true
                )
            } else {
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
                    isMainPostInThread = isMain
                )
            }
        }
    }
}
