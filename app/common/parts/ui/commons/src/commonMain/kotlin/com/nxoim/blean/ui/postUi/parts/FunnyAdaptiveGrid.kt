package com.nxoim.blean.ui.postUi.parts

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun <T> FunnyAdaptiveGrid(
    items: List<T>,
    modifier: Modifier = Modifier,
    gutter: Dp = 0.dp,
    content: @Composable (Int, T) -> Unit
) = when (items.size) {
    1 -> Box(modifier) { content(0, items[0]) }

    2, 3 -> {
        val mainItemWeight = if (items.size == 2) 0.5f else 0.7f

        ItemFocusGrid(
            items,
            modifier,
            gutter = gutter,
            mainItemMaxWeight = mainItemWeight,
            content = content
        )
    }

    else -> BasicGrid(
        items,
        modifier,
        columns = 2,
        gutter,
        content
    )
}