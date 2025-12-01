package com.nxoim.blean.ui.postUi

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nxoim.blean.postRelatedCommons.models.PostContainer
import com.nxoim.blean.postRelatedCommons.models.PostMediaContent
import com.nxoim.blean.postRelatedCommons.models.PostType
import com.nxoim.blean.ui.composeUiCommons.CombinedSharedTransitionScope
import com.nxoim.blean.ui.postUi.parts.ContentDetached
import com.nxoim.blean.ui.postUi.parts.ContentFromBlockedAccount
import com.nxoim.blean.ui.postUi.parts.ContentNotFound
import com.nxoim.blean.ui.postUi.parts.ContentUnsupported

@Composable
fun FeedPost(
    postContainer: PostContainer<PostType.Feed>,
    mediaViewerSharedTransitionScope: CombinedSharedTransitionScope?,
    onRequestToMaximizeMedia: (focusOn: PostMediaContent) -> Unit,
    onMediaSharedElementKeyRequest: (of: PostMediaContent) -> String,
    interactionButtonActions: InteractionButtonActions,
    modifier: Modifier = Modifier,
    onPostClicked: ((post: PostContainer<PostType>) -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp)
) {
    Column(modifier) {
        when (postContainer) {
            is PostContainer.Unavailable.Unsupported -> ContentUnsupported()
            is PostContainer.Unavailable.Blocked -> ContentFromBlockedAccount(postContainer.blockReason)
            is PostContainer.Unavailable.Detached -> ContentDetached()
            is PostContainer.Unavailable.NotFound -> ContentNotFound()

            is PostContainer.Available -> {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    postContainer.value.replyRoot?.let { replyRoot ->
                        if (replyRoot.uri != postContainer.value.replyTarget?.uri) {
                            BasePost(
                                postContainer = replyRoot,
                                interactionButtonActions = interactionButtonActions,
                                modifier = modifier,
                                onPostClicked = onPostClicked,
                                contentPadding = contentPadding,
                                layoutStyle = PostLayoutStyle.ReferredTo,
                                showReplyLine = true
                            )
                        }
                    }

                    postContainer.value.replyTarget?.let { replyTarget ->
                        BasePost(
                            postContainer = replyTarget,
                            interactionButtonActions = interactionButtonActions,
                            modifier = modifier,
                            onPostClicked = onPostClicked,
                            contentPadding = contentPadding,
                            layoutStyle = PostLayoutStyle.ReferredTo,
                            showReplyLine = true
                        )
                    }

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
            }
        }
    }
}
