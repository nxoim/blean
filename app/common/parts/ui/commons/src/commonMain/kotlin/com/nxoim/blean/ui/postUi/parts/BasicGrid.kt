package com.nxoim.blean.ui.postUi.parts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun <T> BasicGrid(
    items: List<T>,
    modifier: Modifier = Modifier,
    columns: Int = 2,
    gutter: Dp = 0.dp,
    content: @Composable (Int, T) -> Unit
) {
    Column(
        modifier,
        verticalArrangement = Arrangement.spacedBy(gutter)
    ) {
        items.chunked(columns).forEachIndexed { rowIndex, rowItems ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(gutter)
            ) {
                rowItems.forEachIndexed { itemIndex, item ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                    ) {
                        content(rowIndex * columns + itemIndex, item)
                    }
                }
            }
        }
    }
}