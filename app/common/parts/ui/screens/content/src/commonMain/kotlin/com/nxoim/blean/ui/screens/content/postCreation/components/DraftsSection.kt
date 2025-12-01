package com.nxoim.blean.ui.screens.content.postCreation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBox
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nxoim.blean.ui.composeUiCommons.Spacer
import com.nxoim.blean.ui.screens.content.postCreation.Draft
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
@Composable
fun DraftsSection(
    modifier: Modifier = Modifier,
    drafts: List<Draft>,
    onRequestMediaUri: (key: String) -> String?,
    onRequestDraftRestoration: (id: String) -> Unit
) {
    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.End) {
        AnimatedVisibility(drafts.isNotEmpty()) {
            TextButton(
                onClick = { },
                modifier = Modifier.padding(horizontal = 4.dp)
            ) {
                Icon(
                    Icons.Outlined.AccountBox,
                    null
                )

                Text("See all drafts")
            }
        }

        Spacer(8.dp)

        val lazyListState = rememberLazyListState()
        LaunchedEffect(drafts.firstOrNull()) {
            lazyListState.animateScrollToItem(0)
        }

        LazyRow(
            state = lazyListState,
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
        ) {
            items(drafts, key = { it.id }) { item ->
                DraftItem(
                    Modifier.height(128.dp).clickable { onRequestDraftRestoration(item.id) },
                    label = {
                        Text(
                            item.creationDate.toString(),
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    text = {
                        Text(
                            item.text,
                            maxLines = 3,
                            minLines = 3
                        )
                    },
                    media = {
                        DraftMedia(
                            media = item.media,
                            onRequestMediaUri = onRequestMediaUri
                        )
                    }
                )
            }
        }
    }
}