package com.nxoim.blean.ui.postUi.parts

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.nxoim.blean.postRelatedCommons.ProfilePreviewData
import com.nxoim.blean.ui.composeUiCommons.coilImageRequest

@Composable
fun AuthorPicture(authorProfilePreview: ProfilePreviewData) {
    if (authorProfilePreview.avatarUrl != null) AsyncImage(
        coilImageRequest(authorProfilePreview.avatarUrl!!),
        contentDescription = "Avatar",
        modifier = Modifier.size(authorThingHeightDp.dp).clip(CircleShape)
    )
}

const val authorThingHeightDp = 36