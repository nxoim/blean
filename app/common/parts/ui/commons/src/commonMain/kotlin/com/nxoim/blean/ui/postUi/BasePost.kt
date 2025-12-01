package com.nxoim.blean.ui.postUi

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nxoim.blean.composeVideoPlayer.PlayerSettings
import com.nxoim.blean.composeVideoPlayer.PlayerState
import com.nxoim.blean.composeVideoPlayer.rememberVideoPlayer
import com.nxoim.blean.postRelatedCommons.models.ContentPreview
import com.nxoim.blean.postRelatedCommons.models.PostContainer
import com.nxoim.blean.postRelatedCommons.models.PostMediaContent
import com.nxoim.blean.postRelatedCommons.models.PostType
import com.nxoim.blean.postRelatedCommons.models.PostVisibilityWarning
import com.nxoim.blean.postRelatedCommons.models.TextAndFacets
import com.nxoim.blean.postRelatedCommons.models.UserInteractionInfo
import com.nxoim.blean.postRelatedCommons.models.optimisticIsLiked
import com.nxoim.blean.postRelatedCommons.models.optimisticIsReposted
import com.nxoim.blean.postRelatedCommons.models.userInteractionInfo
import com.nxoim.blean.ui.composeMaterial3Extensions.MediaSharedBoundsTransition
import com.nxoim.blean.ui.composeUiCommons.CombinedSharedTransitionScope
import com.nxoim.blean.ui.composeUiCommons.Layout
import com.nxoim.blean.ui.composeUiCommons.LocalIsInFocus
import com.nxoim.blean.ui.composeUiCommons.Spacer
import com.nxoim.blean.ui.postUi.parts.AccountListPreview
import com.nxoim.blean.ui.postUi.parts.AuthorPicture
import com.nxoim.blean.ui.postUi.parts.ContentDetached
import com.nxoim.blean.ui.postUi.parts.ContentFromBlockedAccount
import com.nxoim.blean.ui.postUi.parts.ContentIsMutedByKeyword
import com.nxoim.blean.ui.postUi.parts.ContentNotFound
import com.nxoim.blean.ui.postUi.parts.ContentUnsupported
import com.nxoim.blean.ui.postUi.parts.FeedGeneratorPreview
import com.nxoim.blean.ui.postUi.parts.FunnyAdaptiveGrid
import com.nxoim.blean.ui.postUi.parts.ImageMedia
import com.nxoim.blean.ui.postUi.parts.InteractionButtons
import com.nxoim.blean.ui.postUi.parts.LabelerPreview
import com.nxoim.blean.ui.postUi.parts.Labels
import com.nxoim.blean.ui.postUi.parts.LinkPreview
import com.nxoim.blean.ui.postUi.parts.PostDate
import com.nxoim.blean.ui.postUi.parts.ProfileDetails
import com.nxoim.blean.ui.postUi.parts.QuoteAndMediaAndLinkPreviewOutline
import com.nxoim.blean.ui.postUi.parts.ReplyLineThing
import com.nxoim.blean.ui.postUi.parts.ReplyPermissionIndicator
import com.nxoim.blean.ui.postUi.parts.StarterPackPreview
import com.nxoim.blean.ui.postUi.parts.UnsupportedMedia
import com.nxoim.blean.ui.postUi.parts.VideoMedia
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun BasePost(
    postContainer: PostContainer<PostType>,
    interactionButtonActions: InteractionButtonActions,
    modifier: Modifier = Modifier,
    onPostClicked: ((post: PostContainer<PostType>) -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    mediaViewerSharedTransitionScope: CombinedSharedTransitionScope? = null,
    onRequestToMaximizeMedia: ((focusOn: PostMediaContent) -> Unit)? = null,
    onMediaSharedElementKeyRequest: ((of: PostMediaContent) -> String)? = null,
    layoutStyle: PostLayoutStyle = PostLayoutStyle.Main,
    showReplyLine: Boolean = false,
    isMainPostInThread: Boolean = false
) {
    Layout(modifier) {
        when (postContainer) {
            is PostContainer.Unavailable.Unsupported -> QuoteAndMediaAndLinkPreviewOutline(
                shape = RoundedCornerShape(16.dp)
            ) {
                ContentUnsupported()
            }

            is PostContainer.Unavailable.Blocked -> QuoteAndMediaAndLinkPreviewOutline(
                shape = RoundedCornerShape(16.dp)
            ) {
                ContentFromBlockedAccount(postContainer.blockReason)
            }

            is PostContainer.Unavailable.Detached -> QuoteAndMediaAndLinkPreviewOutline(
                shape = RoundedCornerShape(16.dp)
            ) {
                ContentDetached()
            }

            is PostContainer.Unavailable.NotFound -> QuoteAndMediaAndLinkPreviewOutline(
                shape = RoundedCornerShape(16.dp)
            ) {
                ContentNotFound()
            }

            is PostContainer.Available -> PostLayout(
                postContainer,
                onPostClicked,
                layoutStyle,
                isMainPostInThread,
                showReplyLine,
                mediaViewerSharedTransitionScope,
                onMediaSharedElementKeyRequest,
                onRequestToMaximizeMedia,
                interactionButtonActions,
                contentPadding
            )
        }
    }
}

@Composable
private fun PostLayout(
    postContainer: PostContainer.Available<PostType>,
    onPostClicked: ((PostContainer<PostType>) -> Unit)?,
    layoutStyle: PostLayoutStyle,
    isMainPostInThread: Boolean,
    showReplyLine: Boolean,
    mediaViewerSharedTransitionScope: CombinedSharedTransitionScope?,
    onMediaSharedElementKeyRequest: ((PostMediaContent) -> String)?,
    onRequestToMaximizeMedia: ((PostMediaContent) -> Unit)?,
    interactionButtonActions: InteractionButtonActions,
    contentPadding: PaddingValues,
) {
    val post = postContainer.value
    val userInteractionInfo = post.userInteractionInfo

    val canHide = post.visibilityWarnings.any { it is PostVisibilityWarning.Muted }
    var hide by remember { mutableStateOf(canHide) }

    val isBookmarked = false

    AnimatedContent(hide) { hide ->
        if (hide) {
            ContentIsMutedByKeyword() // todo not only muted by keyword
        } else {
            VisibleContent(
                onPostClicked,
                postContainer,
                layoutStyle,
                post,
                isMainPostInThread,
                showReplyLine,
                mediaViewerSharedTransitionScope,
                onMediaSharedElementKeyRequest,
                onRequestToMaximizeMedia,
                userInteractionInfo,
                interactionButtonActions,
                isBookmarked,
                contentPadding
            )
        }
    }
}

@Composable
private fun VisibleContent(
    onPostClicked: ((PostContainer<PostType>) -> Unit)?,
    postContainer: PostContainer.Available<PostType>,
    layoutStyle: PostLayoutStyle,
    post: PostType,
    isMainPostInThread: Boolean,
    showReplyLine: Boolean,
    mediaViewerSharedTransitionScope: CombinedSharedTransitionScope?,
    onMediaSharedElementKeyRequest: ((PostMediaContent) -> String)?,
    onRequestToMaximizeMedia: ((PostMediaContent) -> Unit)?,
    userInteractionInfo: UserInteractionInfo?,
    interactionButtonActions: InteractionButtonActions,
    isBookmarked: Boolean,
    contentPadding: PaddingValues
) {
    val containerModifier = Modifier
        .let {
            if (onPostClicked == null)
                it
            else
                it.clickable { onPostClicked(postContainer) }
        }
        .padding(contentPadding)
        .height(IntrinsicSize.Max)

    if (layoutStyle == PostLayoutStyle.ReferredTo) {
        Row(
            containerModifier,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ProfilePictureAndReplyLine(post, showReplyLine)

            AuthorDetailsAndAllOfTheContent(
                displayProfilePicture = false,
                post = post,
                isMainPostInThread = isMainPostInThread,
                mediaViewerSharedTransitionScope = mediaViewerSharedTransitionScope,
                onMediaSharedElementKeyRequest = onMediaSharedElementKeyRequest,
                onRequestToMaximizeMedia = onRequestToMaximizeMedia,
                userInteractionInfo = userInteractionInfo,
                interactionButtonActions = interactionButtonActions,
                onPostClicked = onPostClicked,
                isBookmarked = isBookmarked
            )
        }
    } else {
        AuthorDetailsAndAllOfTheContent(
            displayProfilePicture = true,
            post = post,
            isMainPostInThread = isMainPostInThread,
            mediaViewerSharedTransitionScope = mediaViewerSharedTransitionScope,
            onMediaSharedElementKeyRequest = onMediaSharedElementKeyRequest,
            onRequestToMaximizeMedia = onRequestToMaximizeMedia,
            userInteractionInfo = userInteractionInfo,
            interactionButtonActions = interactionButtonActions,
            isBookmarked = isBookmarked,
            onPostClicked = onPostClicked,
            modifier = containerModifier
        )
    }
}

@Composable
private fun AuthorDetailsAndAllOfTheContent(
    displayProfilePicture: Boolean,
    post: PostType,
    isMainPostInThread: Boolean,
    mediaViewerSharedTransitionScope: CombinedSharedTransitionScope?,
    onMediaSharedElementKeyRequest: ((PostMediaContent) -> String)?,
    onRequestToMaximizeMedia: ((PostMediaContent) -> Unit)?,
    userInteractionInfo: UserInteractionInfo?,
    interactionButtonActions: InteractionButtonActions,
    isBookmarked: Boolean,
    onPostClicked: ((PostContainer<PostType>) -> Unit)?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (displayProfilePicture) {
                    AuthorPicture(post.content.author)
                }
                ProfileDetails(oneLineProfileDetails = !displayProfilePicture, post)
            }

            if (!isMainPostInThread) {
                @OptIn(ExperimentalTime::class)
                PostDate(post.content.creationDate)
            }
        }

        // idk gotta look into this
        if (!post.content.labels.isNullOrEmpty()) {
            Labels(post.content)
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (!post.content.text?.text.isNullOrEmpty()) {
                TextContent(post.content.text!!)
            }

            if (!post.content.media.isNullOrEmpty()) {
                val isFocused = LocalIsInFocus.current ?: false

                MediaPreview(
                    post.content.media,
                    mediaViewerSharedTransitionScope,
                    onMediaSharedElementKeyRequest,
                    post,
                    onRequestToMaximizeMedia,
                    isFocused
                )
            }
        }

        val contentPadding = 12.dp
        val radius = 24.dp
        val contentPaddingValues = PaddingValues(contentPadding)
        val containerShape = RoundedCornerShape(radius)
        val contentShape = RoundedCornerShape(radius - contentPadding)

        post.content.quote?.let { quote ->
            QuoteAndMediaAndLinkPreviewOutline(containerShape) {
                QuotedPost(
                    postContainer = quote,
                    contentPadding = contentPaddingValues,
                    modifier = Modifier.let {
                        if (onPostClicked == null)
                            it
                        else
                            it.clickable { onPostClicked(quote) }
                    }
                )
            }
        }

        post.content.contentPreview?.let {
            QuoteAndMediaAndLinkPreviewOutline(containerShape) {
                SpecialContentPreview(
                    it,
                    contentPaddingValues,
                    contentShape
                )
            }
        }

        if (isMainPostInThread) DateAndReplyPermission(post)

        if (userInteractionInfo != null) InteractionButtons(
            isLiked = userInteractionInfo.likeStatus.optimisticIsLiked,
            onLikeClick = { interactionButtonActions.onLikeClicked(post) },
            onReplyClick = { interactionButtonActions.onCommentClicked(PostContainer.Available(post)) },
            isReposted = userInteractionInfo.repostStatus.optimisticIsReposted,
            onRepostQuoteClick = { /*TODO*/ },
            onShareClick = { /*TODO*/ },
            isBookmarked = isBookmarked,
            onBookmarkClick = { /*TODO*/ },
            onMoreClick = { /*TODO*/ },
            likes = post.content.likeCount,
            replies = post.content.replyCount,
            repostsQuotes = (post.content.repostCount + post.content.repostCount)
        )
    }
}


@Composable
private fun ProfileDetails(
    oneLineProfileDetails: Boolean,
    post: PostType
) {
    if (oneLineProfileDetails) {
        Row(
            Modifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            ProfileDetails(post.content.author)
        }
    } else {
        Column(
            Modifier,
            verticalArrangement = Arrangement.Center
        ) {
            ProfileDetails(post.content.author)
        }
    }
}

@Composable
private fun ProfilePictureAndReplyLine(
    post: PostType,
    showReplyLine: Boolean
) {
    Column(
        Modifier.fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        post.content.author.let { AuthorPicture(it) }

        if (showReplyLine) ReplyLineThing()
    }
}

@Composable
private fun TextContent(facets: TextAndFacets) {
    Text(
        facets.toAnnotated(),
        style = MaterialTheme.typography.bodyLarge.copy(
            lineHeightStyle = LineHeightStyle(
                LineHeightStyle.Alignment.Proportional,
                LineHeightStyle.Trim.Both
            )
        )
    )
}

@Composable
private fun MediaPreview(
    media: List<PostMediaContent>?,
    mediaViewerSharedTransitionScope: CombinedSharedTransitionScope?,
    onMediaSharedElementKeyRequest: ((PostMediaContent) -> String)?,
    post: PostType,
    onRequestToMaximizeMedia: ((PostMediaContent) -> Unit)?,
    isFocused: Boolean
) {
    FunnyAdaptiveGrid(
        media!!,
        gutter = 8.dp,
    ) { index, mediaContent ->
        val forceImageAspectRatio =
            index == 0 && media!!.size < 4
        val videoAutoplay = true
        val initialVideoPlayerSettings = remember {
            {
                PlayerSettings(
                    volume = 0f,
                    looping = true
                )
            }
        }
        val maxHeight = 340.dp

        val sharedElementModifier = Modifier.let {
            val scope = mediaViewerSharedTransitionScope

            if (scope == null || onMediaSharedElementKeyRequest == null)
                it
            else
                with(scope) {
                    it.sharedBounds(
                        rememberSharedContentState(
                            onMediaSharedElementKeyRequest(
                                mediaContent
                            )
                        ),
                        enter = MediaSharedBoundsTransition.NoOverlay.enter,
                        exit = MediaSharedBoundsTransition.NoOverlay.exit,
                        renderInOverlayDuringTransition = false,
                        resizeMode =
                            if (mediaContent is PostMediaContent.ImageWithCDNLinks)
                                SharedTransitionScope.ResizeMode.RemeasureToBounds
                            else
                                SharedTransitionScope.ResizeMode.ScaleToBounds(
                                    ContentScale.Crop
                                )
                    )
                }
        }

        var showSpoiler by remember {
            mutableStateOf(post.visibilityWarnings.any { it is PostVisibilityWarning.HiddenBySpoiler })
        }

        Box(contentAlignment = Alignment.Center) {
            Media(
                sharedElementModifier,
                showSpoiler,
                mediaContent,
                onRequestToMaximizeMedia,
                forceImageAspectRatio,
                maxHeight,
                initialVideoPlayerSettings,
                isFocused,
                videoAutoplay
            )

            if (showSpoiler) Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Contains ${post.visibilityWarnings}")
                Button(onClick = { showSpoiler = false }) {
                    Text("Show")
                }
            }
        }
    }
}

@Composable
private fun Media(
    sharedElementModifier: Modifier,
    showSpoiler: Boolean,
    mediaContent: PostMediaContent,
    onRequestToMaximizeMedia: ((PostMediaContent) -> Unit)?,
    forceImageAspectRatio: Boolean,
    maxHeight: Dp,
    initialVideoPlayerSettings: () -> PlayerSettings,
    isFocused: Boolean,
    videoAutoplay: Boolean
) {
    Layout(
        Modifier
            .then(sharedElementModifier)
            .clip(RoundedCornerShape(8.dp))
            .blur(if (showSpoiler) 128.dp else 0.dp)
    ) {
        when (mediaContent) {
            is PostMediaContent.ImageWithCDNLinks -> ImageMedia(
                modifier = Modifier.clickable() {
                    onRequestToMaximizeMedia?.invoke(
                        mediaContent
                    )
                },
                forceImageAspectRatio = forceImageAspectRatio,
                mediaContent = mediaContent,
                maxHeight = maxHeight
            )

            is PostMediaContent.Video -> {
                val player = rememberVideoPlayer(
                    key = mediaContent.playlistUri,
                    initialPlayerSettings = initialVideoPlayerSettings
                )

                // initialize video, but not right away
                LaunchedEffect(isFocused) {
                    delay(300)
                    if (isActive && isFocused) {
                        player.loadFromUri(mediaContent.playlistUri)
                    }
                }

                // autoplay
                LaunchedEffect(
                    player.state,
                    isFocused
                ) {
                    val playerState = player.state

                    if (playerState is PlayerState.Initialized) {
                        if (isFocused) {
                            if (videoAutoplay) {
                                playerState.controller.play()
                            }
                        } else {
                            playerState.controller.pause()
                        }
                    }
                }

                VideoMedia(
                    modifier = Modifier.clickable() {
                        onRequestToMaximizeMedia?.invoke(
                            mediaContent
                        )
                    },
                    forceImageAspectRatio = forceImageAspectRatio,
                    player = player,
                    mediaContent = mediaContent,
                    maxHeight = maxHeight,
                )
            }

            PostMediaContent.Unsupported -> UnsupportedMedia(
                modifier = Modifier
            )
        }
    }
}

@Composable
private fun DateAndReplyPermission(post: PostType, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val thread = post as? PostType.Thread
        val replyPermission = thread?.replyPermission
        if (replyPermission == null)
            Spacer(0.dp)
        else
            ReplyPermissionIndicator(replyPermission)

        @OptIn(ExperimentalTime::class)
        PostDate(
            post.content.creationDate,
            Modifier,
            full = true
        )
    }
}

@Composable
private fun SpecialContentPreview(
    contentPreview: ContentPreview,
    contentPaddingValues: PaddingValues,
    contentShape: RoundedCornerShape
) {
    when (contentPreview) {
        is ContentPreview.FeedGenerator -> FeedGeneratorPreview(
            contentPreview,
            contentPadding = contentPaddingValues,
            picturePreviewShape = contentShape
        )

        is ContentPreview.Labeler -> LabelerPreview(
            contentPreview,
            contentPadding = contentPaddingValues,
            picturePreviewShape = contentShape
        )

        is ContentPreview.Link -> LinkPreview(
            contentPreview,
            contentPadding = contentPaddingValues,
            picturePreviewShape = contentShape
        )

        is ContentPreview.List -> AccountListPreview(
            contentPreview,
            contentPadding = contentPaddingValues,
            picturePreviewShape = contentShape
        )

        is ContentPreview.StarterPack -> StarterPackPreview(
            contentPreview,
            contentPadding = contentPaddingValues,
        )

//                                        is ContentPreview.Unavailable.NotFound -> ContentNotFound()
        is ContentPreview.Unsupported -> ContentUnsupported()
    }
}