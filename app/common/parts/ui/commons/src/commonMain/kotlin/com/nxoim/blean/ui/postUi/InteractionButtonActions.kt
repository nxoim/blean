package com.nxoim.blean.ui.postUi

import com.nxoim.blean.postRelatedCommons.models.PostContainer
import com.nxoim.blean.postRelatedCommons.models.PostType

/**
 * Just a set of callbacks
 */
class InteractionButtonActions(
    val onLikeClicked: (PostType) -> Unit,
    val onCommentClicked: (PostContainer<PostType>) -> Unit,
)