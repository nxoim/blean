package com.nxoim.blean.ui.composeUiCommons

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.UiComposable
import androidx.compose.ui.layout.Layout

/**
 * Places the content with a [Modifier] without creation of extra objects
 */
@UiComposable
@Composable
inline fun Layout(
    modifier: Modifier,
    content: @Composable @UiComposable () -> Unit
) = Layout(
    modifier = modifier,
    content = content
) { measurables, constraints ->
    val placeable = measurables[0].measure(constraints)

    layout(placeable.width, placeable.height) {
        placeable.placeRelative(0, 0)
    }
}