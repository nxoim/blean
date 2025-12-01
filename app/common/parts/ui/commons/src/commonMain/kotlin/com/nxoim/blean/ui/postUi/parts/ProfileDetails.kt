package com.nxoim.blean.ui.postUi.parts

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import com.nxoim.blean.postRelatedCommons.ProfilePreviewData

@Composable
fun ProfileDetails(
    authorProfilePreview: ProfilePreviewData,
    modifier: Modifier = Modifier,
    displayNameStyle: TextStyle = MaterialTheme.typography.labelLarge,
    displayNameColor: Color = MaterialTheme.colorScheme.onBackground,
    handleStyle: TextStyle = MaterialTheme.typography.labelLarge,
    handleColor: Color = MaterialTheme.colorScheme.outline,
) {
    val displayNameIsEmpty = authorProfilePreview.displayName
        .let { it == null || it == "" }

    Text(
        if (displayNameIsEmpty) {
            "@" + authorProfilePreview.handle
        } else {
            authorProfilePreview.displayName!!
        },
        modifier = modifier,
        style = displayNameStyle,
        color = if (displayNameIsEmpty) handleColor else displayNameColor,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )

    if (!displayNameIsEmpty) {
        Text(
            "@" + authorProfilePreview.handle,
            modifier = modifier,
            style = handleStyle,
            color = handleColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}