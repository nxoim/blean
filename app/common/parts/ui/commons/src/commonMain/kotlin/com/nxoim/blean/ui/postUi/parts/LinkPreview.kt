package com.nxoim.blean.ui.postUi.parts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.nxoim.blean.postRelatedCommons.models.ContentPreview
import com.nxoim.blean.ui.composeUiCommons.coilImageRequest

@Composable
fun LinkPreview(
    externalContentPreview: ContentPreview.Link,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    picturePreviewShape: Shape = RectangleShape
) {
    val containerColor = MaterialTheme.colorScheme.surface
    val containsContent = (externalContentPreview.title != null && externalContentPreview.title!!.isNotEmpty())
            || (externalContentPreview.description != null && externalContentPreview.description!!.isNotEmpty())

    Surface(modifier.fillMaxWidth(), color = containerColor) {
        Column(
            Modifier.padding(contentPadding),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(contentPadding.calculateTopPadding() * 0.75f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Outlined.Link,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        externalContentPreview.url,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                if (containsContent) {
                    HorizontalDivider(Modifier)
                }
            }

            // make sure the composable is not empty because
            // otherwise the Arrangement will give an unnecessary spacing here
            if (containsContent) Column(
                verticalArrangement = spacedBy(4.dp)
            ) {
                externalContentPreview.title?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.titleMedium,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 2
                    )
                }

                Row(
                    horizontalArrangement = spacedBy(8.dp)
                ) {
                    if (externalContentPreview.thumbnailCdnUrl != null) AsyncImage(
                        coilImageRequest(externalContentPreview.thumbnailCdnUrl!!),
                        contentDescription = null,
                        Modifier.widthIn(min = 80.dp, max = 120.dp).heightIn(max = 80.dp).clip(picturePreviewShape)
                    )

                    externalContentPreview.description?.let {
                         Text(
                            it,
                            style = MaterialTheme.typography.bodySmall,
                            overflow = TextOverflow.Ellipsis,
                            maxLines = 4
                        )
                    }
                }
            }
        }
    }
}