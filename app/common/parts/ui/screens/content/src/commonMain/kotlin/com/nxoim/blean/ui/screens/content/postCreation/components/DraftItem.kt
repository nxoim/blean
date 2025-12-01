package com.nxoim.blean.ui.screens.content.postCreation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.nxoim.blean.ui.composeUiCommons.Spacer
import com.nxoim.blean.ui.composeUiCommons.coilImageRequest
import com.nxoim.blean.ui.screens.content.postCreation.LocalDraftMedia
import org.jetbrains.compose.ui.tooling.preview.Preview

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DraftItem(
    modifier: Modifier = Modifier,
    label: @Composable () -> Unit,
    text: @Composable () -> Unit,
    media: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)

    Column(
        Modifier
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
            .clip(shape)
            .then(modifier)
            .padding(16.dp)
            .width(240.dp)
    ) {
        CompositionLocalProvider(
            LocalTextStyle provides MaterialTheme.typography.labelSmall,
            LocalContentColor provides MaterialTheme.colorScheme.outline,
        ) {
            Box(Modifier.fillMaxWidth()) {
                label()
            }
        }

        Spacer(4.dp)

        Row(
            horizontalArrangement = Arrangement.End,
//            verticalAlignment = Alignment.CenterVertically
        ) {
            CompositionLocalProvider(
                LocalTextStyle provides MaterialTheme.typography.bodyMedium,
                LocalContentColor provides MaterialTheme.colorScheme.onSurface,
            ) {
                Box(Modifier.weight(1f, false)) {
                    text()
                }
            }

            Spacer(8.dp)

            Box(Modifier.size(60.dp)) {
                media()
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DraftMedia(
    modifier: Modifier = Modifier,
    media: Iterable<LocalDraftMedia>,
    onRequestMediaUri: (key: String) -> String?
) {
    val gutterPadding = 4.dp
    val mediaCount = media.count()
    val displayedMediaCount = if (mediaCount > 4) 3 else mediaCount

    LazyVerticalGrid(
        modifier = modifier,
        columns = GridCells.Fixed(if (media.count() > 1) 2 else 1),
        horizontalArrangement = Arrangement.spacedBy(gutterPadding),
        verticalArrangement = Arrangement.spacedBy(gutterPadding),
        userScrollEnabled = false
    ) {
        items(media.take(displayedMediaCount)) { item ->
            AsyncImage(
                onRequestMediaUri(item.key)?.let { coilImageRequest(it) },
                null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                    .let {
                        if (media.count() > 2)
                            it.fillMaxSize().aspectRatio(1f)
                        else
                            it.size(999.dp)
                    }
            )
        }

        if (mediaCount > 4) item {
            Box(
                Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .aspectRatio(1f)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "+${mediaCount - 3}"
                )
            }
        }
    }
}

@Preview
@Composable
fun DraftItemPreview() {
    DraftItem(
        label = {
            Text("created 12 minutes ago")
        },
        text = {
            Text(
                "dbufbufdbufbufdbufbuf db ufbufdbufbu fdb ufbufdbufb ufdbufbuf dbufbufdbufb ufd bufbufd bu fbufdb ufbufdbufb uf dbufb ufdbufbuf",
                minLines = 3,
                maxLines = 3
            )
        },
        media = {
            DraftMediaPreview()
        }
    )
}

@Preview
@Composable
fun DraftMediaPreview() {
    val media = listOf(
        LocalDraftMedia.Image(""),
        LocalDraftMedia.Image(""),
        LocalDraftMedia.Image(""),
        LocalDraftMedia.Image(""),
        LocalDraftMedia.Image("")
    )

    DraftMedia(
        modifier = Modifier.size(60.dp),
        media = media,
        onRequestMediaUri = { null }
    )
}