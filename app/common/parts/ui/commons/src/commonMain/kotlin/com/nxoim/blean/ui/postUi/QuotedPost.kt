package com.nxoim.blean.ui.postUi

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.dp
import com.nxoim.blean.postRelatedCommons.models.ContentPreview
import com.nxoim.blean.postRelatedCommons.models.PostContainer
import com.nxoim.blean.postRelatedCommons.models.PostType
import com.nxoim.blean.postRelatedCommons.models.PostVisibilityWarning
import com.nxoim.blean.ui.postUi.parts.AccountListPreview
import com.nxoim.blean.ui.postUi.parts.ContentDetached
import com.nxoim.blean.ui.postUi.parts.ContentFromBlockedAccount
import com.nxoim.blean.ui.postUi.parts.ContentIsMutedByKeyword
import com.nxoim.blean.ui.postUi.parts.ContentNotFound
import com.nxoim.blean.ui.postUi.parts.ContentUnsupported
import com.nxoim.blean.ui.postUi.parts.FeedGeneratorPreview
import com.nxoim.blean.ui.postUi.parts.FunnyAdaptiveGrid
import com.nxoim.blean.ui.postUi.parts.LabelerPreview
import com.nxoim.blean.ui.postUi.parts.Labels
import com.nxoim.blean.ui.postUi.parts.LinkPreview
import com.nxoim.blean.ui.postUi.parts.Media
import com.nxoim.blean.ui.postUi.parts.ProfileDetails
import com.nxoim.blean.ui.postUi.parts.QuoteAndMediaAndLinkPreviewOutline
import com.nxoim.blean.ui.postUi.parts.StarterPackPreview

@Composable
fun QuotedPost(
    modifier: Modifier = Modifier,
    postContainer: PostContainer<PostType.Quote>,
    contentPadding: PaddingValues,
) {
    Column(modifier) {
        when (postContainer) {
            is PostContainer.Unavailable.Unsupported -> ContentUnsupported()
            is PostContainer.Unavailable.Blocked -> ContentFromBlockedAccount(postContainer.blockReason)
            is PostContainer.Unavailable.Detached -> ContentDetached()
            is PostContainer.Unavailable.NotFound -> ContentNotFound()


            is PostContainer.Available -> {
                val content = postContainer.value.content
                val postVisibilityWarnings = postContainer.value.visibilityWarnings
                val canHide = postVisibilityWarnings.any { it is PostVisibilityWarning.Muted }
                var hide by rememberSaveable() { mutableStateOf(canHide) }

                AnimatedContent(hide) { hide ->
                    if (hide) {
                        ContentIsMutedByKeyword() // todo not only muted by keyword
                    } else {
                        Column(
                            modifier = Modifier.padding(contentPadding),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(contentPadding.calculateTopPadding() * 0.75f)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.FormatQuote,
                                        contentDescription = null,
                                        Modifier.size(16.dp)
                                    )

                                    ProfileDetails(
                                        content.author,
                                        displayNameStyle = MaterialTheme.typography.labelMedium,
                                        handleStyle = MaterialTheme.typography.labelMedium,
                                        displayNameColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        handleColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                HorizontalDivider()
                            }


                            if (!content.labels.isNullOrEmpty()) {
                                Labels(content)
                            }

                            Column(
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                if (!content.text?.text.isNullOrEmpty()) content.text?.let {
                                    Text(
                                        it.toAnnotated(),
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            lineHeightStyle = LineHeightStyle(
                                                LineHeightStyle.Alignment.Proportional,
                                                LineHeightStyle.Trim.Both
                                            )
                                        )
                                    )
                                }

                                if (!content.media.isNullOrEmpty()) {
                                    FunnyAdaptiveGrid(
                                        content.media!!,
                                        gutter = 4.dp
                                    ) { index, item ->
                                        var showSpoiler by rememberSaveable() {
                                            mutableStateOf(postVisibilityWarnings.any { it is PostVisibilityWarning.HiddenBySpoiler })
                                        }

                                        Box(contentAlignment = Alignment.Center) {
                                            Media(
                                                mediaContent = item,
                                                Modifier
                                                    .blur(if (showSpoiler) 128.dp else 0.dp)
                                                    .clip(RoundedCornerShape(8.dp)),
                                                forceImageAspectRatio = index == 0 && content.media!!.size < 4,
                                                maxHeight = 64.dp,
                                                videoAutoplay = false
                                            )

                                            if (showSpoiler) Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text("Contains $postVisibilityWarnings")
                                                Button(onClick = { showSpoiler = false }) {
                                                    Text("Show")
                                                }
                                            }
                                        }
                                    }
                                }

                                if (content.contentPreview != null) QuoteAndMediaAndLinkPreviewOutline(
                                    RoundedCornerShape(8.dp)
                                ) {
                                    when (val contentPreview = content.contentPreview!!) {
                                        is ContentPreview.FeedGenerator -> FeedGeneratorPreview(
                                            contentPreview,
                                            contentPadding = PaddingValues(8.dp),
                                            picturePreviewShape = RoundedCornerShape(4.dp)
                                        )

                                        is ContentPreview.Labeler -> LabelerPreview(
                                            contentPreview,
                                            contentPadding = PaddingValues(8.dp),
                                            picturePreviewShape = RoundedCornerShape(4.dp)
                                        )

                                        is ContentPreview.Link ->
                                            LinkPreview(
                                                contentPreview,
                                                contentPadding = PaddingValues(8.dp),
                                                picturePreviewShape = RoundedCornerShape(4.dp)
                                            )

                                        is ContentPreview.List -> AccountListPreview(
                                            contentPreview,
                                            contentPadding = PaddingValues(8.dp),
                                            picturePreviewShape = RoundedCornerShape(4.dp)
                                        )

                                        is ContentPreview.StarterPack -> StarterPackPreview(
                                            contentPreview,
                                            contentPadding = PaddingValues(8.dp),
//                                            picturePreviewShape = RoundedCornerShape(4.dp)
                                        )
//                                is ContentPreview.Unavailable.NotFound -> ContentNotFound()
                                        is ContentPreview.Unsupported -> ContentUnsupported()
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