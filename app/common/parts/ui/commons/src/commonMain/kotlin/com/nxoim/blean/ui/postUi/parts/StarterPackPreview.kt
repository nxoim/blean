package com.nxoim.blean.ui.postUi.parts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PeopleOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nxoim.blean.postRelatedCommons.models.ContentPreview
import com.nxoim.blean.ui.postUi.toAnnotated

@Composable
fun StarterPackPreview(
    contentPreview: ContentPreview.StarterPack,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Outlined.PeopleOutline,
                contentDescription = null,
            )

            Column {
                Text(
                    contentPreview.title ?: "Starter pack",
                    style = MaterialTheme.typography.labelLarge
                )

                Text(
                    "Starter pack by ${contentPreview.creator.handle}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }

        contentPreview.description?.let {
            Text(
                it.toAnnotated(),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}