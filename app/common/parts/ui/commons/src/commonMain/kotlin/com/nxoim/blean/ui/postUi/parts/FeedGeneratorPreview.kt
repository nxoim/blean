package com.nxoim.blean.ui.postUi.parts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.nxoim.blean.postRelatedCommons.models.ContentPreview
import com.nxoim.blean.ui.composeUiCommons.coilImageRequest

@Composable
fun FeedGeneratorPreview(
    contentPreview: ContentPreview.FeedGenerator,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues,
    picturePreviewShape: Shape,
) {
    Row(
        modifier.padding(contentPadding).fillMaxWidth(),
        verticalAlignment = CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            Modifier.weight(1f),
            horizontalArrangement = spacedBy(8.dp),
            verticalAlignment = CenterVertically
        ) {
            Box(
                Modifier
                    .size(48.dp)
                    .clip(picturePreviewShape)
                    .background(MaterialTheme.colorScheme.surfaceContainer),
                contentAlignment = Alignment.Center
            ) {
                if (contentPreview.avatarUrl == null) {
                    Icon(
                        Icons.Filled.Menu,
                        contentDescription = null
                    )
                } else {
                    AsyncImage(
                        coilImageRequest(contentPreview.avatarUrl!!),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Column {
                Text(
                    "Feed by @${contentPreview.creator.handle}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary,
                )

                Text(
                    contentPreview.title,
                    style = MaterialTheme.typography.headlineSmall,
                )
            }

        }

        Icon(
            Icons.AutoMirrored.Outlined.ArrowForward,
            contentDescription = null,
            Modifier.padding(end = 8.dp)
        )
    }
}