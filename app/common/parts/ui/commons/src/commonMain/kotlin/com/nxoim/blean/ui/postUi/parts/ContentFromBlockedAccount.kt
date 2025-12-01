package com.nxoim.blean.ui.postUi.parts

import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nxoim.blean.postRelatedCommons.models.BlockReason

@Composable
fun ContentFromBlockedAccount(
    reason: BlockReason
) = ContentUnavailabilityContainer {
    Row(
        horizontalArrangement = spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
            shape = CircleShape
        ) {
            Icon(
                Icons.Default.PersonOff,
                contentDescription = null,
                modifier = Modifier.padding(6.dp)
            )
        }

        Text(
            when (reason) {
                is BlockReason.AuthorByTheUserFromModList -> {
                    if (reason.title != null)
                        """This post's author was blocked by moderation list "${reason.title}""""
                    else
                        "This post's autjor was blocked by a moderation list"
                }
                BlockReason.AuthorByTheUserManual ->
                    "This post's author was blocked by you"
                BlockReason.Unknown -> "This post's author was blocked"
                BlockReason.UserByTheAuthor -> "This post's author has blocked you"
            }
        )
    }
}
